package sfs.proc.console;

import sfs.proc.console.msg.ConsoleMessageProto.ConsoleMessage;

import java.util.Arrays;
import java.io.Console;

import java.util.*;
import java.net.*;
import java.io.*;

public class SfsExtProcMngrConsole {
    private static boolean active = true;
    private static final int adminPort = 7763;
    private static Socket adminSock = null;
	
	public static final void main(String... aArgs){

        Console console = System.console();
        try {
            InetAddress adminAddr = InetAddress.getByName("localhost");
            adminSock = new Socket(adminAddr, adminPort);
        } catch(Exception e){
            e.printStackTrace();
            return;
        }
        while(active){
            //read user name, using java.util.Formatter syntax :
            String command = console.readLine("__sfs_proc_mngr>> ");
            
            //verify user name and password using some mechanism (elided)
            //console.printf("Command: %1$s.\n", command);
            processCommand(console, command);
        }
		
		//this version just exits, without asking the user for more input
		console.printf("Bye.\n");
	}
	

    private static void processCommand(Console c, String command){
        StringTokenizer tokenizer = new StringTokenizer(command);
        ArrayList<String> tokens = new ArrayList<String>(tokenizer.countTokens());
        if(tokenizer.countTokens()>0){
            for(int i=0; i<tokenizer.countTokens(); i++)
                tokens.add(tokenizer.nextToken());
            String mainCommand = tokens.get(0);
            if(mainCommand.equalsIgnoreCase("reload")){
                c.printf("Command: %1$s\n", command);
                ConsoleMessage outmsg = ConsoleMessage.newBuilder()
                                            .setType(ConsoleMessage.ConsoleMessageType.RELOAD)
                                            .build();
                sendToAdmin();
            } else if(mainCommand.equalsIgnoreCase("quit")){
                active = false;
                ConsoleMessage outmsg = ConsoleMessage.newBuilder()
                                            .setType(ConsoleMessage.ConsoleMessageType.QUIT)
                                            .build();
                sendToAdmin(outmsg);
            } else if(mainCommand.equalsIgnoreCase("help")){
                c.printf("%s", command);
                ConsoleMessage outmsg = ConsoleMessage.newBuilder()
                                            .setType(ConsoleMessage.ConsoleMessageType.INFO)
                                            .build();
                sendToAdmin(outmsg);
            }
        }
    }

    private static void sendToAdmin(ConsoleMessage m){
        try {
            OutputStream output = adminSock.getOutputStream();
            m.writeDelimitedTo(output);
            output.flush();

            InputStream input = adminSock.getInputStream();
            ConsoleMessage inMsg = ConsoleMessage.parseDelimitedFrom(input);
            System.out.println(inMsg);
        } catch(Exception e){
            e.printStackTrace();
        }
    }

}
