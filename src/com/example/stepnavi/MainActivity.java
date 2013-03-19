package com.example.stepnavi;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
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

import com.example.stepnavi.filters.ADKFilter;
import com.example.stepnavi.filters.LowPassFilterMulti;
import com.example.stepnavi.filters.ValidDataFilterMulti;

public class MainActivity extends Activity implements SensorEventListener {

	private TextView tv1, tv2, tv3;
	
	private long begin;
	private ArrayList<Long> times;
	private ArrayList<Double> data;
	private ArrayList<Double> velDataX;
	private ArrayList<Double> velDataXsm;
	private ArrayList<Double> velDataY;
	private ArrayList<Double> velDataYsm;
	private ArrayList<Double> velDataZ;
	private ArrayList<Double> velDataZsm;
	private boolean isRecording = false;
	private ToggleButton toggle;
	private SensorManager sensorManager;
	private Sensor magneto;
	private Sensor accelero;
	private Sensor gyro;
	private static final double SAMPLE_FREQ = 25.0;
	private static final double TIMING_CORRECTION = SAMPLE_FREQ / 25.0;
	private MadgwickAHRS madgwick;
	private UpdaterThread thread;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		times = new ArrayList<Long>();
		data = new ArrayList<Double>();
		velDataX = new ArrayList<Double>();
		velDataY = new ArrayList<Double>();
		velDataZ = new ArrayList<Double>();
		velDataXsm = new ArrayList<Double>();
		velDataYsm = new ArrayList<Double>();
		velDataZsm = new ArrayList<Double>();
		
		tv1 = (TextView) findViewById(R.id.textView1);
		tv2 = (TextView) findViewById(R.id.textView2);
		tv3 = (TextView) findViewById(R.id.textView3);
		
		tv1.setTypeface(Typeface.MONOSPACE);
		tv2.setTypeface(Typeface.MONOSPACE);
		tv3.setTypeface(Typeface.MONOSPACE);
		
		toggle = (ToggleButton) findViewById(R.id.toggleButton1);
		toggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked)
				{
					times.clear();
					data.clear();
					velDataX.clear();
					velDataY.clear();
					velDataZ.clear();
					velDataXsm.clear();
					velDataYsm.clear();
					velDataZsm.clear();
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

            	/*
            	for (int j=0; j<NUM-1; j++)
            	{
	            	bw.append(data.get(NUM*i+j).toString());
	            	bw.append(";");
            	}
            	bw.append(data.get(NUM*i+NUM-1).toString());
            	*/
            	
            	bw.append(velDataX.get(i).toString());
            	bw.append(";");
            	bw.append(velDataY.get(i).toString());
            	bw.append(";");
            	bw.append(velDataZ.get(i).toString());
            	bw.append(";");
            	bw.append(velDataXsm.get(i).toString());
            	bw.append(";");
            	bw.append(velDataYsm.get(i).toString());
            	bw.append(";");
            	bw.append(velDataZsm.get(i).toString());
            	
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

	public Object sync = new Object();
	private double[] mAcc = null;
	private double[] mGeo = null;
	private double[] mGyro = null;
	private double[] mLin = new double[3];
	
	private double[] mAngles = new double[3];
	private double[] mLinear = new double[4];
	private double[] mLinearWorld = new double[4];
	
	private ValidDataFilterMulti mFilterMagnetic = new ValidDataFilterMulti(3);
	private ValidDataFilterMulti mFilterGyroscope = new ValidDataFilterMulti(3);
	private ValidDataFilterMulti mFilterAcceleration = new ValidDataFilterMulti(3);
	private ValidDataFilterMulti mFilterLinear = new ValidDataFilterMulti(3);
	
	private double movingAvg = 0;
	private LowPassFilterMulti mFilterLinearLowPass = new LowPassFilterMulti(3);
	private LowPassFilterMulti mFilterAcceleroLowPass = new LowPassFilterMulti(3);
	private LowPassFilterMulti mFilterMagneticLowPass = new LowPassFilterMulti(3);
	private LowPassFilterMulti mFilterGyroLowPass = new LowPassFilterMulti(3);
	
	private ADKFilter mADKX = new ADKFilter(7, 0.1, 0.1, 0.1);
	private ADKFilter mADKY = new ADKFilter(7, 0.1, 0.1, 0.1);
	private ADKFilter mADKZ = new ADKFilter(7, 0.1, 0.1, 0.1);
	
	@Override
	public void onSensorChanged(SensorEvent event) {
		
	    if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
	        	return;
	    }
	    
	    double[] values = new double[3];
	    values[0] = event.values[0];
	    values[1] = event.values[1];
	    values[2] = event.values[2];
		
		switch (event.sensor.getType()) {
		case Sensor.TYPE_ACCELEROMETER:
			// save acceleration as mAcc
			synchronized (sync)
			{
				mAcc = mFilterAcceleration.filter(values, 0.2);
				mAcc = mFilterAcceleroLowPass.filter(values, 5.0 * TIMING_CORRECTION);
				
				// calculate moving average as mLin
				mLin[0] = (movingAvg/(movingAvg+1)) *mLin[0] + (1/(movingAvg+1))*mAcc[0];
				mLin[1] = (movingAvg/(movingAvg+1)) *mLin[1] + (1/(movingAvg+1))*mAcc[1];
				mLin[2] = (movingAvg/(movingAvg+1)) *mLin[2] + (1/(movingAvg+1))*mAcc[2];
				movingAvg++;	
			}
			
			break;
		case Sensor.TYPE_GYROSCOPE:
			synchronized (sync)
			{
				if (mGyro == null)
				{
					mGyro = mFilterGyroscope.filter(values, 0.2f);
					//mGyro = mFilterGyroLowPass.filter(mGyro, 3.0f);
				}
			}
			break;
		case Sensor.TYPE_MAGNETIC_FIELD:
			synchronized (sync)
			{
				if (mGeo == null){
					mGeo = mFilterMagnetic.filter(values, 0.2f);
					//mGeo = mFilterMagneticLowPass.filter(mGeo, 5.0f);
				}
			}
			break;
		default:
			break;
		}
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
	    	// An effort to make timing more precise
	    	int sleepCorrection = 0;
	    	// Do the work!
	        while (!mFinished) {
	            try {
	                if (mActivity != null) {
        	        	 // try to calc new values, as soon as they are available
	                	 while (!calculate_AHRS())
        	        	 {
        	        		try {
        	        			sleepCorrection++;
								Thread.sleep(0, 1000);
							} catch (InterruptedException ignored) {}
        	        	 }
	                	// show values to user
	       	        	 mActivity.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									tv1.setText("X:  " + (mAngles[0]<0?"":" ") + new DecimalFormat("000").format(mAngles[0]/3.14*180) + 
											  " Y:  " + (mAngles[1]<0?"":" ") + new DecimalFormat("000").format(mAngles[1]/3.14*180) + 
											  " Z:  " + (mAngles[2]<0?"":" ") + new DecimalFormat("000").format(mAngles[2]/3.14*180));	
									tv2.setText("X:" + (mLinearWorld[0]<0?"":" ") + new DecimalFormat("0.000").format(mLinearWorld[0]) + 
											  " Y:" + (mLinearWorld[1]<0?"":" ") + new DecimalFormat("0.000").format(mLinearWorld[1]) + 
											  " Z:" + (mLinearWorld[2]<0?"":" ") + new DecimalFormat("0.000").format(mLinearWorld[2]));	
								}
							});
	                }
	                // Sleep (corrected time)
	                Thread.sleep(Math.max(PERIOD - sleepCorrection,0));
	                sleepCorrection = 0;
	            } catch (InterruptedException ignored) {}
	        }
	    }

	    public void quit() {
	        mFinished = true;
	    }
	}
	
	public boolean calculate_AHRS()
	{
		double[] angles = null;
		boolean res = false;
		synchronized (sync)
		{
			if ((mAcc != null) && (mGeo != null) && (mGyro != null) && (mLin != null))
			{
				madgwick.MadgwickAHRSupdate(mGyro[0], mGyro[1], mGyro[2],
											mAcc[0], mAcc[1], mAcc[2],
											mGeo[0], mGeo[1], mGeo[2]);
				angles = madgwick.getEulerAngles();
				mAngles = angles;
				
				// res
				res = true;
			
				// Calc Moving
				
				//mLin = mFilterLinearLowPass.filter(mLin, 2.0f);
				//mLin = mFilterLinearHighPass.filter(mLin, 0.1f);	
				//mLin = mFilterLinearLowPass2.filter(mLin, 10.0f);
				
				//mLinear[0] = mLin[0];
				//mLinear[1] = mLin[1];
				//mLinear[2] = mLin[2];
				//mLinear[3] = 0.0f;
						
				mLin = mFilterLinearLowPass.filter(mLin, 2.0 * TIMING_CORRECTION);
				
				double[] rotMatrix = madgwick.getMatrix4(); 
	
				// float castings
				float[] tlin = new float[4];
				tlin[0] = (float) mLin[0];
				tlin[1] = (float) mLin[1];
				tlin[2] = (float) mLin[2];
				tlin[3] = 0.0f;
				float[] tWorld = new float[4];
				float[] rot = new float[16];
				for (int i=0; i<16; i++)
				{
					rot[i] = (float) rotMatrix[i];
				}
				
				// rotate!
				android.opengl.Matrix.multiplyMV(tWorld, 0, rot, 0, tlin, 0);
				
				// back casting
				for (int i=0; i<4; i++)
				{
					mLinearWorld[i] = tWorld[i];
				}
				
				// Record
				if (isRecording == true)
				{
					//data.add(mAcc[0]);
					//data.add(mAcc[1]);
					//data.add(mAcc[2]);
					
					//data.add(mLinearWorld[0]);
					//data.add(mLinearWorld[1]);
					//data.add(mLinearWorld[2]);
					
					// Integrate into veloc buff
					if (velDataX.size() <= 1) {
						velDataX.add(mLinearWorld[0]);
						velDataXsm.add(0.0);
						velDataY.add(mLinearWorld[1]);
						velDataYsm.add(0.0);
						velDataZ.add(mLinearWorld[2]);
						velDataZsm.add(0.0);
					}
					else
					{
						velDataX.add(velDataX.get(velDataX.size()-1) + mLinearWorld[0]);
						velDataXsm.add(0.0);
						velDataY.add(velDataY.get(velDataY.size()-1) + mLinearWorld[1]);
						velDataYsm.add(0.0);
						velDataZ.add(velDataZ.get(velDataZ.size()-1) + mLinearWorld[2]);
						velDataZsm.add(0.0);
					}
					
					mADKX.tryFilter(velDataX, velDataXsm);
					mADKY.tryFilter(velDataY, velDataYsm);
					mADKZ.tryFilter(velDataZ, velDataZsm);
					
					//data.add(mAngles[0]);
					//data.add(mAngles[1]);
					//data.add(mAngles[2]);
		
					times.add(System.currentTimeMillis()-begin);
				}
				
				// next round waits until all components are new
				mAcc = null;
				mGeo = null;
				mGyro = null;
				mLin = new double[3];
				movingAvg = 0;
			}
		}
		return res;
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
		sensorManager.registerListener(this, gyro, SensorManager.SENSOR_DELAY_FASTEST);
		sensorManager.registerListener(this, magneto, SensorManager.SENSOR_DELAY_FASTEST);
	}
}
