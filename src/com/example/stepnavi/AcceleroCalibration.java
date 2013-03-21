package com.example.stepnavi;

import java.util.ArrayList;

public class AcceleroCalibration {

	private static ArrayList<Double> mAccX = new ArrayList<Double>();
	private static ArrayList<Double> mAccY = new ArrayList<Double>();
	private static ArrayList<Double> mAccZ = new ArrayList<Double>();
	
	public static void reset()
	{
		mAccX = new ArrayList<Double>();
		mAccY = new ArrayList<Double>();
		mAccZ = new ArrayList<Double>();
	}
	
	public static void addValues(double[] currentAcc)
	{
		mAccX.add(currentAcc[0]);
		mAccY.add(currentAcc[1]);
		mAccZ.add(currentAcc[2]);
	}
	
	public static double[] getCorrections()
	{
		double[] res = new double[3];
		res[0] = 0.0; res[1] = 0.0; res[2] = 0.0;
		for(int i=0; i<mAccX.size(); i++)
		{
			res[0] = (1.0/(i+1.0))*mAccX.get(i) + (i/(i+1.0))*res[0];
			res[1] = (1.0/(i+1.0))*mAccY.get(i) + (i/(i+1.0))*res[1];
			res[2] = (1.0/(i+1.0))*mAccZ.get(i) + (i/(i+1.0))*res[2];
		}
		return res;
	}
}
