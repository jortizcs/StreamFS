/*
 * ResponseEntity.java February 2001
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

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.channels.WritableByteChannel;
import java.util.Map;

import org.simpleframework.http.ContentType;
import org.simpleframework.http.Cookie;
import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

/**
 * This is used to represent the HTTP response. This provides methods 
 * that can be used to set various characteristics of the response.
 * The <code>OutputStream</code> of the <code>Response</code> can be 
 * retrieved from this interface as can the I.P address of the client 
 * that will be receiving the <code>Response</code>. The attributes 
 * of the connection can be retrieved also. This provides a set of 
 * methods that can be used to set the attributes of the stream so 
 * the <code>Response</code> can be transported properly. The headers
 * can be set and will be sent once a commit is made, or when there
 * is content sent over the output stream.
 * <p>
 * This should never allow the message body be sent if it should not 
 * be sent with the headers as of RFC 2616 rules for the presence of 
 * a message body. A message body must not be included with a HEAD 
 * request or with a 304 or a 204 response. A proper implementation
 * of this will prevent a message body being sent if the response
 * is to a HEAD request of if there is a 304 or 204 response code.
 * <p>
 * It is important to note that the <code>Response</code> controls
 * the processing of the HTTP pipeline. The next HTTP request is 
 * not processed until the response has committed. The response is
 * committed once the <code>commit</code> method is invoked if there
 * is NO content body. Committing with a content body is done only if
 * correct content is given. The <code>OutputStream</code> acts as 
 * a client and commits the response once the specified content has 
 * been written to the issued <code>OutputStream</code>.
 *
 * @author Niall Gallagher
 */ 
class ResponseEntity extends ResponseMessage implements Response {
   
   /**
    * This is the conversation used to determine connection type.
    */
   private Conversation support;
  
   /**
    * This is used to buffer the bytes that are sent to the client.
    */
   private Accumulator buffer;
   
   /**
    * This is the underlying channel for the connected pipeline.
    */
   private Channel channel;  
   
   /**
    * This is the sender object used to deliver to response data.
    */
   private Sender sender;
   
   /**
    * This is the header which contains the session cookie used.
    */
   private Header header;
   
   /**
    * This is used to determine if the response has been committed.
    */
   private boolean committed;
   
   /**
    * Constructor for the <code>ResponseEntity</code> object. This is
    * used to create a response instance using the provided request,
    * entity, and monitor object. To ensure that the response is
    * compatible with client the <code>Request</code> is used. Also
    * to ensure the next request can be processed the provided monitor
    * is used to signal response events to the server kernel.
    * 
    * @param request this is the request that was sent by the client
    * @param entity this is the entity that contains the channel
    * @param monitor this is the monitor used to signal events
    */
   public ResponseEntity(Request request, Entity entity, Monitor monitor) {
      this.support = new Conversation(request, this);
      this.buffer = new Accumulator(support, entity, monitor);
      this.channel = entity.getChannel();
      this.sender = channel.getSender();
      this.header = entity.getHeader();
   }
   
   /**
    * This is used as a shortcut for acquiring attributes for the
    * response. This avoids acquiring the <code>Attributes</code>
    * in order to retrieve the attribute directly from that object.
    * The attributes contain data specific to the response.
    * 
    * @param name this is the name of the attribute to acquire
    * 
    * @return this returns the attribute for the specified name
    */ 
   public Object getAttribute(Object name) {
      return getAttributes().get(name);
   }

   /**
    * This can be used to retrieve certain attributes about
    * this <code>Response</code>. The attributes contains certain
    * properties about the <code>Response</code>. For example if
    * this Response goes over a secure line then there may be any
    * arbitrary attributes.
    *
    * @return the response attributes of that have been set
    */    
   public Map getAttributes() {     
      return channel.getAttributes();
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
      set("Content-Length", length);      
   }
   
   /**
    * This determines the charset for <code>PrintStream</code> objects
    * returned from the <code>getPrintStream</code> method. This will
    * return a valid charset regardless of whether the Content-Type
    * header has been set, set without a charset, or not set at all.
    * If unspecified, the charset returned is <code>ISO-8859-1</code>,
    * as suggested by RFC 2616, section 3.7.1.
    *
    * @return returns the charset used by this response object
    */
   private String getCharset() {
      ContentType type = getContentType();
      
      if(type == null) {         
         return "iso-8859-1";
      }
      if(type.getCharset()==null){
         return "iso-8859-1";
      }
      return type.getCharset();
   }
   
   /**
    * Used to write a message body with the <code>Response</code>. The 
    * semantics of this <code>OutputStream</code> will be determined 
    * by the HTTP version of the client, and whether or not the content
    * length has been set, through the <code>setContentLength</code>
    * method. If the length of the output is not known then the output
    * is chunked for HTTP/1.1 clients and closed for HTTP/1.0 clients.
    *
    * @return an output stream object used to write the message body
    */ 
   public OutputStream getOutputStream() throws IOException {
      return buffer;
   }
   
   /**
    * Used to write a message body with the <code>Response</code>. The 
    * semantics of this <code>OutputStream</code> will be determined 
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
    * @return an output stream object used to write the message body
    */ 
   public OutputStream getOutputStream(int size) throws IOException {
      if(size > 0) {
         buffer.expand(size);
      }
      return buffer;
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
    *
    * @return a print stream object used to write the message body
    */
   public PrintStream getPrintStream() throws IOException {
      return getPrintStream(0, getCharset());
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
    *
    * @param size the minimum size that the response buffer must be
    *
    * @return a print stream object used to write the message body
    */
   public PrintStream getPrintStream(int size) throws IOException {
      return getPrintStream(size, getCharset());
   }
   
   /**  
    * This is used to wrap the <code>getOutputStream</code> object in
    * a <code>PrintStream</code>, which will write content using a 
    * specified charset. The <code>PrintStream</code> created will not
    * buffer the content, it will write directly to the underlying
    * <code>OutputStream</code> where it is buffered (if there is a
    * buffer size greater than zero specified). In future the buffer
    * of the <code>PrintStream</code> may be usable.
    *
    * @param size the minimum size that the response buffer must be
    * @param charset this is the charset used by the resulting stream
    *
    * @return a print stream that encodes in the given charset    
    */
   private PrintStream getPrintStream(int size, String charset) throws IOException {
      if(size > 0) {
         buffer.expand(size);
      }
      return new PrintStream(buffer, false, charset);
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
      return buffer;
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
      if(size > 0) {
         buffer.expand(size);
      }
      return buffer;      
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
      return committed;
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
      if(!committed) {       
         Cookie cookie = header.getSession(false);
         
         if(cookie != null) {
            setSession(cookie);
         }
         byte[] message = getMessage();
         
         sender.send(message);
         committed = true;
      }
   }
   
   /**
    * This is used to set the session cookie for the response. This
    * checks to see if the session cookie has not been overwritten. 
    * If the cookie was overwritten then the provided session cookie
    * will not be used and the existing one will be used by this.
    * 
    * @param session this is the session cookie that is to be set
    */
   private void setSession(Cookie session) {
      String name = session.getName();
      Cookie cookie = getCookie(name);
      
      if(cookie == null) {
         cookie = session;
      }
      if(cookie.isNew()) {
         setCookie(cookie);
      }
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
      buffer.reset();
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
      buffer.close();
   }
}
