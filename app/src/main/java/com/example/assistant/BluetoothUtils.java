package com.example.assistant;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;


public class BluetoothUtils {
    private Context context;
    private final Handler handler;
    private BluetoothAdapter bluetoothAdapter;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;

    private final UUID APP_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    private final String APP_NAME = "HC-05";

    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;

    private int state;

    public BluetoothUtils(Context context, Handler handler) {
        this.context = context;
        this.handler = handler;

        state = STATE_NONE;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public int getState() {
        return state;
    }

    public synchronized void setState(int state) {
        this.state = state;
        handler.obtainMessage(HomeActivity.MESSAGE_STATE_CHANGED, state, -1).sendToTarget();
    }

    private synchronized void start() {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        setState(STATE_LISTEN);

    }

    public synchronized void stop() {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        setState(STATE_NONE);

    }

    public void connect(BluetoothDevice device) {
        if (state == STATE_CONNECTING) {
            connectThread.cancel();
            connectThread = null;
        }

        connectThread = new ConnectThread(device);
        connectThread.start();

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        setState(STATE_CONNECTING);
    }



    private class ConnectThread extends Thread {
        private final BluetoothSocket socket;
        private final BluetoothDevice device;

        public ConnectThread(BluetoothDevice device) {
            this.device = device;

            BluetoothSocket tmp = null;
            try {
                tmp = device.createRfcommSocketToServiceRecord(APP_UUID);
            } catch (IOException e) {
                Log.e("Connect->Constructor", e.toString());
            }

            socket = tmp;
        }

        public void run() {
            try {
                socket.connect();
            } catch (IOException e) {
                Log.e("Connect->Run", e.toString());
                try {
                    socket.close();
                } catch (IOException e1) {
                    Log.e("Connect->CloseSocket", e.toString());
                }
                connectionFailed();
                return;
            }

            synchronized (this) {
                connectThread = null;
            }

            connected(socket, device);
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e("Connect->Cancel", e.toString());
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket socket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public ConnectedThread(BluetoothSocket socket) {
            this.socket = socket;

            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            inputStream = tmpIn;
            outputStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = inputStream.read(buffer);
                    Log.d("Input debugging-> ", buffer.toString());
                    handler.obtainMessage(HomeActivity.MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    connectionLost();
                }
            }
        }

        public void write(byte[] buffer) {
            try {
                outputStream.write(buffer);
                handler.obtainMessage(HomeActivity.MESSAGE_WRITE, -1, -1, buffer).sendToTarget();
            } catch (IOException e) {

            }
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {

            }
        }
    }

    private void connectionLost() {
        Message message = handler.obtainMessage(HomeActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(HomeActivity.TOAST, "Connection Lost");
        message.setData(bundle);
        handler.sendMessage(message);

        this.start();
    }

    private synchronized void connectionFailed() {
        Message message = handler.obtainMessage(HomeActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(HomeActivity.TOAST, "Cant connect to the device");
        message.setData(bundle);
        handler.sendMessage(message);

        this.start();
    }

    private synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        connectedThread = new ConnectedThread(socket);
        connectedThread.start();

        Message message = handler.obtainMessage(HomeActivity.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(HomeActivity.DEVICE_NAME, device.getName());
        message.setData(bundle);
        handler.sendMessage(message);

        setState(STATE_CONNECTED);
    }
}
