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

import android.util.Log;

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
			Point[] pointsArray = new Point[FEATURE_COUNT];
			for (int i=0; i<FEATURE_COUNT; i++){
				pointsArray[i] = new Point(0, 0);
			}
			tempPoints = new MatOfPoint(pointsArray);
			Imgproc.goodFeaturesToTrack(
					currentFrame, 	// the image
					tempPoints, 	// the output detected features
					FEATURE_COUNT,	// the maximum number of features
					0.01, 			// quality level
					0.01 			// min distance between two features
			);
			if ((tempPoints.size().width <= 0) || (tempPoints.size().height <= 0)){
				return;
			}
			prevPoints = new MatOfPoint2f(tempPoints.toArray());
			prevFrame = currentFrame.clone();
			currentPoints = new MatOfPoint2f(prevPoints.toArray());
			state = STATE_MATCH;
			Log.d(this.getClass().getSimpleName(), "Searched");
		}
		else if (state == STATE_MATCH){
			currentPoints = new MatOfPoint2f();
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
			
			byte[] statuses = status.toArray();
			Point[] from = prevPoints.toArray();
			Point[] to = currentPoints.toArray();
			Scalar color = new Scalar(255, 128, 32);
			float howGood = 0.0f;
			for (int i=0; i< statuses.length; i++){
				if (statuses[i] == 0) continue;
				howGood += 1.0f;
				Core.line(inputFrame, 
						new Point(from[i].x/ratio, from[i].y/ratio), 
						new Point(to[i].x/ratio, to[i].y/ratio), 
						color);
			}
			howGood = howGood / FEATURE_COUNT;
			Log.d(this.getClass().getSimpleName(), "Find: " + howGood);
			
			if (howGood < 0.5f){
				state = STATE_SEARCH;
			} else {
				prevFrame = currentFrame.clone();
				prevPoints = currentPoints;
			}
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
