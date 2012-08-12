/*
 * FlushMonitor.java February 2007
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

/**
 * The <code>FlushMonitor</code> object is used to monitor response
 * streams. If there is an error or a close requested this will
 * close the underlying transport. If however there is a successful
 * response then this will flush the transport and hand the channel
 * for the pipeline back to the server kernel. This ensures that
 * the next HTTP request can be consumed from the transport.
 * 
 * @author Niall Gallagher
 */
class FlushMonitor implements Monitor {   
   
   /**
    * This is the initiator used to initiate a new request.
    */
   private Initiator reactor;
   
   /**
    * This is the channel associated with the client connection.
    */
   private Channel channel;
   
   /**
    * This flag determines whether the connection was closed.
    */
   private boolean closed;
   
   /**
    * This flag determines whether the was a response error.
    */
   private boolean error;
   
   /**
    * Constructor for the <code>FlushMonitor</code> object. This is
    * used to create a monitor using a HTTP request entity and an
    * initiator which is used to reprocess a channel if there was a
    * successful deliver of a response.
    * 
    * @param reactor this is the reactor used to process channels
    * @param entity this is the entity associated with the channel
    */ 
   public FlushMonitor(Initiator reactor, Entity entity) {
      this.channel = entity.getChannel();
      this.reactor = reactor;
   }

   /**
    * This is used to close the underlying transport. A closure is
    * typically done when the response is to a HTTP/1.0 client
    * that does not require a keep alive connection. Also, if the
    * container requests an explicit closure this is used when all
    * of the content for the response has been sent.
    * 
    * @param sender this is the sender used to send the response
    */   
   public void close(Sender sender) {
      try {
         if(!isClosed()) {
            closed = true;
            sender.close();
         }
      } catch(Exception e) {
         fail(sender);
      }
   }
   
   /**
    * This is used when there is an error sending the response. On
    * error RFC 2616 suggests a connection closure is the best
    * means to handle the condition, and the one clients should be
    * expecting and support. All errors result in closure of the
    * underlying transport and no more requests are processed.
    * 
    * @param sender this is the sender used to send the response
    */   
   public void error(Sender sender) {
      try {
         if(!isClosed()) {
            error = true;
            sender.close();
         }            
      } catch(Exception e) {
         fail(sender);
      }
   }
   
   /**
    * This is used when the response has been sent correctly and
    * the connection supports persisted HTTP. When ready the channel
    * is handed back in to the server kernel where the next request
    * on the pipeline is read and used to compose the next entity.
    * 
    * @param sender this is the sender used to send the response
    */   
   public void ready(Sender sender) {
      try {
         if(!isClosed()) {
            closed = true;
            sender.flush();
            reactor.start(channel);
         }
      } catch(Exception e) {
         fail(sender);
      }
   }
   
   /**
    * This is used to purge the sender so that it closes the socket
    * ensuring there is no connection leak on shutdown. This is used
    * when there is an exception signaling the state of the sender. 
    * 
    * @param sender this is the sender that is to be purged
    */
   private void fail(Sender sender) {
      try {
         sender.close();
      } catch(Exception e) {
         return;
      }
   }
   
   /**
    * This is used to determine if the response has completed or
    * if there has been an error. This basically allows the sender
    * of the response to take action on certain I/O events.
    * 
    * @return this returns true if there was an error or close
    */   
   public boolean isClosed() {
      return closed || error;
   }
   
   /**
    * This is used to determine if the response was in error. If
    * the response was in error this allows the sender to throw an
    * exception indicating that there was a problem responding.
    * 
    * @return this returns true if there was a response error
    */   
   public boolean isError(){
      return error;
   }
   
}
