/*
 * FileContext.java March 2002
 *
 * Copyright (C) 2002, Niall Gallagher <niallg@users.sf.net>
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
 * The <code>FileContext</code> provides an implementation of the 
 * <code>Context</code> object that provides a direct mapping from
 * a request URI as defined in RFC 2616 to an OS specific target.
 * This uses a <code>File</code> object to define the mapping
 * for the request URI paths. Using a <code>File</code> object 
 * allows the <code>FileContext</code> to be easily used with both
 * DOS and UNIX systems.
 * <p>
 * This <code>Indexer</code> implementation uses an MIME database
 * to obtain mappings for the <code>getContentType</code> method. 
 * The file used is acquired from the class path as a mapping from
 * file extension to MIME type. This file can be modified if any
 * additional types are required. However it is more advisable to
 * simple extend this object and override the content type method.
 *
 * @author Niall Gallagher
 *
 * @see org.simpleframework.http.resource.FileIndexer
 */
public class FileContext implements Context {

   /**
    * This is used to extract any user specified MIME types.
    */ 
   private final FileIndexer indexer;

   /**
    * This will be used to fetch the real OS system paths.
    */
   private final File base;
   
   /**
    * Constructor for creating an instance that operates from
    * the given current working path. This instance will use
    * the current path to translate the HTTP request URIs
    * into the OS specific path. This will load configuration
    * files from the current working directory.
    */
   public FileContext() {
      this(new File("."));
   }

   /**
    * Constructor for creating an instance that operates from
    * the given OS specific base path. This instance will use
    * the given base path to translate the HTTP request URIs
    * into the OS specific path. This will load configuration
    * files from the specified directory path. 
    *
    * @param base this is the OS specific base path for this
    */
   public FileContext(File base) {
      this.indexer = new FileIndexer(base);
      this.base = base;
   }
   
   /**
    * This is used to retrieve the base path of the context. The
    * base path of the context is that path that that this will
    * retrieve system information from. This represents a base
    * that the request URI paths are served from on the system.
    * For instance a base of "c:\path" would translate a URI
    * path of "/index.html" into "c:\path\index.html". Every
    * resource request must be relative to the context path
    * this allows the <code>FileEngine</code> to map the URIs
    * onto the specific OS. The base path is the OS file system
    * specific path. So on UNIX it could be "/home/user/" and
    * on a DOS system it could be "c:\web\html" for example.
    *
    * @return this returns the base path of the context
    */
   public String getBasePath() {
      return base.getAbsolutePath();
   }

   /**
    * This is used to translate the HTTP request URI into the OS
    * specific path that it represents. This will convert the 
    * URI to a format that the system can use and also represents
    * the resource path on that system. So if for example the
    * context path was "c:\path" on a DOS system and the HTTP URI 
    * given was "/index.html" this returns "c:\path\index.html".
    * If a UNIX system was running the VM and the context base
    * was for example "/home/" then this would return the UNIX
    * path "/home/index.html" for the same request URI.
    *
    * @param target this is the HTTP request URI path that is to 
    * be translated into the OS specific path
    * 
    * @return this returns the OS specific path name for the 
    * translate request URI
    */
   public String getRealPath(String target){
      return getIndex(target).getRealPath();
   }
  
   /**
    * This is used to translate the HTTP request URI into the URI
    * path normalized and without query or parameter parts. This
    * is used so that the resource requested by the client can be
    * discovered. For example this will convert the HTTP request
    * URI "http://hostname/bin;param=value/../index.html?query" 
    * into the relative URI path /index.html. This is useful if 
    * a logging mechanism requires the name of the resource that
    * was requested, it can also be used help find the resource.
    *
    * @param target this is the HTTP request URI that is to be
    * converted into a normalized relative URI path
    *
    * @return the HTTP request URI as a normalized relative path
    */
   public String getRequestPath(String target){
      return getIndex(target).getRequestPath();
   }
  
   /**
    * This is used to translate the HTTP request URI into the 
    * <code>File</code> object that it represents. This will convert 
    * the URI to a format that the system can use and then create    
    * the <code>File</code> object for that path. So if for example 
    * the context path was "c:\path" on a DOS system and the HTTP 
    * URI given was "/index.html" this returns the <code>File</code> 
    * "c:\path\index.html". This is basically for convenience as the
    * same could be achieved using the <code>getRealPath</code> and
    * then creating the <code>File</code> from that OS specific path.
    *
    * @param target this is the HTTP request URI path that is used
    * to retrieve the <code>File</code> object
    * 
    * @return returns the <code>File</code> for the given path
    */   
   public File getFile(String target) {
      return getIndex(target).getFile();
   }

   /**
    * This is used to translate the HTTP request URI into the
    * <code>File</code> object that it represent the parent directory
    * of the URI. This will convert the URI to a format that the host
    * system can use and then create the <code>File</code> object for
    * that path. So if for example the context path was "c:\path" on
    * a DOS system and the HTTP URI given was "/index.html" this 
    * returns the <code>File</code> "c:\path\". This is basically 
    * for convenience as the same could be achieved using the file
    * retrieved from <code>getFile</code> and acquiring the parent.
    *
    * @param target this is the HTTP request URI path that is used
    * to retrieve the <code>File</code> object
    * 
    * @return returns the <code>File</code> for the directory
    */ 
   public File getDirectory(String target) {
      return getIndex(target).getDirectory();
   }
    
   /**
    * This is used to translate the HTTP request URI into the
    * <code>Path</code> object that it represents. This enables the
    * HTTP request URI to be examined thoroughly an allows various
    * other files to be examined relative to it. For example if the
    * URI referenced a path "/usr/bin/file" and some resource
    * in the same directory is required then the <code>Path</code>
    * can be used to acquire the relative path. This is useful if
    * links within a HTML page are to be dynamically generated. The
    * <code>Path.getRelative</code> provides this functionality.
    * 
    * @param target this is the HTTP request URI path that is used
    * to retrieve the <code>Path</code> object
    *
    * @return returns the <code>Path</code> for the given path
    */
   public Path getPath(String target){
      return getIndex(target).getPath();
   }

   /**
    * This method will extract the type attribute of this URI. The
    * MIME type of the request URI is extracted from the name of the
    * target. The name for the <code>Context</code> is the last path
    * segment in the token defined by RFC 2396 as path_segments. So
    * for example if the target was "some.host:8080/bin/index.html"
    * then the name for that resource would be "index.html". Once
    * the name has been extracted the MIME is defined by the file
    * extension, which for the example is text/html. 
    *
    * @param target the request URI to be parsed for its type    
    *
    * @return the type of the given request URI path refers to
    */ 
   public String getContentType(String target){
      return getIndex(target).getContentType();
   }
   
   /**
    * This will parse and return the file name that this request URI
    * references. The name for the <code>Context</code> is the last 
    * path segment is the token defined by RFC 2396 as path_segments. 
    * So for example if the target was "some.host:8080/home/user/"
    * then the name for that resource would be "user". If the path 
    * references the root path "/" then null should be returned.
    *
    * @param target the request URI to be parsed for its name
    *
    * @return this will return the name that this references
    */ 
   public String getName(String target){
      return getIndex(target).getName();
   }

   /**
    * This is an all in one method that allows all the information 
    * on the target URI to be gathered at once. The motivation for
    * this method is primarily convenience. However it is also used
    * to increase the performance of the <code>FileEngine</code>
    * when the <code>Context</code> implementation is synchronized.
    * This will enable the <code>FileEngine</code> to gather the
    * information on the target by acquiring the lock for the object
    * instance only once.
    *
    * @param target this is the request URI that is to be parsed
    */
   public Index getIndex(String target){
      return indexer.getIndex(target);
   } 
}

