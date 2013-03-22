package com.example.stepnavi.filters;

public class LowPassFilter {

	private float current = 0.0f;
	
	public void reset()
	{
		current = 0.0f;
	}
	
	public float getCurrentOutput()
	{
		return current;
	}
	
	public float filter(float newValue, float magic)
	{
		current += (newValue - current) / magic;	
		return current;
	}
}
