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
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity implements SensorEventListener {

	private TextView tv;
	
	private long begin;
	private ArrayList<Long> times;
	private ArrayList<Float> data;
	private boolean isRecording = false;
	private ToggleButton toggle;
	private SensorManager sensorManager;
	//private Sensor linear;
	private Sensor gravity;
	private Sensor magnetic;
	private Sensor accelerometer;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		times = new ArrayList<Long>();
		data = new ArrayList<Float>();
		
		tv = (TextView) findViewById(R.id.textView1);
		
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
		//linear = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
		//orientation = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
		accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		//linear = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
		gravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
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
            	
            	for (int j=0; j<5; j++)
            	{
	            	bw.append(data.get(6*i+j).toString());
	            	bw.append(";");
            	}
            	bw.append(data.get(6*i+5).toString());
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

	private float[] mGravity = new float[3];
	private float[] mGeomagnetic = new float[3];
	private float[] mRotationMatrixA = new float[16];
	private float[] mRotationMatrixB = new float[16];
	private float[] mRotationMatrix = new float[16];
	private float[] mRotationMatrixInv = new float[16];
	private float[] mAcceleration = new float[3];
	private float[] mAngles = new float[3];
	private float[] mLinear = new float[4];
	private float[] mLinearWorld = new float[4];
	private boolean ready = false;
	private MagicLowPassFilterMulti mFilterGravity = new MagicLowPassFilterMulti(3);
	private MagicLowPassFilterMulti mFilterMagnetic = new MagicLowPassFilterMulti(3);
	//private MagicLowPassFilterMulti mFilterLinear = new MagicLowPassFilterMulti(4);
	private MagicLowPassFilterMulti mFilterAcceleration = new MagicLowPassFilterMulti(3);
	
	@Override
	public void onSensorChanged(SensorEvent event) {
		
		
		// TODO: Ez pedig kéne
	    if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
	        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
	        	return;
	    }
	    
		

		//Calculate orientation
		if ((event.sensor.getType() == Sensor.TYPE_GRAVITY) || (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD))
		{
		    if (event.sensor.getType() == Sensor.TYPE_GRAVITY)  
		    	mGravity = mFilterGravity.filter(event.values, 0.2f);
		    if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) 
		    	mGeomagnetic =  mFilterMagnetic.filter(event.values, 0.2f);
	
		    if (mGravity != null && mGeomagnetic != null) {
	
		        float[] rotationMatrixA = mRotationMatrixA;
		        if (SensorManager.getRotationMatrix(rotationMatrixA, null, mGravity, mGeomagnetic)) {
		        	mRotationMatrix = rotationMatrixA;
		            ready = true;
		        	float[] rotationMatrixB = mRotationMatrixB;
		            SensorManager.remapCoordinateSystem(rotationMatrixA,
		                    SensorManager.AXIS_X, SensorManager.AXIS_Z,
		                    rotationMatrixB);
		            SensorManager.getOrientation(rotationMatrixB, mAngles);
		        }
		        tv.setText("X: " + String.valueOf((int)(mAngles[0]/3.14*180)) + " Y: " + String.valueOf((int)(mAngles[1]/3.14*180)) + " Z: " + String.valueOf((int)(mAngles[2]/3.14*180)));    
		    }
		}
		
		// Calc Moving
		
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
		{
			mAcceleration = event.values;
			
			if (ready == true)
			{
				mLinear[0] = mAcceleration[0];
				mLinear[1] = mAcceleration[1];
				mLinear[2] = mAcceleration[2];
				mLinear[3] = 0.0f;
				android.opengl.Matrix.invertM(mRotationMatrixInv, 0, mRotationMatrix, 0);
				android.opengl.Matrix.multiplyMV(mLinearWorld, 0, mRotationMatrixInv, 0, mLinear, 0);
			}
		}
		
		
		// Record
		if (isRecording == true)
		{
			data.add(mLinear[0]);
			data.add(mLinear[1]);
			data.add(mLinear[2]);
			data.add(mAngles[0]);
			data.add(mAngles[1]);
			data.add(mAngles[2]);

			times.add(System.currentTimeMillis()-begin);
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
		//sensorManager.registerListener(this, orientation, SensorManager.SENSOR_DELAY_FASTEST);
		//sensorManager.registerListener(this, linear, SensorManager.SENSOR_DELAY_FASTEST);
		sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
		//sensorManager.registerListener(this, linear, SensorManager.SENSOR_DELAY_FASTEST);
		sensorManager.registerListener(this, gravity, SensorManager.SENSOR_DELAY_FASTEST);
		sensorManager.registerListener(this, magnetic, SensorManager.SENSOR_DELAY_FASTEST);
	}
}
