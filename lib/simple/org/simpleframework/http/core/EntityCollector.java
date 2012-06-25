/*
 * EntityCollector.java October 2002
 *
 * Copyright (C) 2002, Niall Gallagher <niallg@users.sf.net>
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

import java.io.IOException;
import java.nio.channels.SocketChannel;

import org.simpleframework.http.session.Session;
import org.simpleframework.transport.Cursor;
import org.simpleframework.util.buffer.Allocator;
import org.simpleframework.util.lease.LeaseException;

/**
 * The <code>EntityCollector</code> object is used to collect all of
 * the data used to form a request entity. This will collect the 
 * data fragment by fragment from the underlying transport. When 
 * all of the data is consumed and the entity is created and then
 * it is sent to the <code>Selector</code> object for processing.
 * When the request has completed the next request can be collected
 * from the underlying transport using a new collector object.  
 * 
 * @author Niall Gallagher
 */
class EntityCollector implements Collector {
   
   /**
    * This is used to consume the request entity from the channel.
    */
   private final Consumer entity; 
   
   /**
    * This is used to build the entity for its constituent parts.
    */
   private final Builder builder;
   
   /**
    * This is the channel used to acquire the underlying data.
    */
   private final Channel channel;
   
   /**
    * This is the cursor used to read and reset the data.
    */
   private final Cursor cursor;   
   
   /**
    * The <code>EntityCollector</code> object used to collect the 
    * data from the underlying transport. In order to collect a body
    * this must be given an <code>Allocator</code> which is used to
    * create an internal buffer to store the consumed body.
    * 
    * @param allocator this is the allocator used to buffer data
    * @param tracker this is the tracker used to create sessions
    * @param channel this is the channel used to read the data
    */
   public EntityCollector(Allocator allocator, Tracker tracker, Channel channel) { 
      this.builder = new TrackerBuilder(tracker, channel);
      this.entity = new BuilderConsumer(allocator, builder, channel);
      this.cursor = channel.getCursor();      
      this.channel = channel;
   }

   /**
    * This is used to collect the data from a <code>Channel</code>
    * which is used to compose the entity. If at any stage there
    * are no ready bytes on the socket the selector provided can be
    * used to queue the collector until such time as the socket is
    * ready to read. Also, should the entity have completed reading
    * all required content it is handed to the selector as ready,
    * which processes the entity as a new client HTTP request.
    * 
    * @param selector this is the selector used to queue this
    */
   public void collect(Selector selector) throws IOException {
      while(cursor.isReady()) { 
         if(entity.isFinished()) {
             break;
         }  else {
            entity.consume(cursor);               
         }      
      }     
      // XXX What happens to the socket connection here
      if(cursor.isOpen()) { // reject if closed from client side
         if(entity.isFinished()) {
             selector.ready(this);
         } else {    
             selector.select(this);             
         }
      }
   }
   
   /**
    * This method is used to acquire a <code>Session</code> for the
    * request. The object retrieved provides a container for data
    * associated to the connected client. This allows the request
    * to perform more complex operations based on knowledge that is
    * built up through a series of requests. The session is known
    * to the system using a <code>Cookie</code>, which contains
    * the session reference. This cookie value should not be 
    * modified as it used to reference the active session object.
    *
    * @param create creates the session if it does not exist
    *
    * @return returns an active session object for the entity
    */    
   public Session getSession(boolean create) throws LeaseException {      
      return builder.getSession(create);
   }

   /**
    * This provides the HTTP request header for the entity. This is
    * always populated and provides the details sent by the client
    * such as the target URI and the query if specified. Also this
    * can be used to determine the method and protocol version used.
    * 
    * @return the header provided by the HTTP request message
    */   
   public Header getHeader() {
      return builder.getHeader();
   }
   
   /**
    * This is used to acquire the body for this HTTP entity. This
    * will return a body which can be used to read the content of
    * the message, also if the request is multipart upload then all
    * of the parts are provided as <code>Part</code> objects. Each
    * part can then be read as an individual message.
    *  
    * @return the body provided by the HTTP request message
    */   
   public Body getBody() {
      return builder.getBody();
   }
   
   /**
    * This provides the connected channel for the client. This is
    * used to send and receive bytes to and from an transport layer.
    * Each channel provided with an entity contains an attribute 
    * map which contains information about the connection.
    * 
    * @return the connected channel for this HTTP entity
    */   
   public Channel getChannel() {
      return builder.getChannel();
   }   
   
   /**
    * This returns the socket channel that is used by the collector
    * to read content from. This is a selectable socket, in that
    * it can be registered with a Java NIO selector. This ensures
    * that the system can be notified when the socket is ready.
    * 
    * @return the socket channel used by this collector object
    */   
   public SocketChannel getSocket() {
      return channel.getSocket();
   }
}

