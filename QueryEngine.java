import java.sql.*;
import java.util.*;
import java.text.*;
import java.io.*;
import java.nio.*;

public class QueryEngine {
    public static NameServer n = new NameServer();
    public static TsdbShim tsdb = new TsdhShim();
    public QueryEngine(){}

    public static void main(String[] args){
        try{
            QueryEngine qe = new QueryEngine();
            SimpleDateFormat sdf  = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            java.util.Date start = sdf.parse("2013-01-30 12:55:49", new ParsePosition(0));
            java.util.Date end = sdf.parse("2013-01-30 12:55:57", new ParsePosition(0));
            TimeRange range = new TimeRange(new Timestamp(start.getTime()),new Timestamp(end.getTime()));
            qe.queryNode("/one", range);
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Queries a node for the timeseries data associated with it's existence.
     * For example, if you query /one/two, it will only fetch the data associated with /one/two during the times
     * it existed in that interval.
     */
    public List<Datapoint> queryNode(String name, TimeRange range) throws Exception{
        Connection conn = null;
        try {
            conn = n.pool.getConnection(1000);
            ArrayList<Integer> ids = new ArrayList<Integer>();
            //1.  query the index table to load the associated indices.
            StringBuffer query = new StringBuffer().append("select `id` from `offsets` where unix_timestamp(offsets.ts)>=? and unix_timestamp(offsets.ts)<= ?");
            PreparedStatement ps = conn.prepareStatement(query.toString());
            ps.setLong(1, range.getStartTimeSec());
            ps.setLong(2, range.getEndTimeSec());

            System.out.println(query.toString().replaceFirst("\\?",new Long(range.getStartTimeSec()).toString()).replaceFirst("\\?",new Long(range.getEndTimeSec()).toString()));
            ResultSet rs = ps.executeQuery();
            while(rs.next()){
                //populate an id set to be used in the next query
                ids.add(new Integer(rs.getInt("id")));
            }
            ps.close();
           
            //get the index for the state of the graph at the start of this range query. 
            query = new StringBuffer().append("select `id` from offsets where offsets.id=(select max(id) as maxid from offsets where unix_timestamp(offsets.ts)<=?)");
            ps = conn.prepareStatement(query.toString());
            ps.setLong(1, range.getStartTimeSec());
            System.out.println(query.toString().replaceFirst("\\?",new Long(range.getStartTimeSec()).toString()));
            rs = ps.executeQuery();
            while(rs.next()){
                Integer newid = new Integer(rs.getInt("id"));
                if(!ids.contains(newid))
                    //populate an id set to be used in the next query
                    ids.add(new Integer(rs.getInt("id")));
            }
            ps.close();

            //2.  query the names table in this range to determine the id's associated with specific time ranges
            //      - inner join
            query = new StringBuffer().append("select names.id as id, ts, index_filepath, offset, name from offsets, names where names.created=offsets.ts and name=? and offsets.id in (");
            int s = ids.size();
            for(int i=0; i<s; ++i){
                query.append(ids.get(i).toString());
                if(i<s-1)
                    query.append(",");
            }
            query.append(")");
            ps = conn.prepareStatement(query.toString());
            ps.setString(1, name);
            System.out.println(query.toString().replaceFirst("\\?",name));
            rs = ps.executeQuery();
            ArrayList<RowSet> rows = new ArrayList<RowSet>();
            while(rs.next()){
                RowSet r = new RowSet(rs.getInt("id"), rs.getTimestamp("ts"), rs.getString("index_filepath"), 
                    rs.getLong("offset"), rs.getString("name"));
                rows.add(r);
            }
            ps.close();

            ArrayList<TimeRange> ranges = new ArrayList<TimeRange>();
            //3.  load a list of time ranges over which the node in question existed.

            Timestamp st = null;
            Timestamp et = null;
            for(int i=0; i<rows.size(); i++){
                RowSet thisRow = rows.get(i);
                System.out.println(thisRow);
                //grab the index (id) for this node 
                //load the bit set, check the bit at index(id)
                //if it's set and the start time (st) is null, set it, otherwise go to the next one

                //if it's not set and start time(st) is null, keep going, otherwise set the end time (et) to the current
                // time -1; then create a timerange object using st, et, add it to the time-range list and keep going.
                boolean exists = isSet(thisRow);
                if(exists && st==null){
                    st = thisRow.getTimestamp();
                } else if(!exists && st!=null){
                    et = new Timestamp(thisRow.getTimestamp().getTime()-1);
                } 

                if(i==rows.size()-1 && st!=null && et==null){
                    et = new Timestamp(System.currentTimeMillis());
                }

                //record the range if it's set
                if(st!=null && et!=null){
                    //record the range
                    try {
                        TimeRange tr = new TimeRange(st,et);
                        ranges.add(tr);
                    } catch(Exception e){
                        e.printStackTrace();
                    }
                    st=null;
                    et=null;
                }
            }

            //test only
            System.out.println("Count(TimeRanges)=" + ranges.size());
            for(int i=0; i<ranges.size(); i++)
                System.out.println(ranges.get(i));

            //4.  apply the time ranges and compile a list of data point for data in those time intervals.
            for(int i=0; i<ranges.size(); i++){
                JSONArray dataArray = tsdb.runTsdbQuery(startTimeStr, endTimeStr, "sbs.user.123");
            }
        } catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }

    private List<Datapoint> getTsDatapoints(TimeRange range, String metric, String label){
        try {
            //http://localhost:4242/q?start=2011/10/09-11:18:47&end=2011/10/18-08:59:38&m=sum:sbs.user.123&ascii
            URL url = new URL("http://localhost:4242/q?start=2011/10/09-11:18:47&end=2011/10/18-08:59:38&m=sum:sbs.user.123&ascii");
            HttpURLConnection conn = new HttpURLConnection(url);
            //formati_ex: sbs.user.123 1318952940 2413.219970703125 host=jortiz-laptop label=11F_EHP_Pana_DLL1_PAC11-4_112C1-C2
            //[metric] [ts] [value] host=[host] [label]
        } catch(Exception e){
            e.printStackTrace();
        }

    }

    /**
     * Opens the index file, loads the index and checks returns the value of the bit at the id index set 
     * in this RowSet object, rs.
     */
    public boolean isSet(RowSet rs){
        RandomAccessFile indexFile =null;
        try {
            indexFile = new RandomAccessFile(rs.getIndexFilepath(), "r");
            indexFile.seek(rs.getOffset());
            byte[] bitvecLengthBuf = new byte[4];
            indexFile.read(bitvecLengthBuf, 0, 4);
            int bitvecLength = ByteBuffer.allocate(4).
                put(bitvecLengthBuf).getInt(0);
            byte[] index = new byte[bitvecLength];
            indexFile.read(index, 0, bitvecLength);
            BitSet namesIndex = BitSet.valueOf(index);
            return namesIndex.get(rs.getId());
        } catch(Exception e){
            e.printStackTrace();
        } finally {
            try {
                indexFile.close();
            } catch(Exception e){}
        }
        return false;
    }

    /**
     * Returns the time intervals over which there exists a path from the src node to the destination node.
     */
    public List<TimeRange> pathExistTimes(String src, String dst, TimeRange range){
        return null;
    }

    /**
     * Returns the time intervals over which there exists a path from the src node to the destination node.
     */
    public List<TimeRange> pathsNotExistTimes(String src, String dst, TimeRange range){
        return null;
    }

    /**
     * Returns the time intervals over which there exists a path from the src node to the a destination
     * node with the given tags.
     */
    public List<TimeRange> pathExistTimes(String src, String[] dsttags, TimeRange range){
        return null;
    }

    /**
     * Returns the time intervals over which there exists a path from the src node to the a destination
     * node with the given tags.
     */
    public List<TimeRange> pathExistTimes(String[] srctags, String dst, TimeRange range){
        return null;
    }

    /**
     * Returns the time intervals over which there exists a path from the src node with the given tag set to a
     * node with the given tag set.
     */
    public List<TimeRange> pathExistTimes(String[] srctags, String[] dsttags, TimeRange range){
        return null;
    }

    /**
     * Returns the data over which there exists a path from the src node to a
     * node with the given tag set.
     */
    public List<Datapoint> getPathData(String src, String[] dsttags, TimeRange range){
        return null;
    }

    /**
     * Returns the data over which there exists a path from the src node to a
     * node with the given tag set.
     */
    public List<Datapoint> getPathData(String[] srctags, String[] dsttags, TimeRange range){
        return null;
    }

    public boolean isSet(String indexFile, long offset, long nodeId){
        //open the file, seek to the offset, load the bitset, check the bitset at index nodeId, return that.
        return false;
    }

    public class RowSet {
        private int id = -1;
        private  Timestamp ts = null;
        private String indexFilepath = null;
        private long offset = -1L;
        private String nodeName = null;
        private String str = null;

        public RowSet(int id_, Timestamp t, String indexFile, long off, String name){
            id = id_;
            ts = t;
            indexFilepath = indexFile;
            offset =off;
            nodeName = name;

            StringBuffer strbuf = new StringBuffer().append("[id=").append(new Integer(id).toString()).append(",");
            strbuf.append(",ts=").append(ts.toString()).append(",indexFilepath=").append(indexFilepath);
            strbuf.append(",offset=").append(new Long(offset).toString());
            strbuf.append(",nodeName=").append(nodeName).append("]");
            str = strbuf.toString();
        }

        public int getId(){
            return id;
        }

        public String toString(){
            return str;
        }

        public Timestamp getTimestamp(){
            return ts;
        }

        public String getIndexFilepath(){
            return indexFilepath;
        }

        public long getOffset(){
            return offset;
        }

        public String getNodeName(){
            return nodeName;
        }
    }
}
