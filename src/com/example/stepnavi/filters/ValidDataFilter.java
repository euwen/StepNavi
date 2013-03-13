package com.example.stepnavi.filters;


public class ValidDataFilter {

	private double current = 0.0f;
	private double lastInput = 0.0f;
	private double alpha = 0.0f;
	
	public void reset()
	{
		current = 0.0f;
		lastInput = 0.0f;
		alpha = 0.0f;
	}
	
	public double getCurrentOutput()
	{
		return current;
	}
	
	// In Excel:
	// =(1/(1+(B2-B1)*(B2-B1)))*B2+H1*(1-1/(1+(B2-B1)*(B2-B1)))
	// where H column is the output and B column is the input
	
	public double filter(double newValue, double magic)
	{
		alpha = 1/(1 + (double)Math.pow((double)(Math.abs(newValue-lastInput)), (double)magic));
		current = current*(1-alpha)	+ alpha*newValue;
		
		return current;
	}
	
}
