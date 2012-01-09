package local.analytics;

import java.io.Serializable;

public class RouterCommand implements Serializable{
    public static enum CommandType{
        PUSH, PUSH_ACK, PULL, PULL_ACK,
        ADD_NODE, ADD_NODE_ACK, REMOVE_NODE, REMOVE_NODE_ACK,
        ADD_LINK, ADD_LINK_ACK, REMOVE_LINK, REMOVE_LINK_ACK,
        STOP_ROUTER, CREATE_AGG_PNT, CREATE_AGG_PNT_ACK
    }

    public CommandType type = CommandType.PUSH;

    public String sourcepath = null;
    public String destpath = null;
    public String units = null;
    public String data = null;
    public String aggType = null;
    public long lowts = -1;
    public long hights = -1;
    public boolean symlinkFlag = false;
    public boolean state = false;

    public RouterCommand(CommandType t){
        type = t;
    }

    public void setSymlinkFlag(boolean flag){
        symlinkFlag = flag;
    }

    public void setData(String dat){
        data = dat;
    }

    public void setAggType(String aggTypeStr){
        aggType = aggTypeStr;
    }

    public void setLowTs(long lowTs){
        lowts = lowTs;
    }

    public void setHighTs(long highTs){
        hights = highTs;
    }

    public void setSrcVertex(String srcPath){
        sourcepath = srcPath;
    }

    public void setDstVertex(String dstPath){
        destpath = dstPath;
    }

    public void setUnits(String u){
        units = u;
    }

    public void setAggState(boolean aggstate){
        state = aggstate;
    }
}
