/*
 * SecurePolicy.java February 2007
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

import java.util.UUID;

import org.simpleframework.http.Cookie;

/**
 * The <code>SecurePolicy</code> object is used to create a policy
 * that can create cookies that are secure. This ensures that the
 * cookies can not be predicted and thus hijacked. Because there is
 * no standard for session cookies, this uses the Java Servlet API
 * naming convention, which names cookie JSESSIONID.
 * 
 * @author Niall Gallagher
 */
class SecurePolicy implements Policy {

   /**
    * This provides the name of the cookies that are created.
    */
   private static final String NAME = "JSESSIONID";
   
   /**
    * This is the header to acquire the cookie value from.
    */
   private Header header;
   
   /**
    * This is the cookie created for the session identifier.
    */
   private Cookie cookie;

   /**
    * Constructor for the <code>SecurePolicy</code> object. This is
    * used to create a policy that will generate cookies which have
    * secure value, which ensures session hijacking is not possible.
    * 
    * @param header this is the header to search for the cookie
    */
   public SecurePolicy(Header header) {
      this.header = header;
   } 

   /**
    * This is used to acquire the session cookie for the request. The
    * session cookie is either sent with the HTTP request header or
    * it can be created if required. This ensures that if no cookie
    * has been sent one can be created on demand. 
    * 
    * @param create if true the session cookie will be created
    * 
    * @return the cookie associated with the session or null
    */   
   public Cookie getSession(boolean create) {
      if(cookie != null) {
         return cookie;
      }
      cookie = header.getCookie(NAME);
      
      if(cookie == null) {
         if(create) {
            cookie = getCookie(NAME);
         }
      }      
      return cookie;
   }
   
   /**
    * This is used to create the cookie value to be used. The value 
    * used for the cookie is created using a random number generator.
    * This ensures that the cookie names created are secure to a
    * point that they can not be hijacked by another used.
    * 
    * @param name this is the name of the cookie to be created
    * 
    * @return a session cookie with a secure cookie value
    */
   private Cookie getCookie(String name) {
      UUID identity = UUID.randomUUID();
      String value = identity.toString();
      
      return new Cookie(name, value, true);
   }
}
