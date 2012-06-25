/*
 * Policy.java February 2007
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

import org.simpleframework.http.Cookie;

/**
 * The <code>Policy</code> object represents a policy for creating
 * session cookies. This governs the cookie name used and the value
 * that is created for the cookie. Typically the cookie value needs
 * to be a unique identifier such as the time of creation or an
 * incrementing number.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.http.core.Header
 */
interface Policy {

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
   public Cookie getSession(boolean create);
}
