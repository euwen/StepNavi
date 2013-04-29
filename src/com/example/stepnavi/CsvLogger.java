package com.example.stepnavi;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

public class CsvLogger {

	private Context context;
	private int columnCount;
	private String logPrefix;
	
	public ArrayList<Long> times;
	public ArrayList<Float> data;
	
	public CsvLogger(Context context, int columnCount, String logPrefix)
	{
		data = new ArrayList<Float>();
		times = new ArrayList<Long>();
		
		this.context = context;
		this.columnCount = columnCount;
		this.logPrefix = logPrefix;
	}
	
	public void save()
	{
        try {
            File myFile = new File(Environment.getExternalStorageDirectory().getPath()+"/"+ logPrefix +System.currentTimeMillis()+".csv");
            myFile.createNewFile();
            BufferedWriter bw = new BufferedWriter(new FileWriter(myFile));
            for (int i=0; i<times.size(); i++)
            {
            	bw.append(times.get(i).toString());
            	bw.append(";");
            	
            	for (int j=0; j<columnCount-1; j++)
            	{
	            	bw.append(data.get(columnCount*i+j).toString());
	            	bw.append(";");
            	}
            	bw.append(data.get(columnCount*i+columnCount-1).toString());
            	
            	bw.newLine();
            }
            
            bw.close();
            Toast.makeText(context,"Done writing SD", Toast.LENGTH_SHORT).show();
        } 
        catch (Exception e) 
        {
            Toast.makeText(context, e.getMessage(),Toast.LENGTH_SHORT).show();
        }
	}
	
}
