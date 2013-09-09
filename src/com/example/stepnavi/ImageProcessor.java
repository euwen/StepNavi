package com.example.stepnavi;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.TermCriteria;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

import android.content.Context;
import android.util.Log;

import com.example.stepnavi.filters.MedianFilter;

public class ImageProcessor {

	private boolean USE_RESIZE = true;
	private float RESIZE_WIDTH = 320.0f;
	private static final int FEATURE_COUNT = 100;
	
	private static final int STATE_SEARCH = 0;
	private static final int STATE_MATCH = 1;
	
	private Mat prevFrame = null;
	private Mat currentFrame = null;
	private Mat tempFrame = null;
	private MatOfPoint tempPoints = null;
	private MatOfPoint2f prevPoints = null;
	private MatOfPoint2f currentPoints = null;
	private MatOfByte status = new MatOfByte();
	private MatOfFloat error = new MatOfFloat();
	private Size window = new Size(3.0, 3.0);
	private TermCriteria criteria = new TermCriteria(TermCriteria.COUNT+TermCriteria.EPS, 20, 0.3);
	
	private Size workSize = null;
	private float ratio = 1.0f;
	
	private int state = STATE_SEARCH;
	
	//private ArrayList<Integer> search = new ArrayList<Integer>();
	//private ArrayList<Integer> match = new ArrayList<Integer>();
	//private ArrayList<Float> found = new ArrayList<Float>();
	
	//public CsvLogger logger;
	//private int cnt;
	
	public ImageProcessor(Context c){
		//cnt = 0;
		//logger = new CsvLogger(c, 2, "match");
	}
	
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
			this.search();
		}
		else if (state == STATE_MATCH){
			this.match(inputFrame);
		}
	}
	
	private void search(){
		// Make place for the points
		TimeMeasure tm = new TimeMeasure(true);
		Point[] pointsArray = new Point[FEATURE_COUNT];
		for (int i=0; i<FEATURE_COUNT; i++){
			pointsArray[i] = new Point(0, 0);
		}
		tempPoints = new MatOfPoint(pointsArray);
		//Log.d(this.getClass().getSimpleName(), "Searched init needed: " + tm.getDelta());
		// Search!
		Imgproc.goodFeaturesToTrack(
				currentFrame, 	// the image
				tempPoints, 	// the output detected features
				FEATURE_COUNT,	// the maximum number of features
				0.01, 			// quality level
				1.00 			// min distance between two features
		);
		long temp = tm.getDelta();
		//search.add((int) temp);
		Log.d(this.getClass().getSimpleName(), "Searched work needed: " + temp);
		if ((tempPoints.size().width <= 0) || (tempPoints.size().height <= 0)){
			return;
		}
		// init variables for match
		prevPoints = new MatOfPoint2f(tempPoints.toArray());
		prevFrame = currentFrame.clone();
		currentPoints = new MatOfPoint2f(prevPoints.toArray());
		state = STATE_MATCH;
		Log.d(this.getClass().getSimpleName(), "Searched finish needed: " + tm.getDelta());
	}
	
	private void match(Mat inputFrame){
		TimeMeasure tm = new TimeMeasure(true);
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
				0.0001				// minEigThreshold 
		);
		long temp = tm.getDelta();
		//search.add((int) temp);
		Log.d(this.getClass().getSimpleName(), "Match work needed: " + temp);
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
		X /= goodCount;
		Y /= goodCount;
		int w = inputFrame.width();
		int h = inputFrame.height();
		int d = inputFrame.height() / 6;
		Core.line(inputFrame, 
				new Point( w/2, h/2  + d*-1), 
				new Point( w/2 + X/ratio, h/2 + d*-1 + Y/ratio), 
				color2, 10);
		Core.line(inputFrame, 
				new Point( w/2, h/2  + d*0), 
				new Point( w/2 + (medianX.filter(null))/ratio, h/2  + d*0 + (medianY.filter(null))/ratio), 
				color2, 10);			
		float l = medianL.filter(null);
		float a = medianA.filter(null);
		Core.line(inputFrame, 
				new Point( w/2, h/2  + d*1), 
				new Point( w/2 + Math.sin(a)*l/ratio, h/2 + d*1 + Math.cos(a)*l/ratio), 
				color2, 10);	
		Log.d(this.getClass().getSimpleName(), "Match finish needed: " + tm.getDelta());
		//logger.times.add((long)cnt++);
		//logger.data.add((float)temp);
		//logger.data.add((float)howGood);
		//found.add(howGood);
		
		if (howGood < 0.5f){
			state = STATE_SEARCH;
			this.search();
		} else {
			prevFrame = currentFrame.clone();
			prevPoints = currentPoints;
		}
	}
	
	public void init( Mat input){
		float newWidth = RESIZE_WIDTH;
		ratio = newWidth / (float)input.width();
		float newHeight = ratio*(float)input.height();
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
}
