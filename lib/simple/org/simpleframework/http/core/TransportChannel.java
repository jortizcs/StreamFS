/*
 * TransportChannel.java February 2007
 *
 * Copyright (C) 2007, Niall Gallagher <niallg@users.sf.net>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or 
 * implied. See the License for the specific language governing 
 * permissions and limitations under the License.
 */

package org.simpleframework.http.core;

import java.nio.channels.SocketChannel;
import java.util.Map;

import javax.net.ssl.SSLEngine;

import org.simpleframework.transport.Cursor;
import org.simpleframework.transport.Transport;
import org.simpleframework.transport.TransportCursor;

/**
 * The <code>TransportChannel</code> provides a means to deliver and
 * receive content over a transport. This essentially provides two
 * adapters which enable simpler communication with the underlying
 * transport. They hide the complexities involved with buffering and
 * resetting data written to and read from the socket.
 *
 * @author Niall Gallagher
 *
 * @see org.simpleframework.transport.Processor
 */ 
class TransportChannel implements Channel {

   /**
    * This represents the underlying transport that is to be used.
    */         
   private final Transport transport;
   
   /**
    * This is the engine that is used to secure the transport.
    */
   private final SSLEngine engine;

   /**
    * This is used to provide a cursor view on the input.
    */ 
   private final Cursor cursor;

   /**
    * This is used to provide a blocking means for sending data.
    */ 
   private final Sender sender;
   
   /**
    * Constructor for the <code>TransportChannel</code> object. The
    * transport channel basically wraps a channel and provides a
    * means to send and receive data using specialized adapters. 
    * These adapters provide a simpler means for communicating over
    * the network to the connected client.
    *
    * @param transport this is the underlying transport to be used
    */ 
   public TransportChannel(Transport transport) {
      this.cursor = new TransportCursor(transport);
      this.sender = new TransportSender(transport);
      this.engine = transport.getEngine();
      this.transport = transport;
   }
   
   /**
    * This is used to determine if the channel is secure and that
    * data read from and data written to the request is encrypted.
    * Channels transferred over SSL are considered secure and will
    * have this return true, otherwise it will return false.
    * 
    * @return true if this is secure for reading and writing
    */
   public boolean isSecure() {
      return engine != null;
   }
   
   /**
    * This is the connected socket channel associated with this. In
    * order to determine if content can be read or written to or
    * from the channel this socket can be used with a selector. This
    * provides a means to react to I/O events as they occur rather
    * than polling the channel which is generally less performant.
    *
    * @return this returns the connected socket channel
    */    
   public SocketChannel getSocket() {
      return transport.getChannel();
   }

   /**
    * This returns the <code>Map</code> of attributes used to hold
    * connection information for the channel. The attributes here 
    * are taken from the pipeline attributes and may contain details
    * such as SSL certificates or other such useful information.
    *
    * @return returns the attributes associated with the channel
    */    
   public Map getAttributes() {
      return transport.getAttributes();
   }

   /**
    * This provides the <code>Cursor</code> for this channel. The
    * cursor provides a resettable view of the input buffer and will
    * allow the server kernel to peek into the input buffer without
    * having to take the data from the input. This allows overflow
    * to be pushed back on to the cursor for subsequent reads.
    *
    * @return this returns the input cursor for the channel
    */  
   public Cursor getCursor() {
      return cursor;
   }

   /**
    * This provides the <code>Sender</code> for the channel. This is
    * used to provide a blocking output mechanism for the channel.
    * Enabling blocking reads ensures that output buffering can be
    * limited to an extent, which ensures that memory remains low at
    * high load periods. Writes to the sender may result in the data
    * being copied and queued until the socket is write ready.
    *
    * @return this returns the output sender for this channel
    */    
   public Sender getSender() {
      return sender;
   }
      
   /**
    * Because the channel represents a duplex means of communication
    * there needs to be a means to close it down. This provides such
    * a means. By closing the channel the cursor and sender will no
    * longer send or recieve data to or from the network. The client
    * will also be signaled that the connection has been severed.
    */  
   public void close() {
      try {
         transport.close();
      }catch(Exception e) {
         return;
      }
   }

}
