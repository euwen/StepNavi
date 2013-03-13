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
