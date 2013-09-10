package com.example.stepnavi.filters;

import java.util.ArrayList;

public class MedianFilter {
	
	private ArrayList<Float> window;
	private float current = 0;
	
	public MedianFilter(int windowSize) {
		window = new ArrayList<Float>();
		for(int i=0; i<windowSize; i++)
		{
			window.add(0.0f);
		}
	}
	
	public void reset()
	{
		current = 0.0f;
		for(int i=0; i<window.size(); i++)
		{
			window.set(i,0.0f);
		}
	}
	
	public float getCurrentOutput()
	{
		return current;
	}
	
	public void insertShift(float newValue)
	{
		// insert at first place
		window.add(0, newValue);
		// remove from last place
		window.remove(window.size()-1);
	}
	
	
	/*
	 * procedure bubbleSort( A : list of sortable items )
		    n = length(A)
		    repeat
		       newn = 0
		       for i = 1 to n-1 inclusive do
		          if A[i-1] > A[i] then
		             swap(A[i-1], A[i])
		             newn = i
		          end if
		       end for
		       n = newn
		    until n = 0
		end procedure
	 */
	
	private float[] getSorted()
	{
		float temp;
		int newn = 0;
		int n = window.size();
		float[] data = new float[n];
		for (int i=0; i<n; i++)
		{
			data[i] = window.get(i);
		}
		while (n != 0)
		{
			newn = 0;
			for (int i=1; i<n; i++)
			{
				if (data[i-1] > data[i])
				{
					temp = data[i-1];
					data[i-1] = data[i];
					data[i] = temp;
					newn = i;
				}
			}
			n = newn;
		}
		return data;
	}
	
	public float filter(Float newValue)
	{
		try{
			if (newValue != null)
			{
				insertShift(newValue);
			}
			float[] data = getSorted();
			int p = data.length / 2;
			if ((data.length % 2) == 0)
			{
				current = (data[p] + data[p+1]) / 2;
			}
			else
			{
				current = data[p+1];
			}
			return current;
		} catch (IndexOutOfBoundsException e){
			return 0;
		}
	}
}
