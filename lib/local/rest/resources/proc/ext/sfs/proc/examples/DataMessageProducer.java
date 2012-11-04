import sfs.proc.msg.DataMessageProto.*;

public class DataMessageProducer{

    public DataMessageProducer(){}

    public static void main(String[] args){
        for(int i=0; i<5; i++){

            try{
                DataMessage dm = DataMessage.newBuilder()
                    .setSrcpath("/path/to/stream")
                    .setType(DataMessage.DataMessageType.DATA_IN)
                    .build();
                dm.writeDelimitedTo(System.out);
                Thread.sleep(1000);
            } catch(Exception e){
                e.printStackTrace();
            }
        }
    }

}
