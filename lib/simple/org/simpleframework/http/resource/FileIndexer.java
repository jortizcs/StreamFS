/*
 * FileIndexer.java December 2005
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
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

import org.simpleframework.http.Address;
import org.simpleframework.http.Path;

/**
 * The <code>FileIndexer</code> provides an implementation of the 
 * <code>Indexer</code> object that provides a direct mapping from
 * a request URI as defined in RFC 2616 to the resources meta data.
 * This uses a <code>File</code> object to define the mapping
 * for the request URI paths. Using a <code>File</code> object 
 * allows the <code>FileIndexer</code> to be easily used with both
 * DOS and UNIX systems.
 * <p>
 * This <code>Indexer</code> implementation uses a MIME database
 * to obtain mappings for the <code>getContentType</code> method. 
 * The file used is <code>FileIndexer.properties</code>, which is 
 * packaged within <code>org.simpleframework.http.resource</code>. 
 * This determines the MIME type of the request URI by matching file 
 * extension of the resource with the MIME type as defined in the
 * "FileIndexer.properties" file. 
 *
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.http.parse.AddressParser
 * @see org.simpleframework.http.parse.PathParser
 */
class FileIndexer implements Indexer {

   /**
    * This is used to extract any user specified MIME types.
    */ 
   private ResourceBundle resolver;

   /**
    * This is used to cache the meta information acquired.
    */ 
   private Cache cache;

   /**
    * This will be used to fetch the real OS system paths.
    */
   private File base;
   
   /**
    * Constructor for the <code>FileIndexer</code> object. This is
    * used to create a centralized store for meta data. The meta
    * data created by this is acquired from the context frequently,
    * so in order to improve performance all indexes are cached,
    * except those URI targets that contain query parameters.
    *
    * @param base this is the root of the context that is used
    */ 
   public FileIndexer(File base) {      
      this.cache = new Cache();
      this.base = base;
   }
  
   /**
    * This is an all in one method that allows all the information 
    * on the target URI to be gathered at once. The motivation for
    * this method is primarily convenience. However it is also used
    * to increase the performance of the <code>FileIndexer</code>
    * by using a cache of the most recently used indexes. This will
    * help to reduce the amount or parsing and memory required.
    *
    * @param target this is the request URI that is to be parsed
    *
    * @return this is the index of meta data for the URI target
    */   
   public Index getIndex(String target) {
      Index index = cache.get(target);
      
      if(index == null) {
         index = getIndex(this, target);
      }      
      return index;
   }

   /**
    * This is an all in one method that allows all the information 
    * on the target URI to be gathered at once. The motivation for
    * this method is primarily convenience. However it is also used
    * to increase the performance of the <code>FileIndexer</code>
    * by using a cache of the most recently used indexes. This will
    * help to reduce the amount or parsing and memory required.
    * This is used as a convinience method for caching indexes.
    *
    * @param indexer this is typically the current indexer object
    * @param target this is the request URI that is to be parsed
    *
    * @return this is the index of meta data for the URI target
    */    
   public Index getIndex(Indexer indexer, String target) {
      Index index = new FileIndex(indexer, target);
      
      if(target.indexOf('?') < 0) {
         cache.put(target, index);              
      }      
      return index;
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
   public File getFile(Address target) {
      return getFile(target.getPath());
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
   public Path getPath(Address target){
      return target.getPath();
   }
   
   /**
    * This is used to translate the request URI path into the 
    * <code>File</code> object that it represents. This will convert 
    * the path to a format that the system can use and then create    
    * the <code>File</code> object for that path. So if for example 
    * the context path was "c:\path" on a DOS system and the request
    * URI given was "/index.html" this returns the <code>File</code> 
    * "c:\path\index.html". This is basically for convenience as the
    * same could be achieved using the <code>getRealPath</code> and
    * then creating the <code>File</code> from that OS specific path.
    *
    * @param path this is the URI path that is used to retrieve the
    * <code>File</code> object
    * 
    * @return returns the <code>File</code> for the given path
    */   
   private File getFile(Path path) {
      String file = path.toString();
      
      if(file != null) {
         file = file.replace('/', File.separatorChar);
      }      
      return new File(base, file);  
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
   public String getContentType(Address target){
      return getContentType(target.getPath());
   }
   
   /**
    * This method will extract the type attribute of this path. The
    * MIME type of the request path is extracted from the name of the
    * target. The name for the <code>Context</code> is the last path
    * segment in the token defined by RFC 2396 as path_segments. So
    * for example if the target was "some.host:8080/bin/index.html"
    * then the name for that resource would be "index.html". Once
    * the name has been extracted the MIME is defined by the file
    * extension, which for the example is text/html. 
    *
    * @param path path that is to have its MIME type determined
    *
    * @return the type of the given resource path refers to
    */    
   private String getContentType(Path path){
      String ext = path.getExtension();
      String target = path.getPath();
      
      return getContentType(target, ext);
   }  

   /**
    * This method will extract the type attribute of this path. The
    * MIME type of the request path is extracted from the name of the
    * target. The name for the <code>Context</code> is the last path
    * segment is the token defined by RFC 2396 as path_segments. So
    * for example if the target was "some.host:8080/bin/index.html"
    * then the name for that resource would be "index.html". Once
    * the name has been extracted the MIME is defined by the file
    * extension, which for the example is text/html. 
    *
    * @param path path that is to have its MIME type determined
    * @param ext this is the file extension for the given path
    *
    * @return the type of the given resource path refers to
    */    
   private String getContentType(String path, String ext) {
      try {
         ResourceBundle bundle = getBundle();

         if(bundle != null) {
            return bundle.getString(ext);
         }         
      }catch(MissingResourceException e){
      }
      return "application/octetstream";          
   }
   
   /**
    * This is used to acquire the resource bundle used to map all of
    * the MIME types for the indexer. By default this will acquire
    * a properties file named <code>FileIndexer.properties</code>. If
    * this file is not found then every content type will be the
    * default <code>application/octetstream</code> type.
    * 
    * @return the resource bundle to used for this file indexer
    */
   private ResourceBundle getBundle() {
      if(resolver == null) {
         resolver = getClassBundle();
      }
      return resolver;
   }
   
   /**
    * This is used to acquire the resource bundle used to map all of
    * the MIME types for the indexer. By default this will acquire
    * a properties file named <code>FileIndexer.properties</code>. If
    * this file is not found then every content type will be the
    * default <code>application/octetstream</code> type.
    * 
    * @return the resource bundle to used for this file indexer
    */   
   private ResourceBundle getClassBundle() {
      Class type = FileIndexer.class;
      String name = type.getName();  
      
      return ResourceBundle.getBundle(name);
   }
   
   /**
    * The <code>Cache</code> object essentially acts as a convenient
    * typedef for caching addresses to their index. This allows the
    * index to be retrieved much quicker when requested again. Each
    * address key contains the query parameters and path parameters.
    * 
    * @author Niall Gallagher   
    */
   private class Cache extends ConcurrentHashMap<String, Index> {
      
      /**
       * Constructor for the <code>Cache</code> object. This acts 
       * as a quick means to lookup data on a resource by taking
       * an unparsed address and mapping that to an index object.       
       */
      public Cache() {
         super();
      }
   }
}

