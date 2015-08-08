package com.udiboy.xlr8remotecontrol;


import android.app.ProgressDialog;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.concurrent.ScheduledThreadPoolExecutor;

public class BotController implements View.OnTouchListener{
    BluetoothChatService mChatService;
    MainActivity mContext;
    public static final String TAG = " BotControl";

    private byte mMotorState = 0b00000000;

    //private byte mLastSent;

    //private ProgressDialog mTestingDialog;
    //private boolean testSucceeded = false;

    //private ScheduledThreadPoolExecutor testingTimer;

    private static final byte LT_MOTOR_FWD=1;
    private static final byte LT_MOTOR_BCK=3;
    private static final byte RT_MOTOR_FWD=0;
    private static final byte RT_MOTOR_BCK=2;

    public BotController(MainActivity context, BluetoothChatService chatService){
        mContext=context;
        mChatService=chatService;

        //testingTimer = new ScheduledThreadPoolExecutor(1);
    }

    private void setBit(byte which, int bit){
        if(bit==1){
            mMotorState = (byte) (mMotorState | (1<<which));
        } else {
            mMotorState = (byte) (mMotorState & ~(1<<which));
        }
    }

    private void reset(){
        mMotorState = 0x00;
    }

    public void sendMessage(int b) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Log.w("BluetoothWarn", "Not connected to any device, Please connect first!");
            return;
        }

        byte msg[] = {(byte)b};
        mChatService.write(msg);
    }

    /*public void test(){
        int randomByte = (int) (Math.random()*128);
        sendMessage((byte) randomByte);
        mTestingDialog = ProgressDialog.show(mContext,null,"Testing",false,true);

        testingTimer.schedule(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG,"scheduled run");

                if(!testSucceeded){
                    Log.d(TAG,"test failed");
                    mContext.setStatus("Communication error", R.color.status_negative);
                }

                //if(mTestingDialog!=null) {
                    Log.d(TAG,"Trying to dismiss");
                    mTestingDialog.dismiss();
                    mTestingDialog = null;
                //}
            }
        }, 5, TimeUnit.SECONDS);
    }*/

    public void sent(byte b){
        //mLastSent = b;
        //testSucceeded=false;
    }

    public void received(byte b){
        /*if(b==mLastSent){
            testSucceeded=true;

            if(mTestingDialog!=null) {
                Log.d(TAG,"Trying to dismiss 1");
                mTestingDialog.dismiss();
                mTestingDialog = null;
            }

            mContext.setStatus("Communication working", R.color.status_positive);
        } else {
            mContext.setStatus("Communication error", R.color.status_negative);
        }*/
    }

    @Override
    public boolean onTouch(View v, MotionEvent event){
        boolean updated = true;
        switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.d(TAG, "Action Down");
                v.setPressed(true);
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

        if(updated) sendMessage(mMotorState);
        return true;
    }
}
