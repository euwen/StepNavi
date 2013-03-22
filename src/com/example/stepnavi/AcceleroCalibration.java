package com.example.stepnavi;

import java.util.ArrayList;

public class AcceleroCalibration {

	private static ArrayList<Float> mAccX = new ArrayList<Float>();
	private static ArrayList<Float> mAccY = new ArrayList<Float>();
	private static ArrayList<Float> mAccZ = new ArrayList<Float>();
	
	public static void reset()
	{
		mAccX = new ArrayList<Float>();
		mAccY = new ArrayList<Float>();
		mAccZ = new ArrayList<Float>();
	}
	
	public static void addValues(float[] currentAcc)
	{
		mAccX.add(currentAcc[0]);
		mAccY.add(currentAcc[1]);
		mAccZ.add(currentAcc[2]);
	}
	
	public static float[] getCorrections()
	{
		float[] res = new float[3];
		res[0] = 0.0f; res[1] = 0.0f; res[2] = 0.0f;
		for(int i=0; i<mAccX.size(); i++)
		{
			res[0] = (1.0f/(i+1.0f))*mAccX.get(i) + (i/(i+1.0f))*res[0];
			res[1] = (1.0f/(i+1.0f))*mAccY.get(i) + (i/(i+1.0f))*res[1];
			res[2] = (1.0f/(i+1.0f))*mAccZ.get(i) + (i/(i+1.0f))*res[2];
		}
		return res;
	}
}
