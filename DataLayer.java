import java.util.*;
import org.json.simple.*;
import java.text.*;
import java.net.*;
import java.io.*;

public class DataLayer {

    public String host = "localhost";
    public int port = 4242;
    private String metric = "sfs.ts_data.raw";
    public String shimHost = "localhost";
    public int shimPort = 1338;
    public Socket inputSocket = null;
    public TsdbShimDirect shim = null;
    public static boolean createdMetric = false;

    public static SimpleDateFormat sdf  = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");

    /**
     *
     */
    public static void main(String[] args){
        Date d = sdf.parse("2013/02/22-00:00:00", new ParsePosition(0));
        Date e = sdf.parse("2013/02/23-00:00:00", new ParsePosition(0));
        try {
            DataLayer dl = new DataLayer();
            dl.generate("/one/str1", d, e, 900L, 0L);
        } catch(Exception err){
            err.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Instantiate the data layer.
     */
    public DataLayer() throws Exception{
        try {
            createMetric();
            inputSocket = new Socket(InetAddress.getByName(host), port);
        } catch(Exception e){
            throw e;
        }
        shim = new TsdbShimDirect();
    }

    public void createMetric() throws Exception{
        if(!createdMetric){
            String openTsdbPath=null;
            try {
                openTsdbPath = System.getenv().get("OPENTSDB_HOME");
                if(openTsdbPath!=null){
                    StringBuffer cmd = new StringBuffer().append(openTsdbPath).
                        append("/build/tsdb mkmetric ").append(metric);
                    System.out.println("+++Executing: " + cmd);
                    Process p = Runtime.getRuntime().exec(cmd.toString());
                    System.out.println(p);
                    BufferedReader in = new BufferedReader(
                                           new InputStreamReader(p.getInputStream()) );
                    String line = null;
                    while ((line = in.readLine()) != null) 
                        System.out.println(line);
                    in.close();
                    p.waitFor();
                    System.out.println("done");
                    createdMetric = true;
                    return;
                }
            } catch(Exception e){
                e.printStackTrace();
            }
        }
        //System.out.println("create metric throwing e");
        throw new Exception("OPENTSDB_HOME not set");
    }

    /**
     * Seconds.
     */
    public boolean generate(String nodeName, Date startTime, Date endTime, long period_ms, 
            long jitter_ms)
    {
        if(endTime.compareTo(startTime)>=0){
            JSONArray data = generateDataSec(period_ms/1000L, jitter_ms/1000L, startTime, endTime);
            //JSONArray data = generateDataMillis(period_ms, jitter_ms, startTime, endTime);
            //now put it in opentsdb
            return insertData(nodeName, data);
        }
        return false;
    }

    public boolean insertData(String label, JSONArray data){
        if(data==null)
            return false;
    
        OutputStream os;
        try {
            os = inputSocket.getOutputStream();
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
        try {
            //  put proc.loadavg.1m 1288946927 0.36 host=foo
            for(int i=0; i<data.size(); i++){
                JSONArray dp =(JSONArray) data.get(i);
                StringBuffer strbuf = new StringBuffer().append("put ");
                strbuf.append(metric).append(" ");
                strbuf.append(dp.get(0)).append(" ").append(dp.get(1));
                strbuf.append(" node=").append(label).append("\n");
                String str = strbuf.toString();


                //put the data in the database
                try {
                    if(inputSocket != null){
                        os.write(str.getBytes());
                        os.flush();
                        System.out.print(str);
                    }
                } catch(Exception innerE1){
                    innerE1.printStackTrace();
                }
            }
            return true;
        } catch(Exception e){
            e.printStackTrace();
        } finally {
            try{
                inputSocket.close();
            } catch(Exception e){e.printStackTrace();}
        }
        return false;
    }

    public JSONArray fetch(String nodeName, String startTime, String endTime){
        return shim.runTsdbQuery(metric, nodeName, startTime, endTime);
    }


    /**
     *  Generates fake data at the given period from the given start time.
     *  Jitter is used to add random noise to the data generation time.  The values
     *  genrated are uniformly distributed between 0 and 1.
     */
    public JSONArray generateDataMillis(long T_ms, long jitter_ms, Date start, Date end){
        JSONArray data = new JSONArray();
        Random r = new Random();
        try {
            if(start !=null){
                long start_ms = start.getTime();
                long this_ms = start_ms;
                long end_ms= 0L;
                if(end==null)
                    end_ms = System.currentTimeMillis();
                else
                    end_ms = end.getTime();
                while(this_ms<=end_ms){
                    long ts = 0L;
                    if(jitter_ms>0)
                        ts = this_ms + r.nextLong()%jitter_ms;
                    else
                        ts =this_ms;
                    double value = r.nextDouble();
                    JSONArray pair = new JSONArray();
                    pair.add(ts);
                    pair.add(value);
                    data.add(pair);
                    this_ms += T_ms;
                }
            }
        } catch(Exception e){
            e.printStackTrace();
        }
        System.out.println("data_array.size=" + data.size());
        return data;
    }

    /**
     *  Generates fake data at the given period from the given start time.
     *  Jitter is used to add random noise to the data generation time.  The values
     *  genrated are uniformly distributed between 0 and 1.
     */
    public JSONArray generateDataSec(long T_sec, long jitter_sec, Date start, Date end){
        JSONArray data = new JSONArray();
        Random r = new Random();
        try {
            if(start !=null){
                long start_sec = start.getTime();
                long this_sec = start_sec;
                long end_sec= 0L;
                if(end==null)
                    end_sec = System.currentTimeMillis();
                else
                    end_sec = end.getTime();
                while(this_sec<=end_sec){
                    long ts = 0L;
                    if(jitter_sec>0)
                        ts = this_sec + r.nextLong()%(jitter_sec*1000L);
                    else
                        ts =this_sec;
                    double value = r.nextDouble();
                    JSONArray pair = new JSONArray();
                    pair.add(ts/1000L);
                    pair.add(value);
                    data.add(pair);
                    this_sec += T_sec*1000L;
                }
            }
        } catch(Exception e){
            e.printStackTrace();
        }
        System.out.println("data_array.size=" + data.size());
        return data;
    }

    public void deleteData(String nodeName, String startTime, String endTime)
        throws Exception
    {

        //./build/tsdb scan --delete 2000/01/01 $(date --date='2 weeks ago'
         //       +'%Y/%m/%d') sum $metric

        String openTsdbPath=null;
        try {
            openTsdbPath = System.getenv().get("OPENTSDB_HOME");
            if(openTsdbPath!=null){
                StringBuffer cmd = new StringBuffer().append(openTsdbPath).
                    append("/build/tsdb scan --delete ").
                    append(startTime).append(" ").append(endTime).
                    append(" sum ").append(metric);
                System.out.println("+++Executing: " + cmd);
                Process p = Runtime.getRuntime().exec(cmd.toString());
                System.out.println(p);
                BufferedReader in = new BufferedReader(
                                       new InputStreamReader(p.getInputStream()) );
                String line = null;
                while ((line = in.readLine()) != null) 
                    System.out.println(line);
                in.close();
                p.waitFor();
                System.out.println("done");
                return;
            }
        } catch(Exception e){
            e.printStackTrace();
        }
        //System.out.println("create metric throwing e");
        throw new Exception("OPENTSDB_HOME not set");
    }
}
