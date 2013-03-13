package com.example.stepnavi.filters;

public class LowPassFilter {

	private double current = 0.0f;
	
	public void reset()
	{
		current = 0.0f;
	}
	
	public double getCurrentOutput()
	{
		return current;
	}
	
	public double filter(double newValue, double magic)
	{
		current += (newValue - current) / magic;	
		return current;
	}
}
