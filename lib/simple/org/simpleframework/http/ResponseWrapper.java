/*
 * ResponseWrapper.java February 2001
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

package org.simpleframework.http;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.channels.WritableByteChannel;
import java.util.List;

/** 
 * The <code>ResponseWrapper</code> object is used so that the original
 * <code>Response</code> object can be wrapped in a filtering proxy
 * object. This allows a container to interact with an implementation
 * of this with overridden methods providing specific functionality.
 * the <code>Response</code> object in a concurrent environment.
 * <pre>
 *
 *    public void handle(Request req, Response resp) {
 *       handler.handle(req, new ZipResponse(resp));
 *    }
 *
 * </pre>
 * The above is an example of how the <code>ResponseWrapper</code> can 
 * be used to provide extra functionality to a <code>Response</code>
 * in a transparent manner. Such an implementation could apply a
 * Content-Encoding header and compress the response for performance
 * over a slow network. Filtering can be applied with the use of
 * layered <code>Container</code> objects.
 *
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.http.core.Container
 */
public class ResponseWrapper implements Response {
   
   /**
    * This is the response instance that is being wrapped.
    */
   protected Response response;          

   /** 
    * Constructor for <code>ResponseWrapper</code> object. This allows
    * the original <code>Response</code> object to be wrapped so that 
    * adjustments to the behavior of a request object handed to the 
    * container can be provided by a subclass implementation. 
    *
    * @param response the response object that is being wrapped
    */
   public ResponseWrapper(Response response){
      this.response = response;
   }
   
   /**
    * This represents the status code of the HTTP response. 
    * The response code represents the type of message that is
    * being sent to the client. For a description of the codes
    * see RFC 2616 section 10, Status Code Definitions. 
    *
    * @return the status code that this HTTP response has
    */ 
   public int getCode() {
      return response.getCode();
   }
     
   /**
    * This method allows the status for the response to be 
    * changed. This MUST be reflected the the response content
    * given to the client. For a description of the codes see
    * RFC 2616 section 10, Status Code Definitions.
    *
    * @param code the new status code for the HTTP response
    */ 
   public void setCode(int code) {
      response.setCode(code);
   }

   /**
    * This can be used to retrieve the text of a HTTP status
    * line. This is the text description for the status code.
    * This should match the status code specified by the RFC.
    *
    * @return the message description of the response
    */ 
   public String getText() {
      return response.getText();
   }

   /**
    * This is used to set the text of the HTTP status line.
    * This should match the status code specified by the RFC.
    *
    * @param text the descriptive text message of the status
    */ 
   public void setText(String text) {
      response.setText(text);
   }

   /**
    * This can be used to get the major number from a HTTP version.
    * The major version corresponds to the major type that is the 1
    * of a HTTP/1.0 version string. 
    *
    * @return the major version number for the request message
    */ 
   public int getMajor() {
      return response.getMajor();
   }

   /**
    * This can be used to set the major number from a HTTP version.
    * The major version corresponds to the major type that is the 1
    * of a HTTP/1.0 version string. 
    *
    * @param major the major version number for the request message
    */ 
   public void setMajor(int major) {
      response.setMajor(major);
   }

   /**
    * This can be used to get the minor number from a HTTP version.
    * The minor version corresponds to the major type that is the 0
    * of a HTTP/1.0 version string. This is used to determine if 
    * the request message has keep alive semantics.
    *
    * @return the minor version number for the request message
    */ 
   public int getMinor() {
      return response.getMinor();
   }
   
   /**
    * This can be used to get the minor number from a HTTP version.
    * The minor version corresponds to the major type that is the 0
    * of a HTTP/1.0 version string. This is used to determine if 
    * the request message has keep alive semantics.
    *
    * @param minor the minor version number for the request message
    */ 
   public void setMinor(int minor) {
      response.setMinor(minor);
   }
   
   /**
    * This is used to acquire the names of the of the headers that
    * have been set in the response. This can be used to acquire all
    * header values by name that have been set within the response.
    * If no headers have been set this will return an empty list.
    * 
    * @return a list of strings representing the set header names
    */
   public List<String> getNames() {
      return response.getNames();
   }

   /**
    * This can be used to add a HTTP message header to this object.
    * The name and value of the HTTP message header will be used to
    * create a HTTP message header object which can be retrieved using
    * the <code>getValue</code> in combination with the get methods.
    *
    * @param name the name of the HTTP message header to be added
    * @param value the value the HTTP message header will have
    */   
   public void add(String name, String value) {
      response.add(name, value);
   }
   
   /**
    * This can be used to add a HTTP message header to this object.
    * The name and value of the HTTP message header will be used to
    * create a HTTP message header object which can be retrieved using
    * the <code>getInteger</code> in combination with the get methods.
    *
    * @param name the name of the HTTP message header to be added
    * @param value the value the HTTP message header will have
    */    
   public void add(String name, int value) {
      response.add(name, value);
   }
   
   /**
    * This is used as a convenience method for adding a header that
    * needs to be parsed into a HTTPdate string. This will convert
    * the date given into a date string defined in RFC 2616 sec 3.3.1.
    *
    * @param name the name of the HTTP message header to be added
    * @param date the value constructed as an RFC 1123 date string
    */   
   public void addDate(String name, long date) {
      response.addDate(name, date);
   }
   
   /**
    * This can be used to set a HTTP message header to this object.
    * The name and value of the HTTP message header will be used to
    * create a HTTP message header object which can be retrieved using
    * the <code>getValue</code> in combination with the get methods.
    * This will perform a <code>remove</code> using the issued header
    * name before the header value is set.       
    *
    * @param name the name of the HTTP message header to be added
    * @param value the value the HTTP message header will have
    */    
   public void set(String name, String value) {
      response.set(name, value);
   }
   
   /**
    * This can be used to set a HTTP message header to this object.
    * The name and value of the HTTP message header will be used to
    * create a HTTP message header object which can be retrieved using
    * the <code>getValue</code> in combination with the get methods.
    * This will perform a <code>remove</code> using the issued header
    * name before the header value is set.       
    *
    * @param name the name of the HTTP message header to be added
    * @param value the value the HTTP message header will have
    */    
   public void set(String name, int value) {
      response.set(name, value);
   }
   
   /**
    * This is used as a convenience method for adding a header that
    * needs to be parsed into a HTTP date string. This will convert
    * the date given into a date string defined in RFC 2616 sec 3.3.1.
    * This will perform a <code>remove</code> using the issued header
    * name before the header value is set.       
    *
    * @param name the name of the HTTP message header to be added
    * @param date the value constructed as an RFC 1123 date string
    */ 
   public void setDate(String name, long date) {
      response.setDate(name, date);
   }

   /**
    * This is used to remove the named header from the response. This
    * removes all header values assigned to the specified name. If it
    * does not exist then this will return without modifying the HTTP
    * response. Headers names removed are case insensitive.
    *
    * @param name the HTTP message header to remove from the response
    */     
   public void remove(String name) {
      response.remove(name);
   }

   /**
    * This is used to see if there is a HTTP message header with the
    * given name in this container. If there is a HTTP message header
    * with the specified name then this returns true otherwise false.
    *
    * @param name the HTTP message header to get the value from
    *
    * @return this returns true if the HTTP message header exists
    */   
   public boolean contains(String name) {
      return response.contains(name);
   }
   
   /**
    * This can be used to get the value of the first message header
    * that has the specified name. This will return the full string
    * representing the named header value. If the named header does
    * not exist then this will return a null value.
    *
    * @param name the HTTP message header to get the value from
    *
    * @return this returns the value that the HTTP message header
    */   
   public String getValue(String name) {
      return response.getValue(name);
   }
   
   /**
    * This can be used to get the value of the first message header
    * that has the specified name. This will return the integer
    * representing the named header value. If the named header does
    * not exist then this will return a value of minus one, -1.
    *
    * @param name the HTTP message header to get the value from
    *
    * @return this returns the value that the HTTP message header
    */     
   public int getInteger(String name) {
      return response.getInteger(name);
   }
   
   /**
    * This can be used to get the value of the first message header
    * that has the specified name. This will return the long value
    * representing the named header value. If the named header does
    * not exist then this will return a value of minus one, -1.
    *
    * @param name the HTTP message header to get the value from
    *
    * @return this returns the value that the HTTP message header
    */ 
   public long getDate(String name) {
      return response.getDate(name);
   }

   /**
    * This can be used to get the values of HTTP message headers
    * that have the specified name. This is a convenience method that 
    * will present that values as tokens extracted from the header.
    * This has obvious performance benefits as it avoids having to 
    * deal with <code>substring</code> and <code>trim</code> calls.
    * <p>
    * The tokens returned by this method are ordered according to
    * there HTTP quality values, or "q" values, see RFC 2616 section
    * 3.9. This also strips out the quality parameter from tokens
    * returned. So "image/html; q=0.9" results in "image/html". If
    * there are no "q" values present then order is by appearance.
    * <p> 
    * The result from this is either the trimmed header value, that
    * is, the header value with no leading or trailing whitespace
    * or an array of trimmed tokens ordered with the most preferred
    * in the lower indexes, so index 0 is has highest preference.
    *
    * @param name the name of the headers that are to be retrieved
    *
    * @return ordered list of tokens extracted from the header(s)
    */    
   public List<String> getValues(String name) {
      return response.getValues(name);
   }
   
   /**
    * The <code>setCookie</code> method is used to set a cookie value 
    * with the cookie name. This will add a cookie to the response
    * stored under the name of the cookie, when this is committed it 
    * will be added as a Set-Cookie header to the resulting response.
    *
    * @param cookie this is the cookie to be added to the response
    * 
    * @return returns the cookie that has been set in the response
    */
   public Cookie setCookie(Cookie cookie) {
      return response.setCookie(cookie);
   }

   /**
    * The <code>setCookie</code> method is used to set a cookie value 
    * with the cookie name. This will add a cookie to the response
    * stored under the name of the cookie, when this is committed it 
    * will be added as a Set-Cookie header to the resulting response.
    * This is a convenience method that avoids cookie creation.     
    *
    * @param name this is the cookie to be added to the response
    * @param value this is the cookie value that is to be used
    * 
    * @return returns the cookie that has been set in the response
    */
   public Cookie setCookie(String name, String value) {
      return response.setCookie(name, value);
   }

   /**
    * This returns the <code>Cookie</code> object stored under the
    * specified name. This is used to retrieve cookies that have been 
    * set with the <code>setCookie</code> methods. If the cookie does
    * not exist under the specified name this will return null. 
    *
    * @param name this is the name of the cookie to be retrieved
    * 
    * @return returns the cookie object send with the request
    */   
   public Cookie getCookie(String name) {
      return response.getCookie(name);
   }
   
   /**
    * This returns all <code>Cookie</code> objects stored under the
    * specified name. This is used to retrieve cookies that have been 
    * set with the <code>setCookie</code> methods. If there are no
    * cookies then this will return an empty list. 
    * 
    * @return returns all the cookie objects for this response
    */     
   public List<Cookie> getCookies() {
      return response.getCookies();
   }
   
   /**
    * This is a convenience method that can be used to determine the 
    * content type of the message body. This will determine whether
    * there is a <code>Content-Type</code> header, if there is then
    * this will parse that header and represent it as a typed object
    * which will expose the various parts of the HTTP header.
    *
    * @return this returns the content type value if it exists
    */   
   public ContentType getContentType() {
      return response.getContentType();
   }
   
   /**
    * This is a convenience method that can be used to determine the 
    * content type of the message body. This will determine whether
    * there is a <code>Transfer-Encoding</code> header, if there is 
    * then this will parse that header and return the first token in
    * the comma separated list of values, which is the primary value.
    *
    * @return this returns the transfer encoding value if it exists
    */   
   public String getTransferEncoding() {
      return response.getTransferEncoding();
   }
   
   /**
    * This is a convenience method that can be used to determine
    * the length of the message body. This will determine if there
    * is a <code>Content-Length</code> header, if it does then the
    * length can be determined, if not then this returns -1.
    *
    * @return content length, or -1 if it cannot be determined
    */
   public int getContentLength() {
      return response.getContentLength();
   }   
   
   /**
    * This should be used when the size of the message body is known. For 
    * performance reasons this should be used so the length of the output
    * is known. This ensures that Persistent HTTP (PHTTP) connections 
    * can be maintained for both HTTP/1.0 and HTTP/1.1 clients. If the
    * length of the output is not known HTTP/1.0 clients will require a
    * connection close, which reduces performance (see RFC 2616).
    * <p>
    * This removes any previous Content-Length headers from the message 
    * header. This will then set the appropriate Content-Length header with
    * the correct length. If a the Connection header is set with the close
    * token then the semantics of the connection are such that the server
    * will close it once the <code>OutputStream.close</code> is used.
    *
    * @param length this is the length of the HTTP message body
    */ 
   public void setContentLength(int length) {
      response.setContentLength(length);
   }
   
   /**
    * Used to write a message body with the <code>Response</code>. The 
    * semantics of this <code>OutputStream</code> will be determined 
    * by the HTTP version of the client, and whether or not the content
    * length has been set, through the <code>setContentLength</code>
    * method. If the length of the output is not known then the output
    * is chunked for HTTP/1.1 clients and closed for HTTP/1.0 clients.
    * The <code>OutputStream</code> issued must be thread safe so that 
    * it can be used in a concurrent environment.
    *
    * @exception IOException this is thrown if there was an I/O error
    *
    * @return an output stream used to write the response body
    */ 
   public OutputStream getOutputStream() throws IOException {
      return response.getOutputStream();
   }

   /**
    * Used to write a message body with the <code>Response</code>. The 
    * semantics of this <code>OutputStream</code> will be determined 
    * by the HTTP version of the client, and whether or not the content
    * length has been set, through the <code>setContentLength</code>
    * method. If the length of the output is not known then the output
    * is chunked for HTTP/1.1 clients and closed for HTTP/1.0 clients.
    * The <code>OutputStream</code> issued must be thread safe so that 
    * it can be used in a concurrent environment.   
    * <p>
    * This will ensure that there is buffering done so that the output
    * can be reset using the <code>reset</code> method. This will 
    * enable the specified number of bytes to be written without
    * committing the response. This specified size is the minimum size
    * that the response buffer must be. 
    *
    * @param size the minimum size that the response buffer must be
    *
    * @return an output stream used to write the response body
    *
    * @exception IOException this is thrown if there was an I/O error
    */ 
   public OutputStream getOutputStream(int size) throws IOException {
      return response.getOutputStream(size);
   }

   /**
    * This method is provided for convenience so that the HTTP content
    * can be written using the <code>print</code> methods provided by
    * the <code>PrintStream</code>. This will basically wrap the 
    * <code>getOutputStream</code> with a buffer size of zero.
    * <p>
    * The retrieved <code>PrintStream</code> uses the charset used to
    * describe the content, with the Content-Type header. This will
    * check the charset parameter of the contents MIME type. So if 
    * the Content-Type was <code>text/plain; charset=UTF-8</code> the
    * resulting <code>PrintStream</code> would encode the written data
    * using the UTF-8 encoding scheme. Care must be taken to ensure
    * that bytes written to the stream are correctly encoded.
    * <p> 
    * Implementations of the <code>Response</code> must guarantee
    * that this can be invoked repeatedly without effecting any issued 
    * <code>OutputStream</code> or <code>PrintStream</code> object.
    *
    * @return a print stream used for writing the response body
    *
    * @exception IOException this is thrown if there was an I/O error
    */
   public PrintStream getPrintStream() throws IOException {
      return response.getPrintStream();
   }

   /**
    * This method is provided for convenience so that the HTTP content
    * can be written using the <code>print</code> methods provided by
    * the <code>PrintStream</code>. This will basically wrap the 
    * <code>getOutputStream</code> with a specified buffer size.
    * <p>
    * The retrieved <code>PrintStream</code> uses the charset used to
    * describe the content, with the Content-Type header. This will
    * check the charset parameter of the contents MIME type. So if 
    * the Content-Type was <code>text/plain; charset=UTF-8</code> the
    * resulting <code>PrintStream</code> would encode the written data
    * using the UTF-8 encoding scheme. Care must be taken to ensure
    * that bytes written to the stream are correctly encoded.
    * <p> 
    * Implementations of the <code>Response</code> must guarantee
    * that this can be invoked repeatedly without effecting any issued 
    * <code>OutputStream</code> or <code>PrintStream</code> object.
    *
    * @param size the minimum size that the response buffer must be
    *
    * @return a print stream used for writing the response body
    *
    * @exception IOException this is thrown if there was an I/O error
    */
   public PrintStream getPrintStream(int size) throws IOException {
      return response.getPrintStream(size);
   }
   
   /**
    * Used to write a message body with the <code>Response</code>. The 
    * semantics of this <code>WritableByteChannel</code> are determined 
    * by the HTTP version of the client, and whether or not the content
    * length has been set, through the <code>setContentLength</code>
    * method. If the length of the output is not known then the output
    * is chunked for HTTP/1.1 clients and closed for HTTP/1.0 clients.
    * 
    * @return a writable byte channel used to write the message body
    */ 
   public WritableByteChannel getByteChannel() throws IOException {
      return response.getByteChannel();
   }

   /**
    * Used to write a message body with the <code>Response</code>. The 
    * semantics of this <code>WritableByteChannel</code> are determined 
    * by the HTTP version of the client, and whether or not the content
    * length has been set, through the <code>setContentLength</code>
    * method. If the length of the output is not known then the output
    * is chunked for HTTP/1.1 clients and closed for HTTP/1.0 clients.
    * <p>
    * This will ensure that there is buffering done so that the output
    * can be reset using the <code>reset</code> method. This will 
    * enable the specified number of bytes to be written without
    * committing the response. This specified size is the minimum size
    * that the response buffer must be. 
    * 
    * @param size the minimum size that the response buffer must be
    * 
    * @return a writable byte channel used to write the message body
    */ 
   public WritableByteChannel getByteChannel(int size) throws IOException {
      return response.getByteChannel(size);
   }

   /**
    * This can be used to determine whether the <code>Response</code>
    * has been committed. This is true if the <code>Response</code> 
    * was committed, either due to an explicit invocation of the
    * <code>commit</code> method or due to the writing of content. If
    * the <code>Response</code> has committed the <code>reset</code> 
    * method will not work in resetting content already written.
    *
    * @return true if the response has been fully committed
    */ 
   public boolean isCommitted() {
      return response.isCommitted();
   }
   
   /**
    * This is used to write the headers that where given to the
    * <code>Response</code>. Any further attempts to give headers 
    * to the <code>Response</code> will be futile as only the headers
    * that were given at the time of the first commit will be used 
    * in the message header.
    * <p>
    * This also performs some final checks on the headers submitted.
    * This is done to determine the optimal performance of the 
    * output. If no specific Connection header has been specified
    * this will set the connection so that HTTP/1.0 closes by default.
    *
    * @exception IOException thrown if there was a problem writing
    */
   public void commit() throws IOException {
      response.commit();
   }

   /**
    * This can be used to determine whether the <code>Response</code>
    * has been committed. This is true if the <code>Response</code> 
    * was committed, either due to an explicit invocation of the
    * <code>commit</code> method or due to the writing of content. If
    * the <code>Response</code> has committed the <code>reset</code> 
    * method will not work in resetting content already written.
    *
    * @throws IOException thrown if there is a problem resetting 
    */ 
   public void reset() throws IOException {
      response.reset();
   }
   
   /**
    * This is used to close the connection and commit the request. 
    * This provides the same semantics as closing the output stream
    * and ensures that the HTTP response is committed. This will
    * throw an exception if the response can not be committed.
    * 
    * @throws IOException thrown if there is a problem writing
    */
   public void close() throws IOException {
      response.close();
   }
}
