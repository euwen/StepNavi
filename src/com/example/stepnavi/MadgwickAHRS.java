package com.example.stepnavi;

//	Implementation of Madgwick's IMU and AHRS algorithms.
//	See: http://www.x-io.co.uk/node/8#open_source_ahrs_and_imu_algorithms
//
//	Date			Author          Notes
//	29/09/2011	SOH Madgwick    Initial release
//	02/10/2011	SOH Madgwick	Optimised for reduced CPU load

public class MadgwickAHRS {

	// freq
	private volatile float sampleFreq;
	// algorithm gain
	private volatile float beta;
	// quaternion of sensor frame relative to auxiliary frame
	private volatile float[] quaternion;		

	public MadgwickAHRS() 
	{
		sampleFreq = 20.0f;
		beta = 0.5f;
		quaternion = new float[4];
		quaternion[0] = 1.0f;
		quaternion[1] = 0.0f;
		quaternion[2] = 0.0f;
		quaternion[3] = 0.0f;
	}
	
	public void MadgwickAHRSupdate(float gx, float gy, float gz, float ax, float ay, float az, float mx, float my, float mz)
	{
		float recipNorm;
		float s0, s1, s2, s3;
		float qDot1, qDot2, qDot3, qDot4;
		float hx, hy;
		float _2q0mx, _2q0my, _2q0mz, _2q1mx, _2bx, _2bz, _4bx, _4bz, _2q0, _2q1, _2q2, _2q3, _2q0q2, _2q2q3, q0q0, q0q1, q0q2, q0q3, q1q1, q1q2, q1q3, q2q2, q2q3, q3q3;

		// Use IMU algorithm if magnetometer measurement invalid (avoids NaN in magnetometer normalisation)
		if((mx == 0.0f) && (my == 0.0f) && (mz == 0.0f)) {
			MadgwickAHRSupdateIMU(gx, gy, gz, ax, ay, az);
			return;
		}

		// Rate of change of quaternion from gyroscope
		qDot1 = 0.5f * (-quaternion[1] * gx - quaternion[2] * gy - quaternion[3] * gz);
		qDot2 = 0.5f * (quaternion[0] * gx + quaternion[2] * gz - quaternion[3] * gy);
		qDot3 = 0.5f * (quaternion[0] * gy - quaternion[1] * gz + quaternion[3] * gx);
		qDot4 = 0.5f * (quaternion[0] * gz + quaternion[1] * gy - quaternion[2] * gx);

		// Compute feedback only if accelerometer measurement valid (avoids NaN in accelerometer normalisation)
		if(!((ax == 0.0f) && (ay == 0.0f) && (az == 0.0f))) {

			// Normalise accelerometer measurement
			recipNorm = 1.0f / (float)Math.sqrt(ax * ax + ay * ay + az * az);
			ax *= recipNorm;
			ay *= recipNorm;
			az *= recipNorm;   

			// Normalise magnetometer measurement
			recipNorm = 1.0f / (float)Math.sqrt(mx * mx + my * my + mz * mz);
			mx *= recipNorm;
			my *= recipNorm;
			mz *= recipNorm;

			// Auxiliary variables to avoid repeated arithmetic
			_2q0mx = 2.0f * quaternion[0] * mx;
			_2q0my = 2.0f * quaternion[0] * my;
			_2q0mz = 2.0f * quaternion[0] * mz;
			_2q1mx = 2.0f * quaternion[1] * mx;
			_2q0 = 2.0f * quaternion[0];
			_2q1 = 2.0f * quaternion[1];
			_2q2 = 2.0f * quaternion[2];
			_2q3 = 2.0f * quaternion[3];
			_2q0q2 = 2.0f * quaternion[0] * quaternion[2];
			_2q2q3 = 2.0f * quaternion[2] * quaternion[3];
			q0q0 = quaternion[0] * quaternion[0];
			q0q1 = quaternion[0] * quaternion[1];
			q0q2 = quaternion[0] * quaternion[2];
			q0q3 = quaternion[0] * quaternion[3];
			q1q1 = quaternion[1] * quaternion[1];
			q1q2 = quaternion[1] * quaternion[2];
			q1q3 = quaternion[1] * quaternion[3];
			q2q2 = quaternion[2] * quaternion[2];
			q2q3 = quaternion[2] * quaternion[3];
			q3q3 = quaternion[3] * quaternion[3];

			// Reference direction of Earth's magnetic field
			hx = mx * q0q0 - _2q0my * quaternion[3] + _2q0mz * quaternion[2] + mx * q1q1 + _2q1 * my * quaternion[2] + _2q1 * mz * quaternion[3] - mx * q2q2 - mx * q3q3;
			hy = _2q0mx * quaternion[3] + my * q0q0 - _2q0mz * quaternion[1] + _2q1mx * quaternion[2] - my * q1q1 + my * q2q2 + _2q2 * mz * quaternion[3] - my * q3q3;
			_2bx = (float) Math.sqrt(hx * hx + hy * hy);
			_2bz = -_2q0mx * quaternion[2] + _2q0my * quaternion[1] + mz * q0q0 + _2q1mx * quaternion[3] - mz * q1q1 + _2q2 * my * quaternion[3] - mz * q2q2 + mz * q3q3;
			_4bx = 2.0f * _2bx;
			_4bz = 2.0f * _2bz;

			// Gradient decent algorithm corrective step
			s0 = -_2q2 * (2.0f * q1q3 - _2q0q2 - ax) + _2q1 * (2.0f * q0q1 + _2q2q3 - ay) - _2bz * quaternion[2] * (_2bx * (0.5f - q2q2 - q3q3) + _2bz * (q1q3 - q0q2) - mx) + (-_2bx * quaternion[3] + _2bz * quaternion[1]) * (_2bx * (q1q2 - q0q3) + _2bz * (q0q1 + q2q3) - my) + _2bx * quaternion[2] * (_2bx * (q0q2 + q1q3) + _2bz * (0.5f - q1q1 - q2q2) - mz);
			s1 = _2q3 * (2.0f * q1q3 - _2q0q2 - ax) + _2q0 * (2.0f * q0q1 + _2q2q3 - ay) - 4.0f * quaternion[1] * (1 - 2.0f * q1q1 - 2.0f * q2q2 - az) + _2bz * quaternion[3] * (_2bx * (0.5f - q2q2 - q3q3) + _2bz * (q1q3 - q0q2) - mx) + (_2bx * quaternion[2] + _2bz * quaternion[0]) * (_2bx * (q1q2 - q0q3) + _2bz * (q0q1 + q2q3) - my) + (_2bx * quaternion[3] - _4bz * quaternion[1]) * (_2bx * (q0q2 + q1q3) + _2bz * (0.5f - q1q1 - q2q2) - mz);
			s2 = -_2q0 * (2.0f * q1q3 - _2q0q2 - ax) + _2q3 * (2.0f * q0q1 + _2q2q3 - ay) - 4.0f * quaternion[2] * (1 - 2.0f * q1q1 - 2.0f * q2q2 - az) + (-_4bx * quaternion[2] - _2bz * quaternion[0]) * (_2bx * (0.5f - q2q2 - q3q3) + _2bz * (q1q3 - q0q2) - mx) + (_2bx * quaternion[1] + _2bz * quaternion[3]) * (_2bx * (q1q2 - q0q3) + _2bz * (q0q1 + q2q3) - my) + (_2bx * quaternion[0] - _4bz * quaternion[2]) * (_2bx * (q0q2 + q1q3) + _2bz * (0.5f - q1q1 - q2q2) - mz);
			s3 = _2q1 * (2.0f * q1q3 - _2q0q2 - ax) + _2q2 * (2.0f * q0q1 + _2q2q3 - ay) + (-_4bx * quaternion[3] + _2bz * quaternion[1]) * (_2bx * (0.5f - q2q2 - q3q3) + _2bz * (q1q3 - q0q2) - mx) + (-_2bx * quaternion[0] + _2bz * quaternion[2]) * (_2bx * (q1q2 - q0q3) + _2bz * (q0q1 + q2q3) - my) + _2bx * quaternion[1] * (_2bx * (q0q2 + q1q3) + _2bz * (0.5f - q1q1 - q2q2) - mz);
			recipNorm = 1.0f / (float)Math.sqrt(s0 * s0 + s1 * s1 + s2 * s2 + s3 * s3); // normalise step magnitude
			s0 *= recipNorm;
			s1 *= recipNorm;
			s2 *= recipNorm;
			s3 *= recipNorm;

			// Apply feedback step
			qDot1 -= beta * s0;
			qDot2 -= beta * s1;
			qDot3 -= beta * s2;
			qDot4 -= beta * s3;
		}

		// Integrate rate of change of quaternion to yield quaternion
		quaternion[0] += qDot1 * (1.0f / sampleFreq);
		quaternion[1] += qDot2 * (1.0f / sampleFreq);
		quaternion[2] += qDot3 * (1.0f / sampleFreq);
		quaternion[3] += qDot4 * (1.0f / sampleFreq);

		// Normalise quaternion
		recipNorm = 1.0f / (float)Math.sqrt(quaternion[0] * quaternion[0] + quaternion[1] * quaternion[1] + quaternion[2] * quaternion[2] + quaternion[3] * quaternion[3]);
		quaternion[0] *= recipNorm;
		quaternion[1] *= recipNorm;
		quaternion[2] *= recipNorm;
		quaternion[3] *= recipNorm;
	}
	
	public void MadgwickAHRSupdateIMU(float gx, float gy, float gz, float ax, float ay, float az)
	{
		float recipNorm;
		float s0, s1, s2, s3;
		float qDot1, qDot2, qDot3, qDot4;
		float _2q0, _2q1, _2q2, _2q3, _4q0, _4q1, _4q2 ,_8q1, _8q2, q0q0, q1q1, q2q2, q3q3;

		// Rate of change of quaternion from gyroscope
		qDot1 = 0.5f * (-quaternion[1] * gx - quaternion[2] * gy - quaternion[3] * gz);
		qDot2 = 0.5f * (quaternion[0] * gx + quaternion[2] * gz - quaternion[3] * gy);
		qDot3 = 0.5f * (quaternion[0] * gy - quaternion[1] * gz + quaternion[3] * gx);
		qDot4 = 0.5f * (quaternion[0] * gz + quaternion[1] * gy - quaternion[2] * gx);

		// Compute feedback only if accelerometer measurement valid (avoids NaN in accelerometer normalisation)
		if(!((ax == 0.0f) && (ay == 0.0f) && (az == 0.0f))) {

			// Normalise accelerometer measurement
			recipNorm = 1.0f / (float)Math.sqrt(ax * ax + ay * ay + az * az);
			ax *= recipNorm;
			ay *= recipNorm;
			az *= recipNorm;   

			// Auxiliary variables to avoid repeated arithmetic
			_2q0 = 2.0f * quaternion[0];
			_2q1 = 2.0f * quaternion[1];
			_2q2 = 2.0f * quaternion[2];
			_2q3 = 2.0f * quaternion[3];
			_4q0 = 4.0f * quaternion[0];
			_4q1 = 4.0f * quaternion[1];
			_4q2 = 4.0f * quaternion[2];
			_8q1 = 8.0f * quaternion[1];
			_8q2 = 8.0f * quaternion[2];
			q0q0 = quaternion[0] * quaternion[0];
			q1q1 = quaternion[1] * quaternion[1];
			q2q2 = quaternion[2] * quaternion[2];
			q3q3 = quaternion[3] * quaternion[3];

			// Gradient decent algorithm corrective step
			s0 = _4q0 * q2q2 + _2q2 * ax + _4q0 * q1q1 - _2q1 * ay;
			s1 = _4q1 * q3q3 - _2q3 * ax + 4.0f * q0q0 * quaternion[1] - _2q0 * ay - _4q1 + _8q1 * q1q1 + _8q1 * q2q2 + _4q1 * az;
			s2 = 4.0f * q0q0 * quaternion[2] + _2q0 * ax + _4q2 * q3q3 - _2q3 * ay - _4q2 + _8q2 * q1q1 + _8q2 * q2q2 + _4q2 * az;
			s3 = 4.0f * q1q1 * quaternion[3] - _2q1 * ax + 4.0f * q2q2 * quaternion[3] - _2q2 * ay;
			recipNorm = 1.0f / (float)Math.sqrt(s0 * s0 + s1 * s1 + s2 * s2 + s3 * s3); // normalise step magnitude
			s0 *= recipNorm;
			s1 *= recipNorm;
			s2 *= recipNorm;
			s3 *= recipNorm;

			// Apply feedback step
			qDot1 -= beta * s0;
			qDot2 -= beta * s1;
			qDot3 -= beta * s2;
			qDot4 -= beta * s3;
		}

		// Integrate rate of change of quaternion to yield quaternion
		quaternion[0] += qDot1 * (1.0f / sampleFreq);
		quaternion[1] += qDot2 * (1.0f / sampleFreq);
		quaternion[2] += qDot3 * (1.0f / sampleFreq);
		quaternion[3] += qDot4 * (1.0f / sampleFreq);

		// Normalise quaternion
		recipNorm = 1.0f / (float)Math.sqrt(quaternion[0] * quaternion[0] + quaternion[1] * quaternion[1] + quaternion[2] * quaternion[2] + quaternion[3] * quaternion[3]);
		quaternion[0] *= recipNorm;
		quaternion[1] *= recipNorm;
		quaternion[2] *= recipNorm;
		quaternion[3] *= recipNorm;
	}

	public float getBeta() {
		return beta;
	}

	public void setBeta(float beta) {
		this.beta = beta;
	}

	public float[] getQuaternion() {
		return quaternion;
	}

	public float getSampleFreq() {
		return sampleFreq;
	}

	public void setSampleFreq(float sampleFreq) {
		this.sampleFreq = sampleFreq;
	}	
	
	public float[] getEulerAngles()
	{
		float[] angles = new float[3];
		
		angles[0] = (float) Math.atan2(2*quaternion[1]*quaternion[2]-2*quaternion[0]*quaternion[3],
				2*quaternion[0]*quaternion[0] + 2*quaternion[1]*quaternion[1] - 1);
		angles[1] = (float) (-1 * Math.asin(2*quaternion[1]*quaternion[3] + 2*quaternion[0]*quaternion[2]));
		angles[2] = (float) Math.atan2(2*quaternion[2]*quaternion[3]-2*quaternion[0]*quaternion[1],
				2*quaternion[0]*quaternion[0] + 2*quaternion[3]*quaternion[3] - 1);		
		
		return angles;
	}
	
	public float[] getMatrix3()
	{
		float[] matrix3 = new float[9];
		
		float a = quaternion[0];
		float b = quaternion[1];
		float c = quaternion[2];
		float d = quaternion[3];
		
		matrix3[0] = a*a + b*b - c*c - d*d;
		matrix3[1] = 2*b*c + 2*a*d;
		matrix3[2] = 2*b*d - 2*a*c;
		
		matrix3[3] = 2*b*c - 2*a*d;
		matrix3[4] = a*a - b*b + c*c - d*d;
		matrix3[5] = 2*c*d + 2*a*b;
		
		matrix3[6] = 2*b*d + 2*a*c;
		matrix3[7] = 2*c*d - 2*a*b;
		matrix3[8] = a*a - b*b - c*c + d*d;
		
		return matrix3;
	}
	
	public float[] getMatrix4()
	{
		float[] matrix4 = new float[16];
		
		float a = quaternion[0];
		float b = quaternion[1];
		float c = quaternion[2];
		float d = quaternion[3];
		
		matrix4[0] = a*a + b*b - c*c - d*d;
		matrix4[1] = 2*b*c + 2*a*d;
		matrix4[2] = 2*b*d - 2*a*c;
		matrix4[3] = 0.0f;
		
		matrix4[4] = 2*b*c - 2*a*d;
		matrix4[5] = a*a - b*b + c*c - d*d;
		matrix4[6] = 2*c*d + 2*a*b;
		matrix4[7] = 0.0f;
		
		matrix4[8] = 2*b*d + 2*a*c;
		matrix4[9] = 2*c*d - 2*a*b;
		matrix4[10] = a*a - b*b - c*c + d*d;
		matrix4[11] = 0.0f;

		matrix4[12] = 0.0f;
		matrix4[13] = 0.0f;
		matrix4[14] = 0.0f;
		matrix4[15] = 1.0f;
		
		return matrix4;		
	}
}
