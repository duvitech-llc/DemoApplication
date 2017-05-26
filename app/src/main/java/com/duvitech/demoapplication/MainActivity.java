package com.duvitech.demoapplication;


import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.duvitech.hud.UsbService;

import java.io.File;
import java.io.FileInputStream;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    static String exampleText = "Lorem Ipsum is simply dummy text of the printing and typesettin";
    static boolean flag = false;
    private static final int DisplayWidth = 640;
    private static final int DisplayHeight = 400;
    private final static Lock qUSBSenderLock = new ReentrantLock();

    private static final int RESULT_SEND_FRAME_BUFFER = 1;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    /*
     * Notifications from UsbService will be received here.
     */
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case UsbService.ACTION_USB_PERMISSION_GRANTED: // USB PERMISSION GRANTED
                    Toast.makeText(context, "USB Ready", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_PERMISSION_NOT_GRANTED: // USB PERMISSION NOT GRANTED
                    Toast.makeText(context, "USB Permission not granted", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_NO_USB: // NO USB CONNECTED
                    Toast.makeText(context, "No USB connected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_DISCONNECTED: // USB DISCONNECTED
                    Toast.makeText(context, "USB disconnected", Toast.LENGTH_SHORT).show();
                    break;
                case UsbService.ACTION_USB_NOT_SUPPORTED: // USB NOT SUPPORTED
                    Toast.makeText(context, "USB device not supported", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };
    private UsbService usbService;
    private ImageView display;
    private MyHandler mHandler;
    private final ServiceConnection usbConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder arg1) {
            usbService = ((UsbService.UsbBinder) arg1).getService();
            usbService.setHandler(mHandler);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            usbService = null;
        }
    };


    /**
     * Checks if the app has permission to write to device storage
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = new MyHandler(this);
        display = (ImageView)findViewById(R.id.imageView);

        Button sendButton = (Button) findViewById(R.id.btnSelectImage);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                verifyStoragePermissions(MainActivity.this);

                // open gallery select an jpeg image
                Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                // Start the Intent
                startActivityForResult(galleryIntent, RESULT_SEND_FRAME_BUFFER);

            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        setFilters();  // Start listening notifications from UsbService
        startService(UsbService.class, usbConnection, null); // Start UsbService(if it was not started before) and Bind it
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(mUsbReceiver);
        unbindService(usbConnection);
    }

    private void startService(Class<?> service, ServiceConnection serviceConnection, Bundle extras) {
        if (!UsbService.SERVICE_CONNECTED) {
            Intent startService = new Intent(this, service);
            if (extras != null && !extras.isEmpty()) {
                Set<String> keys = extras.keySet();
                for (String key : keys) {
                    String extra = extras.getString(key);
                    startService.putExtra(key, extra);
                }
            }
            startService(startService);
        }
        Intent bindingIntent = new Intent(this, service);
        bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void setFilters() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbService.ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(UsbService.ACTION_NO_USB);
        filter.addAction(UsbService.ACTION_USB_DISCONNECTED);
        filter.addAction(UsbService.ACTION_USB_NOT_SUPPORTED);
        filter.addAction(UsbService.ACTION_USB_PERMISSION_NOT_GRANTED);
        registerReceiver(mUsbReceiver, filter);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        final Bitmap bitmap;

        if (requestCode == RESULT_SEND_FRAME_BUFFER && resultCode == Activity.RESULT_OK && data != null) {

            String imgDecodableString;

            // Get the Image from data
            Log.d(TAG, "Get the image data");
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            // Get the cursor
            Cursor cursor = MainActivity.this.getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);

            // Move to first row
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            imgDecodableString = cursor.getString(columnIndex);
            int idxOfSlash = imgDecodableString.lastIndexOf('/');
            int idxOfDot = imgDecodableString.lastIndexOf('.');
            int nameLength = idxOfDot - idxOfSlash;
            String filename = imgDecodableString.substring(idxOfSlash + 1);
            String extName = imgDecodableString.substring(idxOfDot + 1);
            cursor.close();

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            //Returns null, sizes are in the options variable
            bitmap = BitmapFactory.decodeFile(imgDecodableString, options);
            boolean bFits = false;
            int width = options.outWidth;
            int height = options.outHeight;
            if (width <= DisplayWidth && height <= DisplayHeight)
                bFits = true;

            // resize image

            if (extName.toUpperCase().compareTo("JPG") == 0 && bFits) {
                final File file = new File(imgDecodableString);
                long len = file.length();
                int calcCrc = (int)0x0;
                int numPackets = (int)(len/512);
                int remainder =(int) (len % 512);
                if ( remainder > 0) {
                    numPackets++;
                }

                long startTime = System.nanoTime();
                calcCrc = STM32CRC.GenerateZipCrcFast(file);
                startTime = System.nanoTime() - startTime;

                Log.d(TAG, String.format("File ZIPCRC: %08X", calcCrc));
                Log.d(TAG, String.format("ZIPCRC Calc time: %d ns", startTime));

                startTime = System.nanoTime();


                qUSBSenderLock.lock();

                /* send data */
                int packet = 0;
                try {
                    final long fileLength = file.length();
                    FileInputStream inputStream = new FileInputStream(file);
                    ByteBuffer temp;
                    FileChannel fileChannel = inputStream.getChannel();
                    int filelen = (int) fileChannel.size();
                    MappedByteBuffer buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, filelen);
                    Log.d(TAG, "Sending data length " + fileLength);

                    if (usbService != null) { // if UsbService was correctly binded, Send data

                        int gd = 500;
                        int snd_len = 0;
                        byte[] arr;

                        while (buffer.hasRemaining()) {
                            if(buffer.remaining()  == filelen) {
                                if (buffer.remaining() > 500){
                                    snd_len = 512;
                                    gd = 500;
                                }else{
                                    gd =  buffer.remaining();
                                    snd_len = gd + 12;
                                }

                                temp = ByteBuffer.allocate(snd_len);
                                temp.put(0, (byte)0xCA);    /* start byte */
                                temp.put(1, (byte)0xFF);    /* control flag */
                                temp.order(ByteOrder.LITTLE_ENDIAN).putShort(2,(short)gd);
                                temp.order(ByteOrder.LITTLE_ENDIAN).putInt(4,filelen);
                                temp.order(ByteOrder.LITTLE_ENDIAN).putInt(8,calcCrc);
                                temp.position(12);

                            }else {
                                if (buffer.remaining() > 512){
                                    snd_len = 512;
                                    gd = 512;
                                }else{
                                    gd =  buffer.remaining();
                                    snd_len = gd;
                                }

                                temp = ByteBuffer.allocate(snd_len);
                                temp.position(0);
                            }

                            arr = new byte[gd];
                            buffer.get(arr, 0, gd);

                            temp.put(arr);
                            // Log.d(TAG, "====>  " + packet + " <====");

                            // calculate crc
                            flag = false;
                            usbService.write(temp.array());
                            //for(int x=0 ; x<5; x++){} // very slight delay

                            packet++;
                            temp.clear();
                            temp = null;

                        }

                    }
                } catch (Exception ioe) {
                    Log.e(TAG, "Error sendHudPacket: " + ioe.getMessage());
                }

                /* send JPEG END data */
                if (usbService != null) {
                    //usbService.write(stopPacket.getBytes());
                }
                qUSBSenderLock.unlock();

                startTime = System.nanoTime() - startTime;
                Log.d(TAG, String.format("Total Frame Transmit Time: %d", startTime));
                Toast.makeText(MainActivity.this,String.format("Packets: 0x%02X", packet), Toast.LENGTH_SHORT).show();
                Toast.makeText(MainActivity.this,String.format("Xfer Time: %d ms", startTime/1000/1000), Toast.LENGTH_SHORT).show();
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        display.setImageBitmap(bitmap);
                    }
                });
            }else if(extName.toUpperCase().compareTo("BMP") == 0 && bFits){
                Log.e(TAG, "BMP Image is not supported");
                Toast.makeText(MainActivity.this, "BMP Image is not supported", Toast.LENGTH_LONG).show();

            }
            else{
                // send error
                Log.e(TAG, "Image is not a JPG or is larger than display");
                Toast.makeText(MainActivity.this, "Invalid format for device", Toast.LENGTH_LONG).show();
            }

        }else{
            Log.d(TAG, "Unknown Result or Cancelled");
        }
    }


    /*
     * This handler will be passed to UsbService. Data received from serial port is displayed through this handler
     */
    private static class MyHandler extends Handler {
        private final WeakReference<MainActivity> mActivity;

        public MyHandler(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UsbService.MESSAGE_FROM_SERIAL_PORT:
                    String data = (String) msg.obj;
                    if(data.length() == 1){
                        if(data.toCharArray()[0] == 0x00){
                            /* success */
                            flag = true;
                        }else{
                            /* error */
                            Log.d(TAG, "received data");

                        }
                    }else if(data.length() > 1){
                        Log.d(TAG, "received data " + data);
                    }

                    break;
                case UsbService.CTS_CHANGE:
                    Toast.makeText(mActivity.get(), "CTS_CHANGE",Toast.LENGTH_LONG).show();
                    break;
                case UsbService.DSR_CHANGE:
                    Toast.makeText(mActivity.get(), "DSR_CHANGE",Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }


}