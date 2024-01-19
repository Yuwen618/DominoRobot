#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <fcntl.h>
#include <linux/i2c-dev.h>
#include <android/log.h>


#define I2C_DEVICE  "/dev/i2c-1"
#define PCA9685_ADDR 0x40

#define PCA9685_MODE1 0x00
#define PCA9685_PRESCALE 0xFE

#define SERVO_MIN_PULSE_WIDTH 500    // 舵机脉冲宽度最小值 (0.5ms)
#define SERVO_MAX_PULSE_WIDTH 2500   // 舵机脉冲宽度最大值 (2.5ms)

#define PCA9685_LED0_ON_L  0x06
#define PCA9685_LED0_ON_H  0x07
#define PCA9685_LED0_OFF_L 0x08
#define PCA9685_LED0_OFF_H 0x09


#define SERVO_FREQUENCY 50

#define MIN_DELAY 10
#define MAX_DELAY 30


int i2cFd = -1;

void init(void);
void printf_bely(char* str);
int getDelay(int oldOff, int currentOff, int targetOff);

void start()
{
    int status = 0;
    i2cFd = -1;

    i2cFd = open(I2C_DEVICE, O_RDWR);
    if (i2cFd < 0) {
        printf_bely("ERROR: I2C device open failed\n");
        return;
    }

    status = ioctl(i2cFd, I2C_SLAVE, PCA9685_ADDR);
    if (status < 0) {
        printf_bely("ERROR: I2C device ioctl error\n");
        close(i2cFd);
        return;
    }


    init();
}

void stop() {
    close(i2cFd);
}

void sleep_ms(unsigned int milliseconds) {
    usleep(milliseconds * 1000);
}

void I2C_write8(unsigned char reg, unsigned char val)
{
    unsigned char wbuf[2];
    wbuf[0] = reg;
    wbuf[1] = val;
    write(i2cFd, wbuf, 2);
}

int I2C_read(unsigned char reg)
{
    unsigned char buffer[2];
    buffer[0] = reg;

    // Write the register address to PCA9685
    if (write(i2cFd, buffer, 1) != 1) {
        printf_bely("Failed to write to the I2C bus.");
        return -1;
    }

    // Read 2 bytes from PCA9685
    if (read(i2cFd, buffer, 2) != 2) {
        printf_bely("Failed to read from the I2C bus.");
        return -1;
    }

    // Combine the two bytes to form a 16-bit value
    return (buffer[1] << 8) | buffer[0];
}


void init(void) {

    I2C_write8(PCA9685_MODE1, 0x00);

    I2C_write8(PCA9685_MODE1, (1 << 4) | (1 << 5));

    I2C_write8(PCA9685_PRESCALE, 121);

    I2C_write8(PCA9685_MODE1, (1 << 5));

}

float calculate_angle(int offValue) {
    // 计算 PWM 周期的时间（微秒）
    float period_us = 1.0 / SERVO_FREQUENCY * 1000000;

    // 计算每个计数器单位对应的微秒数
    float unit_us = period_us / 4096;

    // 计算 OFF 时间对应的脉冲宽度
    float pulse_width = (float)offValue / 4096 * period_us;

    // 计算对应的角度
    float angle = (pulse_width - SERVO_MIN_PULSE_WIDTH) / (SERVO_MAX_PULSE_WIDTH - SERVO_MIN_PULSE_WIDTH) * 180.0;

    return angle;
}

int readAngle(int channel) {
    int offValue = I2C_read(PCA9685_LED0_OFF_L + (4 * channel));

    int angle = calculate_angle(offValue);
    __android_log_print(ANDROID_LOG_INFO, "bely-", "readAngle , channel=%d,angel=%d,offValue=%d", channel, angle, offValue);
    return angle;
}


void writeValue(int channel, int offvalue) {
    int off_l = offvalue & 0xFF;
    int off_h = ((offvalue >> 8));
    I2C_write8(PCA9685_LED0_ON_L + 4 * channel, 0);
    I2C_write8(PCA9685_LED0_ON_H + 4 * channel, 0);
    I2C_write8(PCA9685_LED0_OFF_L + 4 * channel, off_l);
    I2C_write8(PCA9685_LED0_OFF_H + 4 * channel, off_h);
}

void set_pwm(int channel, int angle) {


    if (angle < 0) angle = 0;
    if (angle > 180) angle = 180;

    int pulse_range = SERVO_MAX_PULSE_WIDTH - SERVO_MIN_PULSE_WIDTH;

    // 计算角度对应的脉冲宽度
    int pulse_width = SERVO_MIN_PULSE_WIDTH + (int)((float)angle / 180.0 * pulse_range);

    // 计算对应的 ON 和 OFF 值
    int onValue = 0;  // 脉冲开始的时间，这里设置为0，可以根据需要调整
    int offValue = (int)((float)pulse_width / 1000000.0 * SERVO_FREQUENCY * 4096.0);  // 脉冲结束的时间

//    __android_log_print(ANDROID_LOG_INFO, "bely", "set_pwm , channel=%d,angel=%d,offValue=%d", channel, angle, offValue);


    int offValue_old = I2C_read(PCA9685_LED0_OFF_L + (4 * channel));

    if (offValue_old > 600) {
        offValue_old = 600;
    }

    __android_log_print(ANDROID_LOG_INFO, "bely", "set_pwm , channel=%d,angel=%d,offValue=%d, oldOffValue=%d", channel, angle, onValue, offValue, offValue_old);

    if (offValue > offValue_old) {
        int val = offValue_old + 1;
        while (val <= offValue) {
            writeValue(channel, val++);
            int delay = getDelay(offValue_old, val, offValue);
            __android_log_print(ANDROID_LOG_INFO, "bely","write %d, delay %d", val, delay);
            sleep_ms(delay); //sleep 5ms
        }
    } else {
        int val = offValue_old - 1;
        while (val >= offValue) {
            writeValue(channel, val--);
            int delay = getDelay(offValue_old, val, offValue);
            __android_log_print(ANDROID_LOG_INFO, "bely","write %d, delay %d", val, delay);
            sleep_ms(delay); //sleep 5ms
        }
    }
    __android_log_print(ANDROID_LOG_INFO, "bely", "set_pwm done");
}

int getDelay(int oldOff, int currentOff, int targetOff) {
    int half = abs(targetOff - oldOff) / 2;

    int distance = 0;
    if (targetOff >= oldOff) {
        //from small to big
        distance = abs(targetOff - half - currentOff);
    } else {
        //from big to small
        distance = abs(oldOff - half - currentOff);
    }
    __android_log_print(ANDROID_LOG_INFO, "bely","distance %d, half %d, oldoff=%d, currentOff=%d, targetOff=%d", distance, half, oldOff, currentOff, targetOff);
    double diff = (double)distance/(double)half; // big at two ends, small at middle
    int delay = ((double)diff * (double)(MAX_DELAY - MIN_DELAY)) + MIN_DELAY;

    return delay;
}

void printf_bely(char* str) {
    __android_log_print(ANDROID_LOG_INFO, "bely", "%s", str);
}

