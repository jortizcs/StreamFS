
//import net.sf.json.*;
import snaq.db.*;
import java.sql.*;

import java.net.*;
import java.util.*;
import javax.sql.rowset.serial.*;
import java.nio.*;
import java.io.*;

public class NameServer {

    private static String HOST = "localhost";
	private static int PORT = 3306;
	private static String LOGIN = "root";
	private static String PW = "root";
	private static String dbName = "mdataprov";
	public static ConnectionPool pool = null;
    
    private static final String indexFn = "hist01.idx";
    private static RandomAccessFile indexFile = null;
    private static BitSet namesIndex = null;

    public NameServer(){
        setupdb();
        setupIndex();
        Runtime.getRuntime().addShutdownHook(new ShutDownHook());
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

    public synchronized boolean createNode(String name, String dst, String tags, String type, Timestamp ts){
        long id = 0;
        Connection conn = null;
        if(!nodeExists(name) && (id=addNameEntry(name,tags,type,ts))>0){
            if(type.equalsIgnoreCase("link") && dst !=null)
                addLink(id, dst, ts);
            return logAction(true, id);
        }
        return false;
    }

    public boolean nodeExists(String name){
        long id = -1L;
        if((id=getNameId(name))>0 && namesIndex.get(new Long(id).intValue())){
            return true;
        } else {
            if(id>0)
                System.out.println("nodeExists::id=" + id + ", namesIndex[" + id + "]=" + namesIndex.get(new Long(id).intValue()));
            else
                System.out.println("nodeExists::id=" + id);
        }
        return false;
    }

    public synchronized boolean destroyNode(String name, Timestamp t){
        Connection conn = null;
        PreparedStatement ps = null;
        ByteBuffer uuidByteBuf=null;
        long id = getNameId(name);
        if(removeNameEntry(id, t) && id>0)
            return logAction(false, id);
        return false;
    }

    private boolean logAction(boolean create, long id){
        System.out.println("logAction(create=" + create + ", id=" + id + ")");

        //record the timestamp and offset
        Timestamp thisTs = new Timestamp(System.currentTimeMillis());
        long offset = -1L;
        try {
            offset = indexFile.getFilePointer();
        } catch(Exception e6){
            e6.printStackTrace();
            return false;
        }


        //set the index, id, to bitvector
        namesIndex.set(new Long(id).intValue(), create);
        System.out.println("\tnamesIndex=" + namesIndex);

        //write the current bitvector to disk
        byte[] namesIndexArray = namesIndex.toByteArray();
        int size = namesIndexArray.length;
        ByteBuffer sizeBB = ByteBuffer.allocate(4).putInt(size);
        try {
            byte[] sz=sizeBB.array();
            //write the size of the bit vector, followed by the vector itself
            indexFile.write(sz);
            indexFile.write(namesIndexArray);
            System.out.println("size=" + sz + ", index=" + namesIndexArray);
        } catch(Exception e6){
            e6.printStackTrace();
            return false;
        }


        //record the ref in the table
        Connection conn = null;
        try{
            conn = pool.getConnection(1000);
            PreparedStatement ps = conn.prepareStatement("insert into `offsets` (`ts`, `index_filepath`, `offset`) values(?,?,?)");
            ps.setTimestamp(1, thisTs);
            ps.setString(2, indexFn);
            ps.setLong(3, offset);
            int count=ps.executeUpdate();
            if(count>0)
                return true;
        } catch(Exception e){
            e.printStackTrace();
        } finally{
            try {
                if (conn != null && !conn.isClosed())
                    conn.close ();
            } catch(Exception e){e.printStackTrace();}
        }
        return false;
    }

    public void addLink(long srcid, String dstName, Timestamp ts){
        Connection conn = null;
        PreparedStatement ps = null;
        long dstid=-1;
        try {
            
            conn = pool.getConnection(1000);
            try {
                //find the source id and the destination id

                dstName = cleanPath(dstName);
                String query = "select max(id) as mid from `names` where `name`=?";
                System.out.println("addLink::Query_1=" + query.replaceFirst("\\?",dstName));
                ps = conn.prepareStatement(query);
                ps.setString(1,dstName);
                ResultSet rs = ps.executeQuery();
                //get the destination id
                if(rs.next())
                    dstid = rs.getLong("mid");
                if(dstid<0 || !namesIndex.get((int)dstid))
                    return;
            } catch(Exception e){
                e.printStackTrace();
                return;
            }
            try{
                Timestamp thisTs = new Timestamp(System.currentTimeMillis());
                if(ts!=null)
                    thisTs = ts;
                String query = "insert into `links` (`src_id`, `dst_id`,`created`) values(?,?,?)";
                System.out.println("addLink::Query_2=" + 
                        query.replaceFirst("\\?","\"" + new Long(srcid).toString() + "\"").
                        replaceFirst("\\?", "\"" + new Long(dstid).toString() + "\"").
                        replaceFirst("\\?", "\"" + thisTs.toString() + "\""));
                ps = conn.prepareStatement(query);
                ps.setLong(1, srcid);
                ps.setLong(2, dstid);
                ps.setTimestamp(3, thisTs);
                int count = ps.executeUpdate();
                ps.close();
            } catch(Exception e){
                e.printStackTrace();
            } finally {
                try {
                    ps.close();
                } catch(Exception e){}
            }
        } 
        catch(Exception e3){e3.printStackTrace();}
        finally {
            try {
                if (conn != null && !conn.isClosed())
                    conn.close ();
            } catch(Exception e){e.printStackTrace();}
		}
    }

    public long addNameEntry(String name, String tags, String type, Timestamp ts){
        Connection conn = null;
        PreparedStatement ps = null;
        UUID uid = null;
        ByteBuffer uuidByteBuf=null;
        long id =-1;
        if(!(type.equalsIgnoreCase("node") || type.equalsIgnoreCase("strm") || 
                    type.equalsIgnoreCase("proc") || type.equalsIgnoreCase("link")))
            return id;
        try {
            //write the new value
            try{
                if(name != null){
                    name = cleanPath(name);
                    conn = pool.getConnection(1000);
                    Timestamp thisTs = new Timestamp(System.currentTimeMillis());
                    if(ts!=null)
                        thisTs = ts;
                    String query = "insert into `names` (`name`, `tags`, `uid`, `type`,`created`) values (?, ?, ?, ?, ?)";
                    ps = conn.prepareStatement(query);
                    ps.setString(1, name);
                    ps.setString(2, tags);
                    uid = UUID.randomUUID();
                    uuidByteBuf = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN).
                        putLong(uid.getMostSignificantBits()).
                        putLong(uid.getLeastSignificantBits());
                    ps.setBytes(3, uuidByteBuf.array());
                    ps.setString(4, type);
                    ps.setTimestamp(5, thisTs);

                    System.out.println("ADD_ENTRY::query=" + query.replaceFirst("\\?",name).
                            replaceFirst("\\?",tags).replaceFirst("\\?", uid.toString()).
                            replaceFirst("\\?", type).
                            replaceFirst("\\?", thisTs.toString()));

                    int count = ps.executeUpdate();
                    ps.close();
                } else {
                    return -1L;
                }
            } catch(Exception e){
                e.printStackTrace();
                return -1;
            } 

            //grab the id and return it
            try  {
                String query = "select `id` from `names` where `uid`=?";
                System.out.println(query.replaceFirst("\\?",uid.toString()));
                ps= conn.prepareStatement(query);
                ps.setBytes(1, uuidByteBuf.array());
                ResultSet rs = ps.executeQuery();
                if(rs.next())
                    id = rs.getLong("id");
                return id;
            } catch(Exception e){
                e.printStackTrace();
            } finally {
                try {
                    ps.close();
                } catch(Exception e){}
            }
        } 
        
        catch(Exception e3){e3.printStackTrace();}
        finally {
            try {
                if (conn != null && !conn.isClosed())
                    conn.close ();
            } catch(Exception e){e.printStackTrace();}
		}
        return id;
    }

    public boolean removeNameEntry(long id, Timestamp t){
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            try{
                conn = pool.getConnection(1000);
                Timestamp thisTs = new Timestamp(System.currentTimeMillis());
                if(t!=null)
                    thisTs = t;
                String query = "insert into `deleted` (`id`, `ts`) values (?, ?)";
                ps = conn.prepareStatement(query);
                ps.setLong(1, id);
                ps.setTimestamp(2, thisTs);
                
                System.out.println("DELETE_ENTRY::query=" + query.replaceFirst("\\?",new Long(id).toString()).replaceFirst("\\?", thisTs.toString()));

                int count = ps.executeUpdate();
                ps.close();
            } catch(Exception e){
                e.printStackTrace();
                return false;
            } 
        } 
        catch(Exception e3){e3.printStackTrace();}
        finally {
            try {
                if (conn != null && !conn.isClosed())
                    conn.close ();
            } catch(Exception e){e.printStackTrace();}
		}
        return true;

    }

    public long getNameId(String name){
        Connection conn = null;
        PreparedStatement ps = null;
        try {
            try{
                conn = pool.getConnection(1000);
                String query = "select `id` from `names` where `name`=?";
                ps = conn.prepareStatement(query);
                ps.setString(1, name);
                
                System.out.println("GET_ID(name=" + name + ")::query=" + query.replaceFirst("\\?", name));

                ResultSet rs =ps.executeQuery();
                //rs.setFetchDirection(ResultSet.FETCH_REVERSE);
                if(rs.next()){
                    rs.last();
                    System.out.println("\tid=" + rs.getLong("id"));
                    return rs.getLong("id");
                }
            } catch(Exception e){
               e.printStackTrace();
            } finally {
                try { ps.close();} catch(Exception e){}
            }
        } 
        catch(Exception e3){e3.printStackTrace();}
        finally {
            try {
                if (conn != null && !conn.isClosed())
                    conn.close ();
            } catch(Exception e){e.printStackTrace();}
		}
       return -1L; 
    }

    public List<String> getChildren(String name, List<Integer> ids){
        StringBuffer queryBuf = new StringBuffer().append("select * from `names` where `id` in (");
        for(int i=0; i<ids.size(); ++i){
            queryBuf.append(new Integer(ids.get(i).intValue() + 1));
            if(i<ids.size()-1)
                queryBuf.append(",");
        }
        queryBuf.append(")").append(" and `name` like \"").append(name).append("%\"");
        System.out.println(queryBuf);

        return null;
    }

    public void setupIndex(){
        String filename = null;
        long offset = -1;
        PreparedStatement ps=null;
        Connection conn = null;

        //get the latest filename and index
        try {
            conn = pool.getConnection(1000);
            StringBuffer queryBuf = new StringBuffer().
                append("select `index_filepath`, `offset` from `offsets` where ").
                //append("ts=(select max(ts) from `offsets`)");
                append("id=(select max(id) from `offsets`)");
            ps = conn.prepareStatement(queryBuf.toString());
            System.out.println(queryBuf.toString());
            ResultSet rs = ps.executeQuery();
            if(rs.next()){
                filename = rs.getString("index_filepath");
                offset = rs.getLong("offset");
                System.out.println("index::filename="+ filename + ", offset=" + offset);
            }
        } catch(Exception e1){
            e1.printStackTrace();
        } finally {
            try{
                ps.close();
            } catch(Exception e){
                e.printStackTrace();
            }
        }
        try {
            if(indexFile == null && filename!=null){
                System.out.println("index::fetching index from file");
                //fetch the namesIndex from the file
                indexFile = new RandomAccessFile(filename, "rw");
                indexFile.seek(offset);
                byte[] bitvecLengthBuf = new byte[4];
                indexFile.read(bitvecLengthBuf, 0, 4);
                int bitvecLength = ByteBuffer.allocate(4).
                    put(bitvecLengthBuf).getInt(0);
                System.out.println("index::length=" + bitvecLength);
                byte[] index = new byte[bitvecLength];
                //indexFile.seek();
                indexFile.read(index, 0, bitvecLength);
                namesIndex = BitSet.valueOf(index);
                System.out.println("index:namesIndex= "+ namesIndex);
            } else if(indexFile == null && filename==null){
                indexFile = new RandomAccessFile(indexFn, "rw");
                offset =0;
                namesIndex = new BitSet();
            }
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    public static String cleanPath(String path){
        //clean up the path
        if(path == null)
            return path;

        if(!path.startsWith("/"))
            path = "/" + path;
        path = path.replaceAll("/+", "/");
        if(path.endsWith("/"))
            path = path.substring(0, path.length()-1);
        /*if(!path.endsWith("/"))
            path += "/";*/
        return path;
    }

    public class ShutDownHook extends Thread{
        public void run(){
            System.out.println("Cleaning up");
            if(indexFile!= null){
                try{
                    indexFile.close();
                } catch(Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

}
