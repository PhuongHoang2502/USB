import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;

public class UsbActivity extends AppCompatActivity {

    private static final String ACTION_USB_PERMISSION = "com.example.USB_PERMISSION";
    private static final String TAG = "UsbActivity";

    private UsbManager usbManager;
    private UsbDevice usbDevice;
    private UsbDeviceConnection connection;
    private UsbInterface usbInterface;
    private UsbEndpoint endpointIn;

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            setupDevice(device);
                        }
                    } else {
                        Log.d(TAG, "Permission denied for device " + device);
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usb);

        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        registerReceiver(usbReceiver, filter);

        findDevice();
    }

    private void findDevice() {
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        if (!deviceList.isEmpty()) {
            for (UsbDevice device : deviceList.values()) {
                // You might want to add some filtering logic here
                // to select the specific device you're interested in
                requestPermission(device);
                break;
            }
        } else {
            Toast.makeText(this, "No USB devices found", Toast.LENGTH_SHORT).show();
        }
    }

    private void requestPermission(UsbDevice device) {
        PendingIntent permissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
        usbManager.requestPermission(device, permissionIntent);
    }

    private void setupDevice(UsbDevice device) {
        usbDevice = device;
        usbInterface = device.getInterface(0);
        endpointIn = usbInterface.getEndpoint(0);
        connection = usbManager.openDevice(device);

        if (connection != null && connection.claimInterface(usbInterface, true)) {
            Toast.makeText(this, "USB Connection established", Toast.LENGTH_SHORT).show();
            // You can start reading data here
            readData();
        } else {
            Toast.makeText(this, "Failed to open USB connection", Toast.LENGTH_SHORT).show();
        }
    }

    private void readData() {
        // This is a simple example of reading data
        // You should run this in a separate thread in a real application
        byte[] buffer = new byte[endpointIn.getMaxPacketSize()];
        int bytesRead = connection.bulkTransfer(endpointIn, buffer, buffer.length, 5000);
        if (bytesRead >= 0) {
            String data = new String(buffer, 0, bytesRead);
            Log.d(TAG, "Received data: " + data);
        } else {
            Log.e(TAG, "Error reading data");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(usbReceiver);
        if (connection != null) {
            connection.close();
        }
    }
}