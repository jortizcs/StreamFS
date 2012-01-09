package lib.maths;

import lib.data.TSDataset;
import org.json.*;
import java.util.*;

public class Interpolator{
	
	private Interpolator(){}

	private static String calledFrom = null;

	private static double linearInterp(JSONArray point1, JSONArray point2, long t) throws Exception{
		if(point1.length()!=2 || point2.length()!=2)
			throw new Exception("points MUST have 2 values");
		//y=mt+b
		long t1 = point1.getLong(0);
		long t2 = point2.getLong(0);
		double y1 = point1.getDouble(1);
		double y2 = point2.getDouble(1);

		double m = (y2-y1)/((double)t2-t1);
		double b = y2 - m*(double)t2;
		double new_y = m*(double)t + b;
		//System.out.println(calledFrom + ":: (" + t1 + "," + y1 + ")\t" + "(" + t2 + "," + y2 + "); Input=" + t + ", y_new=" + new_y);
		return new_y;
	}

	private static double stepInterp(JSONArray point1, JSONArray point2, long x) throws Exception{
		if(point1.length()!=2 || point2.length()!=2)
			throw new Exception("points MUST have 2 values");
		long pt1_ts = point1.getLong(0);
		long pt2_ts = point2.getLong(0);

		if(Math.abs(x-pt1_ts)>Math.abs(x-pt2_ts)){
			return point1.getLong(1);
		} else if(Math.abs(x-pt1_ts)<Math.abs(x-pt2_ts)){
			return point2.getLong(1);
		} else{
			return point1.getLong(1);
		}
	}

	public static void main(String[] args){
		try {
			Random r = new Random();
			TSDataset ds1 = new TSDataset();
			TSDataset ds2 = new TSDataset();
			for(int i=0; i<10; ++i){
				long ts1 = r.nextLong()%1322357385;
				while(ts1<0)
					ts1 = r.nextLong()%1322357385;
				long ts2 = r.nextLong()%1322357385;
				while(ts2<0)
					ts2 = r.nextLong()%1322357385;
				ds1.put(ts1, r.nextDouble());
				ds2.put(ts2, r.nextDouble());
			}

			Vector<TSDataset> vd = new Vector<TSDataset>();
			vd.add(ds1); vd.add(ds2);
			interp(vd);
		} catch(Exception e){
			e.printStackTrace();
		}
	}

	public static Vector<TSDataset> interp(Vector<TSDataset> datasets){
		try {
			JSONObject acc = new JSONObject();
			for(int i=0; i<datasets.size(); i++){
				TSDataset dset = datasets.elementAt(i);
				long[] timestamps = dset.timestamps();
				for(int j=0; j<timestamps.length; ++j)
					acc.put(new Long(timestamps[j]).toString(), true);
			}

			//sort the list of all timestamps
			Iterator keys = acc.keys();
			ArrayList<Long> keys2 = new ArrayList<Long>();
			while(keys.hasNext())
				keys2.add((new Long((String)keys.next())).longValue());

			for(int i=0; i<datasets.size(); i++){
				TSDataset thisDataset = datasets.elementAt(i);
				if(thisDataset.getDataset().length()>0){
					Long[] inputs = new Long[keys2.size()];
					inputs = keys2.toArray(inputs);
					Arrays.sort(inputs);
					//System.out.println("pre_stretch::" + thisDataset.getDataset().toString());
					stretch(thisDataset, inputs, thisDataset.getInterpType());
					//System.out.println("post_stretch::" + thisDataset.getDataset().toString());
					fill(thisDataset, inputs, thisDataset.getInterpType());
					//System.out.println("post_fill::" + thisDataset.getDataset().toString() + "\n\n\n");
				} else {
					//if there are no values in this dataset, initialize to 0
					for(int j=0; j<keys2.size(); j++)
						thisDataset.put(keys2.get(j), 0.0);
				}
			}
		} catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}

	private static void stretch(TSDataset thisDataset,Long[] inputs, InterpType TYPE){
		try {
			long[] timestamps = thisDataset.timestamps();
			Arrays.sort(timestamps);
			if(timestamps.length>0){
				long mints = timestamps[0]; long maxts = timestamps[timestamps.length-1];
				for(int i =0; i<inputs.length; i++){
					if(inputs[i].longValue()<mints){
						double new_y =-1;
						if(timestamps.length==1){
							new_y = thisDataset.value(timestamps[0]);
						} else {
							JSONArray p1 = new JSONArray();
							JSONArray p2 = new JSONArray();
							p1.put(timestamps[0]); p1.put(thisDataset.value(timestamps[0]));
							p2.put(timestamps[1]); p2.put(thisDataset.value(timestamps[1]));
							switch(TYPE){
								case LINEAR:
									calledFrom = "stretch::linear";
									new_y = linearInterp(p1, p2, inputs[i].longValue());
									 break;
								case STEP:
									calledFrom = "stretch::step";
									 new_y = stepInterp(p1, p2, inputs[i].longValue());
									 break;
								default: 
									calledFrom = "stretch::linear";
									 new_y = linearInterp(p1, p2, inputs[i].longValue());
									 break;
							}
						}
						thisDataset.put(inputs[i].longValue(), new_y);
						mints=  inputs[i].longValue();
					} else if(inputs[i].longValue()>maxts){
						double new_y =-1;
						if(timestamps.length==1){
							new_y = thisDataset.value(timestamps[0]);
						} else {
							JSONArray p1 = new JSONArray();
							JSONArray p2 = new JSONArray();
							p1.put(timestamps[timestamps.length-1]); p1.put(thisDataset.value(timestamps[timestamps.length-1]));
							p2.put(timestamps[timestamps.length-2]); p2.put(thisDataset.value(timestamps[timestamps.length-2]));
							switch(TYPE){
								case LINEAR:calledFrom = "stretch::linear";
									new_y = linearInterp(p1, p2, inputs[i].longValue());
									 break;
								case STEP:
									calledFrom = "stretch::step";
									 new_y = stepInterp(p1, p2, inputs[i].longValue());
									 break;
								default:calledFrom = "stretch::linear"; new_y = linearInterp(p1, p2, inputs[i].longValue());
									 break;
							}
							//new_y = linearInterp(p1, p2, inputs[i].longValue());
						}
						thisDataset.put(inputs[i].longValue(), new_y);
						maxts=  inputs[i].longValue();
					}
				}
			}
		} catch(Exception e){
			e.printStackTrace();
		}
	}

	private static void fill(TSDataset thisDataset, Long[] inputs, InterpType TYPE){
		try {
			long[] timestamps = thisDataset.timestamps();
			Arrays.sort(timestamps);
			if(timestamps.length>0){
				long mints = timestamps[0]; long maxts = timestamps[timestamps.length-1];
				for(int i =0; i<inputs.length; i++){
					if(!thisDataset.containsValForInput(inputs[i].longValue())){
						JSONArray pts = bookends(thisDataset, inputs[i].longValue());
						//System.out.println("pts="+pts.toString());
						double new_y = -1;
						JSONArray p1= pts.getJSONArray(0); JSONArray p2= pts.getJSONArray(1);
						switch(TYPE){
							case LINEAR:calledFrom = "fill::linear";
								new_y = linearInterp(p1, p2, inputs[i].longValue());
								 break;
							case STEP:calledFrom = "fill::step";
								 new_y = stepInterp(p1, p2, inputs[i].longValue());
								 break;
							default:calledFrom = "fill::linear"; new_y = linearInterp(p1, p2, inputs[i].longValue());
								 break;
						}
						//thisDataset.put(inputs[i].longValue(), linearInterp(pts.getJSONArray(0), pts.getJSONArray(1), inputs[i].longValue()));
						//System.out.println("new_y=" + new_y);
						thisDataset.put(inputs[i].longValue(),new_y);
					}
				}
			}
		} catch(Exception e){
			e.printStackTrace();
		}
	}

	private static JSONArray bookends(TSDataset dataset, long ts){
		JSONArray bookends=new JSONArray();
		try {
			long[] timestamps = dataset.timestamps();
			Arrays.sort(timestamps);
			int min=0;
			int max=timestamps.length-1;
			//System.out.println("[min= " + min + ", max=" + max + ", ts="+ ts+ "]");
			int mid=-1;
			do{
				mid = (min+max)/2;
				if(ts>timestamps[mid])
					min = mid +1;
				else
					max = mid-1;
			} while(timestamps[mid]!=ts && min<max);
			
			if(timestamps[mid]!=ts){

				if(min<max){
					//System.out.println("Not Found, " + min + " " + max);
					JSONArray pt1 = new JSONArray();
					JSONArray pt2 = new JSONArray();
					pt1.put(timestamps[max]); pt1.put(dataset.value(timestamps[max]));
					pt2.put(timestamps[min]); pt2.put(dataset.value(timestamps[min]));
					bookends.put(pt1);bookends.put(pt2);
				} else {
					//System.out.println("Not Found2, " + min + " " + max);
					JSONArray pt1 = new JSONArray();
					JSONArray pt2 = new JSONArray();
					if(min==0)
						max = min+1;
					else if(min==timestamps.length-1)
						max = min-1;
					else
						max=min+1;
					pt1.put(timestamps[max]); pt1.put(dataset.value(timestamps[max]));
					pt2.put(timestamps[min]); pt2.put(dataset.value(timestamps[min]));
					bookends.put(pt1);bookends.put(pt2);
				}
			} else {
				//System.out.println("Found");
				JSONArray p = new JSONArray();
				p.put(timestamps[mid]); p.put(dataset.value(timestamps[mid]));
				bookends.put(p);
			}
		} catch(Exception e){
			e.printStackTrace();
		}
		return bookends;
	}
}
