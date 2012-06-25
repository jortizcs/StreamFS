/*
 * Body.java February 2007
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

import java.io.IOException;
import java.io.InputStream;

import org.simpleframework.http.Part;

/**
 * The <code>Body</code> interface is used to represent the body of
 * a HTTP entity. It contains the information that is delivered with
 * the request. Typically this will be a form POST or an XML message
 * from a SOAP request. However, it can be any stream of bytes. In
 * order to access the entity body this interface provides a stream
 * which can be used to read it. Also, should the message be encoded
 * as a multipart message the individual parts can be read using the
 * <code>Part</code> instance for it.
 *
 * @author Niall Gallagher 
 */ 
interface Body {           
   
   /**
    * This will acquire the contents of the body in UTF-8. If there
    * is no content encoding and the user of the request wants to
    * deal with the body as a string then this method can be used.
    * It will simply create a UTF-8 string using the body bytes.
    *
    * @return returns a UTF-8 string representation of the body
    */ 
   public String getContent() throws IOException;
   
   /**
    * This will acquire the contents of the body in the specified
    * charset. Typically this will be given the charset as taken 
    * from the HTTP Content-Type header. Although any encoding can
    * be specified to convert the body to a string representation.
    *
    * @return returns an encoded string representation of the body
    */ 
   public String getContent(String charset) throws IOException;   

   /**
    * This is used to acquire the contents of the body as a stream.
    * Each time this method is invoked a new stream is created that
    * will read the contents of the body from the first byte. This
    * ensures that the stream can be acquired several times without
    * any issues arising from previous reads.
    *
    * @return this returns a new string used to read the body
    */    
   public InputStream getInputStream() throws IOException;   
 
   /**
    * This method is used to acquire a <code>Part</code> from the
    * body using a known name for the part. This is typically used 
    * when there is a file upload with a multipart POST request.
    * All parts that are not files are added to the query values
    * as strings so that they can be used in a convenient way.
    * 
    * @param name this is the name of the part to acquire
    * 
    * @return the named part or null if the part does not exist
    */ 
   public Part getPart(String name);   
   
   /**
    * This method provides all parts for this body. The parts for a
    * body can contain text parameters or files. Each file part can
    * contain headers, which are the typical HTTP headers. Typically
    * headers describe the content and any encoding if required.
    * 
    * @return this returns a list of parts for this body
    */
   public PartList getParts();
}


