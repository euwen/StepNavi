package com.example.stepnavi.filters;

import java.util.ArrayList;

public class LowPassFilterMulti {

	private ArrayList<LowPassFilter> filters;
	
	public LowPassFilterMulti(int dimension)
	{
		if (dimension < 1) return;
		
		filters = new ArrayList<LowPassFilter>();
		for (int i=0; i<dimension; i++)
		{
			filters.add(new LowPassFilter());
		}
	}
	
	public void reset()
	{
		for (LowPassFilter filter : filters) {
			filter.reset();
		}
	}
	
	public double[] getCurrentOutputs()
	{
		int n = filters.size();
		double[] result = new double[n];
		for (int i=0; i<n; i++){
			result[i] = filters.get(i).getCurrentOutput();
		}
		return result;
	}
	
	public double[] filter(double[] values, double magic)
	{
		int n = filters.size();
		double[] result = new double[n];
		for (int i=0; i<n; i++){
			result[i] = filters.get(i).filter(values[i], magic);
		}
		return result;
	}

	
}
