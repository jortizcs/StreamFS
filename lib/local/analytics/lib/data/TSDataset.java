package lib.data;

import org.json.*;
import lib.maths.*;
import java.util.Arrays;

public class TSDataset{
	private JSONArray dataset;
	private JSONObject datasetLookup;
	private String label = null;

	private InterpType iType = InterpType.LINEAR;

	public TSDataset(){
		dataset = new JSONArray();
		datasetLookup = new JSONObject();
	}

	public TSDataset(String l){
		label = l;
		dataset = new JSONArray();
		datasetLookup = new JSONObject();
	}

	public TSDataset(long[] timestamps, double[] values){
		if(timestamps.length != values.length)
			return;
		dataset = new JSONArray();
		datasetLookup = new JSONObject();
		Arrays.sort(timestamps);
		Arrays.sort(values);
		for(int i=0; i< timestamps.length; ++i)
			put(timestamps[i], values[i]);
	}

	public boolean putAll(long[] timestamps, double[] values){
		//System.out.println("putAll " + timestamps.length + " " + values.length);
		if(timestamps.length != values.length)
			return false;
		for(int i=0; i<timestamps.length; i++)
			this.put(timestamps[i], values[i]);
		return true;
	}

	public void setLabel(String l){
		label = l;
	}

	public String getLabel(){
		return label;
	}

	public void setInterpType(InterpType t){
		iType = t;
	}

	public InterpType getInterpType(){
		return iType;
	}

	public synchronized boolean put(long ts, double value){
		if(ts>0){
			try {
				JSONArray dp = new JSONArray();
				//System.out.println("ts=" + ts + "; val=" + value);
				dp.put(ts); dp.put(value);
				dataset.put(dp);
				datasetLookup.accumulate(new Long(ts).toString(), value);
				return true;
			} catch(Exception e){
				e.printStackTrace();
			}
		}
		return false;
	}

	public JSONArray getDataset(){
		return dataset;
	}
	
	public synchronized long[] timestamps(){

		long[] ts = new long[dataset.length()];
		try {
			for(int i=0; i<dataset.length(); i++)
				ts[i] = dataset.getJSONArray(i).getLong(0);
			Arrays.sort(ts);
			//System.out.println("timestamps(): " + Arrays.toString(ts));
		} catch(Exception e){
			e.printStackTrace();
		}
		return ts;
	}

	public boolean containsValForInput(long x){
		String xstr = new Long(x).toString();
		return datasetLookup.has(xstr);
	}

	/**
	 * Returns the data is order of increasing timestamp value;
	 */
	public synchronized double[] values(){

		long[] tstamps = this.timestamps();
		double[] v = new double[tstamps.length];
		try {
			for(int i=0; i<tstamps.length; i++)
				v[i] = datasetLookup.getDouble(new Long(tstamps[i]).toString());
		} catch(Exception e){
			e.printStackTrace();
		}
		return v;
	}

	public double value(long timestamp){
		try {
			Object values = datasetLookup.get(new Long(timestamp).toString());
			//System.out.println("values::" + values);
			if(values instanceof JSONArray){
				JSONArray vArray = (JSONArray) values;
				double vs = vArray.getDouble(0);
				return vs;
			} else {
				double vs = (double)values;
				return vs;
			}
		} catch(Exception e){
			e.printStackTrace();
		}
		return -1.0;
	}
}
