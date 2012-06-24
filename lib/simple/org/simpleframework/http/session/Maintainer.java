/*
 * Maintainer.java May 2007
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

import java.util.concurrent.TimeUnit;

import org.simpleframework.util.lease.Lease;
import org.simpleframework.util.lease.LeaseException;
import org.simpleframework.util.lease.LeaseManager;
import org.simpleframework.util.lease.LeaseMap;

/**
 * The <code>Maintainer</code> object is used to manage all life cycle
 * events for sessions. This is used to start and renew all leases
 * issued. The start life cycle event is used when the session is first
 * created, this will lease the new session for some fixed duration. 
 * The renew event is performed when an existing session is accessed 
 * again so that the session can be maintained for the default period.
 * 
 * @author Niall Gallagher
 */
class Maintainer<T> implements Controller<T> {
   
   /**
    * This is the lease manager used to create the lease objects.
    */
   private final LeaseManager<T> manager;
   
   /**    
    * This is the lease map used to maintain the leases by key.
    */
   private final LeaseMap<T> map;
   
   /**
    * This is the time unit used for the fixed lease duration.
    */
   private final TimeUnit unit;
   
   /**
    * This is the duration the lease objects are renewed for.
    */
   private final long duration;
   
   /**
    * Constructor for the <code>Maintainer</code> object. This is used 
    * to maintain a lease for a resource using only the key for that
    * resource. Each renewal of the lease is performed using a fixed
    * duration so that the the time does not need to be specified.
    * 
    * @param manager this is the manager used to create the leases
    * @param duration this is the fixed duration to renew for
    * @param unit this is the unit of measurement for the duration
    */
   public Maintainer(LeaseManager<T> manager, long duration, TimeUnit unit) {
      this.map = new LeaseMap<T>();
      this.manager = manager;
      this.duration = duration;
      this.unit = unit;
   }
   
   /**
    * The <code>start</code> method is used when a session is to 
    * be created for the first time. This will ensure that the key 
    * specified is used to dispose of the session when its idle
    * timeout has expired. 
    * 
    * @param key this is the unique key identifying the session
    */
   public Lease<T> start(T key) throws LeaseException {
      Lease<T> lease = manager.lease(key, duration, unit);
      
      if(lease != null) {
         map.put(key, lease);
      }
      return lease;
   }   
   
   /**
    * The <code>renew</code> method is used when a session has been 
    * accessed for a again. This ensures that the key specified is 
    * used to dispose of the session when its idle timeout expires. 
    * 
    * @param key this is the unique key identifying the session
    */
   public void renew(T key) throws LeaseException {
      Lease<T> lease = map.get(key);
      
      if(lease == null) {
         throw new SessionException("Session does not exist");
      }
      lease.renew(duration, unit);    
   }
   
   /**
    * The <code>cancel</code> method is used when a session is no
    * longer required and is to be disposed of. This is typically
    * invoked by a web application to release occupied resources.
    * 
    * @param key this is the unique key identifying the session
    */
   public void cancel(T key) throws LeaseException {
      Lease<T> lease = map.remove(key);
      
      if(lease == null) {
         throw new SessionException("Session does not exist");         
      }
   }
}
