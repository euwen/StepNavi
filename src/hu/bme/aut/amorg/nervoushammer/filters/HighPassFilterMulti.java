package hu.bme.aut.amorg.nervoushammer.filters;

import java.util.ArrayList;

public class HighPassFilterMulti {

	private ArrayList<HighPassFilter> filters;
	
	public HighPassFilterMulti(int dimension)
	{
		if (dimension < 1) return;
		
		filters = new ArrayList<HighPassFilter>();
		for (int i=0; i<dimension; i++)
		{
			filters.add(new HighPassFilter());
		}
	}
	
	public void reset()
	{
		for (HighPassFilter filter : filters) {
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
