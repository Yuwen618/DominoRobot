package com.bely.dominogpt;


import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Random;


public class MainActivity extends Activity {
    private Process mProcess;
    EditText channel1;
    EditText channel2;
    EditText channel3;
    EditText channel4;
    EditText channel5;
    EditText channel6;
    Handler mHandler;

    Handler mBGHandler;
    // Used to load the 'dominogpt' library on application startup.
    static {
        System.loadLibrary("dominogpt");
    }

    EditText mchannellist [] = new EditText[6];

    int marrowbuttonlist[] = new int[12];

    final int ANGEL_INTERVAL = 10;
    final int CHANNEL_SERVO_1 = 1;
    final int CHANNEL_SERVO_2 = 2;
    final int CHANNEL_SERVO_3 = 3;
    final int CHANNEL_SERVO_4 = 4;
    final int CHANNEL_SERVO_5 = 5;
    final int CHANNEL_SERVO_6 = 6;

    final int MSG_SET_ANGLE_CHANNEL_1 = 1000;
    final int MSG_SET_ANGLE_CHANNEL_2 = 2000;
    final int MSG_SET_ANGLE_CHANNEL_3 = 3000;
    final int MSG_SET_ANGLE_CHANNEL_4 = 4000;
    final int MSG_SET_ANGLE_CHANNEL_5 = 5000;
    final int MSG_SET_ANGLE_CHANNEL_6 = 6000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mHandler = new Handler(mUIHandlerCallback);
        //startRobot();

        try {
            mProcess = Runtime.getRuntime().exec("su");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        channel1  = (EditText) findViewById(R.id.text_channel1);
        channel2 = (EditText) findViewById(R.id.text_channel2);
        channel3= (EditText) findViewById(R.id.text_channel3);
        channel4= (EditText) findViewById(R.id.text_channel4);
        channel5= (EditText) findViewById(R.id.text_channel5);
        channel6= (EditText) findViewById(R.id.text_channel6);

        HandlerThread bgThread = new HandlerThread("bgthread");
        bgThread.start();

        mBGHandler = new Handler(bgThread.getLooper());
    }

    private Handler.Callback mUIHandlerCallback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            int channel = 0;
            int angle = 0;
            switch (msg.what) {
                case MSG_SET_ANGLE_CHANNEL_1:
                    channel = 1;
                    break;
                case MSG_SET_ANGLE_CHANNEL_2:
                    channel = 2;
                    break;
                case MSG_SET_ANGLE_CHANNEL_3:
                    channel = 3;
                    break;
                case MSG_SET_ANGLE_CHANNEL_4:
                    channel = 4;
                    break;
                case MSG_SET_ANGLE_CHANNEL_5:
                    channel = 5;
                    break;
                case MSG_SET_ANGLE_CHANNEL_6:
                    channel = 6;
                    break;
                default:
                    return false;
            }
            angle = msg.arg1;
            changeAngle(channel, angle);
            return true;
        }
    };

    private void scheduleChangeAngle(int channel, int angle) {
        Message msg = Message.obtain();
        msg.arg1 = angle;
        switch(channel) {
            case CHANNEL_SERVO_1:
                msg.what = MSG_SET_ANGLE_CHANNEL_1;
                break;
            case CHANNEL_SERVO_2:
                msg.what = MSG_SET_ANGLE_CHANNEL_2;
                break;
            case CHANNEL_SERVO_3:
                msg.what = MSG_SET_ANGLE_CHANNEL_3;
                break;
            case CHANNEL_SERVO_4:
                msg.what = MSG_SET_ANGLE_CHANNEL_4;
                break;
            case CHANNEL_SERVO_5:
                msg.what = MSG_SET_ANGLE_CHANNEL_5;
                break;
            case CHANNEL_SERVO_6:
                msg.what = MSG_SET_ANGLE_CHANNEL_6;
                break;
            default:
                return;
        }
        mHandler.removeMessages(msg.what);
        mHandler.sendMessageDelayed(msg, 500);
    }

    public void onReduceOffValue(View view) {
        if (!mStarted) {
            Toast.makeText(this, "start first", Toast.LENGTH_SHORT).show();
            return;
        }

        EditText offValue = (EditText) findViewById(R.id.text_off_value);
        if (offValue.getText() == null) return;
        String strOffValue = offValue.getText().toString();
        int oldvalue = 0;
        if (TextUtils.isEmpty(strOffValue)) {
            offValue.setText(String.valueOf(oldvalue));
        } else {
            oldvalue = Integer.valueOf(strOffValue);
            oldvalue -= 2;
            if (oldvalue < 0) oldvalue = 0;
            offValue.setText(String.valueOf(oldvalue));
            testSetOffValue(0, oldvalue);
        }

    }

    public void onIncreaseOffValue(View view) {
        if (!mStarted) {
            Toast.makeText(this, "start first", Toast.LENGTH_SHORT).show();
            return;
        }
        EditText offValue = (EditText) findViewById(R.id.text_off_value);
        if (offValue.getText() == null) return;
        String strOffValue = offValue.getText().toString();
        int oldvalue = 0;
        if (TextUtils.isEmpty(strOffValue)) {
            offValue.setText(String.valueOf(oldvalue));
        } else {
            oldvalue = Integer.valueOf(strOffValue);
            oldvalue += 2;
            if (oldvalue > 600) oldvalue = 600;
            offValue.setText(String.valueOf(oldvalue));
            testSetOffValue(0, oldvalue);
        }
    }

    public void onSetChannel6(View view) {
        setValuePerUI(channel6, CHANNEL_SERVO_6);
    }

    public void onSetChannel5(View view) {
        setValuePerUI(channel5, CHANNEL_SERVO_5);
    }

    public void onSetChannel4(View view) {
        setValuePerUI(channel4, CHANNEL_SERVO_4);
    }

    public void onSetChannel3(View view) {
        setValuePerUI(channel3, CHANNEL_SERVO_3);
    }

    public void onSetChannel2(View view) {
        setValuePerUI(channel2, CHANNEL_SERVO_2);
    }

    private void setValuePerUI(EditText edit, int channel) {
        if (!mStarted) {
            Toast.makeText(MainActivity.this, "Start first", Toast.LENGTH_SHORT).show();
            return;
        }
        Editable e = edit.getText();
        if (e != null && !TextUtils.isEmpty(e.toString())) {
            int angle = Integer.valueOf(e.toString());
            scheduleChangeAngle(channel, angle);
        }
    }

    public void onSetChannel1(View view) {
        setValuePerUI(channel1, CHANNEL_SERVO_1);
    }

    private class ServoTaskRunnable implements Runnable {
        boolean mIsWriteAction = false;
        int mChannel;
        int mValue;

        ServoTaskRunnable(int channel, boolean write, int value) {
            mChannel = channel;
            mIsWriteAction = write;
            mValue = value;
        }

        @Override
        public void run() {
            if (mIsWriteAction) {
                setPWM((mChannel-1)*2, mValue);
            } else { //read
                int angle = getAngle(mChannel);

            }
        }
    }

    private void updateUIWithAngle(final int channel, final int angle) {
        switch(channel) {
            case 1:
                mHandler.post(()-> {channel1.setText(String.valueOf(angle));});
                break;
            case 2:
                mHandler.post(()-> {channel2.setText(String.valueOf(angle));});
                break;
            case 3:
                mHandler.post(()-> {channel3.setText(String.valueOf(angle));});
                break;
            case 4:
                mHandler.post(()-> {channel4.setText(String.valueOf(angle));});
                break;
            case 5:
                mHandler.post(()-> {channel5.setText(String.valueOf(angle));});
                break;
            case 6:
                mHandler.post(()-> {channel6.setText(String.valueOf(angle));});
                break;
        }
    }

    private int getChannelAngle(int channel) {
        return getAngle((channel-1)*2);
    }
    private void readAllAngles() {
        try {

            int finalAngle = getChannelAngle(CHANNEL_SERVO_1);
            mHandler.post(()-> {channel1.setText(String.valueOf(finalAngle));});

            Thread.sleep(20);

            int finalAngle2 = getChannelAngle(CHANNEL_SERVO_2);
            mHandler.post(()-> {channel2.setText(String.valueOf(finalAngle2));});

            Thread.sleep(20);

            int finalAngle3 = getChannelAngle(CHANNEL_SERVO_3);
            mHandler.post(()-> {channel3.setText(String.valueOf(finalAngle3));});

            Thread.sleep(20);

            int finalAngle4 = getChannelAngle(CHANNEL_SERVO_4);
            mHandler.post(()-> {channel4.setText(String.valueOf(finalAngle4));});

            Thread.sleep(20);

            int finalAngle5 = getChannelAngle(CHANNEL_SERVO_5);
            mHandler.post(()-> {channel5.setText(String.valueOf(finalAngle5));});

            Thread.sleep(20);

            int finalAngle6 = getChannelAngle(CHANNEL_SERVO_6);
            mHandler.post(()-> {channel6.setText(String.valueOf(finalAngle6));});
        } catch (InterruptedException e) {

        }
    }

    private void changeAngle(final int channel, final int angle) {
        ServoTaskRunnable r = new ServoTaskRunnable(channel, true, angle);
        mBGHandler.post(r);
    }

    public void onChangeAngle(View view) {

        if (!mStarted) {
            Toast.makeText(MainActivity.this, "Start first", Toast.LENGTH_SHORT).show();
            return;
        }
        int id = view.getId();
        int value=0;
        if (id == R.id.imageButton1 || id == R.id.imageButton2) {
            value = channel1.getText() == null ? 0 : Integer.valueOf(TextUtils.isEmpty(channel1.getText().toString())?"0":channel1.getText().toString());
            value = value + (id == R.id.imageButton1 ? -ANGEL_INTERVAL : ANGEL_INTERVAL);
            if (value < 0) value = 0;
            if (value > 180) value = 180;
            channel1.setText(String.valueOf(value));
            scheduleChangeAngle(1, value);
        } else if (id == R.id.imageButton3 || id == R.id.imageButton4) {
            value = channel2.getText() == null ? 0 : Integer.valueOf(TextUtils.isEmpty(channel2.getText().toString())?"0":channel2.getText().toString());
            value = value + (id == R.id.imageButton3 ? -ANGEL_INTERVAL : ANGEL_INTERVAL);
            if (value < 0) value = 0;
            if (value > 180) value = 180;
            channel2.setText(String.valueOf(value));
            scheduleChangeAngle(2, value);
        }  else if (id == R.id.imageButton5 || id == R.id.imageButton6) {
            value = channel3.getText() == null ? 0 : Integer.valueOf(TextUtils.isEmpty(channel3.getText().toString())?"0":channel3.getText().toString());
            value = value + (id == R.id.imageButton5 ? -ANGEL_INTERVAL : ANGEL_INTERVAL);
            if (value < 0) value = 0;
            if (value > 180) value = 180;
            channel3.setText(String.valueOf(value));
            scheduleChangeAngle(3, value);
        } else if (id == R.id.imageButton7 || id == R.id.imageButton8) {
            value = channel4.getText() == null ? 0 : Integer.valueOf(TextUtils.isEmpty(channel4.getText().toString())?"0":channel4.getText().toString());
            value = value + (id == R.id.imageButton7 ? -ANGEL_INTERVAL : ANGEL_INTERVAL);
            if (value < 0) value = 0;
            if (value > 180) value = 180;
            channel4.setText(String.valueOf(value));
            scheduleChangeAngle(4, value);
        } else if (id == R.id.imageButton9 || id == R.id.imageButton10) {
            value = channel5.getText() == null ? 0 : Integer.valueOf(TextUtils.isEmpty(channel5.getText().toString())?"0":channel5.getText().toString());
            value = value + (id == R.id.imageButton9 ? -ANGEL_INTERVAL : ANGEL_INTERVAL);
            if (value < 0) value = 0;
            if (value > 180) value = 180;
            channel5.setText(String.valueOf(value));
            scheduleChangeAngle(5, value);
        } else if (id == R.id.imageButton11 || id == R.id.imageButton12) {
            value = channel6.getText() == null ? 0 : Integer.valueOf(TextUtils.isEmpty(channel6.getText().toString())?"0":channel6.getText().toString());
            value = value + (id == R.id.imageButton11 ? -ANGEL_INTERVAL : ANGEL_INTERVAL);
            if (value < 0) value = 0;
            if (value > 180) value = 180;
            channel6.setText(String.valueOf(value));
            scheduleChangeAngle(6, value);
        }

    }

    public void onRandomAll(View view) {

        if (!mStarted) {
            Toast.makeText(MainActivity.this, "Start first", Toast.LENGTH_SHORT).show();
            return;
        }

        Random r = new Random();

        final int angel1 = r.nextInt(513);
        final int angel2 = r.nextInt(513);
        final int angel3 = r.nextInt(513);
        final int angel4 = r.nextInt(513);
        final int angel5 = r.nextInt(513);
        final int angel6 = r.nextInt(513);


        changeAngle(1, angel1);
        changeAngle(2, angel2);
        changeAngle(3, angel3);
        changeAngle(4, angel4);
        changeAngle(5, angel5);
        changeAngle(6, angel6);


    }



    /**
     * A native method that is implemented by the 'dominogpt' native library,
     * which is packaged with this application.
     */

    public native void start();

    public native void stop();

    public native void setPWM(int channel, int angle);

    public native int getAngle(int channel);

    public native void testSetOffValue(int channel, int offvalue);

    private void insmodI2C() {
        try {
            DataOutputStream os = new DataOutputStream(mProcess.getOutputStream());
            os.writeBytes("insmod /system/lib/modules/aml_i2c.ko\n");
            os.flush();
            Thread.sleep(100);
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void rmmodI2C() {
        try {
            DataOutputStream os = new DataOutputStream(mProcess.getOutputStream());
            os.writeBytes("rmmod aml_i2c\n");
            os.flush();
            Thread.sleep(100);
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    boolean mStarted = false;

    private void startStopRobot() {
        if (mStarted) {
            insmodI2C();
            start();
        } else {
            mBGHandler.removeCallbacksAndMessages(null);
            stop();
            rmmodI2C();
        }

    }

    public void onStart(View view) {
        mStarted = !mStarted;
        Button startBtn = (Button) findViewById(R.id.button);
        startBtn.setText(mStarted ? "Stop" : "Start");


        mBGHandler.postDelayed(() -> {
            startStopRobot();
            if (mStarted) {
                readAllAngles();
            }
        }, 2000);

    }

    public void onStop(View view) {


    }
}