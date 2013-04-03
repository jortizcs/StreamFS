import java.sql.*;

public class Datapoint {
    long ts = -1L;
    double val = 0.0;
    public Datapoint(Timestamp timestamp, double value){
        ts = timestamp.getTime();
        val = value;
    }

    public Timestamp getTimestamp(){
        return new Timestamp(ts);
    }

    public double getValue(){
        return val;
    }
}
