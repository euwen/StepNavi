package com.example.stepnavi.filters;

public class HighPassFilter {

	private double current = 0.0f;
	private double temp = 0.0f;
	
	public void reset()
	{
		current = 0.0f;
		temp = 	0.0f;
	}
	
	public double getCurrentOutput()
	{
		return current;
	}
	
	public double filter(double newValue, double magic)
	{
		temp = temp*(1-magic) + magic*newValue;
		current = newValue - temp;
		
		return current;
	}
	
}
