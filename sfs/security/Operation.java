package sfs.security;

public enum Operation {
    //actions performed on objects
    READ, WRITE, EXECUTE, ALL_ACTIONS, NONE,//GET, PUT, POST, DELETE, MOVE,
    
    //manamgent of actions on objects
    GRANT_PERMISSION,

    //streamfs specific
    CREATE_USER, DELETE_USER, CREATE_GROUP, DELETE_GROUP
}
