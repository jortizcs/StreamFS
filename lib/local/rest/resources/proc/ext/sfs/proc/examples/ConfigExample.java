import java.lang.*;
import java.io.*;
import java.util.*;
import org.json.simple.*;
import org.json.simple.parser.*;
import sfs.proc.msg.DataMessageProto.*;

public class ConfigExample{
    public ConfigExample(){}

    public static void main(String args[]){
        try {
            String configStr = getConfigFileContents();
            JSONParser parser = new JSONParser();
            JSONObject configObj =(JSONObject) parser.parse(configStr);
            configObj =(JSONObject)((JSONArray) configObj.get("processes")).get(0);
            String dir = (String) configObj.get("working_dir");
            String cmd = (String) configObj.get("command");
            JSONArray arguments = (JSONArray) configObj.get("arguments");
            ArrayList<String> arrgs = new ArrayList<String>();
            arrgs.add(cmd);
            for(int i=0; i<arguments.size(); i++)
                arrgs.add((String)arguments.get(i));
            ProcessBuilder pb = new ProcessBuilder(arrgs);
            pb.directory(new File(dir));
            pb.redirectErrorStream(true);
            Process p = pb.start();

            //System.out.println(pb.environment());

            int i=0;
            OutputStream out = p.getOutputStream();
            while(true){

                i+=1;
                DataMessage dm = DataMessage.newBuilder()
                    .setSrcpath("/path/to/stream")
                    .setType(DataMessage.DataMessageType.DATA_IN)
                    .build();
                //System.out.println("Writing: " + dm);
                dm.writeDelimitedTo(out);
                out.flush();
                Thread.sleep(1000);

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(p.getInputStream()));
                String line = null;
                if(reader.ready() && (line=reader.readLine())!=null){
                    System.out.println(line);
                }

                
            }
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    private static String getConfigFileContents(){
        String configFileContents = null;
        String configPath = "../../../config/config.json";
        if(configFileContents ==null){
            File aFile = new File(configPath);
            StringBuilder contents = new StringBuilder();
            
            try {
                //use buffering, reading one line at a time
                //FileReader always assumes default encoding is OK!
                BufferedReader input =  new BufferedReader(new FileReader(aFile));
                try {
                    String line = null; //not declared within while loop
                    /*
                     * readLine is a bit quirky :
                     * it returns the content of a line MINUS the newline.
                     * it returns null only for the END of the stream.
                     * it returns an empty String if two newlines appear in a row.
                     */
                    while (( line = input.readLine()) != null){
                        contents.append(line);
                        contents.append(System.getProperty("line.separator"));
                    }
                }
                finally {
                    input.close();
                }
            }
            catch (IOException ex){
                ex.printStackTrace();
            }
            configFileContents = contents.toString();
        }
        return configFileContents;
    }

}
