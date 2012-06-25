/*
 * Entity.java February 2007
 *
 * Copyright (C) 2001, Niall Gallagher <niallg@users.sf.net>
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
 * The <code>Entity</code> object is used to represent the HTTP entity
 * received from the client. The entity contains a header and body as
 * well as the underlying <code>Channel</code> for the connection. If
 * there is no body with the entity this will provide an empty body
 * object which provides a zero length sequence of bytes.
 * 
 * @author Niall Gallagher
 */
interface Entity {
   
   /**
    * This is used to acquire the body for this HTTP entity. This
    * will return a body which can be used to read the content of
    * the message, also if the request is multipart upload then all
    * of the parts are provided as <code>Part</code> objects. Each
    * part can then be read as an individual message.
    *  
    * @return the body provided by the HTTP request message
    */
   public Body getBody();   
   
   /**
    * This provides the HTTP request header for the entity. This is
    * always populated and provides the details sent by the client
    * such as the target URI and the query if specified. Also this
    * can be used to determine the method and protocol version used.
    * 
    * @return the header provided by the HTTP request message
    */
   public Header getHeader();
   
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
   public Session getSession(boolean create) throws LeaseException;

   /**
    * This provides the connected channel for the client. This is
    * used to send and receive bytes to and from an transport layer.
    * Each channel provided with an entity contains an attribute 
    * map which contains information about the connection.
    * 
    * @return the connected channel for this HTTP entity
    */
   public Channel getChannel();
}
