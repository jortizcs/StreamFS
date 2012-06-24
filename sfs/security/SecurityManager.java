package sfs.security;

import sfs.db.MySqlDriver;

public class SecurityManager{
    private SecurityMananger secmngr = null;
    private MySqlDriver db = SFSServer.getDB();
    private SecurityManager(){}
    public static SecurityManager getInstance(){
        if(secmngr==null)
            secmngr = new SecurityMananger();
        return secmngr;
    }
}
