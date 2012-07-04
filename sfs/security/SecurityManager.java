package sfs.security;

import sfs.db.MySqlDriver;

public class SecurityManager{
    private static SecurityManager secmngr = null;
    private MySqlDriver db = MySqlDriver.getInstance();
    private SecurityManager(){}

    public static SecurityManager getInstance(){
        if(secmngr==null)
            secmngr = new SecurityManager();
        return secmngr;
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

}
