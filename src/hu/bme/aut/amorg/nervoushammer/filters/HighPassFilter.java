package hu.bme.aut.amorg.nervoushammer.filters;

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
		temp += (newValue - temp) / magic;
		current = newValue - temp;
		
		return current;
	}
	
}
