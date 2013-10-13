package com.example.stepnavi;

import java.util.ArrayList;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

import android.util.FloatMath;
import android.util.Log;

import com.example.stepnavi.filters.MedianFilter;

public class ImageProcessor {

	//private boolean USE_RESIZE = true;
	private float RESIZE_WIDTH = 240.0f;
	private static final int FEATURE_MATRIX_WIDTH = 16;
	private static final int FEATURE_MATRIX_HEIGHT = 16;
	
	//private static final int STATE_SEARCH = 0;
	//private static final int STATE_MATCH = 1;
	
	private Mat prevFrame = null;
	private Mat currentFrame = null;
	private Mat tempFrame = null;
	//private MatOfPoint tempPoints = null;
	private MatOfPoint2f prevPoints = null;
	private MatOfPoint2f currentPoints = null;
	private MatOfByte status = new MatOfByte();
	private MatOfFloat error = new MatOfFloat();
	private Size window = new Size(10.0, 10.0);
	private TermCriteria criteria = new TermCriteria(TermCriteria.COUNT+TermCriteria.EPS, 10, 0.3);
	
	//private Size workSize = null;
	//private float ratio = 1.0f;
	
	//private int state = STATE_SEARCH;
	
	//private float avgX=0, avgY=0;
	private float medX=0, medY=0;
	//private float angX=0, angY=0;
	private float rotT1  = 0;
	private float rotT2  = 0;
	
	TimeMeasure tm;
	
	/*
	public ImageProcessor(Context c){
		//cnt = 0;
		//logger = new CsvLogger(c, 2, "match");
	}
	*/
	
	public void process2(Mat input){
		
		float ratio = RESIZE_WIDTH / input.width();
		Size s = new Size(input.width()*ratio, input.height()*ratio);
		tempFrame = new Mat(s, input.type());
		currentFrame = new Mat(s, CvType.CV_8U);
		Imgproc.resize(input, tempFrame, s);
		Imgproc.cvtColor(tempFrame, currentFrame, Imgproc.COLOR_RGB2GRAY);
		
		if (prevFrame == null)
		{
			tm = new TimeMeasure(true);
			prevFrame = new Mat(currentFrame.rows(), currentFrame.cols(), currentFrame.type());
			currentFrame.copyTo(prevFrame);
			
			// Make place for the points
			Point[] pointsArray = new Point[FEATURE_MATRIX_WIDTH*FEATURE_MATRIX_HEIGHT];
			int stepX = currentFrame.width()/(FEATURE_MATRIX_WIDTH+1);
			int stepY = currentFrame.height()/(FEATURE_MATRIX_HEIGHT+1);
			for (int i=0; i<FEATURE_MATRIX_WIDTH; i++){
				for (int j=0; j<FEATURE_MATRIX_HEIGHT; j++){
					pointsArray[i*FEATURE_MATRIX_WIDTH+j] = 
							new Point(i*stepX+stepX, j*stepY+stepY);
				}
			}
			prevPoints = new MatOfPoint2f(pointsArray);
		}
		currentPoints = new MatOfPoint2f(); 
		
		Video.calcOpticalFlowPyrLK(
				prevFrame, 
				currentFrame, 
				prevPoints, 
				currentPoints, 
				status, 
				error
		);
		
		long temp = tm.getDelta();
		Log.d(this.getClass().getSimpleName(), "Delta: " + temp + " LengthX: " + medX  + " LengthY: " + medY);
		
		byte[] statuses = status.toArray();
		int goodCount = 0;
		for (int i=0; i< statuses.length; i++){
			if (statuses[i] == 0) continue;
			goodCount++;
		}
		float howGood = (float)goodCount / (float)(FEATURE_MATRIX_WIDTH*FEATURE_MATRIX_HEIGHT);
		
		MedianFilter medianX = new MedianFilter(goodCount);
		MedianFilter medianY = new MedianFilter(goodCount);
		ArrayList<Float> thetas = new ArrayList<Float>();
		Point[] from = prevPoints.toArray();
		Point[] to = currentPoints.toArray();
		Scalar color1 = new Scalar(255, 128, 32);
		Scalar color2 = new Scalar(64, 255, 0);
		for (int i=0; i< statuses.length; i++){
			if (statuses[i] == 0) continue;
			
			float l1 = FloatMath.sqrt((float) (to[i].x*to[i].x + to[i].y*to[i].y));
			float l2 = FloatMath.sqrt((float) (from[i].x*from[i].x + from[i].y*from[i].y));
			// points too close to center wont play
			if (!((l1 < RESIZE_WIDTH/3) || (l2 < RESIZE_WIDTH/3))){
				float theta = (float) (Math.atan2(to[i].y/l1,to[i].x/l1) - Math.atan2(from[i].y/l2,from[i].x/l2)) ;
				if (theta < -Math.PI){
					theta += 2*Math.PI;
				}
				thetas.add(theta);
			}	
			
			medianX.insertShift((float)(to[i].x-from[i].x));
			medianY.insertShift((float)(to[i].y-from[i].y));
			/*
			Core.line(input, 
					new Point(from[i].x/ratio, from[i].y/ratio), 
					new Point(to[i].x/ratio, to[i].y/ratio), 
					color1, 2);
			*/
		}
		
		MedianFilter medianT = new MedianFilter(thetas.size());
		float tempR = 0.0f;
		for (int i=0; i<thetas.size(); i++){
			medianT.insertShift(thetas.get(i));
			tempR += thetas.get(i);
		}
		rotT1 = medianT.filter(null); 
		rotT2 = tempR/thetas.size();
		
		medX = medianX.filter(null);
		medY = medianY.filter(null);
		
		int w = input.width();
		int h = input.height();
		int d = input.height() / 6;
		Core.line(input, 
				new Point( w/2, h/2  + d*-1), 
				new Point( w/2 + medX/ratio, h/2 + d*-1 + medY/ratio), 
				color2, 10);
		
		currentPoints.copyTo(prevPoints);
		currentFrame.copyTo(prevFrame);
		
		if (howGood <= 0.65){
			// Make place for the points
			Point[] pointsArray = new Point[FEATURE_MATRIX_WIDTH*FEATURE_MATRIX_HEIGHT];
			int stepX = currentFrame.width()/(FEATURE_MATRIX_WIDTH+1);
			int stepY = currentFrame.height()/(FEATURE_MATRIX_HEIGHT+1);
			for (int i=0; i<FEATURE_MATRIX_WIDTH; i++){
				for (int j=0; j<FEATURE_MATRIX_HEIGHT; j++){
					pointsArray[i*FEATURE_MATRIX_WIDTH+j] = 
							new Point(i*stepX+stepX, j*stepY+stepY);
				}
			}
			prevPoints = new MatOfPoint2f(pointsArray);
		}
	}
	
	/*
	public void process(Mat inputFrame){
		if (workSize == null){
			init(inputFrame);
		}
		if (USE_RESIZE == true){
			Imgproc.resize(inputFrame, tempFrame, workSize);
			Imgproc.cvtColor(tempFrame, currentFrame, Imgproc.COLOR_RGB2GRAY);
		} else {
			Imgproc.cvtColor(inputFrame, currentFrame, Imgproc.COLOR_RGB2GRAY);
		}
		
		if (state == STATE_SEARCH){
			this.search(inputFrame);
			return;
		}
		else if (state == STATE_MATCH){
			this.match(inputFrame);
			return;
		}
		
	}
	
	private void search(Mat inputFrame){
		// Make place for the points
		Point[] pointsArray = new Point[FEATURE_COUNT];
		for (int i=0; i<FEATURE_COUNT; i++){
			pointsArray[i] = new Point(0, 0);
		}
		tempPoints = new MatOfPoint(pointsArray);
		// Search!
		Imgproc.goodFeaturesToTrack(
				currentFrame, 	// the image
				tempPoints, 	// the output detected features
				FEATURE_COUNT,	// the maximum number of features
				0.01, 			// quality level
				10.0 			// min distance between two features
		);
		
		Log.d(this.getClass().getSimpleName(), "Keresés...");
		if ((tempPoints.size().width <= 0) || (tempPoints.size().height <= 0)){
			Log.d(this.getClass().getSimpleName(), "Baj van!");
			return;
		}
		// init variables for match
		prevPoints = new MatOfPoint2f(tempPoints.toArray());
		prevFrame = new Mat(currentFrame.rows(), currentFrame.cols(), currentFrame.type());
		currentFrame.copyTo(prevFrame);
		//currentPoints = new MatOfPoint2f(tempPoints.toArray());
		state = STATE_MATCH;
		Scalar color = new Scalar(255, 0, 0);
		Core.line(inputFrame, 
				new Point( 100, 100), 
				new Point( 101, 101), 
				color, 32);
	}
	
	private void match(Mat inputFrame){
		
		currentPoints = new MatOfPoint2f();
		// Match
		Video.calcOpticalFlowPyrLK(
				prevFrame, 			// previous frame
				currentFrame, 		// current frame
				prevPoints,			// previous points
				currentPoints,		// output current points
				status, 			// output vector of success
				error, 				// output vector of error
				window, 			// size of the search window at each pyramid level
				5, 					// 0-based maximal pyramid level number
				criteria, 			// the termination criteria of the iterative search 
				0, 					// flags 
				0.001				// minEigThreshold 
		);

		// Create a few variable
		byte[] statuses = status.toArray();
		Point[] from = prevPoints.toArray();
		Point[] to = currentPoints.toArray();
		Scalar color1 = new Scalar(255, 128, 32);
		Scalar color2 = new Scalar(64, 255, 0);
		// Count good items
		int goodCount = 0;
		for (int i=0; i< statuses.length; i++){
			if (statuses[i] == 0) continue;
			goodCount++;
		}
		float howGood = (float)goodCount / (float)FEATURE_COUNT;
		// Plot and filter
		float X = 0;
		float Y = 0;
		MedianFilter medianX = new MedianFilter(goodCount);
		MedianFilter medianY = new MedianFilter(goodCount);
		MedianFilter medianA = new MedianFilter(goodCount);
		MedianFilter medianL = new MedianFilter(goodCount);
		for (int i=0; i< statuses.length; i++){
			if (statuses[i] == 0) continue;
			medianA.insertShift( (float)Math.atan2(to[i].x-from[i].x, to[i].y-from[i].y));
			medianL.insertShift( (float)Math.sqrt( Math.pow(to[i].x-from[i].x,2) + Math.pow(to[i].y-from[i].y,2)));
			X += to[i].x-from[i].x;
			Y += to[i].y-from[i].y;
			medianX.insertShift((float)(to[i].x-from[i].x));
			medianY.insertShift((float)(to[i].y-from[i].y));
			Core.line(inputFrame, 
					new Point(from[i].x/ratio, from[i].y/ratio), 
					new Point(to[i].x/ratio, to[i].y/ratio), 
					color1, 2);
		}
		avgX = X / goodCount;
		avgY = Y / goodCount;
		if (avgX == 0){
			Log.d("aha", "nulla");
		}
		medX = medianX.filter(null);
		medY = medianY.filter(null);
		float l = medianL.filter(null);
		float a = medianA.filter(null);
		angX = (float)Math.sin(a)*l;
		angY = (float)Math.cos(a)*l;
		int w = inputFrame.width();
		int h = inputFrame.height();
		int d = inputFrame.height() / 6;
		Core.line(inputFrame, 
				new Point( w/2, h/2  + d*-1), 
				new Point( w/2 + avgX/ratio, h/2 + d*-1 + avgY/ratio), 
				color2, 10);
		Core.line(inputFrame, 
				new Point( w/2, h/2  + d*0), 
				new Point( w/2 + medX/ratio, h/2  + d*0 + medY/ratio), 
				color2, 10);			
		Core.line(inputFrame, 
				new Point( w/2, h/2  + d*1), 
				new Point( w/2 + angX/ratio, h/2 + d*1 + angY/ratio), 
				color2, 10);	
		//Log.d(this.getClass().getSimpleName(), "HowGood: " + howGood + " Length X: " + avgX + " Y: " + avgY);
		
		long temp = tm.getDelta();
		Log.d(this.getClass().getSimpleName(), "Delta: " + temp + " Length: " + Math.sqrt(medX*medX + medY*medY));
		
		if (howGood < 0.5f){
			state = STATE_SEARCH;
			this.search(inputFrame);
		} else {
			prevFrame = new Mat(currentFrame.rows(), currentFrame.cols(), currentFrame.type());
			currentFrame.copyTo(prevFrame);
			prevPoints.fromArray(currentPoints.toArray());
		}
	}
	*/
	
	/*
	public float[] getMovementAverage()
	{
		float[] res = new float[2];
		res[0] = avgX;
		res[1] = avgY;
		return res;
	}
	*/
	
	public float[] getMovementMedian()
	{
		float[] res = new float[2];
		res[0] = medX;
		res[1] = medY;
		return res;
	}
	
	public float getRotZ1()
	{
		return rotT1;
	}
	public float getRotZ2()
	{
		return rotT2;
	}
	
	/*
	public float[] getMovementAngleLength()
	{
		float[] res = new float[2];
		res[0] = angX;
		res[1] = angY;
		return res;
	}
	*/
	
	/*
	public void init( Mat input){
		tm = new TimeMeasure(true);
		float newWidth = RESIZE_WIDTH;
		float newHeight = 0;
		if (newWidth < input.width()){
			ratio = newWidth / (float)input.width();
			newHeight = ratio*(float)input.height();
		} else {
			ratio = 1.0f;
			newHeight = input.height();
		}
		workSize = new Size(newWidth, newHeight);
		if (USE_RESIZE == true){
			tempFrame = new Mat(workSize, input.type());
			currentFrame = new Mat(workSize, CvType.CV_8U);
		} else {
			currentFrame = new Mat(input.size(), CvType.CV_8U);
		}
	}
	
	public void findNewFeatures(){
		state = STATE_SEARCH;
	}
	*/
}
