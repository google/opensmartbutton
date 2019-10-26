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

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class BLEDeviceListAdapter extends ArrayAdapter<BluetoothDevice> {

    Context context;
    public BLEDeviceListAdapter(@NonNull Context context) {
        super(context, R.layout.ble_device_row);
        this.context = context;
    }

    public void addDevice(BluetoothDevice device) {
        add(device);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) this.context
                .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.ble_device_row, parent, false);
        TextView textView = rowView.findViewById(R.id.ble_device_text);
        textView.setText(this.getItem(position).getName());
        return rowView;
    }
}