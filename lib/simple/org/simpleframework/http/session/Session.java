/*
 * Session.java May 2007
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

import java.util.Map;

import org.simpleframework.util.lease.Lease;

/**
 * The <code>Session</code> object is a simple leased container for
 * state within a web application. This is essentially a map of key
 * value pairs leased on a fixed duration to ensure it remains active
 * between we requests. If the session remains idle for sufficiently
 * long then it is disposed of by the <code>SessionProvider</code> 
 * so that resources occupied can be released.   
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.util.lease.Lease
 */
public interface Session<T> extends Map {  
   
  /**
   * This is used to acquire the <code>Lease</code> object to control
   * the session. The lease is responsible for maintaining this map
   * within the application. Once the lease expires the session will
   * be removed and its mapped values will be available for recovery.
   * 
   * @return this returns the lease used to manage this session
   */
  public Lease<T> getLease();  
}
