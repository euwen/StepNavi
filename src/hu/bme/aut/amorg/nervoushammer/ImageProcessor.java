package hu.bme.aut.amorg.nervoushammer;

import hu.bme.aut.amorg.nervoushammer.filters.MedianFilter;

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

import android.content.Context;
import android.util.FloatMath;
import android.util.Log;

public class ImageProcessor {

	//private boolean USE_RESIZE = true;
	private float RESIZE_WIDTH = 240.0f;
	private static final int FEATURE_MATRIX_WIDTH = 16;
	private static final int FEATURE_MATRIX_HEIGHT = 16;
	
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
	
	CsvLogger rawlog = null;
	Context ctx;
	
	
	public ImageProcessor(Context c)
	{
		ctx = c;
	}
	
	public void process2(Mat input){
		
		if (rawlog == null){
			rawlog = new CsvLogger(ctx, "log_S4_raw_");
		}
		
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
		rawlog.add(0, howGood);
		for (int i=0; i< statuses.length; i++){
			if (statuses[i] == 0)
			{
				rawlog.add(i*3+1, new float[]{10000f,10000f,10000f},true);
				continue;
			}
			
			float l1 = FloatMath.sqrt((float) (to[i].x*to[i].x + to[i].y*to[i].y));
			float l2 = FloatMath.sqrt((float) (from[i].x*from[i].x + from[i].y*from[i].y));
			// points too close to center wont play
			float theta = 0.0f;
			if (!((l1 < RESIZE_WIDTH/5) || (l2 < RESIZE_WIDTH/5))){
				theta = (float) (Math.atan2(to[i].y/l1,to[i].x/l1) - Math.atan2(from[i].y/l2,from[i].x/l2)) ;
				if (theta < -Math.PI){
					theta += 2*Math.PI;
				}
				thetas.add(theta);
			}	
			
			float dx = (float)(to[i].x-from[i].x);
			float dy = (float)(to[i].y-from[i].y);
			medianX.insertShift(dx);
			medianY.insertShift(dy);
			rawlog.add(i*3+1, new float[]{dx,dy,theta},true);
			
			Core.line(input, 
					new Point(from[i].x/ratio, from[i].y/ratio), 
					new Point(to[i].x/ratio, to[i].y/ratio), 
					color1, 2);
			
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
	public void save()
	{
		rawlog.save();
	}
	public void begin()
	{
		rawlog = new CsvLogger(ctx, "log_S4_raw_");
	}
}
