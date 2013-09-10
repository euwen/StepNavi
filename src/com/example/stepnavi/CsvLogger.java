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
	private String logPrefix;

	private ArrayList<Long> times;
	private ArrayList<Float> data;
	private ArrayList<Integer> columns;

	private long beginning = 0;

	public CsvLogger(Context context, String logPrefix) {
		data = new ArrayList<Float>();
		times = new ArrayList<Long>();
		columns = new ArrayList<Integer>();

		this.context = context;
		this.logPrefix = logPrefix;
	}

	public void add(int column, float value) {
		checkBeginning();
		times.add(System.currentTimeMillis());
		data.add(value);
		columns.add(column);
	}

	public void add(int startColumn, float[] values) {
		checkBeginning();
		for (int i = 0; i < values.length; i++) {
			if (i == 0) {
				times.add(System.currentTimeMillis());
			} else {
				times.add((long) -1);
			}
			data.add(values[i]);
			columns.add(startColumn + i);
		}
	}
	
	public void add(int startColumn, float[] values, boolean keepTime ) {
		if (keepTime == false) {
			add(startColumn, values);
			return;
		}
		checkBeginning();
		for (int i = 0; i < values.length; i++) {
			times.add((long) -1);
			data.add(values[i]);
			columns.add(startColumn + i);
		}
	}
	
	public void reset(){
		beginning = 0;
		data = new ArrayList<Float>();
		times = new ArrayList<Long>();
		columns = new ArrayList<Integer>();
	}

	private void checkBeginning() {
		if (beginning == 0) {
			beginning = System.currentTimeMillis();
		}
	}

	public void save() {
		try {
			File myFile = new File(Environment.getExternalStorageDirectory()
					.getPath()
					+ "/"
					+ logPrefix
					+ System.currentTimeMillis()
					+ ".csv");
			myFile.createNewFile();
			BufferedWriter bw = new BufferedWriter(new FileWriter(myFile));

			int width = 0;
			for (int i = 0; i < columns.size(); i++) {
				if (width < columns.get(i)) {
					width = columns.get(i);
				}
			}

			for (int i = 0; i < times.size(); i++) {
				
				int pos = i;
				int combo = 0;
				do { 
					if (times.size()-1 < pos+1) break;
					pos++;
					if (times.get(pos) == -1){
						combo++;
					} else {
						break;
					}
				} while (true);

				bw.append(Long.valueOf(times.get(i)-beginning).toString());
				bw.append(";");
				
				int column = columns.get(i);
				for (int j = 0; j < column; j++) {
					bw.append(";");
				}

				for (int j=0; j<=combo; j++){
					bw.append(data.get(i+j).toString());
					bw.append(";");
				}

				int left = width - column - combo - 1;
				for (int j = 0; j <= left; j++) {
					bw.append(";");
				}

				bw.newLine();
				
				i+=combo;
			}

			bw.close();
			Toast.makeText(context, "Done writing SD", Toast.LENGTH_SHORT)
					.show();
		} catch (Exception e) {
			Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
		}
	}

}
