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

package is4;

import java.lang.Math;
import java.io.Serializable;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.Date;

public class JoinInfoTuple implements Serializable{
	//unix time (seconds since January 1, 1970)
	protected Date UTIME = null;
	protected UUID IDENT = null;
	protected static transient final Logger logger = Logger.getLogger(JoinInfoTuple.class.getPackage().getName());

	public JoinInfoTuple(UUID id, long time){
		IDENT = id;
		UTIME = new Date(time);
		logger.finer("New JoinInfoTuple created (identifier, timestamp): (" + IDENT + ", " + UTIME + ")");
	}

	//for testing purposes
	public static void main(String[] args) {
	}

	public Date getTime(){
		return UTIME;
	}

	public UUID getIdent() {
		return IDENT;
	}

	public String toString() {
		StringBuffer sbuf = new StringBuffer();
		sbuf.append("[").append(UTIME.toString()).append(", ").append(IDENT.toString()).append("]");
		return sbuf.toString();
	}
}
