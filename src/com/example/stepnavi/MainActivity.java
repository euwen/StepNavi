package com.example.stepnavi;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.ArrayList;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.example.stepnavi.filters.ADKFilter;
import com.example.stepnavi.filters.HighPassFilterMulti;
import com.example.stepnavi.filters.LowPassFilterMulti;
import com.example.stepnavi.filters.MedianFilterMulti;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortOut;

public class MainActivity extends Activity implements SensorEventListener,
		CvCameraViewListener {

	// Settings
	private static final float SAMPLE_FREQ = 40.0f;
	private static final float TIMING_CORRECTION = SAMPLE_FREQ / 25.0f;

	private TextView tv1, tv2, tv3, tv4;
	private long begin;
	private ArrayList<Long> times;
	private ArrayList<Float> data;
	private ArrayList<Float> velDataX;
	private ArrayList<Float> velDataXsm;
	private ArrayList<Float> velDataY;
	private ArrayList<Float> velDataYsm;
	private ArrayList<Float> velDataZ;
	private ArrayList<Float> velDataZsm;
	private boolean isRecording = false;
	private ToggleButton toggle;
	private ToggleButton toggleCalib;
	private SensorManager sensorManager;
	private Sensor magneto;
	private Sensor accelero;
	private Sensor gravity;
	private Sensor gyro;
	private MadgwickAHRS madgwick;
	private UpdaterThread thread;
	private float[] accCorr = null;

	private static OSCPortOut sender = null;

	private CameraBridgeViewBase mOpenCvCameraView = null;

	private TextView ipText;
	private Button buttonConnect;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.HelloOpenCvView);
		mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
		mOpenCvCameraView.setCvCameraViewListener(this);

		times = new ArrayList<Long>();
		data = new ArrayList<Float>();
		velDataX = new ArrayList<Float>();
		velDataY = new ArrayList<Float>();
		velDataZ = new ArrayList<Float>();
		velDataXsm = new ArrayList<Float>();
		velDataYsm = new ArrayList<Float>();
		velDataZsm = new ArrayList<Float>();

		tv1 = (TextView) findViewById(R.id.textView1);
		tv2 = (TextView) findViewById(R.id.textView2);
		tv3 = (TextView) findViewById(R.id.textView3);
		tv4 = (TextView) findViewById(R.id.textView4);

		tv1.setTypeface(Typeface.MONOSPACE);
		tv2.setTypeface(Typeface.MONOSPACE);
		tv3.setTypeface(Typeface.MONOSPACE);
		tv4.setTypeface(Typeface.MONOSPACE);

		ipText = (TextView) findViewById(R.id.ipText);
		buttonConnect = (Button) findViewById(R.id.buttonConnect);
		buttonConnect.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				try {
					sender = new OSCPortOut(InetAddress.getByName(ipText
							.getText().toString()), 2234);
				} catch (SocketException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

		toggle = (ToggleButton) findViewById(R.id.toggleButton1);
		toggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (isChecked) {
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
				} else {
					writeData();
					isRecording = false;
				}
			}
		});

		toggleCalib = (ToggleButton) findViewById(R.id.toggleButton2);
		toggleCalib.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (isChecked) {
					AcceleroCalibration.reset();
				} else {
					accCorr = AcceleroCalibration.getCorrections();

					mAcc = null;
					mGeo = null;
					mGyro = null;
					mLin = new float[3];
					movingAvg = 0;
					thread.start();
					toggleCalib.setEnabled(false);
				}
			}
		});

		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		accelero = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		gravity = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
		gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		magneto = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

		thread = new UpdaterThread(this);
		madgwick = new MadgwickAHRS();
		madgwick.setSampleFreq(SAMPLE_FREQ);
	}

	public static final int NUM = 15;

	private void writeData() {
		try {
			File myFile = new File(Environment.getExternalStorageDirectory()
					.getPath()
					+ "/acclog_"
					+ System.currentTimeMillis()
					+ ".csv");
			myFile.createNewFile();
			BufferedWriter bw = new BufferedWriter(new FileWriter(myFile));
			for (int i = 0; i < times.size(); i++) {
				bw.append(times.get(i).toString());
				bw.append(";");

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
				bw.append(";");

				for (int j = 0; j < NUM - 1; j++) {
					bw.append(data.get(NUM * i + j).toString());
					bw.append(";");
				}
				bw.append(data.get(NUM * i + NUM - 1).toString());

				bw.newLine();
			}

			bw.close();
			Toast.makeText(getApplicationContext(), "Done writing SD",
					Toast.LENGTH_SHORT).show();
		} catch (Exception e) {
			Toast.makeText(getApplicationContext(), e.getMessage(),
					Toast.LENGTH_SHORT).show();
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
	private float[] mAcc = null;
	private float[] mGra = null;
	private float[] mGeo = null;
	private float[] mGyro = null;
	private float[] mLin = new float[3];

	private float[] mAnglesA = new float[3];
	private float[] mQuaternionA = new float[4];
	private float[] mAnglesM = new float[3];
	private float[] mLinear = new float[4];
	private float[] mLinearWorldM = new float[4];
	private float[] mLinearWorldA = new float[4];

	private MedianFilterMulti mFilterGravity = new MedianFilterMulti(3, 3);
	private MedianFilterMulti mFilterMagnetic = new MedianFilterMulti(3, 3);
	private MedianFilterMulti mFilterGyroscope = new MedianFilterMulti(3, 3);
	private MedianFilterMulti mFilterAcceleration = new MedianFilterMulti(3, 3);

	private float movingAvg = 0;
	private HighPassFilterMulti mFilterAcceleroHighPass = new HighPassFilterMulti(
			3);
	private LowPassFilterMulti mFilterLinearLowPass = new LowPassFilterMulti(3);
	private LowPassFilterMulti mFilterAcceleroLowPass = new LowPassFilterMulti(
			3);
	private LowPassFilterMulti mFilterMagneticLowPass = new LowPassFilterMulti(
			3);
	private LowPassFilterMulti mFilterGyroLowPass = new LowPassFilterMulti(3);

	private ADKFilter mADKX = new ADKFilter(20, 0.12f, 0.1f, 0.2f);
	private ADKFilter mADKY = new ADKFilter(20, 0.12f, 0.1f, 0.2f);
	private ADKFilter mADKZ = new ADKFilter(20, 0.12f, 0.1f, 0.2f);

	private MedianFilterMulti mMedian = new MedianFilterMulti(3, 5);

	@Override
	public void onSensorChanged(SensorEvent event) {

		/*
		 * if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
		 * return; }
		 */

		float[] values = event.values;

		switch (event.sensor.getType()) {
		case Sensor.TYPE_ACCELEROMETER:
			// save acceleration as mAcc
			synchronized (sync) {
				if (toggleCalib.isChecked() == true) {
					AcceleroCalibration.addValues(values);
				} else if (accCorr != null) {
					mAcc = new float[3];
					mAcc[0] = values[0] - accCorr[0];
					mAcc[1] = values[1] - accCorr[1];
					// TODO: What about Z axis?
					mAcc[2] = values[2];

					mAcc = mFilterAcceleration.filter(mAcc);

					// calculate moving average as mLin
					mLin[0] = (movingAvg / (movingAvg + 1)) * mLin[0]
							+ (1 / (movingAvg + 1)) * mAcc[0];
					mLin[1] = (movingAvg / (movingAvg + 1)) * mLin[1]
							+ (1 / (movingAvg + 1)) * mAcc[1];
					mLin[2] = (movingAvg / (movingAvg + 1)) * mLin[2]
							+ (1 / (movingAvg + 1)) * mAcc[2];
					movingAvg++;
				}
			}

			break;
		case Sensor.TYPE_GRAVITY:
			synchronized (sync) {
				if (mGra == null) {
					mGra = mFilterGravity.filter(values);
				}
			}
			break;
		case Sensor.TYPE_GYROSCOPE:
			synchronized (sync) {
				if (mGyro == null) {
					mGyro = mFilterGyroscope.filter(values);
				}
			}
			break;
		case Sensor.TYPE_MAGNETIC_FIELD:
			synchronized (sync) {
				if (mGeo == null) {
					mGeo = mFilterMagnetic.filter(values);
				}
			}
			break;
		default:
			break;
		}
	}

	class UpdaterThread extends Thread {
		/** Frequency / sleep duration in milliseconds */
		public static final int PERIOD = (int) (1000.0f / SAMPLE_FREQ);
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
						while (!calculate_AHRS()) {
							try {
								sleepCorrection++;
								Thread.sleep(0, 1000);
							} catch (InterruptedException ignored) {
							}
						}
						// show values to user
						mActivity.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								tv1.setText("Madgwick X:  "
										+ (mAnglesM[0] < 0 ? "" : " ")
										+ new DecimalFormat("000")
												.format(mAnglesM[0] / 3.14159 * 180)
										+ " Y:  "
										+ (mAnglesM[1] < 0 ? "" : " ")
										+ new DecimalFormat("000")
												.format(mAnglesM[1] / 3.14159 * 180)
										+ " Z:  "
										+ (mAnglesM[2] < 0 ? "" : " ")
										+ new DecimalFormat("000")
												.format(mAnglesM[2] / 3.14159 * 180));
								tv2.setText("Android  X:  "
										+ (mAnglesA[0] < 0 ? "" : " ")
										+ new DecimalFormat("000")
												.format(mAnglesA[0] / 3.14159 * 180)
										+ " Y:  "
										+ (mAnglesA[1] < 0 ? "" : " ")
										+ new DecimalFormat("000")
												.format(mAnglesA[1] / 3.14159 * 180)
										+ " Z:  "
										+ (mAnglesA[2] < 0 ? "" : " ")
										+ new DecimalFormat("000")
												.format(mAnglesA[2] / 3.14159 * 180));
								tv3.setText("Madgwick X:"
										+ (mLinearWorldM[0] < 0 ? "" : " ")
										+ new DecimalFormat("0.000")
												.format(mLinearWorldM[0])
										+ " Y:"
										+ (mLinearWorldM[1] < 0 ? "" : " ")
										+ new DecimalFormat("0.000")
												.format(mLinearWorldM[1])
										+ " Z:"
										+ (mLinearWorldM[2] < 0 ? "" : " ")
										+ new DecimalFormat("0.000")
												.format(mLinearWorldM[2]));
								tv4.setText("Android  X:"
										+ (mLinearWorldA[0] < 0 ? "" : " ")
										+ new DecimalFormat("0.000")
												.format(mLinearWorldA[0])
										+ " Y:"
										+ (mLinearWorldA[1] < 0 ? "" : " ")
										+ new DecimalFormat("0.000")
												.format(mLinearWorldA[1])
										+ " Z:"
										+ (mLinearWorldA[2] < 0 ? "" : " ")
										+ new DecimalFormat("0.000")
												.format(mLinearWorldA[2]));
							}
						});
					}
					// Sleep (corrected time)
					Thread.sleep(Math.max(PERIOD - sleepCorrection, 0));
					sleepCorrection = 0;
				} catch (InterruptedException ignored) {
				}
			}
		}

		public void quit() {
			mFinished = true;
		}
	}

	private int magic = 0;

	public boolean calculate_AHRS() {
		magic++;
		if (magic >= (int) (1000.0f / SAMPLE_FREQ) * 5) {
			madgwick.setBeta(0.03f);
		}

		float[] angles = null;
		synchronized (sync) {
			// if ((mAcc != null) && (mGeo != null) && (mGyro != null) && (mLin
			// != null))
			if ((mAcc != null) && (mGeo != null) && (mLin != null)
					&& (mGra != null)) {
				// -----------------------------------------------------------------------
				// #1 way
				float[] rotMatrixA = new float[16];
				float[] rotMatrixAtemp = new float[16];
				SensorManager.getRotationMatrix(rotMatrixAtemp, null, mAcc,
						mGeo);
				// SensorManager.remapCoordinateSystem(rotMatrixAtemp,
				// SensorManager.AXIS_X, SensorManager.AXIS_Y, rotMatrixA);
				mQuaternionA = getQuaternionFromMatrix(rotMatrixAtemp);
				// SensorManager.getOrientation(rotMatrixA, mAnglesA);
				SensorManager.getOrientation(rotMatrixAtemp, mAnglesA);

				// -----------------------------------------------------------------------
				// #2 way
				float[] rotMatrixM = null;
				if (mGyro != null) {
					madgwick.MadgwickAHRSupdate(mGyro[0], mGyro[1], mGyro[2],
							mLin[0], mLin[1], mLin[2], mGeo[0], mGeo[1],
							mGeo[2]);
					angles = madgwick.getEulerAngles();
					mAnglesM = angles;
					rotMatrixM = madgwick.getMatrix4();
				}

				// -----------------------------------------------------------------------
				// Filter movement
				// mLin = mFilterAcceleroHighPass.filter(mLin, 30);
				mLin = mMedian.filter(mLin);

				// -----------------------------------------------------------------------
				// Transform acceleration vector
				mLinear[0] = mLin[0];
				mLinear[1] = mLin[1];
				mLinear[2] = mLin[2];
				mLinear[3] = 0.0f;
				// rotate!
				if (mGyro != null) {
					android.opengl.Matrix.multiplyMV(mLinearWorldM, 0,
							rotMatrixM, 0, mLinear, 0);
				}
				float[] rotMatrixAI = new float[16];
				android.opengl.Matrix.invertM(rotMatrixAI, 0, rotMatrixA, 0);
				android.opengl.Matrix.multiplyMV(mLinearWorldA, 0, rotMatrixAI,
						0, mLinear, 0);

				// -----------------------------------------------------------------------
				// Record
				if (isRecording == true) {
					data.add(mAcc[0]);
					data.add(mAcc[1]);
					data.add(mAcc[2]);

					data.add(mLin[0]);
					data.add(mLin[1]);
					data.add(mLin[2]);

					data.add(mLinearWorldM[0]);
					data.add(mLinearWorldM[1]);
					data.add(mLinearWorldM[2]);

					// Integrate into veloc buff
					if (velDataX.size() <= 1) {
						velDataX.add(mLinearWorldM[0]);
						velDataXsm.add(0.0f);
						velDataY.add(mLinearWorldM[1]);
						velDataYsm.add(0.0f);
						velDataZ.add(mLinearWorldM[2]);
						velDataZsm.add(0.0f);
					} else {
						velDataX.add(velDataX.get(velDataX.size() - 1)
								+ mLinearWorldM[0]);
						velDataXsm.add(0.0f);
						velDataY.add(velDataY.get(velDataY.size() - 1)
								+ mLinearWorldM[1]);
						velDataYsm.add(0.0f);
						velDataZ.add(velDataZ.get(velDataZ.size() - 1)
								+ mLinearWorldM[2]);
						velDataZsm.add(0.0f);
					}

					mADKX.tryFilter(velDataX, velDataXsm);
					mADKY.tryFilter(velDataY, velDataYsm);
					mADKZ.tryFilter(velDataZ, velDataZsm);

					data.add(mAnglesM[0]);
					data.add(mAnglesM[1]);
					data.add(mAnglesM[2]);

					data.add(mAnglesA[0]);
					data.add(mAnglesA[1]);
					data.add(mAnglesA[2]);

					times.add(System.currentTimeMillis() - begin);
				}

				// -----------------------------------------------------------------------
				// Send to remote
				if (sender != null) {
					/*
					 * Object args[] = new Object[3]; args[0] =
					 * Float.valueOf(mAnglesA[0]); args[1] =
					 * Float.valueOf(mAnglesA[1]); args[2] =
					 * Float.valueOf(mAnglesA[2]); OSCMessage msg = new
					 * OSCMessage("/data/angles/A", args);
					 */

					Object args[] = new Object[4];
					args[0] = Float.valueOf(mQuaternionA[0]);
					args[1] = Float.valueOf(mQuaternionA[1]);
					args[2] = Float.valueOf(mQuaternionA[2]);
					args[3] = Float.valueOf(mQuaternionA[3]);
					OSCMessage msg = new OSCMessage("/data/quaternion/A", args);

					try {
						sender.send(msg);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

				// -----------------------------------------------------------------------
				// Clear
				// next round waits until all components are new
				mAcc = null;
				mGeo = null;
				mGyro = null;
				mGra = null;
				mLin = new float[3];
				movingAvg = 0;
			}
		}
		return true;
	}

	@Override
	protected void onPause() {
		super.onPause();
		sensorManager.unregisterListener(this);
		if (mOpenCvCameraView != null)
			mOpenCvCameraView.disableView();
	}

	@Override
	protected void onResume() {
		super.onResume();
		// sensorManager.registerListener(this, orientation,
		// SensorManager.SENSOR_DELAY_FASTEST);
		// sensorManager.registerListener(this, linear,
		// SensorManager.SENSOR_DELAY_FASTEST);
		sensorManager.registerListener(this, gravity,
				SensorManager.SENSOR_DELAY_FASTEST);
		sensorManager.registerListener(this, accelero,
				SensorManager.SENSOR_DELAY_FASTEST);
		sensorManager.registerListener(this, gyro,
				SensorManager.SENSOR_DELAY_FASTEST);
		sensorManager.registerListener(this, magneto,
				SensorManager.SENSOR_DELAY_FASTEST);
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, this,
				mLoaderCallback);
	}

	public static float[] getQuaternionFromMatrix(float[] m) {
		float[] q = new float[4];
		q[0] = (float) (Math.sqrt(Math.max(0, 1 + m[0] + m[5] + m[10])) / 2);
		q[1] = (float) (Math.sqrt(Math.max(0, 1 + m[0] - m[5] - m[10])) / 2);
		q[2] = (float) (Math.sqrt(Math.max(0, 1 - m[0] + m[5] - m[10])) / 2);
		q[3] = (float) (Math.sqrt(Math.max(0, 1 - m[0] - m[5] + m[10])) / 2);
		q[1] *= Math.signum(q[1] * (m[9] - m[6]));
		q[2] *= Math.signum(q[2] * (m[2] - m[8]));
		q[3] *= Math.signum(q[3] * (m[4] - m[1]));
		return q;
	}

	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS: {
				Log.i("", "OpenCV loaded successfully");
				mOpenCvCameraView.enableView();
				imageProcessor = new ImageProcessor();
			}
				break;
			default: {
				super.onManagerConnected(status);
			}
				break;
			}
		}
	};

	@Override
	public void onCameraViewStarted(int width, int height) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onCameraViewStopped() {
		// TODO Auto-generated method stub

	}

	private ImageProcessor imageProcessor = null;
	@Override
	public Mat onCameraFrame(Mat inputFrame) {
		if (imageProcessor != null){
			imageProcessor.process(inputFrame);
		}
		return inputFrame;
	}
}
