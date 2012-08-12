/*
 * FileIndex.java December 2005
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
import org.simpleframework.http.parse.AddressParser;

/**
 * The <code>FileIndex</code> provides an implementation of an index
 * that makes use of the OS file system to acquire meta data. This
 * will acquire information directly from the URI target, as well as
 * a MIME database maintained in the <code>FileIndexer.properties</code> 
 * file. This caches all meta data acquired so that there is no expense 
 * in re-acquiring the data. This allows for faster meta data retrieval
 * and facilitates the implementation of the meta data cache used.
 * 
 * @author Niall Gallagher
 *
 * @see org.simpleframework.http.resource.FileIndexer
 */ 
class FileIndex implements Index {

   /**
    * This is the source indexer used to acquire the meta data.
    */         
   private Indexer indexer;

   /**
    * This is the path portion of the specified URI target.
    */ 
   private Path path;

   /**
    * This is the OS specific file referencing the resource. 
    */ 
   private File file;

   /**
    * This is the MIME type resolved for the resource.
    */ 
   private String type;

   /**
    * This contains all the information regarding the URI.
    */ 
   private Address target;

   /**
    * Constructor for the <code>FileIndex</code> object. This uses a
    * URI target to acquire the meta data for the resource. The URI
    * provides the resource name and path and also provides a hint
    * for the MIME type of the resource from the file extension.
    *
    * @param indexer this is the source indexer for this instance
    * @param target this is the URI target that is to be indexed
    */  
   public FileIndex(Indexer indexer, String target) {
      this(indexer, new AddressParser(target));
   }

   /**
    * Constructor for the <code>FileIndex</code> object. This uses a
    * URI target to acquire the meta data for the resource. The URI
    * provides the resource name and path and also provides a hint
    * for the MIME type of the resource from the file extension.
    *
    * @param indexer this is the source indexer for this instance
    * @param target this is the URI target that is to be indexed
    */     
   public FileIndex(Indexer indexer, Address target) {
      this.indexer = indexer;
      this.target = target;      
   }   

   /**
    * This is used to get the path that this object refers to. 
    * This should be the fully qualified normalized path. This
    * refers to the OS system specific path that this represents.
    *
    * @return this returns the OS specific path for the target
    */    
   public String getContentType() {
      if(type == null) {
         type = getContentType(target);              
      }           
      return type;
   }

   /**
    * This is used to get the path that this object refers to. 
    * This should be the fully qualified normalized path. This
    * refers to the OS system specific path that this represents.
    *
    * @param target the index target to get the real path for
    * 
    * @return this returns the OS specific path for the target
    */    
   public String getContentType(Address target) {
      return indexer.getContentType(target);           
   }

   /**
    * This is used to acquire the <code>File</code> reference
    * for the index target. This is typically rooted at a
    * base path, for instance the <code>Context</code> root
    * is typically used. This allows the file to be opened,
    * deleted, or read should the need arise in a service.
    *
    * @return this returns the OS file for the resource
    */    
   public File getFile() {
      if(file == null) {
         file = getFile(target);              
      }           
      return file;
   }

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
   public File getFile(Address target) {
      return indexer.getFile(target);           
   }

   /**
    * This is used to acquire the <code>Path</code> object that 
    * exposes various parts of the URI path. This can be used 
    * to extract the individual path segments as strings as 
    * well as the file extension and various other details.
    *
    * @return this returns a path object with various details
    */    
   public Path getPath() {
      if(path == null) {
         path = getPath(target);
      }
      return path;      
   }

   /**
    * This is used to acquire the <code>Path</code> object that 
    * exposes various parts of the URI path. This can be used 
    * to extract the individual path segments as strings as 
    * well as the file extension and various other details.
    *
    * @param target the index target to get the URI path for
    * 
    * @return this returns a path object with various details
    */    
   public Path getPath(Address target) {
      return indexer.getPath(target);           
   }

   /**
    * This is used to get the path that this object refers to. 
    * This should be the fully qualified normalized path. This
    * refers to the OS system specific path that this represents.
    *
    * @return this returns the OS specific path for the target
    */    
   public String getRealPath() {
      return getFile().getAbsolutePath();           
   }
   
   /**
    * This is used to acquire the <code>File</code> directory
    * for the index target. This is typically rooted at a
    * base path, for instance the <code>Context</code> root
    * is typically used. This allows resources within the 
    * same directory to be acquired easily.
    * 
    * @return this returns the OS file for the directory
    */ 
   public File getDirectory() {
      return getFile().getParentFile();
   }

   /**
    * This is used to acquire the normalized URI style path for
    * the index target. This allows the path to be used within
    * the <code>Context</code> and other such objects that need
    * a normalized URI style path to resolve resources.
    *
    * @return this returns the normalized path for the target
    */    
   public String getRequestPath() {
      return getPath().getPath();
   }

   /**
    * This allows the name for this object to be acquired. The
    * name usually refers to the last entry in the path. So if
    * the index target path was "/usr/bin/" the name is "bin".
    *
    * @return this returns the name of this index target
    */           
   public String getName() {
      return getPath().getName();
   }
}
