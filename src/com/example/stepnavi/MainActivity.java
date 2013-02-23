package com.example.stepnavi;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity implements SensorEventListener {

	private long begin;
	private ArrayList<Long> times;
	private ArrayList<Float> data;
	private boolean isRecording = false;
	private ToggleButton toggle;
	private SensorManager sensorManager;
	private Sensor accelerometer;
	private Sensor linear;
	private Sensor magnetic;
	
	// okay, gányolás
	private float accX = 0;
	private float accY = 0;
	private float accZ = 0;
	private float linX = 0;
	private float linY = 0;
	private float linZ = 0;
	private float magX = 0;
	private float magY = 0;
	private float magZ = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		times = new ArrayList<Long>();
		data = new ArrayList<Float>();
		
		toggle = (ToggleButton) findViewById(R.id.toggleButton1);
		toggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked)
				{
					times.clear();
					data.clear();
					isRecording = true;
					begin = System.currentTimeMillis();
				}
				else
				{
					writeData();
					isRecording = false;
				}
			}
		});
		
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		List<Sensor>  list = sensorManager.getSensorList(Sensor.TYPE_ALL);
		accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		linear = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
		magnetic = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
	}
	
	private void writeData()
	{
        try {
            File myFile = new File(Environment.getExternalStorageDirectory().getPath()+"/acclog_"+System.currentTimeMillis()+".txt");
            myFile.createNewFile();
            BufferedWriter bw = new BufferedWriter(new FileWriter(myFile));
            for (int i=0; i<times.size(); i++)
            {
            	bw.append(times.get(i).toString());
            	bw.append(";");
            	
            	for (int j=0; j<8; j++)
            	{
	            	bw.append(data.get(9*i+j).toString());
	            	bw.append(";");
            	}
            	bw.append(data.get(9*i+8).toString());
            	bw.newLine();
            }
            
            bw.close();
            Toast.makeText(getApplicationContext(),"Done writing SD", Toast.LENGTH_SHORT).show();
        } 
        catch (Exception e) 
        {
            Toast.makeText(getApplicationContext(), e.getMessage(),Toast.LENGTH_SHORT).show();
        }
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (isRecording == true)
		{
			boolean good = false;
			if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
				accX = event.values[0];
				accY = event.values[1];
				accZ = event.values[2];
				good = true;
			} else if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION)
			{
				linX = event.values[0];
				linY = event.values[1];
				linZ = event.values[2];
				good = true;
			} else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
			{
				magX = event.values[0];
				magY = event.values[1];
				magZ = event.values[2];
				good = true;
			}
			
			if (good == true)
			{
				data.add(accX);
				data.add(accY);
				data.add(accZ);
				data.add(linX);
				data.add(linY);
				data.add(linZ);
				data.add(magX);
				data.add(magY);
				data.add(magZ);
				
				times.add(System.currentTimeMillis()-begin);
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		sensorManager.unregisterListener(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
		sensorManager.registerListener(this, linear, SensorManager.SENSOR_DELAY_FASTEST);
		sensorManager.registerListener(this, magnetic, SensorManager.SENSOR_DELAY_FASTEST);
	}
}
