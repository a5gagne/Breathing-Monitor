package test.agagne.Android_BreathingMonitor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Handler;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;


public class Bluetooth extends AppCompatActivity implements AdapterView.OnItemClickListener {

BluetoothAdapter btAdapter;
ListView listView;
ArrayList<String> pairedDevices;
ArrayList<BluetoothDevice> devices;
Set<BluetoothDevice> devicesArray;
ArrayAdapter<String> listAdapter;
IntentFilter filter;
BroadcastReceiver receiver;
int COARSE_LOCATION = 1;
public static final int SUCCESS_CONNECT = 0;
public static final int MESSAGE_READ = 1;
public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
static ConnectedThread connectedThread;


    public static void disconnect(){
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
    }

    public static void gethandler(Handler handler){//Bluetooth handler
        mHandler = handler;
    }
    static Handler mHandler = new Handler();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bluetooth);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);


        listView = findViewById(R.id.listView);
        listView.setOnItemClickListener(this);
        pairedDevices = new ArrayList<>();
        devices = new ArrayList<>();
        listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,0);
        listView.setAdapter(listAdapter);

        filter = new IntentFilter();
        // Register for broadcasts when device is discovered
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (BluetoothDevice.ACTION_FOUND.equals(action)){
                    // Get device information when found
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device.getName() == null) {
                        return;
                    }
                    devices.add(device);
                    // check if the found device is already paired, label it
                    String s = "";
                    for(int a = 0; a < pairedDevices.size(); a++){
                        if(device.getName().equals(pairedDevices.get(a))){
                            s = "(Paired)";
                            break;
                        }
                    }

                    listAdapter.add(device.getName()+" "+s+" "+"\n"+device.getAddress());
                }
                else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                    Toast.makeText(getApplicationContext(), "Discovery Finished", Toast.LENGTH_SHORT).show();
                }
                else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
                    Toast.makeText(getApplicationContext(), "Discovery Started", Toast.LENGTH_SHORT).show();
                }
                else if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)){
                    // if someone turns BT off while app is running, turn it back on
                    if(btAdapter.getState() == btAdapter.STATE_OFF){
                        turnOnBT();
                    }
                }
            }
        };

        registerReceiver(receiver,filter);

        btAdapter = BluetoothAdapter.getDefaultAdapter();

        if (btAdapter.isDiscovering()){
            btAdapter.cancelDiscovery();
        }

        // If the phone doesn't have BT
        if (btAdapter == null) {
            Toast.makeText(getApplicationContext(),"You don't have bluetooth idiot",Toast.LENGTH_SHORT).show();
            finish();
        }

        // If the phone has BT then check that it is enabled
        else{
            // If BT isn't enabled, then turn it on
            if(!btAdapter.isEnabled()){
                turnOnBT();
            }
            // if bt is enabled then...
            startDiscovery();
        }

    }

    private void turnOnBT() {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(intent,1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_CANCELED){
            Toast.makeText(getApplicationContext(),"Don't cancel idiot",Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void getPairedDevices() {
        // Look for devices that are already paired
        devicesArray = btAdapter.getBondedDevices();

        // Add paired devices to array list, if list is not empty
        if(devicesArray.size()>0){
            for(BluetoothDevice device:devicesArray){
                pairedDevices.add(device.getName());
            }
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        btAdapter.cancelDiscovery();
        unregisterReceiver(receiver);
    }

    private void startDiscovery() {

        // Check if permission has been previously granted
        if(ContextCompat.checkSelfPermission(getApplicationContext(),
                android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Permission has already been granted, search for devices
            getPairedDevices();
            btAdapter.startDiscovery();
        }

        else {
            // Explain why the permission is needed
            if(shouldShowRequestPermissionRationale(android.Manifest.permission.ACCESS_COARSE_LOCATION)){
                Toast.makeText(this, "Coarse Location permission is required to " +
                        "connect bluetooth", Toast.LENGTH_SHORT).show();
            }

            // Request permission
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, COARSE_LOCATION);
            }

        }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // Permission is enabled, then proceed with BT discovery
        if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
            // Start discovery
            getPairedDevices();
            btAdapter.startDiscovery();
        }
        else{
            Toast.makeText(getApplicationContext(), "Permission was not granted", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {

            BluetoothDevice selectedDevice = devices.get(arg2);
            ConnectThread connect = new ConnectThread(selectedDevice);
            connect.start();
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {

            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            btAdapter.cancelDiscovery();
            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.
            mHandler.obtainMessage(SUCCESS_CONNECT,mmSocket).sendToTarget();
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }

    static class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }
        StringBuffer sbb = new StringBuffer();
        public void run() {

            byte[] buffer;  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    try {
                        sleep(30);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    buffer = new byte[1024];

                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI activity
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String income) {

            try {
                mmOutStream.write(income.getBytes());
                for(int i=0;i<income.getBytes().length;i++)
                    Log.v("outStream"+Integer.toString(i),Character.toString((char)(Integer.parseInt(Byte.toString(income.getBytes()[i])))));
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

}
