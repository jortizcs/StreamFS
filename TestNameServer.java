import snaq.db.*;
import java.text.*;

import java.net.*;
import java.util.*;
import javax.sql.rowset.serial.*;
import java.nio.*;
import java.io.*;

import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.Timestamp;


/**
 * Name Server testing.  Creates and deletes resources and runs historical
 * metadata queries.  Measures and compares various NameServer implementations
 * and outputs their performance for comparison.
 */
public class TestNameServer{

    private static String HOST = "localhost";
	private static int PORT = 3306;
	private static String LOGIN = "root";
	private static String PW = "root";
	private static String dbName = "mdataprov";
	protected static ConnectionPool pool = null;

    //CommandArgs args = null;
    ArrayList<CommandArgs> dataCmds = new ArrayList<CommandArgs>();

    SimpleDateFormat sdf  = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");

    public static void main(String args[]){
        TestNameServer test = new TestNameServer();
        if(args.length==1 && args[0].equals("reset")){
            test.setupdb();
            test.reset();
            System.exit(1);
        }
        test.playCreateLog();
    }
    
    public TestNameServer(){}

    public void playCreateLog(){
        try {
            NameServer ns = new NameServer();
            DataLayer dg = new DataLayer();
            //Runtime.getRuntime().addShutdownHook(new ShutdownThread());
            BufferedReader reader = new BufferedReader(new FileReader("create.log"));
            String line  = null;
            int cnt = 1;
            while((line=reader.readLine())!=null){
                //parse the operation and set the parameters, then run it
                try {
                    CommandArgs cmd = CommandArgs.parseArgs(line);
                    //ns.createNode(name, tags, "node");
                    //ns.destroyNode(name);
                    Timestamp t = null;
                    if(cmd.getTimestamp()!=null)
                        t = new Timestamp(cmd.getTimestamp().getTime());
                    switch(cmd.getOperation()){
                        case CREATE:
                            //System.out.println(cnt + " CREATE");
                            if(cmd.getType().toString().equalsIgnoreCase("link"))
                                ns.createNode(cmd.getNodeName(), cmd.getDestination(), cmd.getInfo(), cmd.getType().toString(), t);
                            else
                                ns.createNode(cmd.getNodeName(), null, cmd.getInfo(), cmd.getType().toString(), t);
                            break;
                        case DELETE:
                            //System.out.println(cnt + " DELETE");
                            ns.destroyNode(NameServer.cleanPath(cmd.getNodeName()), t);
                            break;
                        case DATA:
                            System.out.println(cnt + " DATA");

                            System.out.println("node="+ cmd.getNodeName());
                            System.out.println("start=" + cmd.getStartTimestamp());
                            System.out.println("end=" + cmd.getEndTimestamp());
                            System.out.println("period=" + cmd.getPeriod());
                            System.out.println("jitter=" + cmd.getJitter());
                            
                            boolean ret= dg.generate(cmd.getNodeName(), 
                                    cmd.getStartTimestamp(), cmd.getEndTimestamp(),
                                    cmd.getPeriod(),
                                    cmd.getJitter()
                                    );
                            System.out.println("ret="+ ret);
                            dataCmds.add(cmd);
                            break;
                        case QUERY:
                            //System.out.println(cnt + " QUERY");
                            long before = System.currentTimeMillis();

                            /*System.out.println("path=" + NameServer.cleanPath(cmd.getNodeName()));
                            System.out.println("cmd_1=" + cmd.getStartTimestamp());
                            System.out.println("cmd_2=" + cmd.getEndTimestamp());*/

                            dg.fetch(NameServer.cleanPath(cmd.getNodeName()),sdf.format(cmd.getStartTimestamp()), 
                                    sdf.format(cmd.getEndTimestamp()));
                            long diff = System.currentTimeMillis()-before;
                            System.out.println("fetch_time=" + diff + " ms");
                            break;
                    }
                } catch(Exception e){
                    e.printStackTrace();
                }
                cnt+=1;
            }
        } catch(Exception e){
            e.printStackTrace();
        }
    }


    public void popDataCmds(){
        try {
            BufferedReader reader = new BufferedReader(new FileReader("create.log"));
            String line  = null;
            int cnt = 1;
            while((line=reader.readLine())!=null){
                //parse the operation and set the parameters, then run it
                try {
                    CommandArgs cmd = CommandArgs.parseArgs(line);
                    switch(cmd.getOperation()){
                        case DATA:
                            System.out.println(cnt + " DATA");
                            dataCmds.add(cmd);
                            break;
                    }
                } catch(Exception e){
                    e.printStackTrace();
                }
                cnt+=1;
            }
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    public void setupdb(){
		try {
			if(pool == null){
				String url = "jdbc:mysql://localhost/" + dbName;
				Driver driver = (Driver)Class.forName ("com.mysql.jdbc.Driver").newInstance ();
				DriverManager.registerDriver(driver);
                pool = new ConnectionPool("sfs",            /*poolname*/
                                            5,              /*minpool*/ 
                                            150,  /*maxpool: mysql has a max of 151 connections*/
                                            150,            /*maxsize*/
                                            500,           /*timeout*/ 
                                            url, LOGIN, PW);
                if(pool == null){
                    System.out.println("couldn't create a pool");
                    System.exit(1);
                } 
                pool.setCaching(true);
                pool.setAsyncDestroy(true);
                AutoCommitValidator validator = new AutoCommitValidator();
                pool.setValidator(validator);
                ConnectionPoolManager.registerGlobalShutdownHook();
			} else {
                System.out.println("pool already made");
			}
		} catch (Exception e){
            e.printStackTrace();
            System.exit(1);
		}
    }

    public void reset(){
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            conn = pool.getConnection(1000);
            String query = "truncate `deleted`";
            ps = conn.prepareStatement(query);
            ps.executeQuery();
            ps.close();

            query ="truncate `names`";
            ps = conn.prepareStatement(query);
            ps.executeQuery();
            ps.close();

            query = "truncate `offsets`";
            ps = conn.prepareStatement(query);
            ps.executeQuery();
            ps.close();

            query = "truncate `links`";
            ps = conn.prepareStatement(query);
            ps.executeQuery();
            ps.close();

            File indexFile = new File("hist01.idx");
            indexFile.delete();

            SimpleDateFormat sdf  = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");
            System.out.println("Deleting data");
            popDataCmds();
            DataLayer dg = new DataLayer();
            for(int i=0; i<dataCmds.size(); i++){
                CommandArgs thisDelCmd = dataCmds.get(i);
                dg.deleteData(thisDelCmd.getNodeName(), 
                        sdf.format((Date)thisDelCmd.getStartTimestamp()),
                        sdf.format((Date)thisDelCmd.getEndTimestamp())
                        );
            }
        } catch(Exception e){
            e.printStackTrace();
        } finally {
            try {
                conn.close();
            } catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    public class ShutdownThread extends Thread{
        public void run(){
            reset();    
        }
    }


    public enum Op {
            CREATE,DELETE,DATA,QUERY,NONE
    }

    public enum Type {
            NODE,STRM,PROC,LINK
    }

    public static class CommandArgs{

        SimpleDateFormat sdf  = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss");

        private Op op = Op.NONE;
        private String n = null;
        private String dest = null;
        private String m = null;
        private Date ts = null;
        private Date st = null;
        private Date et = null;
        private Type t = Type.NODE;
        private long p = -1L;
        private long j = 0L;

        private CommandArgs(){}

        public static CommandArgs parseArgs(String args) throws Exception{
            CommandArgs cmdArgs = new CommandArgs();
            try {
                //parse an input line from the log
                StringTokenizer tokenizer = new StringTokenizer(args,",");
                Vector<String> tokens = new Vector<String>();
                while(tokenizer.hasMoreTokens()){
                    String attrVal = tokenizer.nextToken().replaceAll("\\s+","");
                    StringTokenizer t2 = new StringTokenizer(attrVal,"=");
                    if(t2.countTokens()==2){
                        String attr = t2.nextToken();
                        String value = t2.nextToken();
                        if(attr.equalsIgnoreCase("op")){
                            if(!cmdArgs.setOperation(value))
                                throw new Exception(args + "\nCould not set operation:[" + attr +"," + value+ "]");
                        } else if(attr.equalsIgnoreCase("n")){
                            if(!cmdArgs.setNodeName(value))
                                throw new Exception("Could not set node name:["+ attr +"," + value+ "]");
                        } else if(attr.equalsIgnoreCase("m")){
                            if(!cmdArgs.setInfo(value))
                                throw new Exception("Could not metadata info:["+ attr +"," + value+ "]");
                        } else if(attr.equalsIgnoreCase("t")){
                            if(!cmdArgs.setType(value))
                                throw new Exception(args+ "\nCould not set type:[" + attr +"," + value+ "]");
                        } else if(attr.equalsIgnoreCase("ts")){
                            if(!cmdArgs.setTimestamp(value))
                                throw new Exception("could not set timestamp:["+ attr +"," + value+ "]");
                        } else if(attr.equalsIgnoreCase("st")){
                            if(!cmdArgs.setStartTimestamp(value))
                                throw new Exception("could not set start time:["+ attr +"," + value+ "]");
                        } else if(attr.equalsIgnoreCase("et")){
                            if(!cmdArgs.setEndTimestamp(value))
                                throw new Exception("Could not set end time:["+ attr +"," + value+ "]");
                        } else if(attr.equalsIgnoreCase("p")){
                            if(!cmdArgs.setPeriod(value))
                                throw new Exception(args + "\nCould not set period:["+ attr +"," + value+ "]");
                        } else if(attr.equalsIgnoreCase("j")){
                            if(!cmdArgs.setJitter(value))
                                throw new Exception("Could not set jitter:["+ attr +"," + value+ "]");
                        } else if(attr.equalsIgnoreCase("dest")){
                            if(!cmdArgs.setDestination(value))
                                throw new Exception("Could not set destination:["+ attr +"," + value+ "]");
                        }
                    }
                }
            } catch(Exception e){
                e.printStackTrace();
            }
            return cmdArgs;
        }

        public boolean setOperation(String opStr){
            if(op == Op.NONE){
                if(opStr.equalsIgnoreCase("create")){
                    op = Op.CREATE;
                } else if(opStr.equalsIgnoreCase("delete")){
                    op = Op.DELETE;
                } else if(opStr.equalsIgnoreCase("data")){
                    op = Op.DATA;
                } else if(opStr.equalsIgnoreCase("query")){
                    op = Op.QUERY;
                } else{
                    return false;
                }
            } else{
                return false;
            }
            return true;
        }

        public Op getOperation(){
            return op;
        }

        public boolean setNodeName(String nodeName){
            if(n==null){
                n=nodeName;
            } else{
                return false;
            }
            return true;
        }

        public String getNodeName(){
            return n;
        }

        public boolean setDestination(String destination){
            if(dest==null)
                dest = destination;
            else
                return false;
            return true;
        }

        public String getDestination(){
            return dest;
        }

        public boolean setInfo(String tag){
            if(m==null)
                m = tag;
            else
                return false;
            return true;
        }

        public String getInfo(){
            return m;
        }

        public boolean setTimestamp(String t){
            if(ts!=null)
                return false;
            try {
                ts= sdf.parse(t, new ParsePosition(0));
            } catch(Exception e){
                e.printStackTrace();
                return false;
            }
            return true;
        }

        public Date getTimestamp(){
            return ts;
        }

        public boolean setStartTimestamp(String start){
            if(st!=null)
                return false;
            try {
                st= sdf.parse(start, new ParsePosition(0));
                if(st==null)
                    return false;   /*formatting problems*/
            } catch(Exception e){
                e.printStackTrace();
                return false;
            }
            return true;
        }

        public Date getStartTimestamp(){
            return st;
        }

        public boolean setEndTimestamp(String end){
            if(et!=null)
                return false;
            try {
                et= sdf.parse(end, new ParsePosition(0));
                if(et==null)
                    return false;   /*formatting problems*/
            } catch(Exception e){
                e.printStackTrace();
                return false;
            }
            return true;
        }

        public Date getEndTimestamp(){
            return et;
        }

        public boolean setType(String type){
            if(type==null)
                return false;
            try {
                if(type.equalsIgnoreCase("node")){
                    t = Type.NODE;
                } else if(type.equalsIgnoreCase("strm")){
                    t = Type.STRM;
                } else if(type.equalsIgnoreCase("proc")){
                    t = Type.PROC;
                } else if(type.equalsIgnoreCase("link")){
                    t = Type.LINK;
                } else{
                    return false;
                }
            } catch(Exception e){
                return false;
            }
            return true;
        }

        public Type getType(){
            return t;
        }

        public boolean setPeriod(String periodStr){
            if(p>0 || periodStr==null)
                return false;
            try {
                int i=periodStr.length()-1;
                while(i>0 && !Character.isDigit(periodStr.charAt(i)))
                    i--;
                if(i==0 || i==periodStr.length()-1)
                    return false;
                String timeUnits = periodStr.substring(i+1);
                long timeVal = Long.parseLong(periodStr.substring(0,i+1));
                System.out.println("timeUnits=" + timeUnits + ", timeVal=" + timeVal);
                if(timeVal<=0)
                    return false;
                if(timeUnits.equalsIgnoreCase("d")){
                    //days
                    p = timeVal * 86400000L;
                } else if(timeUnits.equalsIgnoreCase("h") || timeUnits.equalsIgnoreCase("hr")){
                    //hours
                    p = timeVal * 3600000L;
                } else if(timeUnits.equalsIgnoreCase("m") || timeUnits.equalsIgnoreCase("min")){
                    //min
                    p = timeVal * 60000L;
                } else if(timeUnits.equalsIgnoreCase("s") || timeUnits.equalsIgnoreCase("sec")){
                    //second
                    p = timeVal * 1000L;
                } else {
                    return false;
                }
            } catch(Exception e){
                e.printStackTrace();
                return false;
            }
            return true;
        }

        public long getPeriod(){
            return p;
        }

        public boolean setJitter(String jitter){
            if(j>0 || jitter==null)
                return false;
            try {
                int i=jitter.length()-1;
                while(i>0 && !Character.isDigit(jitter.charAt(i)))
                    i--;
                if(i==0 || i==jitter.length()-1)
                    return false;
                String timeUnits = jitter.substring(i+1);
                long timeVal = Long.parseLong(jitter.substring(0,i));
                if(timeVal<=0)
                    return false;
                if(timeUnits.equalsIgnoreCase("d")){
                    //days
                    j = timeVal * 86400000L;
                } else if(timeUnits.equalsIgnoreCase("h") || timeUnits.equalsIgnoreCase("hr")){
                    //hours
                    j = timeVal * 3600000L;
                } else if(timeUnits.equalsIgnoreCase("m") || timeUnits.equalsIgnoreCase("min")){
                    //min
                    j = timeVal * 60000L;
                } else if(timeUnits.equalsIgnoreCase("s") || timeUnits.equalsIgnoreCase("sec")){
                    //second
                   j = timeVal * 1000L;
                } else {
                    return false;
                }
            } catch(Exception e){
                e.printStackTrace();
                return false;
            }
            return true;
        }

        public long getJitter(){
            return j;
        }

    }
}
