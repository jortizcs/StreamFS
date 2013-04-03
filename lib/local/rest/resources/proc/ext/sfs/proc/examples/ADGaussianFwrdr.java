import sfs.proc.msg.DataMessageProto.*;
import org.apache.commons.math3.distribution.*;
import java.io.*;

public class ADGaussianFwrdr{
    public ADGaussianFwrdr(){
    }

    public static void main(String args[]){
        FileOutputStream fileout=null;
        try {
            fileout = new FileOutputStream(new File("ADGaussianFwrdr.log"));
            while(true){
                DataMessage dm = DataMessage.parseDelimitedFrom(System.in);
                fileout = new FileOutputStream(new File("ADGaussianFwrdr.log"));
                if(dm!=null){
                    fileout.write(dm.toString().getBytes());
                }else {
                    System.out.println("done");
                    System.exit(1);
                }
                fileout.close();

                TimeValSet.Builder tvb = TimeValSet.newBuilder();
                tvb.addVal(1L);
                TimeValSet tv = tvb.build();
                dm = DataMessage.newBuilder()
                    .setType(DataMessage.DataMessageType.DATA_OUT)
                    .addData(tv)
                    .build();
                dm.writeDelimitedTo(System.out);
                System.out.flush();

                //append the message sent to the log file
                fileout = new FileOutputStream(new File("ADGaussianFwrdr.log"), true);
                if(dm!=null){
                    fileout.write(dm.toString().getBytes());
                }else {
                    System.out.println("done");
                    System.exit(1);
                }
                fileout.close();
            }
        } catch(Exception e){
            e.printStackTrace();
        }
    }
}
