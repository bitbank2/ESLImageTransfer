package com.example.eslimagetransfer;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.eslimagetransfer.databinding.FragmentSecondBinding;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class SecondFragment extends Fragment {

    private FragmentSecondBinding binding;
    private Context localContext;
    private Handler mHandler;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic mCharacteristic = null;
    private boolean bConnected = false;
    private static final UUID ESL_SERVICE_UUID = UUID.fromString("13187b10-eba9-a3ba-044e-83d3217d9a38");
    private static final UUID ESL_CHARACTERISTIC_UUID = UUID.fromString("4b646063-6264-f3a7-8941-e65356ea82fe");
    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentSecondBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mHandler = new Handler();
        binding.buttonSecond.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Stops scanning after a pre-defined scan period.
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        sendImage();
                    }
                });

//                NavHostFragment.findNavController(SecondFragment.this)
//                        .navigate(R.id.action_SecondFragment_to_FirstFragment);
            }
        });
        localContext = getActivity().getApplicationContext();

        Toast.makeText(localContext,
                "Connecting to ESL...",
                Toast.LENGTH_LONG).show();
        // Get BluetoothAdapter
        final BluetoothManager bluetoothManager =
                (BluetoothManager) localContext.getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (connectGatt(FirstFragment.eslDevice.getAddress())) {
            mBluetoothGatt.discoverServices();
            Toast.makeText(localContext,
                    "Connected!",
                    Toast.LENGTH_LONG).show();
            bConnected = true;
  //          mBluetoothGatt.discoverServices();
        }
    } /* onViewCreated() */

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
    private void sendImage() {
        // Try sending a blank screen command
        byte[] setBytePos = {0x02, 0x00, 0x00};
        byte[] display = {0x01};
        // create a 16x16 pattern of black and white squares
        byte[] evenImg = {0x03, 0x00, 0x00, -1, -1, 0x00, 0x00, -1, -1, 0x00, 0x00, -1, -1, 0x00, 0x00, -1, -1};
        byte[] oddImg = {0x03, -1, -1, 0x00, 0x00, -1, -1, 0x00, 0x00, -1, -1, 0x00, 0x00, -1, -1, 0x00, 0x00};
        mCharacteristic.setValue(setBytePos);
        mCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        mBluetoothGatt.writeCharacteristic(mCharacteristic);
        try {
            //thread to sleep for the specified number of milliseconds
            Thread.sleep(5);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Create a checker board pattern
        for (int i=0; i<250; i++) { // send image data
            if ((i & 16) == 0)
                mCharacteristic.setValue(evenImg);
            else
                mCharacteristic.setValue(oddImg);
            mCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            mBluetoothGatt.writeCharacteristic(mCharacteristic);
            try {
                //thread to sleep for the specified number of milliseconds
                Thread.sleep(5);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } // for i
        // show the image
        mCharacteristic.setValue(display);
        mCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
        mBluetoothGatt.writeCharacteristic(mCharacteristic);

    } /* sendImage() */

    private boolean connectGatt(final String address) {
        if (mBluetoothAdapter == null || address == null) {
      //      Log.w(, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        if (mBluetoothGatt != null) {
        //    Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter
                .getRemoteDevice(address);
        if (device == null) {
       //     Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
// NB: This won't work with LE devices unless you use the SDK 23+ version of this function
        // and specify the transport type as LE
        mBluetoothGatt = device.connectGatt(localContext, false, mGattCallback, BluetoothDevice.TRANSPORT_LE);
        // Log.d(TAG, "Trying to create a new connection.");
        return mBluetoothGatt.connect();
    }
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    //bluetooth is connected so discover services
                    bConnected = true;
                    mBluetoothGatt.discoverServices();

                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    //Bluetooth is disconnected
                    bConnected = false;
                    gatt.close();
                }
            } else {
                // Error - close the connection
                gatt.close();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // services are discovered, find the one we want
                final List<BluetoothGattService> services = gatt.getServices();
                Log.i("ESLImageTransfer", String.format(Locale.ENGLISH,"discovered %d services for ESL", services.size()));
                for (BluetoothGattService service : services) {
                    if (service.getUuid().equals(ESL_SERVICE_UUID)) {
                        service.getCharacteristics();
                        mCharacteristic = service.getCharacteristic(ESL_CHARACTERISTIC_UUID);
                    }
                }
            } else {
                Log.e("ESLImageTransfer", "Service discovery failed");
                gatt.disconnect();
                return;
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {

            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {

        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);

        }
    };
}