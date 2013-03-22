package com.example.stepnavi.filters;

import java.util.ArrayList;

public class MedianFilterMulti {

	private ArrayList<MedianFilter> filters;
	
	public MedianFilterMulti(int dimension, int windowSize)
	{
		if (dimension < 1) return;
		
		filters = new ArrayList<MedianFilter>();
		for (int i=0; i<dimension; i++)
		{
			filters.add(new MedianFilter(windowSize));
		}
	}
	
	public void reset()
	{
		for (MedianFilter filter : filters) {
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
	
	public float[] filter(float[] values)
	{
		int n = filters.size();
		float[] result = new float[n];
		for (int i=0; i<n; i++){
			result[i] = filters.get(i).filter(values[i]);
		}
		return result;
	}

	
}
