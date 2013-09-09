package com.example.stepnavi;

public class TimeMeasure {

	private long beginTime = 0;
	private long lastTime = 0;

	public TimeMeasure(boolean begin)
	{
		if (begin == true){
			this.begin();
		}
	}
	
	public void begin(){
		beginTime = System.currentTimeMillis();
		lastTime = beginTime;
	}

	public long getDelta(){
		long temp = System.currentTimeMillis();
		long res = temp - lastTime;
		lastTime = temp;
		return res;
	}
	
	public long getSinceBeginning(boolean isDelta){
		if (isDelta == true){
			long temp = System.currentTimeMillis();
			long res = temp - beginTime;
			lastTime = temp;
			return res;
		} else {
			long temp = System.currentTimeMillis();
			long res = temp - beginTime;
			return res;
		}
	}
}
