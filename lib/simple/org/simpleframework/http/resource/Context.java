/*
 * Context.java March 2002
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
 * The <code>Context</code> interface is used to give a view of the
 * file system to the <code>ResourceEngine</code>. This provides the 
 * information to the <code>ResourceEngine</code> that it needs in 
 * order to serve content to the client browser. This provides the 
 * path translations for the HTTP request URI. 
 * <p>
 * This object essentially provides a mechanism that allows the file
 * engine to convert the HTTP request URI into OS system paths and
 * system objects such as the <code>File</code> object. A context
 * is rooted a a certain directory in the system. This directory is
 * where the resources are gathered from. For example suppose that
 * a <code>Context</code> implementation is rooted at the directory
 * "c:\web\html\" on a DOS system. Now if the target of the browser 
 * was "http://some.host/web/pub/README". The context needs to be
 * consulted to convert "/web/pub/README" into the real path within
 * the system. So <code>Context.getRealPath</code> is invoked with
 * the path "/web/pub/README", which responds with the system path
 * "c:\web\html\web\pub\README". Also if this was a UNIX system 
 * with the same context rooted at "/home/user/html" then the same
 * URL would result in "/home/user/html/web/pub/README".
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
 * So the <code>Context</code> object must be prepared to accept 
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
 * The <code>Context</code> implementation should be able to 
 * directly take a Request-URI as defined in RFC 2616 and translate 
 * this into a path compatible with the OS specific file system. 
 * This keeps the objects semantics simple and explicit, although 
 * at the expense of performance.
 *
 * @author Niall Gallagher
 *
 * @see org.simpleframework.http.parse.AddressParser
 * @see org.simpleframework.http.parse.PathParser
 */
public interface Context {

   /**
    * This is used to retrieve the base path of the context. The
    * base path of the context is that path that that this will
    * retrieve system information from. This represents a base
    * that the request URI paths are served from on the system.
    * For instance a base of "c:\path" would translate a URI
    * path of "/index.html" into "c:\path\index.html". Every
    * resource request must be relative to the context path
    * this allows the <code>ResourceEngine</code> to map the URIs
    * onto the specific OS. The base path is the OS file system
    * specific path. So on UNIX it could be "/home/user/" and
    * on a DOS system it could be "c:\web\html" for example.
    *
    * @return this returns the base path of the context
    */
   public String getBasePath();
   
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
   public String getRealPath(String target);

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
   public String getRequestPath(String target);

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
   public Path getPath(String target);
           
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
   public File getFile(String target);

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
   public File getDirectory(String target);
   
   /**
    * This method will extract the type attribute of this URI. The
    * MIME type of the request URI is extracted from the name of the
    * target. The name for the <code>Context</code> is the last path
    * segment is the token defined by RFC 2396 as path_segments. So
    * for example if the target was "some.host:8080/bin/index.html"
    * then the name for that resource would be "index.html". Once
    * the name has been extracted the MIME is defined by the file
    * extension which, for the example is text/html.
    * <p>
    * Implementations of the <code>Context</code> may also choose to
    * implement a method that consults the underlying resource and
    * inspect its contents to determine its MIME type. Or for a MAC
    * it may contain its MIME type. If the MIME type cannot be found
    * by any of the above methods RFC 2616 suggests that the resource
    * be given the MIME type application/octetstream. This should also
    * make not predictions as to how the file will be served. 
    *
    * @param target the request URI to be parsed for its type    
    *
    * @return the type of the file this path refers to
    */ 
   public String getContentType(String target);
   
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
   public String getName(String target);
   
   /**
    * This is an all in one method that allows all the information 
    * on the target URI to be gathered at once. The motivation for
    * this method is primarily convenience. However it is also used
    * to increase the performance of the <code>ResourceEngine</code>
    * when the <code>Context</code> implementation is synchronized.
    * This will enable the <code>ResourceEngine</code> to gather the
    * information on the target by acquiring the lock for the object
    * instance only once.
    *
    * @param target this is the request URI that is to be parsed
    */
   public Index getIndex(String target);
}
