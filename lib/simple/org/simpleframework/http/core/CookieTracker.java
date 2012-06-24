/*
 * CookieTracker.java February 2007
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

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.TimeUnit;

import org.simpleframework.http.Cookie;
import org.simpleframework.http.session.Session;
import org.simpleframework.http.session.SessionManager;
import org.simpleframework.http.session.SessionProvider;
import org.simpleframework.util.lease.LeaseException;

/**
 * The <code>CookieTracker</code> object provides a tracker that is
 * used to create and retrieve sessions based on a cookie. This will
 * extract the session cookie object using a provided entity. If
 * the entity header does not contain a cookie then it can be 
 * created. Once created all subsequent requests can acquire the
 * same session using the unique session cookie value in the header.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.http.session.SessionManager
 */
class CookieTracker implements Tracker {

   /**
    * This is the provider used to manage all created sessions.
    */
   private final SessionProvider provider;

   /**
    * Constructor for the <code>CookieTracker</code> object. This is
    * an adapter for a session provider implementation, which uses
    * a unique value provided in a cookie sent with the request. By
    * default the idle life of a session is twenty minutes.
    */
   public CookieTracker() {
      this(1200, SECONDS);
   }

   /**
    * Constructor for the <code>CookieTracker</code> object. This is
    * an adapter for a session provider implementation, which uses
    * a unique value provided in a cookie sent with the request. The
    * idle life of a session can be specified in this constructor.
    * 
    * @param duration this is the idle life of the sessions
    * @param unit the time unit used to measure the duration
    */
   public CookieTracker(long duration, TimeUnit unit) {
      this.provider = new SessionManager(duration, unit);
   }

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
   public Session getSession(Entity entity) throws LeaseException {
      return getSession(entity, true);
   }

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
   public Session getSession(Entity entity, boolean create) throws LeaseException {
      Header header = entity.getHeader();
      Cookie cookie = header.getSession(create);

      if(cookie == null) {
         return null;
      }
      return getSession(cookie, create);
   }

   /**
    * This is used to acquire the <code>Session</code> object using
    * the provided cookie. The value of the cookies is used as a key
    * to acquire the session from the session provider. If one does
    * not exist then it can be created if required.
    * 
    * @param cookie this is the cookie used to acquire the provider
    * @param create determines if a session should be created
    * 
    * @return a session associated with the provided cookie
    */
   private Session getSession(Cookie cookie, boolean create) throws LeaseException {
      String value = cookie.getValue();

      if(value == null) {
         return null;
      }
      return provider.open(value, create);
   }

   /**
    * This <code>close</code> method is used to close the tracker and
    * release all resources associated with it. This means canceling
    * all active sessions and emptying the contents of those sessions.
    * Threads and other such resources are released by this method.
    */
   public void close() throws LeaseException {
      provider.close();
   }
}
