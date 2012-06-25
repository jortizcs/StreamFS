/*
 * Resource.java February 2001
 *
 * Copyright (C) 2001, Niall Gallagher <niallg@users.sf.net>
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
 
package org.simpleframework.http.resource;

import org.simpleframework.http.Response;
import org.simpleframework.http.Request;

/**
 * A <code>Resource</code> provides an abstraction of any given 
 * object that can be retrieved using a HTTP request. The reason
 * for having this abstraction is to simplify the interface with 
 * any given <code>Resource</code>. 
 * <p>
 * This is similar in design to a <code>Container</code>, however
 * this is intended to handle a single resource. At any time a
 * container may manage many resources all of which are resolved
 * using a <code>ResourceEngine</code> implementation. So in 
 * essence this is used to identify a component that can handle
 * a HTTP request routed by a resource engine.
 *
 * @author Niall Gallagher
 */ 
public interface Resource {

   /**
    * This acts as the main processing method for the resources.
    * Implementations are required to provide the functions that 
    * will process the <code>Request</code> and generate a suitable
    * response for that request. This method is also responsible
    * for closing and comitting the <code>Response</code> unless
    * handed (chained) to another <code>Resource</code>.
    *
    * @param req the <code>Request</code> to be processed
    * @param resp the <code>Response</code> to be processed    
    */
   public void handle(Request req, Response resp);
}
