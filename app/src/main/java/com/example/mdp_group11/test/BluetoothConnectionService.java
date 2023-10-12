package com.example.mdp_group11.test;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import android.os.ParcelUuid;
import android.os.AsyncTask;
import android.content.IntentFilter;


import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.mdp_group11.boundary.MainActivity;
import com.example.mdp_group11.boundary.ObstacleView;
import com.example.mdp_group11.boundary.RobotView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.UUID;

public class BluetoothConnectionService extends AppCompatActivity {
    private boolean isConnected = false;
    private static volatile BluetoothConnectionService instance;
    private static final String TAG = "DebuggingTag";
    private static final String appName = "MDP_Group_11";
    public static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final BluetoothAdapter mBluetoothAdapter;
    private Context mContext;
    private AcceptThread mInsecureAcceptThread;
    private Thread sendThread;
    private boolean sendThreadRunning = false;
    //private BluetoothLostReceiver bluetoothLostReceiver;
    private ConnectThread mConnectThread;
    private BluetoothDevice mDevice;
    private UUID deviceUUID;
    ProgressDialog mProgressDialog;
    Intent connectionStatus;
    public static boolean BluetoothConnectionStatus=false;
    private static ConnectedThread mConnectedThread;
    public static boolean isConnected() {
        return mConnectedThread != null;
    }
    public BluetoothConnectionService(Context context) {
        this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.mContext = context;
        startAcceptThread();
    }
    // Obtain instance
    //BluetoothConnectionService service = BluetoothConnectionService.getInstance();

    // Initialize with context
    //service.init(getApplicationContext());

    private class AcceptThread extends Thread{
        private final BluetoothServerSocket ServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            try {
                tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(appName, myUUID);
                Log.d(TAG, "Accept Thread: Setting up Server using: " + myUUID);
            }catch(IOException e){
                Log.e(TAG, "Accept Thread: IOException: " + e.getMessage());
            }
            ServerSocket = tmp;
        }
        public void run(){
            Log.d(TAG, "run: AcceptThread Running. ");
            BluetoothSocket socket =null;
            try {
                Log.d(TAG, "run: RFCOM server socket start here...");

                socket = ServerSocket.accept();
                Log.d(TAG, "run: RFCOM server socket accepted connection.");
            }catch (IOException e){
                Log.e(TAG, "run: IOException: " + e.getMessage());
            }
            if(socket!=null){
                connected(socket, socket.getRemoteDevice());
            }
            Log.i(TAG, "END AcceptThread");
        }
        public void cancel(){
            Log.d(TAG, "cancel: Cancelling AcceptThread");
            try{
                ServerSocket.close();
            } catch(IOException e){
                Log.e(TAG, "cancel: Failed to close AcceptThread ServerSocket " + e.getMessage());
            }
        }
    }

    private class ConnectThread extends Thread{
        private BluetoothSocket mSocket;

        public ConnectThread(BluetoothDevice device, UUID u){
            Log.d(TAG, "ConnectThread: started.");
            mDevice = device;
            deviceUUID = u;
        }

        @SuppressLint("MissingPermission")
        public void run() {
            BluetoothSocket tmp = null;
            Log.d(TAG, "RUN: mConnectThread");

            try {
                Log.d(TAG, "ConnectThread: Trying to create InsecureRfcommSocket using UUID: " + myUUID);
                tmp = mDevice.createRfcommSocketToServiceRecord(deviceUUID);
            } catch (IOException e) {
                Log.e(TAG, "ConnectThread: Could not create InsecureRfcommSocket " + e.getMessage());
            }
            mSocket = tmp;
            mBluetoothAdapter.cancelDiscovery();

            try {
                mSocket.connect();

                Log.d(TAG, "RUN: ConnectThread connected.");

                connected(mSocket, mDevice);

            } catch (IOException e) {
                try {
                    mSocket.close();
                    Log.d(TAG, "RUN: ConnectThread socket closed.");
                } catch (IOException e1) {
                    Log.e(TAG, "RUN: ConnectThread: Unable to close connection in socket." + e1.getMessage());
                }
                Log.d(TAG, "RUN: ConnectThread: could not connect to UUID." + myUUID);
                try {
                    BluetoothPopUp mBluetoothPopUpActivity = (BluetoothPopUp) mContext;
                    mBluetoothPopUpActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mContext, "Failed to connect to the Device.", Toast.LENGTH_LONG).show();
                        }
                    });
                    } catch (Exception z) {
                        z.printStackTrace();
                    }
                }
                try {
                    mProgressDialog.dismiss();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }

        public void cancel(){
            Log.d(TAG, "cancel: Closing Client Socket");
            try{
                mSocket.close();
            } catch(IOException e){
                Log.e(TAG, "cancel: Failed to close ConnectThread mSocket " + e.getMessage());
            }
        }
    }
    public synchronized void startAcceptThread(){
        Log.d(TAG, "start");

        if(mConnectThread!=null){
            mConnectThread.cancel();
            mConnectThread=null;
        }
        if(mInsecureAcceptThread == null){
            mInsecureAcceptThread = new AcceptThread();
            mInsecureAcceptThread.start();
        }
    }

    public void startClientThread(BluetoothDevice device, UUID uuid){
        Log.d(TAG, "startClient: Started.");
        try {
            mProgressDialog = ProgressDialog.show(mContext, "Connecting Bluetooth", "Please Wait...", true);
        } catch (Exception e) {
            Log.d(TAG, "StartClientThread Dialog show failure");
        }
        mConnectThread = new ConnectThread(device, uuid);
        mConnectThread.start();
    }

    private class ConnectedThread extends Thread{
        private final BluetoothSocket mSocket;
        private final InputStream inStream;
        private final OutputStream outStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "ConnectedThread: Starting.");

            connectionStatus = new Intent("ConnectionStatus");
            connectionStatus.putExtra("Status", "connected");
            connectionStatus.putExtra("Device", mDevice);
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(connectionStatus);
            BluetoothConnectionStatus = true;

            this.mSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = mSocket.getInputStream();
                tmpOut = mSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            inStream = tmpIn;
            outStream = tmpOut;
            isConnected = true;

        }

        public void run(){
            byte[] buffer = new byte[1024];
            int bytes;

            while(true){
                try {
                    bytes = inStream.read(buffer);
                    String incomingMessage = new String(buffer, 0, bytes).trim();
                    Log.d(TAG, "InputStream: " + incomingMessage);

                    if (!incomingMessage.contains("{")){
                        MainActivity.getInstance().updateCommsBox(incomingMessage);
                        String[] messageArray = incomingMessage.split("-");
                        runOnUiThread(() -> {
                            try {
                                ObstacleView ov = MainActivity.getInstance().getOV(Integer.parseInt(messageArray[0]));
                                ov.updateImage(messageArray[1]);
                                String commsMessage = "Obstacle ID changed from " + messageArray[0] + " to " + messageArray[1];
                                MainActivity.getInstance().updateCommsBox(commsMessage);
                            } catch (Exception e) {
                                Log.e(TAG, "Image not sent");
                            }
                            });

                        continue;
                    }

                    Queue<String> messageQ = new LinkedList<>();

                    String[] wholeMessage=incomingMessage.split("\\{");
                    for (int sentinal=1;sentinal<wholeMessage.length;sentinal++){
                        if (wholeMessage[sentinal].contains("}")) {
                            messageQ.add("{" + wholeMessage[sentinal]);
                        }
                    }
                    while (!messageQ.isEmpty()) {
                        incomingMessage=messageQ.remove();
                        char[] charIm = incomingMessage.toCharArray();

                        if (charIm[0] == '{') {
                            String[] messageArray = incomingMessage.split("|");
                            messageArray[0] = messageArray[0].substring(1, messageArray[0].length());
                            if (messageArray[0].equals("obstacle")) {
                                messageArray[2] = messageArray[2].substring(0, messageArray[2].length() - 1);
                                Log.e(TAG, messageArray[1]);
                                Log.e(TAG, messageArray[2]);
                                try {
                                    messageArray[2] = messageArray[2].substring(0, messageArray[2].indexOf('}'));
                                } catch (Exception e) {
                                    //pass
                                }
                            } else {
                                messageArray[1] = messageArray[1].substring(0, messageArray[1].length() - 1);
                                try {
                                    messageArray[1] = messageArray[1].substring(0, messageArray[1].indexOf('}'));
                                } catch (Exception e) {
                                    //pass
                                }
                            }
                            //Log.e(TAG, messageArray[0] + ": " + messageArray[1]);

                            switch (messageArray[0]) {

                                case "obstacle":
                                    runOnUiThread(() -> {
                                        try {
                                            ObstacleView ov = MainActivity.getInstance().getOV(Integer.parseInt(messageArray[1]));
                                            ov.updateImage(messageArray[2]);
                                        } catch (Exception e) {
                                            Log.e(TAG, "run: Next");
                                        }
                                    });
                                    break;

                                case "status":
                                    runOnUiThread(() -> {
                                        try {
                                            MainActivity.getInstance().robotStatusAddAndRefresh(messageArray[1]);
                                        } catch (Exception e) {
                                            Log.e(TAG, messageArray[1]);
                                        }
                                    });
                                    break;

                                case "mode":
                                    runOnUiThread(() -> {
                                        MainActivity.getInstance().robotStatusAddAndRefresh(messageArray[1]);
                                    });
                                    if (messageArray[1].equals("EXPLORE")) {
                                        Log.e(TAG, "run: Explore received");
                                        //interruptSendThread();
                                    }
                                    break;

                                case "stm":
                                    runOnUiThread(() -> {
                                        String moveCommand = "";
                                        switch (messageArray[1]) {
                                            case "w":
                                                moveCommand = "w";
                                                MainActivity.getInstance().robotMove("FF");
                                                break;
                                            case "s":
                                                moveCommand = "s";
                                                MainActivity.getInstance().robotMove("BB");
                                                break;
                                            case "e":
                                                moveCommand = "e";
                                                MainActivity.getInstance().robotMove("FR");
                                                break;
                                            case "q":
                                                moveCommand = "q";
                                                MainActivity.getInstance().robotMove("FL");
                                                break;
                                            case "d":
                                                moveCommand = "d";
                                                MainActivity.getInstance().robotMove("BR");
                                                break;
                                            case "a":
                                                moveCommand = "a";
                                                MainActivity.getInstance().robotMove("BL");
                                                break;
                                            default:
                                                break;
                                        }
                                        if (!moveCommand.isEmpty()) {
                                            String commsMessage = "STM|" + moveCommand;
                                            MainActivity.getInstance().updateCommsBox(commsMessage);
                                        }
                                    });
                                    break;

                                case "teleport":
                                    runOnUiThread(() -> {
                                        Log.e(TAG, messageArray[1]);
                                        RobotView rv = MainActivity.getInstance().getRV();
                                        String[] coord = messageArray[1].split(",");
                                        String direction = "";
                                        if(Objects.equals(coord[3], "N")) direction = "0";
                                        if(Objects.equals(coord[3], "E")) direction = "-90";
                                        if(Objects.equals(coord[3], "S")) direction = "180";
                                        if(Objects.equals(coord[3], "W")) direction = "90";
                                        rv.teleport(Integer.parseInt(coord[1]), Integer.parseInt(coord[2]), direction);

                                        String teleportCommand = coord[1] + "," + coord[2] + "," + coord[3];
                                        String commsMessage = "Teleport Robot: " + teleportCommand;
                                        MainActivity.getInstance().updateCommsBox(commsMessage);
                                    });
                                    break;

                                default:
                                    break;
                            }
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "write: Error reading Input Stream. " + e.getMessage() );
                    cancel();
                    try {
                        inStream.close();
                    } catch (IOException ex) {
                        Log.e(TAG, "closeInputStream: Error closing input stream. " + ex.getMessage() );
                    }
                    break;
                }
                catch (Exception e){
                    break;
                }
            }
        }

        public void write(byte[] bytes){
            String text = new String(bytes, Charset.defaultCharset());
            Log.d(TAG, "write: Writing to output stream: "+text);
            try {
                outStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG, "Error writing to output stream. "+e.getMessage());
            }
        }

        public void cancel(){
            Log.d(TAG, "cancel: Closing Client Socket");
            try{
                mSocket.close();
            } catch(IOException e){
                Log.e(TAG, "cancel: Failed to close ConnectThread mSocket " + e.getMessage());
            }
        }
    }

    private void connected(BluetoothSocket mSocket, BluetoothDevice device) {
        Log.d(TAG, "connected: Starting.");
        mDevice =  device;
        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }

        mConnectedThread = new ConnectedThread(mSocket);
        mConnectedThread.start();
    }

    public static void write(byte[] out){
        ConnectedThread tmp;

        Log.d(TAG, "write: Write is called." );
        mConnectedThread.write(out);
    }
    public void interruptSendThread(){
        if (this.sendThread!=null && sendThreadRunning==true) {
            this.sendThread.interrupt();
            this.sendThreadRunning=false;
        }
    }
    /*public static class BluetoothLostReceiver extends BroadcastReceiver {

        MainActivity main = null;
        public void setMainActivity(MainActivity main)
        {
            this.main = main;
        }
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("BLUETOOTH", "Disconnect detected");
            if(BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(intent.getAction()))
            {
                BluetoothConnectionService.getInstance().retryConnection();
            }
        }
    }
    //Retries the connection up to 3 times and restarts the accept thread
    public void retryConnection() {
        isConnected = false; // Reset the flag before trying to reconnect

        for (int x = 0; x < 3; x++) {
            try {
                btThreadSetup(mDevice, mContext);
                startClientThread(mDevice, deviceUUID);
                Thread.sleep(2000); // Sleep for 2 seconds
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (NullPointerException e) {
                e.printStackTrace();
                Log.e("Bluetooth", "Null Pointer Exception");
                return;
            }

            if (isConnected) {
                Log.d(TAG, "Reconnection successful.");
                break; // Exit the loop if connection is successful
            }
        }

        if (!isConnected) {
            Log.e(TAG, "Failed to reconnect after 3 attempts.");
        }

        mInsecureAcceptThread.cancel();
        BluetoothConnectionService.this.startAcceptThread();
    }*/

    public void btThreadSetup(BluetoothDevice device, Context context) {

        BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
        try {
            ParcelUuid[] puuid = mDevice.getUuids();
            ParcelUuid tmp = puuid[0];
            System.out.println(tmp.getUuid());
            startClientThread(mDevice,deviceUUID);
        } catch (Exception e) {
            Log.e(TAG, "createBTThread: IOException: " + e.getMessage());
        }
    }
}
