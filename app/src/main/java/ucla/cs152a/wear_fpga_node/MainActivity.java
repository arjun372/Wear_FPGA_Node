package ucla.cs152a.wear_fpga_node;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class MainActivity extends Activity implements SensorEventListener{

    private static final SimpleDateFormat AMBIENT_DATE_FORMAT = new SimpleDateFormat("HH:mm", Locale.US);
    private static final String FPGA_MAC = "60:03:08:90:69:35";
    private BoxInsetLayout mContainerView;
    private TextView mTextView;
    private TextView mClockView;
    private static BluetoothSocket serialSocket = null;
    private static int BT_STATUS = -1;
    private static OutputStream outputStream = null;
    private static InputStream  inputStream  = null;
    private static SensorManager mSensorManager = null;
    private static final int SAMPLE_RATE = 10;
    private static float[] gyroData = new float[3];
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContainerView = (BoxInsetLayout) findViewById(R.id.container);
        mTextView = (TextView) findViewById(R.id.text);
        mClockView = (TextView) findViewById(R.id.clock);
    }

    @Override
    public void onPause(){
        super.onPause();

        if(BT_STATUS == 2)
            setSensors(false);

        try {
            outputStream.close();
            inputStream.close();
        } catch (IOException e) {
           //
        }

        while(serialSocket.isConnected())
        {
            try {
                serialSocket.close();
                BT_STATUS = 0;
                outputStream = null;
                inputStream = null;
            } catch (IOException e) {
                BT_STATUS =1 ;
            }
            Log.d("s",""+BT_STATUS);
        }
    }

    @Override
    public void onResume(){
        super.onResume();final
        // Pair with FPGA if not paired already and open socket;
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();
        BluetoothDevice FPGA = mBluetoothAdapter.getRemoteDevice(FPGA_MAC);

        try {
            serialSocket = FPGA.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            BT_STATUS = 0;
            Log.d("s",""+BT_STATUS);
        }
        catch (IOException e) {
            serialSocket = null;
            BT_STATUS = -1;
        }

        // begin connection attempt, retry every 0.5s if not connected
        while (BT_STATUS < 1 && BT_STATUS > -1) {
            try {
                serialSocket.connect();
                BT_STATUS = 1;
            } catch (Exception e) {
                SystemClock.sleep(500);
                BT_STATUS = 0;
            }
            Log.d("s",""+BT_STATUS);
        }

        //if connected, try to open a stream, retry every 0.5 not yet opened
        while (serialSocket != null && serialSocket.isConnected() && BT_STATUS <=1 ) {
            try {
                outputStream = serialSocket.getOutputStream();
                inputStream  = serialSocket.getInputStream();
                BT_STATUS = 2;
            } catch (Exception e) {
                SystemClock.sleep(500);
                BT_STATUS = 1;
                outputStream = null;
                inputStream = null;
            }
            Log.d("s",""+BT_STATUS);
        }

        if(BT_STATUS == 2)
            setSensors(true);
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case (Sensor.TYPE_ACCELEROMETER):
                    try {
                        outputStream.write((event.values[0] + "," + event.values[1] + "," + event.values[2] + "," + gyroData[0] + "," + gyroData[1]+","+gyroData[2]+"\n").getBytes());
                        outputStream.flush();
                    } catch (IOException e) {

                    }
                break;

            case (Sensor.TYPE_GYROSCOPE):
                gyroData[0] = event.values[0];
                gyroData[1] = event.values[1];
                gyroData[2] = event.values[2];
                break;

            case(Sensor.TYPE_ROTATION_VECTOR):
                try {
                    outputStream.write((event.values[0] + "," + event.values[1] + "," + event.values[2] + "," + event.values[3] + "\n").getBytes());
                    outputStream.flush();
                } catch (IOException e) {}
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void setSensors(boolean STATE){

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        if(!STATE){
            mSensorManager.unregisterListener(this);
            mSensorManager.flush(this);
            return;
        }
        // Initializing the sensors

        final Sensor mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        final Sensor mGyroscope     = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        final Sensor mRotation      = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        // Start recording data
       // mSensorManager.registerListener(this, mAccelerometer, (1000000 / SAMPLE_RATE));
        //mSensorManager.registerListener(this, mGyroscope, (1000000 / SAMPLE_RATE));
        mSensorManager.registerListener(this, mRotation, (1000000 / SAMPLE_RATE));
    }
}
