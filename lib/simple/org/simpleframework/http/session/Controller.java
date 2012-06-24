/*
 * Controller.java May 2007
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

import org.simpleframework.util.lease.Lease;
import org.simpleframework.util.lease.LeaseException;

/**
 * The <code>Controller</code> object is used to manage all life cycle
 * events for sessions. This is used to start and renew all leases
 * issued. The start life cycle event is used when the session is first
 * created, this will lease the new session for some fixed duration. 
 * The renew event is performed when an existing session is accessed 
 * again so that the session can be maintained for the default period.
 * 
 * @author Niall Gallagher
 */
interface Controller<T> {
   
   /**
    * The <code>start</code> method is used when a session is to 
    * be created for the first time. This will ensure that the key 
    * specified is used to dispose of the session when its idle
    * timeout has expired. 
    * 
    * @param key this is the unique key identifying the session
    */
   public Lease<T> start(T key) throws LeaseException;
   
   /**
    * The <code>renew</code> method is used when a session has been 
    * accessed for a again. This ensures that the key specified is 
    * used to dispose of the session when its idle timeout expires. 
    * 
    * @param key this is the unique key identifying the session
    */   
   public void renew(T key) throws LeaseException; 
   
   /**
    * The <code>cancel</code> method is used when a session is no
    * longer required and is to be disposed of. This is typically
    * invoked by a web application to release occupied resources.
    * 
    * @param key this is the unique key identifying the session
    */
   public void cancel(T key) throws LeaseException;
}