import java.io.*;
import java.nio.*;
import java.nio.channels.*;

public class ProdConPipe {
	public ProdConPipe(){}
	
	public static void main(String[] args){
		try {
			Pipe pipe = Pipe.open();
			String datafile = args[0];
			int sendmax = (new Integer(args[1])).intValue();
			ThreadedConsumer cons = new ThreadedConsumer(pipe.source());
			ThreadedProducer prod = new ThreadedProducer(datafile, sendmax, pipe.sink());
			System.out.println("consumer_id:" + cons.getId() + "\tproducer_id:"+ prod.getId());
			prod.start();
			cons.start();
			
		} catch (Exception e){
			e.printStackTrace();
		}
	}
}