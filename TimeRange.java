import java.sql.*;

public class TimeRange{
    private long startTime = -1L;
    private long endTime = -1L;
    private String str = null;

    public TimeRange(Timestamp start, Timestamp end) throws Exception{
        if(start.before(end) || start.equals(end)){
            //getTime() returns the number of milliseconds since jan1,1970 gmt
            startTime = start.getTime(); 
            endTime = end.getTime();

            StringBuffer strbuf = new StringBuffer().append("[start=" + start.toString());
            strbuf.append(",end=" + end.toString()).append("]");
            str = strbuf.toString();
        } else {
            throw new Exception ("start must be <= end");
        }
    }

    public long getStartTimeSec(){
        return startTime/1000;
    }

    public long getEndTimeSec(){
        return endTime/1000;
    }

    public String toString(){
        return str;
    }
}
