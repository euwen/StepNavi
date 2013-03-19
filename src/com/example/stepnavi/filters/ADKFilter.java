package com.example.stepnavi.filters;

import java.util.ArrayList;

public class ADKFilter {

	private int minLength = 10;
	private double maxDelta = 0.1;
	private double strength = 0.1;
	private double convergence = 0.1;
	
	// third is minimum series length, fourth is the maximal slope, fifth is the low pass filter strength, and sixth is the convergence ratio!
	public ADKFilter(int minLength, double maxDelta, double strength, double convergence)
	{
		this.minLength = minLength;
		this.maxDelta = maxDelta;
		this.strength = strength;
		this.convergence = convergence;
	}
	
	private int goodsLeft = 0;
	private int serieLength = 0;
	private int lastPos = 0;
	private int pos = 0;
	
	// should be called every once a new element is added!
	public void tryFilter(ArrayList<Double> data, ArrayList<Double> out)
	{
		int goods = 0;
		
		if (pos + minLength*2 >= data.size()-1) return;
		
		// only work on not known
		double[] diff = new double[minLength];
	    // calc diffs
		for(int i=pos; i<pos+minLength; i++){
			diff[i-pos] = data.get(i+1) - data.get(i);
		}
		//diff[count-1] = 0;
		
		goods = 0;
		// for the next few elements
		for (int i=0; i<minLength; i++)
		{
			if (Math.abs(diff[i]) < maxDelta)
			{
				goods++;
			}
		}
		// if found required count
		if (goods == minLength)
		{
			int healed = 0;
			// if goodness just begins
			if (serieLength == 0)
			{
				// connect last good with current with a line
				int deltaPos = pos-lastPos;
				double deltaValue = data.get(pos) - data.get(lastPos);
				double slope = 0;
				double lowPassed = data.get(lastPos);
				if (deltaPos > 0)
					slope = ((double) deltaValue)/((double) deltaPos);
				for(int i=1; i<=deltaPos; i++)
				{
				    lowPassed += strength * (data.get(lastPos+i) - lowPassed);
					out.set(lastPos+i, data.get(lastPos+i) - (data.get(lastPos) + slope*(double)i)*(1-convergence) + (convergence)*(lowPassed));
				    healed++;
				}
				if (healed == 0)
				{
					out.set(pos, 0.0);
					healed++;
				}
			}
			else
			{
				// its good, accept it
				//output[pos] = input[pos];
				out.set(pos, 0.0);
				healed++;
			}
			// set values for later
			lastPos = pos + healed - 1;
			serieLength++;
			goodsLeft = minLength - 1;
			pos++;
		}
		// the next few has one ore more errors
		else
		{
			// but after a good serie a few element are still good
			if (goodsLeft-- > 0)
			{
				//output[pos] = input[pos];
				out.set(pos, 0.0);
				lastPos = pos;
				pos++;
			}
			else
			{
				out.set(pos, -4.0);
				pos++;
			}
			// kill length
			serieLength = 0;
		}

		
		//for (int i=count-minLength-1; i<count; i++)
		//{
		//	output[i] = input[i];
		//}
		
		//data.set(pos, 0.0);
		
		
		return;
	}
	
}
