import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;
import org.json.simple.*;
import org.json.simple.parser.*;
import java.lang.StringBuffer;
import java.util.concurrent.*;
import java.util.zip.*;

public class TsdbShimDirect {

    //localport
    private String bindAddress = "localhost";
    private static int localport = 1338;
    private static final String rootPath = "/";

    //opentsdb
    private static String tsdbHome = "/Users/jortiz/work/opentsdb/";
    private static String crtMetric = "tsdb mkmetric ";
    private static String tsdbUrl = "http://localhost:4242";
    private static String metricsq = "/suggest?type=metrics&q=";
    private static String dataq = "/q?start=2013/01/30-12:55:49&end=2013/01/30-12:59:00&m=avg:sbs.user.id&ascii";

    public TsdbShimDirect(){
    }

    public static void main(String[] args){
        TsdbShimDirect shim = new TsdbShimDirect();
        shim.runTsdbQuery("sfs.ts_data.raw", "/one/strm1", 
                "2013/02/22-00:00:00", "2013/02/22-00:00:00");
    }

    public JSONArray runTsdbQuery(String metric, String nodeName, String startTimeStr, String endTimeStr){
        URLConnection conn=null;
        BufferedReader reader = null;
        JSONArray data = new JSONArray();
        try {
            //  format_example:: 2011/06/18-04:50:06
            SimpleDateFormat sdf  = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
            Date startTime = sdf.parse(startTimeStr, new ParsePosition(0));
            Date endTime = sdf.parse(endTimeStr, new ParsePosition(0));
            long start = startTime.getTime()/1000;
            long end = endTime.getTime()/1000;

            StringBuffer tsdbRestQueryBuf = new StringBuffer().append(tsdbUrl).
                append("/q?start=").append(startTimeStr).
                append("&end=").append(endTimeStr).
                append("&m=sum:").append(metric).append("{node=").append(nodeName).append("}").
                append("&ascii");
            String tsdbRestQuery = tsdbRestQueryBuf.toString();

            System.out.println("query=" + tsdbRestQuery);

            tsdbRestQuery = URLDecoder.decode(tsdbRestQuery, "UTF-8");

            URL url = new URL(tsdbRestQuery);
            conn= url.openConnection();
            conn.setDoInput(true);
            conn.setConnectTimeout(5000);
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line= null;
            StringTokenizer tokenizer = null;
            while((line=reader.readLine())!=null){
                tokenizer = new StringTokenizer(line, " ");
                if(tokenizer.hasMoreTokens()){
                    String metric_ = tokenizer.nextToken();
                    String tsStr = tokenizer.nextToken();
                    String valStr = tokenizer.nextToken();
                    String hostStr = tokenizer.nextToken();
                    //String labelStr = tokenizer.nextToken();

                    long thisTs = Long.parseLong(tsStr);
                    double val = Double.parseDouble(valStr);
                    if(thisTs>=start && thisTs<=end){
                        JSONArray pair = new JSONArray();
                        pair.add(thisTs);
                        pair.add(val);
                        data.add(pair);
                    }
                }
            }
        } catch(Exception e){
            e.printStackTrace();
        } finally {
            try {
                if(reader!=null)
                    reader.close();
            } catch(Exception e){
                e.printStackTrace();
            }
        }
        return data;
    }
}
