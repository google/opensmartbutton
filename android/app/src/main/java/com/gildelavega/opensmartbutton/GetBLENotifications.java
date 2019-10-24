/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gildelavega.opensmartbutton;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GetBLENotifications extends Service {

    private static final String TAG = "BLENotificationService";
    public static final String EXTRA_BLE_DEVICE = "bleDevice";
    public static final String NOTIFICATION_CHANNEL_ID = "BLENotificationsChannel";

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    private static final UUID SERVICE_UUID =
            UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b");
    private static final UUID CHARACTERISTIC_UUID =
            UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8");
    private static final UUID DESCRIPTOR_UUID =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private Map<BluetoothDevice, Integer> connectionState;

    public GetBLENotifications() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Starting service");
        connectionState = new HashMap<>();

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification =
                new Notification.Builder(this)
                        .setContentTitle(getText(R.string.notification_title))
                        .setContentText(getText(R.string.notification_message))
                        .setSmallIcon(R.drawable.antenna)
                        .setContentIntent(pendingIntent)
                        .setTicker(getText(R.string.notification_ticker))
                        .build();

        startForeground(1, notification);

        if(intent != null){
            BluetoothDevice device = intent.getParcelableExtra(EXTRA_BLE_DEVICE);
            Log.i(TAG, "Connecting to device...");
            device.connectGatt(this,false, gattCallback);
        }

        return START_STICKY;
    }


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private final BluetoothGattCallback gattCallback =
            new BluetoothGattCallback() {
                @Override
                public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                                    int newState) {
                    Log.i(TAG, "Connection state changed.");
                    String intentAction;
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        connectionState.put(gatt.getDevice(), STATE_CONNECTED);
                        Log.i(TAG, "Connected to GATT server.");
                        gatt.discoverServices();

                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        connectionState.put(gatt.getDevice(), STATE_DISCONNECTED);
                        Log.i(TAG, "Disconnected from GATT server.");
                        gatt.getDevice().connectGatt(GetBLENotifications.this,false, gattCallback);
                    }
                }

                @Override
                // New services discovered
                public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                    Log.i(TAG, "Services discovered");
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        BluetoothGattCharacteristic characteristic =
                                gatt.getService(SERVICE_UUID).
                                        getCharacteristic(CHARACTERISTIC_UUID);
                        Log.i(TAG, "Characteristic got");
                        gatt.setCharacteristicNotification(characteristic, true);
                        Log.i(TAG, "Set characteristic notification");
                        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                                DESCRIPTOR_UUID);
                        Log.i(TAG, "Descriptor got: " + descriptor);
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        Log.i(TAG, "Value set");
                        gatt.writeDescriptor(descriptor);
                        Log.i(TAG, "Descriptor written");
                    } else {
                        Log.w(TAG, "onServicesDiscovered received: " + status);
                    }
                }

                @Override
// Characteristic notification
                public void onCharacteristicChanged(BluetoothGatt gatt,
                                                    BluetoothGattCharacteristic characteristic) {
                    Log.i(TAG, "Notification");
                }
            };

}
