/*
 * Composer.java May 2007
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
import java.util.concurrent.TimeUnit;

import org.simpleframework.util.lease.Cleaner;
import org.simpleframework.util.lease.Lease;
import org.simpleframework.util.lease.LeaseException;
import org.simpleframework.util.lease.LeaseManager;

/**
 * The <code>Composer</code> object is used to create an object that
 * is used to create, store, and find session objects. This is also
 * used to dispose of sessions once they have expired. Creating a 
 * new session is done with the <code>compose</code> method. This
 * will create a lease for the session for a fixed duration of time.
 * 
 * @author Niall Gallagher
 */
class Composer<T> extends ConcurrentHashMap<T, Session> implements Cleaner<T> {
   
   /**
    * This is the lease manager used to maintain the session objects.
    */
   private LeaseManager<T> manager;
   
   /**
    * This is the controller used to manage all the session leases.
    */
   private Controller<T> handler;
   
   /**
    * This is the observer used to observe the session activity.
    */
   private Observer<T> observer;
   
   /**
    * This is used to determine whether the composer has been closed.
    */
   private boolean closed;
   
   /**
    * Constructor for the <code>Composer</code> object. This is used
    * create an object to compose session objects, and to manage the
    * leasing of those sessions. This makes use of the existing lease
    * framework to manage the lifecycle of the session instances.
    * 
    * @param observer this is used to observe the session manager
    * @param duration this is the idle duration for each session
    * @param unit this is the duration time unit measurement used
    */
   public Composer(Observer<T> observer, long duration, TimeUnit unit) {
      this.manager = new LeaseManager<T>(this);
      this.handler = new Maintainer<T>(manager, duration, unit);   
      this.observer = observer;
   }  
   
   /**
    * This is used to acquire an existing session using the unique
    * key for the session. If a session has previously been created
    * and the idle timeout period for that session has not expired
    * then this can be used to acquire and renew the session. 
    * 
    * @param key this is the unique key for the session object
    * 
    * @return this returns the session found for the key
    */
   public Session<T> lookup(T key) throws LeaseException {
      if(closed) {
         throw new SessionException("Session creation is closed");
      }
      return locate(key);
   }
   
   /**
    * This is used to acquire an existing session using the unique
    * key for the session. If a session has previously been created
    * and the idle timeout period for that session has not expired
    * then this can be used to acquire and renew the session. 
    * 
    * @param key this is the unique key for the session object
    * 
    * @return this returns the session found for the key
    */
   private Session<T> locate(T key) throws LeaseException {
      Session<T> session = get(key);     
      
      if(session != null) {
         handler.renew(key);
      }
      return session;
   }
   
   /**
    * This is used to create a new session using the specified key
    * as the unique identifier for that session. The key can be
    * any identifier, typically it is a string however it can be
    * any comparable object, such as an integer.
    * 
    * @param key this is the unique key for the session object
    * 
    * @return this returns the session created for the key
    */
   public Session<T> compose(T key) throws LeaseException {
      if(closed) {
         throw new SessionException("Session creation is closed");
      }
      return create(key);
   }
   
   /**
    * This is used to create a new session using the specified key
    * as the unique identifier for that session. The key can be
    * any identifier, typically it is a string however it can be
    * any comparable object, such as an integer.
    * 
    * @param key this is the unique key for the session object
    * 
    * @return this returns the session created for the key
    */
   private Session<T> create(T key) throws LeaseException {
      Lease<T> lease = handler.start(key);
      
      if(lease != null) {
         return create(key, lease);
      }
      return null;
   }
   
   /**
    * This is used to create a new session using the specified key
    * as the unique identifier for that session. The key can be
    * any identifier, typically it is a string however it can be
    * any comparable object, such as an integer.
    * 
    * @param key this is the unique key for the session object
    * @param lease this is the lease that is used by the session
    * 
    * @return this returns the session created for the key
    */
   private Session<T> create(T key, Lease<T> lease) throws LeaseException {
      Session<T> session = new LeaseSession<T>(lease);
      
      if(key != null) {
         put(key, session);
      }
      if(observer != null) {
         observer.create(session);
      }
      return session;  
   }
   
   /**
    * This <code>close</code> method is used to close the provider 
    * and release all resources associated with it. This includes 
    * canceling all active sessions. All resources held by this are
    * released by this method and all can be garbage collected.
    * 
    * @exception Exception if the composer can not be closed
    */
   public void close() throws LeaseException {
      if(!closed) {
         manager.close();
      }
      closed = true;
   }
   
   /**
    * This is used to remove the keyed session from the composer. If
    * the session does not exist then this will throw an exception.
    * Once this method has been executed the session is no longer 
    * available and a new one is required for the specified key.
    * 
    * @exception if the session does not exist or has expired
    */
   public void clean(T key) throws Exception {
      Session<T> session = remove(key);
      
      if(key != null) {
         handler.cancel(key);
      }
      if(observer != null) {
         observer.cancel(session);
      }
   }  
}