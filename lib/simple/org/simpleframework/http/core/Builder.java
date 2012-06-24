/*
 * Builder.java February 2007
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

package org.simpleframework.http.core;

/**
 * The <code>Builder</code> object is used to build and entity from
 * its constituent parts. Each component of the entity is provided
 * to the builder once they have been extracted from the connected
 * pipeline. Once all parts have been acquired by the builder the
 * entity is ready to used to process the request.
 * 
 * @author Niall Gallagher
 */
interface Builder extends Entity {
   
   /**
    * Provides the <code>Body</code> object for the builder. This 
    * is used by the entity to read the content of the HTTP request.
    * Also, if the entity body is a multipart upload then each of
    * the individual parts of the body is available to read from. 
    * 
    * @param body this is the entity body provided by the request
    */ 
   public void setBody(Body body);
   
   /**
    * Provides the <code>Header</code> object for the builder. This
    * is used by the entity to determine the request URI and method
    * type. The header also provides start in the form of cookies
    * which can be used to track the client.
    * 
    * @param header this is the header provided by the request
    */
   public void setHeader(Header header);
}
