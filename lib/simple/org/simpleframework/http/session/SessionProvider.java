/*
 * SessionProvider.java May 2007
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

package org.simpleframework.http.session;

import org.simpleframework.util.lease.LeaseException;

/**
 * The <code>SessionProvider</code> object is used to create sessions
 * that are active for a given time period. Each session is referenced
 * using a unique key, and only one session exists per key at a time.
 * Once a session has been created it remains active for a fixed time
 * period, this time period is renewed each time that session is open.
 * <p>
 * When the session provider is no longer required it can be closed.
 * Closing the session provider will cancel and destroy all active
 * sessions, and release all resources occupied by the provider such 
 * as threads and memory. Typically it is advisable to close a provider
 * when it is not longer required by the application.
 * 
 * @author Niall Gallagher
 */
public interface SessionProvider<T> {

   /**
    * This <code>open</code> method is used to either open an existing
    * session or create a new session if one does not exist. This is
    * always guaranteed to open a session. Upon each open for a given
    * session that sessions expiry is renewed for a fixed period of 
    * time, typically this is several minutes long. 
    * 
    * @param key the unique key identifying the session instance
    * 
    * @return returns either a new session object or an existing 
    * 
    * @throws LeaseException if the keyed session can not be retrieved      
    */
   public Session open(T key) throws LeaseException;

   /**
    * This <code>open</code> method is used to either open an existing
    * session or create a new session if requested. This is used to 
    * optionally create a new session if one does not already exist.
    * This is used in situations where a session might not be required
    * but the existence of one may be queried. Once created a session
    * is active for a fixed time period. 
    * 
    * @param key the unique key identifying the session instance
    * 
    * @return returns either a new session object or an existing one
    *
    * @throws LeaseException if the keyed session can not be retrieved      
    */
   public Session open(T key, boolean create) throws LeaseException;
   
   /**
    * This <code>close</code> method is used to close the provider and
    * release all resources associated with it. This includes canceling
    * all active sessions and emptying the contents of those sessions.
    * All threads and other such resources are released by this method.
    *    
    * @throws LeaseException if the session provider can not be shutdown
    */
   public void close() throws LeaseException;
}
