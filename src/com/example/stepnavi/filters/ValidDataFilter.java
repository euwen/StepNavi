package com.example.stepnavi.filters;


public class ValidDataFilter {

	private float current = 0.0f;
	private float lastInput = 0.0f;
	private float alpha = 0.0f;
	
	public void reset()
	{
		current = 0.0f;
		lastInput = 0.0f;
		alpha = 0.0f;
	}
	
	public float getCurrentOutput()
	{
		return current;
	}
	
	// In Excel:
	// =(1/(1+(B2-B1)*(B2-B1)))*B2+H1*(1-1/(1+(B2-B1)*(B2-B1)))
	// where H column is the output and B column is the input
	
	public float filter(float newValue, float magic)
	{
		alpha = 1/(1 + (float)Math.pow((double)(Math.abs(newValue-lastInput)), (double)magic));
		current = current*(1-alpha)	+ alpha*newValue;
		
		return current;
	}
	
}
