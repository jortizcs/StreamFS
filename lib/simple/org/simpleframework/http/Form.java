/*
 * Form.java February 2007
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

package org.simpleframework.http;

import java.util.List;

/**
 * The <code>Form</code> interface is used to represent the details
 * submitted with a request. Typically this will be parameters given
 * by a HTML form, however a form can also contain parts. Each part
 * can represent either a file or a parameter. All parts can be 
 * acquired as <code>Part</code> objects from this <code>Form</code>.
 * 
 * @author Niall Gallagher
 */
public interface Form extends Query {
   
   /**
    * This method is used to acquire a <code>Part</code> from the
    * form using a known name for the part. This is typically used 
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
    * This method provides all parts for this <code>Form</code>. The
    * parts for a form can contain text parameters or files. Each file
    * part can contain headers, which take the form of HTTP headers to
    * describe the payload. Typically headers describe the content.
    * 
    * @return this returns a list of parts for this form
    */
   public List<Part> getParts();
}