package nez.main;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import nez.util.ConsoleUtils;
import nez.util.UList;
import nez.util.UMap;

public class Recorder {
	final String logFile;
	Recorder(String logFile) {
		this.logFile = logFile;
	}
	
	class DataPoint {
		String key;
		Object value;
		DataPoint(String key, Object value) {
			this.key = key;
			this.value = value;
		}
	}
	
	private UList<DataPoint> dataPointList = new UList<DataPoint>(new DataPoint[64]);
	private UMap<DataPoint> dataPointMap = new UMap<DataPoint>();

	private void setDataPoint(String key, Object value) {
		if(!this.dataPointMap.hasKey(key)) {
			DataPoint d = new DataPoint(key, value);
			this.dataPointMap.put(key, d);
			this.dataPointList.add(d);
		}
		else {
			DataPoint d = this.dataPointMap.get(key);
			d.value = value;
		}
	}

	public final void setText(String key, String value) {
		this.setDataPoint(key, value);
	}

	public final void setFile(String key, String file) {
		int loc = file.lastIndexOf('/');
		if(loc > 0) {
			file = file.substring(loc+1);
		}
		this.setDataPoint(key, file);
	}

	public final void setCount(String key, long v) {
		this.setDataPoint(key, new Long(v));
	}

	public final void setDouble(String key, double d) {
		this.setDataPoint(key, d);
	}

	public final void setRatio(String key, long v, long v2) {
		double d = v;
		double d2 = v2;
		this.setDataPoint(key, new Double(d/d2));
	}

	public final String formatCommaSeparateValue() {
		StringBuilder sb = new StringBuilder();
		SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy/MM/dd");
		sb.append(sdf1.format(new Date()));
		for(DataPoint d : this.dataPointList) {
			sb.append(",");
			sb.append(d.key);
			sb.append(":,");
			if(d.value instanceof Double) {
				sb.append(String.format("%.5f", d.value));
			}
			else {
				sb.append(d.value);
			}
		}
		return sb.toString();
	}
	
	public final void log() {
		try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(this.logFile, true)))) {
			String csv = formatCommaSeparateValue();
		    Verbose.println("writing .. " + this.logFile + " " + csv);
			out.println(csv);
//			double cf = 0;
//			for(int i = 0; i < 16; i++) {
//				int n = 1 << i;
//				double f = (double)this.backtrackCount[i] / this.BacktrackCount;
//				cf += this.backtrackCount[i];
//				System.out.println(String.format("%d\t%d\t%2.3f\t%2.3f", n, this.backtrackCount[i], f, (cf / this.BacktrackCount)));
//				if(n > this.WorstBacktrackSize) break;
//			}
		}
		catch (IOException e) {
			ConsoleUtils.exit(1, "Can't write csv log: " + this.logFile);
		}
	}
	
	public final static void recordLatencyMS(Recorder rec, String key, long nanoT1, long nanoT2) {
		if(rec != null) {
			long t = (nanoT2 - nanoT1) / 1000; // [micro second]
			rec.setDouble(key + "[ms]", t / 1000.0);
		}
	}

	public final static void recordLatencyS(Recorder rec, String key, long nanoT1, long nanoT2) {
		if(rec != null) {
			long t = (nanoT2 - nanoT1) / 1000; // [micro second]
			rec.setDouble(key + "[s]", t / 10000000.0);
		}
	}

	public final static void recordThroughputKPS(Recorder rec, String key, long length, long nanoT1, long nanoT2) {
		if(rec != null) {
			long micro = (nanoT2 - nanoT1) / 1000; // [micro second]
			double sec = micro / 1000000.0;
			double thr = length / sec / 1024;
			rec.setDouble(key + "[KiB/s]", thr);
		}
	}


}
