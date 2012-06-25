/*
 * Indexer.java December 2005
 *
 * Copyright (C) 2005, Niall Gallagher <niallg@users.sf.net>
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

import java.io.File;

import org.simpleframework.http.Address;
import org.simpleframework.http.Path;

/**
 * The <code>Indexer</code> object is used to acquire meta data for 
 * a address target. This provides a centralized source for meta data
 * within the server. The need to acquire information such as MIME
 * type, locale, and various other details for a Address frequently
 * arises. In order to provide a consistent set of details for a
 * specific target an <code>Indexer</code> implementation is used.
 * This helps various services and resources acquire meta data
 * quickly by facilitating a meta data cache for a context.
 * 
 * @author Niall Gallagher
 *
 * @see org.simpleframework.http.resource.Index
 */ 
interface Indexer {

   /**
    * This is used to acquire the <code>File</code> reference
    * for the index target. This is typically rooted at a
    * base path, for instance the <code>Context</code> root
    * is typically used. This allows the file to be opened,
    * deleted, or read should the need arise in a service.
    *
    * @param target the index target to get the OS file for
    * 
    * @return this returns the OS file for the resource
    */    
   public File getFile(Address target);

   /**
    * This is used to acquire the <code>Path</code> object that 
    * exposes various parts of the address path. This can be used 
    * to extract the individual path segments as strings as 
    * well as the file extension and various other details.
    *
    * @param target the index target to get the Address path for
    * 
    * @return this returns a path object with various details
    */    
   public Path getPath(Address target);

   /**
    * This allows the MIME type of this <code>Index</code> to
    * be acquired. The MIME type of a file is retrieved by the
    * <code>Context.getContentType</code> method for a specific
    * request address. This should have a value and perhaps some
    * parameters like the charset, "text/html; charset=UTF-8".
    *
    * @param target the index target to get the MIME type for
    * 
    * @return the MIME type this object has been set to
    */     
   public String getContentType(Address target);
}