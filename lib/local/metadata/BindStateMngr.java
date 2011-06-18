/*
 * "Copyright (c) 2010-11 The Regents of the University  of California. 
 * All rights reserved.
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose, without fee, and without written agreement is
 * hereby granted, provided that the above copyright notice, the following
 * two paragraphs and the author appear in all copies of this software.
 *
 * IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY FOR
 * DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT
 * OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF THE UNIVERSITY OF
 * CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS
 * ON AN "AS IS" BASIS, AND THE UNIVERSITY OF CALIFORNIA HAS NO OBLIGATION TO
 * PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS."
 *
 * Author:  Jorge Ortiz (jortiz@cs.berkeley.edu)
 * IS4 release version 1.0
 */

package local.metadata;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Hashtable;
import java.util.TimerTask;
import java.util.Timer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Collection;
import java.util.Date;

import java.io.*;

public class BindStateMngr extends TimerTask implements Serializable
{

	private static Logger logger = Logger.getLogger(BindStateMngr.class.getPackage().getName());
	private static BindStateMngr bindStateMngr = null;
	private static Hashtable<Binding, Long> deadlines = new Hashtable<Binding, Long>();
	private static Hashtable<Long, Binding> reverseDeadlines = new Hashtable<Long, Binding>();
	private static Timer timer = new Timer("BindStateMngrTimer");
	private static final long T = 86400;	//5L;	//  //seconds

	protected static String IS4HOME=null;
	protected static String deadlinesObjPath = "/.state/deadlines.obj";
	protected static String reverseDeadlinesObjPath ="/.state/reverseDeadlines.obj";
	private static boolean init = false;

	private BindStateMngr()
	{
		timer.schedule(this, 0L, T*1000);
		logger.info("Started cleanup task timer");

		if(!init) {
			try {
				initGlobalVars();
				File f1 = new File(deadlinesObjPath);
				File f2 = new File(reverseDeadlinesObjPath);

				if (f1.exists()){
					FileInputStream fileIn = new FileInputStream(f1);
					ObjectInputStream in = new ObjectInputStream(fileIn);
					deadlines = (Hashtable<Binding, Long>)in.readObject();
				}

				if (f2.exists()){
					FileInputStream fileIn = new FileInputStream(f2);
					ObjectInputStream in = new ObjectInputStream(fileIn);
					reverseDeadlines = (Hashtable<Long, Binding>)in.readObject();
				}

				init = true;
			} catch (Exception e) {
				logger.log(Level.WARNING, "Error loading previous state", e);
			}
		}
	}

	public static BindStateMngr getInstance()
	{
		logger.info("fetching BindStateMngr instance");
		if(bindStateMngr == null)
			bindStateMngr = new BindStateMngr();
		return bindStateMngr;
	}

	protected static void initGlobalVars(){
		String home = System.getenv().get("IS4HOME");
		if(IS4HOME == null && home !=null && !home.equals("")){
			IS4HOME = home;
			deadlinesObjPath = IS4HOME + deadlinesObjPath;
			reverseDeadlinesObjPath = IS4HOME + reverseDeadlinesObjPath;
		} else if(IS4HOME == null && home ==null || home.equals("")){
			logger.severe("Environment variable not set");
			System.exit(1);
		}
	}

	public void manageBinding(Binding binding)
	{
		logger.info((new StringBuilder()).append("Adding Binding: ").append(binding.getPid()).toString());
		if(binding != null){
			Long newDeadline = new Long((new Date()).getTime() / 1000L + T);
			deadlines.put(binding, newDeadline);
			reverseDeadlines.put(newDeadline, binding);
			logger.info((new StringBuilder()).append("Added ").append(binding.getPid()).append(" Deadline: ").append(newDeadline.longValue()).toString());

			saveState();
		}
	}

	public void updateBindingTimestamp(Binding binding)
	{
		logger.info("Updating the binding timestamp");
		Long dl = null;
		if((dl = deadlines.get(binding)) != null)
		{
			Date date = new Date();
			Long newDeadline = new Long(date.getTime() / 1000L);
			deadlines.remove(binding);
			reverseDeadlines.remove(dl);
			deadlines.put(binding, newDeadline);
			reverseDeadlines.put(newDeadline, binding);

			saveState();
		}

	}

	public void expireBinding(Binding binding)
	{
		Long dl = null;
		if((dl = (Long)deadlines.get(binding)) != null)
		{
			deadlines.remove(binding);
			reverseDeadlines.remove(dl);
			logger.info((new StringBuilder()).append("removing binding: ").append(binding.getPid()).append(" --> \n\t").append(binding.getMeta()).toString());

			saveState();
		}
	}

	public boolean isActive(Binding binding)
	{
		logger.info("Checking if binding is active");
		return deadlines.get(binding) != null;
	}

	public void run()
	{
		logger.info("Starting periodic cleanup task");
		Collection<Long> deadlineTimes = deadlines.values();
		ArrayList<Long> deadlineTimesList = new ArrayList<Long>(deadlineTimes);
		if(deadlineTimesList.size() > 0)
		{
			logger.info("Found");
			Collections.sort(deadlineTimesList);
			Collections.reverse(deadlineTimesList);
			long now = (new Date()).getTime() / 1000L;
			int i = 0;
			logger.info((new StringBuilder()).append("Now: ").append(now).append(" Deadline: ").append(((Long)deadlineTimesList.get(i)).longValue()).toString());
			while(i < deadlineTimesList.size() && now >= deadlineTimesList.get(i).longValue())
			{
				Binding binding = reverseDeadlines.get(deadlineTimesList.get(i));
				MetadataMngr.getInstance().unbind(binding.getPid());
				i += 1;
			}

		}
	}

	public synchronized void saveState(){
		try {
			FileOutputStream fileOut = new FileOutputStream(deadlinesObjPath);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(deadlines);

			fileOut = new FileOutputStream(reverseDeadlinesObjPath);
			out = new ObjectOutputStream(fileOut);
			out.writeObject(reverseDeadlines);

			logger.info("Wrote objectes");
		} catch(Exception e) {
			logger.log(Level.WARNING, "Likely and IOException while writing objects", e);
		}
	}

}
