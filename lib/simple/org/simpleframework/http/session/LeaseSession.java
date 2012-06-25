/*
 * Delegate.java May 2007
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

import java.util.concurrent.ConcurrentHashMap;

import org.simpleframework.util.lease.Lease;

/**
 * The <code>LeaseSession</code> object is used to provide a session 
 * instance that is created from a concurrent hash map. This makes 
 * use of a <code>Lease</code> to handle all life cycle events such
 * as canceling and renewing the lease maintaining the session.
 * 
 * @author Niall Gallagher
 */
class LeaseSession<T> extends ConcurrentHashMap implements Session<T> {
   
   /**
    * This is the key used to uniquely manage the session state.
    */
   private final Lease<T> lease;
   
   /**
    * Constructor for the <code>LeaseSession</code> object. This will
    * create an instance using the provided lease for the session. 
    * The lease is used to handle all session life cycle events.
    * 
    * @param lease this is used to manage all the life cycle events 
    */
   public LeaseSession(Lease<T> lease) {
      this.lease = lease;
   }

   /**
    * This is used to acquire the <code>Lease</code> object to control
    * the session. The lease is responsible for maintaining this map
    * within the application. Once the lease expires the session will
    * be removed and its mapped values will be available for recovery.
    * 
    * @return this returns the lease used to manage this session
    */
   public Lease<T> getLease() {
      return lease;
   }
}
