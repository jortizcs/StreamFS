/*
 * TrackerBuilder.java February 2007
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

import org.simpleframework.http.session.Session;
import org.simpleframework.util.lease.LeaseException;

/**
 * The <code>TrackerBuilder</code> object is used to build an entity
 * with the aid of a session tracker implementation. This basically
 * accepts the header and body for the entity and delegates the
 * session creation to the tracker. 
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.http.core.CookieTracker
 */
class TrackerBuilder implements Builder {

   /**
    * This is the tracker object used to create the sessions.
    */
   private Tracker tracker;
   
   /**
    * This is the channel that represents the underlying transport.
    */
   private Channel channel;
   
   /**
    * This is the header object that holds the request headers.
    */
   private Header header;
   
   /**
    * This is the body object used to hold the body delivered.
    */
   private Body body;
   
   /**
    * Constructor for the <code>TrackerBuilder</code> object. This
    * is used to create a builder that delegates creation of the
    * session object to a tracker implementation. Also the channel
    * for the entity must also be provided.
    * 
    * @param tracker this is the tracker used to create sessions
    * @param channel this is the channel representing the transport
    */
   public TrackerBuilder(Tracker tracker, Channel channel) {
      this.tracker = tracker;
      this.channel = channel;
   }
   
   /**
    * Provides the <code>Body</code> object for the builder. This 
    * is used by the entity to read the content of the HTTP request.
    * Also, if the entity body is a multipart upload then each of
    * the individual parts of the body is available to read from. 
    * 
    * @param body this is the entity body provided by the request
    */    
   public void setBody(Body body) {
      this.body = body;      
   }

   /**
    * Provides the <code>Header</code> object for the builder. This
    * is used by the entity to determine the request URI and method
    * type. The header also provides start in the form of cookies
    * which can be used to track the client.
    * 
    * @param header this is the header provided by the request
    */   
   public void setHeader(Header header) {
      this.header = header;      
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
      return body;
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
      return channel;
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
      return header;
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
      return tracker.getSession(this, create);
   }
}
