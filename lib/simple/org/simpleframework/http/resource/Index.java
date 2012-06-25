/*
 * Index.java December 2005
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

import org.simpleframework.http.Path;

/**
 * The <code>Index</code> object is used to represent the properties 
 * a URI can contain. This is used so that properties relating to a
 * file can be quickly extracted from an <code>Indexer</code>. This
 * will contain all necessary meta data for a file or resource. With
 * this the <code>File</code> reference to a resource as well as the
 * locale, MIME type, name  and other such data can be accessed.
 * 
 * @author Niall Gallagher
 *
 * @see org.simpleframework.http.resource.Indexer
 */ 
public interface Index {

   /**
    * This allows the name for this object to be acquired. The
    * name usually refers to the last entry in the path. So if
    * the index target path was "/usr/bin/" the name is "bin".
    *
    * @return this returns the name of this index target
    */         
   public String getName();
   
   /**
    * This allows the MIME type of this <code>Index</code> to
    * be acquired. The MIME type of a file is retrieved by the
    * <code>Context.getContentType</code> method for a specific
    * request URI. This should have a value and perhaps some
    * parameters like the charset, "text/html; charset=UTF-8".
    *
    * @return the MIME type this object has been set to
    */    
   public String getContentType();

   /**
    * This is used to get the path that this object refers to. 
    * This should be the fully qualified normalized path. This
    * refers to the OS system specific path that this represents.
    *
    * @return this returns the OS specific path for the target
    */    
   public String getRealPath();

   /**
    * This is used to acquire the normalized URI style path for
    * the index target. This allows the path to be used within
    * the <code>Mapper</code> and other such objects that need
    * a normalized URI style path to resolve resources.
    *
    * @return this returns the normalized path for the target
    */ 
   public String getRequestPath();

   /**
    * This is used to acquire the <code>File</code> directory
    * for the index target. This is typically rooted at a
    * base path, for instance the <code>Context</code> root
    * is typically used. This allows resources within the 
    * same directory to be acquired easily.
    *
    * @return this returns the OS file for the directory
    */ 
   public File getDirectory();
   
   /**
    * This is used to acquire the <code>File</code> reference
    * for the index target. This is typically rooted at a
    * base path, for instance the <code>Context</code> root
    * is typically used. This allows the file to be opened,
    * deleted, or read should the need arise in a service.
    *
    * @return this returns the OS file for the resource
    */ 
   public File getFile();

   /**
    * This is used to acquire the <code>Path</code> object that 
    * exposes various parts of the URI path. This can be used 
    * to extract the individual path segments as strings as 
    * well as the file extension and various other details.
    *
    * @return this returns a path object with various details
    */ 
   public Path getPath();
}
