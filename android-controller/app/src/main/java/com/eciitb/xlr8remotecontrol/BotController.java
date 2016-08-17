package com.eciitb.xlr8remotecontrol;


import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

/**
 * class BotController
 *
 * A seperate controller object which processes UI touch input
 * and interfaces with the Bluetooth communication service.
 *
 * It generates and stores the proper bit sequence
 * for each corresponding button, and handles the process of
 * initiating a "send" operation whenever needed
 */
public class BotController implements View.OnTouchListener, SensorEventListener{
    BluetoothChatService mChatService;
    MainActivity mContext;
    public static final String TAG = " BotControl";

    private byte mMotorState = 0b00000000; // Store the last bit sequence sent|to be sent

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;

    // PIN mappings of motor control bits to ATtiny PORTB pins
    private static final byte LT_MOTOR_FWD=1;
    private static final byte LT_MOTOR_BCK=3;
    private static final byte RT_MOTOR_FWD=0;
    private static final byte RT_MOTOR_BCK=2;

    public BotController(MainActivity context, BluetoothChatService chatService){
        mContext=context;
        mChatService=chatService;

        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    // Set the bit specified by "which" using the PIN mappping to "bit"(0|1)
    // Only affects individual bits so two buttons pressed simultaneously will work
    private void setBit(byte which, int bit){
        if(bit==1){
            mMotorState = (byte) (mMotorState | (1<<which));
        } else {
            mMotorState = (byte) (mMotorState & ~(1<<which));
        }
    }

    // reset bits to 0b0000000
    private void reset(){
        mMotorState = 0x00;
    }


    // Initiate a send operation with the message as the bit sequence "b"
    public void sendMessage(int b) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Log.w("BluetoothWarn", "Not connected to any device, Please connect first!");
            return;
        }

        byte msg[] = {(byte)b};
        mChatService.write(msg);
    }


    public void startSwagMode(){
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
    }

    public void stopSwagMode(){
        mSensorManager.unregisterListener(this);
    }

    // Handle touch events on every button
    @Override
    public boolean onTouch(View v, MotionEvent event){
        boolean updated = true;
        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.d(TAG, "Action Down");
                v.setPressed(true);

                // Switch case to set a corresponding
                // bit sequence according to the button pressed
                switch (v.getId()) {
                    case R.id.forward:
                        setBit(LT_MOTOR_FWD,1);
                        setBit(LT_MOTOR_BCK,0);
                        setBit(RT_MOTOR_FWD,1);
                        setBit(RT_MOTOR_BCK,0);
                        break;
                    case R.id.backward:
                        setBit(LT_MOTOR_FWD,0);
                        setBit(LT_MOTOR_BCK,1);
                        setBit(RT_MOTOR_FWD,0);
                        setBit(RT_MOTOR_BCK,1);
                        break;
                    case R.id.left:
                        setBit(LT_MOTOR_FWD,1);
                        setBit(LT_MOTOR_BCK,0);
                        setBit(RT_MOTOR_FWD,0);
                        setBit(RT_MOTOR_BCK,1);
                        break;
                    case R.id.right:
                        setBit(LT_MOTOR_FWD,0);
                        setBit(LT_MOTOR_BCK,1);
                        setBit(RT_MOTOR_FWD,1);
                        setBit(RT_MOTOR_BCK,0);
                        break;
                    case R.id.left_fwd:
                        setBit(LT_MOTOR_FWD,1);
                        break;
                    case R.id.left_bck:
                        setBit(LT_MOTOR_BCK,1);
                        break;
                    case R.id.right_fwd:
                        setBit(RT_MOTOR_FWD,1);
                        break;
                    case R.id.right_bck:
                        setBit(RT_MOTOR_BCK,1);
                        break;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_OUTSIDE:
                Log.d(TAG, "Action Up");
                v.setPressed(false);

                // Similar switch case to reset bits when button is left
                // In effect the motion of bot will last till the button is pressed
                switch (v.getId()) {
                    case R.id.forward:
                    case R.id.backward:
                    case R.id.left:
                    case R.id.right:
                        reset();
                        break;
                    case R.id.left_fwd:
                        setBit(LT_MOTOR_FWD,0);
                        break;
                    case R.id.left_bck:
                        setBit(LT_MOTOR_BCK,0);
                        break;
                    case R.id.right_fwd:
                        setBit(RT_MOTOR_FWD,0);
                        break;
                    case R.id.right_bck:
                        setBit(RT_MOTOR_BCK,0);
                        break;
                }
                break;
            default:
                updated=false;
                break;
        }

        // Check if updated, and then only send
        // We don want to unnecessarily send data
        if(updated) sendMessage(mMotorState);
        return true;
    }


    /**
     * SWAG mode :D :P
     *
     * Control the bot using tilt gestures of the phone.
     * Basically motion control
     *
     * It simply checks range of x and y values to see
     * how much the phone is tilted in what direction
     *
     * That is mapped to a corresponding bit sequence
     * If the tilt is less than certain values, bit sequence is reset
     */
    @Override
    public void onSensorChanged(SensorEvent event) {

        float x = event.values[SensorManager.DATA_X],
              y = event.values[SensorManager.DATA_Y],
              z = event.values[SensorManager.DATA_Z];

        ((TextView)mContext.findViewById(R.id.swag_x)).setText("x: "+x);
        ((TextView)mContext.findViewById(R.id.swag_y)).setText("y: "+y);
        ((TextView)mContext.findViewById(R.id.swag_z)).setText("z: "+z);

        int old = mMotorState;

        if(x>6){
            setBit(LT_MOTOR_FWD,1);
            setBit(LT_MOTOR_BCK,0);
            setBit(RT_MOTOR_FWD,0);
            setBit(RT_MOTOR_BCK,1);
        } else if(x<-6){
            setBit(LT_MOTOR_FWD,0);
            setBit(LT_MOTOR_BCK,1);
            setBit(RT_MOTOR_FWD,1);
            setBit(RT_MOTOR_BCK,0);
        } else {
            if(x>4){
                setBit(LT_MOTOR_FWD,1);
                setBit(LT_MOTOR_BCK,0);
                setBit(RT_MOTOR_FWD,0);
                setBit(RT_MOTOR_BCK,0);
            } else if(x<-4){
                setBit(LT_MOTOR_FWD,0);
                setBit(LT_MOTOR_BCK,0);
                setBit(RT_MOTOR_FWD,1);
                setBit(RT_MOTOR_BCK,0);
            } else {
                if(y>4){
                    setBit(LT_MOTOR_FWD,1);
                    setBit(LT_MOTOR_BCK,0);
                    setBit(RT_MOTOR_FWD,1);
                    setBit(RT_MOTOR_BCK,0);
                } else if(y<-4){
                    setBit(LT_MOTOR_FWD,0);
                    setBit(LT_MOTOR_BCK,1);
                    setBit(RT_MOTOR_FWD,0);
                    setBit(RT_MOTOR_BCK,1);
                } else {
                    reset();
                }
            }
        }

        ((TextView)mContext.findViewById(R.id.swag_bits)).setText("bits: "+Integer.toBinaryString(mMotorState));
        if(old!=mMotorState)
            if(mChatService.getState() == BluetoothChatService.STATE_CONNECTED)
                sendMessage(mMotorState);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
