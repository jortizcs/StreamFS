import org.mozilla.javascript.*;
import java.util.Hashtable;
import java.io.*;

public class mytest {
    public static void main(String args[])
    {
        Context cx = Context.enter();
        try {
            Scriptable scope = cx.initStandardObjects();

			FileReader freader = new FileReader(args[0]);
			BufferedReader breader = new BufferedReader(freader);
			String line = breader.readLine();
			StringBuffer o=new StringBuffer();
			while(line != null){
				line = line.trim();
				o.append(line);
				line=breader.readLine();
			}
			System.out.println(o.toString());

			String s = "var blah=" + o.toString() + "; blah.s(blah.buffer_size);";

            Object result = cx.evaluateString(scope, s, "<cmd>", 1, null);
			Scriptable resultScriptable = (Scriptable) result;
			//System.out.println(resultScriptable.get("reply",resultScriptable).toString());
			
			Object[] ids = resultScriptable.getIds();
			for(int k=0; k<ids.length; k++){
				System.out.println(ids[k].toString() + ":" + resultScriptable.get(ids[k].toString(), resultScriptable).toString());
			}
			
			
		    freader = new FileReader("data01.json");
			breader = new BufferedReader(freader);
			line = breader.readLine();
			o=new StringBuffer();
			while(line != null){
				line = line.trim();
				o.append(line);
				line=breader.readLine();
			}
			String fakeData = "var data01="+o.toString()+"; data01";
			System.out.println(fakeData);
			Scriptable rs = (Scriptable) cx.evaluateString(scope,fakeData, "<cmd2>", 1, null);
			
			System.out.println("\n\n\n");
			Object[] ids2 = rs.getIds();
			for(int k=0; k<ids2.length; k++){
				System.out.println(ids2[k].toString() + ":" + rs.get(ids2[k].toString(), rs).toString());
			}

        } catch(Exception e){
			e.printStackTrace();
		} finally {
            Context.exit();
        }
    }
}