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
	//private Sensor gravity;
	private Sensor magneto;
	private Sensor accelero;
	private Sensor gyro;
	private static final float SAMPLE_FREQ = 10.0f;
	private MadgwickAHRS madgwick;
	private UpdaterThread thread;
	
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
		accelero = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		magneto = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		
		thread = new UpdaterThread(this);
		madgwick = new MadgwickAHRS();
		madgwick.setSampleFreq(SAMPLE_FREQ);
		thread.start();
	}
	
	public static final int NUM = 3;
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
            	
            	for (int j=0; j<NUM-1; j++)
            	{
	            	bw.append(data.get(NUM*i+j).toString());
	            	bw.append(";");
            	}
            	bw.append(data.get(NUM*i+NUM-1).toString());
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

	private float[] mAcc = null;
	private float[] mGeo = null;
	private float[] mGyro = null;
	//private float[] mRotationMatrixA = new float[16];
	//private float[] mRotationMatrixB = new float[16];
	//private float[] mRotationMatrix = new float[16];
	//private float[] mRotationMatrixInv = new float[16];
	//private float[] mAcceleration = new float[3];
	private float[] mAngles = new float[3];
	//private float[] mLinear = new float[4];
	//private float[] mLinearWorld = new float[4];
	//private boolean ready = false;
	//private MagicLowPassFilterMulti mFilterGravity = new MagicLowPassFilterMulti(3);
	//private MagicLowPassFilterMulti mFilterMagnetic = new MagicLowPassFilterMulti(3);
	//private MagicLowPassFilterMulti mFilterLinear = new MagicLowPassFilterMulti(4);
	//private MagicLowPassFilterMulti mFilterAcceleration = new MagicLowPassFilterMulti(3);
	
	@Override
	public void onSensorChanged(SensorEvent event) {
		
		// TODO: Ez pedig kene
		
	    if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
	        //if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
	        	return;
	    }
	    
	    
		//Calculate orientation
		/*
		if ((event.sensor.getType() == Sensor.TYPE_GRAVITY) || (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD))
		{
		    if (event.sensor.getType() == Sensor.TYPE_GRAVITY)  
		    	mGravity = mFilterGravity.filter(event.values, 0.2f);
		    if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) 
		    	mGeomagnetic =  mFilterMagnetic.filter(event.values, 0.2f);
	
		    if (mGravity != null && mGeomagnetic != null) {
		        tv.setText("X: " + String.valueOf((int)(mAngles[0]/3.14*180)) + " Y: " + String.valueOf((int)(mAngles[1]/3.14*180)) + " Z: " + String.valueOf((int)(mAngles[2]/3.14*180)));    
		    }
		}
		*/
		
		switch (event.sensor.getType()) {
		case Sensor.TYPE_ACCELEROMETER:
			mAcc = event.values;
			break;
		case Sensor.TYPE_GYROSCOPE:
			mGyro = event.values;
			break;
		case Sensor.TYPE_MAGNETIC_FIELD:
			mGeo = event.values;
			break;

		default:
			break;
		}
		
		// Calc Moving
		/*
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
		*/
	}
	
	class UpdaterThread extends Thread {
	    /** Frequency / sleep duration in milliseconds */
	    public static final int PERIOD = (int)(1000.0f/SAMPLE_FREQ);      
	    private volatile boolean mFinished;     
	    private Activity mActivity;

	    public UpdaterThread(Activity activity) {
	        mActivity = activity;
	    }

	    public void run() {
	        while (!mFinished) {
	            try {
	                if (mActivity != null) {
	                    // Your method.
	                    mActivity.runOnUiThread(
	                    		new Runnable() { 
	                    	         public void run() { 
	                    	        	 calculate_AHRS();
	                    	         } 
	                    		}
	                    );
	                }
	                Thread.sleep(PERIOD);
	            } catch (InterruptedException ignored) {}
	        }
	    }

	    public void quit() {
	        mFinished = true;
	    }
	}
	
	public void calculate_AHRS()
	{
		float[] angles = null;
		if ((mAcc != null) && (mGeo != null) && (mGyro != null))
		{
			madgwick.MadgwickAHRSupdate(mGyro[0], mGyro[1], mGyro[2],
										mAcc[0], mAcc[1], mAcc[2],
										mGeo[0], mGeo[1], mGeo[2]);
			angles = madgwick.getEulerAngles();
		}
		
		if (angles!=null)
		{
			// Record
			if (isRecording == true)
			{
				//data.add(mLinear[0]);
				//data.add(mLinear[1]);
				//data.add(mLinear[2]);
				data.add(mAngles[0]);
				data.add(mAngles[1]);
				data.add(mAngles[2]);
	
				times.add(System.currentTimeMillis()-begin);
			}
			
			tv.setText("X: " + String.valueOf((int)(angles[0]/3.14*180)) + " Y: " + String.valueOf((int)(angles[1]/3.14*180)) + " Z: " + String.valueOf((int)(angles[2]/3.14*180)));
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
		sensorManager.registerListener(this, accelero, SensorManager.SENSOR_DELAY_FASTEST);
		//sensorManager.registerListener(this, linear, SensorManager.SENSOR_DELAY_FASTEST);
		sensorManager.registerListener(this, gyro, SensorManager.SENSOR_DELAY_FASTEST);
		sensorManager.registerListener(this, magneto, SensorManager.SENSOR_DELAY_FASTEST);
	}
}
