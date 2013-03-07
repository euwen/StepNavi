package com.example.stepnavi.filters;

public class HighPassFilter {

	private float current = 0.0f;
	private float temp = 0.0f;
	
	public void reset()
	{
		current = 0.0f;
		temp = 	0.0f;
	}
	
	public float getCurrentOutput()
	{
		return current;
	}
	
	public float filter(float newValue, float magic)
	{
		temp = temp*(1-magic) + magic*newValue;
		current = newValue - temp;
		
		return current;
	}
	
}
