package sfs.security;

import sfs.db.MySqlDriver;
import sfs.types.Default;
import sfs.util.ResourceUtils;

import org.simpleframework.http.Response;
import org.simpleframework.http.Request;
import org.simpleframework.http.Query;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

import java.util.logging.Logger;
import java.util.logging.Level;

public class SecurityManager extends Default {
    private static SecurityManager secmngr = null;
    private static MySqlDriver mysqlDB = MySqlDriver.getInstance();
    private static final JSONParser parser = new JSONParser();
    private static final ResourceUtils utils = ResourceUtils.getInstance();
    
    
    private SecurityManager(){}

    public static SecurityManager getInstance(){
        if(secmngr==null)
            secmngr = new SecurityManager();
        return secmngr;
    }

    //overwritten methods inherited from the Default type object
    public static void get(Request request, Response response, String path, 
            boolean internalCall, JSONObject internalResp){
        utils.sendResponse(request, response, 200, null, internalCall, internalResp);
    }

    public static void put(Request request, Response response, String path, String data,
            boolean internalCall, JSONObject internalResp){
        try {
            JSONArray errors = new JSONArray();
            JSONObject retObj = new JSONObject();
            Query query = request.getQuery();

            //fetch the necessary URL query parameters
            String op = null;
            if(query!=null)
                op = (String)request.getQuery().get("op");
            String sid = null;
            if(query!=null)
                sid = (String)request.getQuery().get("sid");

            //process the request
            if(path.equals("/login") && op!=null && op.equals("create") && data != null){
                JSONObject dataObj = (JSONObject) parser.parse(data);
                if(dataObj.containsKey("username") && dataObj.containsKey("password") &&
                        dataObj.containsKey("email"))
                {
                    String username = (String)dataObj.get("username");
                    String pw = (String) dataObj.get("password");
                    String email = (String) dataObj.get("email");
                    long userid = mysqlDB.createNewUser(username,pw,email);
                    if(userid>0){
                        long sid = createNewSession(userid);
                        retObj.put("status", "success");
                        retObj.put("session_id", sid);
                        utils.sendResponse(request, response, 201, retObj.toString(), 
                                internalCall, internalResp);
                    } else {
                        errors.add("Conflict while creating new user, check username; it must be unique");
                        retObj.put("status","fail");
                        retObj.put("errors", errors);
                        utils.sendResponse(request, response, 400, retObj.toString(), 
                                internalCall, internalResp);
                        return;
                    }
                } else {
                    errors.add("Request must include `username`, `password`, and `email`");
                    retObj.put("status", "fail");
                    retObj.put("errors", errors);
                    utils.sendResponse(request, response, 400, retObj.toString(), 
                            internalCall, internalResp);
                    return;
                }
            } else if(path.equals("/users")){
            }
        } catch(Exception e){
            logger.log(Level.WARNING, "", e);
        }
        utils.sendResponse(request, response, 200, null, internalCall, internalResp);
    }

    public static void delete(Request request, Response response, String path, String data,
            boolean internalCall, JSONObject internalResp){
        utils.sendResponse(request, response, 200, null, internalCall, internalResp);
    }

    public static void delete(Request request, Response response, String path, 
            boolean internalCall, JSONObject internalResp){
        utils.sendResponse(request, response, 200, null, internalCall, internalResp);
    }

    /**
     * Creates a new user.  Returns -1 if the requestorUid does not have permission
     * to create new users.
     *
     * @param requestorUid The identifier for the user making the request to create
     *          create a new user.
     * @return the user id of the newly created user, -1 if not successful.
     */
    public long createNewUser(long requestorUid){
        return 1L;
    }

    /**
     * Deletes a user account with the given uid.
     * 
     * @param requestorUid the unique id of the user making the request.
     * @param uid the user account to delete.
     */
    public boolean deleteUser(long requestorUid, long uid){
        return true;
    }

    /**
     * Check if the user with the given identifier 'uid' has permission to perform
     * operation 'op' on the object located by the given resource 'path'
     *
     * @param uid user identifer
     * @param op the Operation to be formed; read sfs.security.Operation for details
     * @param path the path to the resource
     * @return true if operation is permitted, false otherwise
     */
    public boolean hasPermission(long uid, Operation op, String path){
        //check the object id that this path points to and make sure you grant
        //the most conservative access rights to this object
        //For example, if uid has access to two paths that point to this object, 
        //grant the strictest between the two.
        return true;
    }

    /**
     * Grant permission for user uid to perforn operation 'op' on the given
     * 'path'.
     * 
     * @param requestorUid the user id of the entity making the request to grant permission.
     * @param uid the user id that permission is being granted to. 
     * @return true if successful, false otherwise
     */
    public boolean grantPermission(long requestorUid, String uid, Operation op, String path){
        return true;
    }

    /**
     * Revoke permission for user uid to perforn operation 'op' on the given
     * 'path'.
     * 
     * @param requestorUid the user id of the entity making the request to grant permission.
     * @param uid the user id that permission is being granted to. 
     * @return true if successful, false otherwise
     */
    public boolean removePermission(String requestorUid, String uid, Operation op, String path){
        return true;
    }

    //  Grant_Permission operation management

    /**
     * Checks if the user has permission to grant permission to others.
     */
    protected boolean canGrantPermission(String uid){
        return true;
    }

    /**
     * Sets the permission for the user identified with uid to grant permission or
     * revokes them.
     * 
     * @param requestorUid the user id of the user making the request to grant permission to
     *          another user.
     * @param uid the user id of the user to whom permission will be set
     * @param set true if the ability to grant permissions is set, false otherwise.
     */
    public boolean setCanGrantPermission(String requestorUid, String uid, boolean set){
        return true;
    }

    //Session management
    private static long createNewSession(long userid){
        return 1L;
    }

}
