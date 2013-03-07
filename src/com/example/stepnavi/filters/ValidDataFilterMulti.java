package com.example.stepnavi.filters;

import java.util.ArrayList;

public class ValidDataFilterMulti {

	private ArrayList<ValidDataFilter> filters;
	
	public ValidDataFilterMulti(int dimension)
	{
		if (dimension < 1) return;
		
		filters = new ArrayList<ValidDataFilter>();
		for (int i=0; i<dimension; i++)
		{
			filters.add(new ValidDataFilter());
		}
	}
	
	public void reset()
	{
		for (ValidDataFilter filter : filters) {
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
