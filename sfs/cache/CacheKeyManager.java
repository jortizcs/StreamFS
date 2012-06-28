package sfs.cache;

import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.logging.Logger;
import java.util.logging.Level;
import com.meetup.memcached.*;

public class CacheKeyManager {
    protected static transient final Logger logger = Logger.getLogger(CacheKeyManager.class.getPackage().getName());

    public static CacheKeyManager ckm =null;
    private static Vector<String> keys = null;
    
    private CacheKeyManager(){
        keys = new Vector<String>();
    }

    public static CacheKeyManager getInstance(){
        if (ckm==null)
            ckm= new CacheKeyManager();
        return ckm;
    }

    private void addKey(String key){
        keys.add(key);
    }

    private void removeKey(String key){
        keys.remove(key);
    }

    private Vector<String> getKeys(String pattern){
        try {
            Pattern p = Pattern.compile(pattern);
            Vector<String> allmatches = new Vector<String>();
            for(int i=0; i<keys.size(); i++){
                String thiskey = keys.get(i);
                Matcher m = p.matcher(thiskey);
                if(m.matches())
                    allmatches.add(thiskey);
            }
            return allmatches;
        } catch(Exception e){
            logger.log(Level.WARNING, "", e);
        }
        return null;
    }

    public void invalidateAllEntries(MemcachedClient mcc, String pattern){
        Vector<String> keysToRemove = getKeys(pattern);
        if(keysToRemove!=null){
            for(int i=0; i<keysToRemove.size(); i++){
                mcc.delete(keysToRemove.get(i));
                keys.remove(keysToRemove.get(i));
            }
        }
    }

    public void set(MemcachedClient mcc, String key, String value){
        mcc.set(key, value);
        addKey(key);
    }

    public void delete(MemcachedClient mcc, String key){
        mcc.delete(key);
        keys.remove(key);
    }





}
