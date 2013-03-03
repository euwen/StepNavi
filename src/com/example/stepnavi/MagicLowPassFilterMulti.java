package com.example.stepnavi;

import java.util.ArrayList;

public class MagicLowPassFilterMulti {

	private ArrayList<MagicLowPassFilter> filters;
	
	public MagicLowPassFilterMulti(int dimension)
	{
		if (dimension < 1) return;
		
		filters = new ArrayList<MagicLowPassFilter>();
		for (int i=0; i<dimension; i++)
		{
			filters.add(new MagicLowPassFilter());
		}
	}
	
	public void reset()
	{
		for (MagicLowPassFilter filter : filters) {
			filter.reset();
		}
	}
	
	public float[] getCurrentOutputs()
	{
		int n = filters.size();
		float[] result = new float[n];
		for (int i=0; i<n; i++){
			result[i] = filters.get(i).getCurrentOutput();
		}
		return result;
	}
	
	public float[] filter(float[] values, float magic)
	{
		int n = filters.size();
		float[] result = new float[n];
		for (int i=0; i<n; i++){
			result[i] = filters.get(i).filter(values[i], magic);
		}
		return result;
	}
}
