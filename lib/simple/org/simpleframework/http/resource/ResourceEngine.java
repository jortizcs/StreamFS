/*
 * ResourceEngine.java February 2001
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

import org.simpleframework.http.Address;

/**
 * The <code>ResourceEngine</code> is used to create implementations
 * of the <code>Resource</code> interface that suit the targeted
 * resource. Different <code>Resource</code> objects may be needed to 
 * handle different files/directories or even applications. The request
 * URI specified must be a HTTP request URI as of RFC 2616. 
 * <p>
 * The meaning of HTTP URI in this instance is the request URI
 * from a HTTP/x.x request, as RFC 2616 and RFC 2396 defines it
 *
 * <pre> 
 * Request-Line = Method SP Request-URI SP HTTP-Version CRLF
 *
 * Request-URI = "*" | absoluteURI | abs_path | authority
 * absoluteURI = "http:" "//" host [":" port] [abs_path ["?" query]] 
 * abs_path = "/" path_segments         
 * path_segments = segment *( "/" segment )
 * </pre> 
 *
 * The <code>ResourceEngine</code> object must be prepared to accept 
 * the request URI that come in the form outlined above. These can
 * include formats like 
 *
 * <pre> 
 * http://some.host/pub;param=value/bin/index.html?name=value
 * http://some.host:8080/index.en_US.html
 * some.host:8080/index.html
 * /usr/bin;param=value/README.txt
 * /usr/bin/compress.tar.gz
 * </pre>
 *
 * The <code>ResourceEngine</code> implementation should be able to 
 * directly take a Request-URI as defined in RFC 2616 and translate 
 * this into a <code>Resource</code>. This keeps the objects semantics 
 * simple and explicit, although at the expense of performance.
 *
 * @author Niall Gallagher
 */
public interface ResourceEngine {

   /**
    * This will look for and retrieve the requested resource. The 
    * target given must be in the form of a request URI. This will
    * locate the resource and return the <code>Resource</code>
    * implementation that will handle the target.
    *
    * @param target the address used to identify the resource 
    *
    * @return this returns the resource used to handle the request
    */
   public Resource resolve(Address target);
}

