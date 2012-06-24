/*
 * Tracker.java February 2007
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
 * The <code>Tracker</code> interface represents an object that is
 * used to track a given client using some identifier. Typically the
 * client is tracked using a cookie or encoded URI. However it is
 * left up to the implementation to determine how the client will be
 * tracked. In order to acquire the information to determine what
 * client is connected the request entity is used.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.http.session.SessionProvider
 */
interface Tracker {

   /**
    * This is used to acquire the <code>Session</code> object using
    * the information within the HTTP entity. The entity contains all
    * of the information that has been sent by the client and is the
    * ideal means to differentiate clients.
    * 
    * @param entity this is the entity consumed from the pipeline
    * 
    * @return a session associated with the provided entity
    */
   public Session getSession(Entity entity) throws LeaseException;
   
   /**
    * This is used to acquire the <code>Session</code> object using
    * the information within the HTTP entity. The entity contains all
    * of the information that has been sent by the client and is the
    * ideal means to differentiate clients.
    * 
    * @param entity this is the entity consumed from the pipeline
    * @param create determines if a session should be created
    * 
    * @return a session associated with the provided entity
    */   
   public Session getSession(Entity entity, boolean create) throws LeaseException;
   
   /**
    * This <code>close</code> method is used to close the tracker and
    * release all resources associated with it. This means canceling
    * all active sessions and emptying the contents of those sessions.
    * Threads and other such resources are released by this method.
    */
   public void close() throws LeaseException;   
}
