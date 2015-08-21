package com.udiboy.xlr8remotecontrol;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;


public class  MainActivity extends Activity{
    private static final String TAG = "MainActivity";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_ENABLE_BT = 3;

    private String mConnectedDeviceName = null;
    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothChatService mChatService = null;


    private BotController mBotController;

    /**
     * A Handler object which handles messages from the BluetoothChatService class
     *
     * It updates the UI based on various events
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            //Disable the "Connect" tab, and enable other tabs

                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName), R.color.status_positive);

                            findViewById(R.id.tabButton1).setEnabled(false);

                            findViewById(R.id.tabButton2).setEnabled(true);
                            findViewById(R.id.tabButton3).setEnabled(true);
                            findViewById(R.id.tabButton4).setEnabled(true);

                            tabSwitch(findViewById(R.id.tabButton2));
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            setStatus(getString(R.string.title_connecting),R.color.status_neutral);
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            // Disable other tabs and go back to "Connect" tab

                            setStatus(getString(R.string.title_not_connected),R.color.status_negative);


                            findViewById(R.id.tabButton1).setEnabled(true);

                            findViewById(R.id.tabButton2).setEnabled(false);
                            findViewById(R.id.tabButton3).setEnabled(false);
                            //findViewById(R.id.tabButton4).setEnabled(false);

                            tabSwitch(findViewById(R.id.tabButton1));
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    Log.i("BluetoothIO","Sent : "+(int)writeBuf[0]);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    Log.i("BluetoothIO","Rcvd : "+(int)readBuf[0]);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    Toast.makeText(MainActivity.this, "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case Constants.MESSAGE_TOAST:
                    Toast.makeText(MainActivity.this, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
        }

        mBotController = new BotController(this,mChatService);

        findViewById(R.id.left).setOnTouchListener(mBotController);
        findViewById(R.id.left_fwd).setOnTouchListener(mBotController);
        findViewById(R.id.left_bck).setOnTouchListener(mBotController);
        findViewById(R.id.right).setOnTouchListener(mBotController);
        findViewById(R.id.right_fwd).setOnTouchListener(mBotController);
        findViewById(R.id.right_bck).setOnTouchListener(mBotController);
        findViewById(R.id.forward).setOnTouchListener(mBotController);
        findViewById(R.id.backward).setOnTouchListener(mBotController);

        tabSwitch(findViewById(R.id.tabButton1));
    }

    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else if (mChatService == null) {
            mChatService = new BluetoothChatService(this, mHandler);
        }


        mBotController.mChatService = mChatService;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mChatService != null) {
            mChatService.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();

                Log.d(TAG,"starting");
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.secure_connect_scan) {
            connectToBot(null);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void connectToBot(View v) {
        if(mChatService.getState()!=BluetoothChatService.STATE_CONNECTED) {
            Intent i = new Intent(this, DeviceListActivity.class);
            startActivityForResult(i, REQUEST_CONNECT_DEVICE_SECURE);
        } else {
            //mChatService.disconnect();
        }
    }

    // Helper for switching tabs in the Tab based UI
    public void tabSwitch(View  v){
        ((ToggleButton)findViewById(R.id.tabButton1)).setChecked(false);
        ((ToggleButton)findViewById(R.id.tabButton2)).setChecked(false);
        ((ToggleButton)findViewById(R.id.tabButton3)).setChecked(false);
        ((ToggleButton)findViewById(R.id.tabButton4)).setChecked(false);

        findViewById(R.id.tab1).setVisibility(View.GONE);
        findViewById(R.id.tab2).setVisibility(View.GONE);
        findViewById(R.id.tab3).setVisibility(View.GONE);
        findViewById(R.id.tab4).setVisibility(View.GONE);

        mBotController.stopSwagMode();

        switch (v.getId()){
            case R.id.tabButton1:
                findViewById(R.id.tab1).setVisibility(View.VISIBLE);
                break;
            case R.id.tabButton2:
                findViewById(R.id.tab2).setVisibility(View.VISIBLE);
                break;
            case R.id.tabButton3:
                findViewById(R.id.tab3).setVisibility(View.VISIBLE);
                break;
            case R.id.tabButton4:
                findViewById(R.id.tab4).setVisibility(View.VISIBLE);
                mBotController.startSwagMode();
                break;
        }

        ((ToggleButton)v).setChecked(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==REQUEST_CONNECT_DEVICE_SECURE && resultCode == RESULT_OK){
            connectDevice(data,true);
        } else if(requestCode==REQUEST_ENABLE_BT){
            if(resultCode != RESULT_OK){
                Log.d(TAG, "Denied bluetooth access");
                finish();
            } else {
                mChatService = new BluetoothChatService(this, mHandler);
                Log.d(TAG, "Allowed bluetooth access");
            }
        }
    }

    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device, secure);
    }

    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    public void setStatus(String status, int color) {
        ((TextView)findViewById(R.id.status)).setText(status);
        ((TextView)findViewById(R.id.status)).setTextColor(getResources().getColor(color));
    }
}
