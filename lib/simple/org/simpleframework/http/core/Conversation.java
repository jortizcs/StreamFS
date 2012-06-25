/*
 * Conversation.java February 2007
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

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

/**
 * The <code>Conversation</code> object is used to set and interpret
 * the semantics of the HTTP headers with regard to the encoding 
 * used for the response. This will ensure the the correct headers 
 * are used so that if chunked encoding or a connection close is 
 * needed that the headers are set accordingly. This allows both the
 * server and client to agree on the best semantics to use.
 *
 * @author Niall Gallagher
 *
 * @see org.simpleframework.http.core.Accumulator
 * @see org.simpleframework.http.core.Transfer
 */
class Conversation {
   
   /**
    * This is the response object that requires HTTP headers set.
    */ 
   private final Response response;
   
   /**
    * This contains the request headers and protocol version.
    */ 
   private final Request request;
   
   /**
    * Constructor for the <code>Conversation</code> object. This is
    * used to create an object that makes use of both the request 
    * and response HTTP headers to determine how best to deliver
    * the response body. Depending on the protocol version and the
    * existing response headers suitable semantics are determined.
    *
    * @param request this is the request from the client
    * @param response this is the response that is to be sent
    */ 
   public Conversation(Request request, Response response) {
      this.response = response;
      this.request = request;
   }   
   
   /**
    * This provides the <code>Request</code> object. This can be 
    * used to acquire the request HTTP headers and protocl version
    * used by the client. Typically the conversation provides all
    * the data needed to determine the type of response required.
    *
    * @return this returns the request object for the conversation
    */ 
   public Request getRequest() {
      return request;
   }
   
   /**
    * This provides the <code>Response</code> object. This is used
    * when the commit is required on the response. By committing 
    * the response the HTTP header is generated and delivered to
    * the underlying transport.
    *
    * @return this returns the response for the conversation
    */ 
   public Response getResponse() {
      return response;
   }
   
   /**
    * This is used to acquire the content length for the response.
    * The content length is acquired fromt he Content-Length header
    * if it has been set. If not then this will return a -1 value.
    *
    * @return this returns the value for the content length header
    */ 
   public int getContentLength() {
      return response.getContentLength();
   }
  
   /**
    * This is used to determine if the <code>Response</code> has a
    * message body. If this does not have a message body then true    
    * is returned. This is determined as of RFC 2616 rules for the 
    * presence of a message body. A message body must not be 
    * included with a HEAD request or with a 304 or a 204 response. 
    * If when this is called there is no message length delimiter 
    * as specified by section RFC 2616 4.4, then there is no body.
    *
    * @return true if there is no response body, false otherwise
    */ 
   public boolean isEmpty() {
      int code = response.getCode();
      
      if(code == 204){ 
         return true;
      } 
      if(code == 304){ 
         return true;
      }      
      return false;      
   }
   
   /**
    * This is used to determine if the request method was HEAD. This
    * is of particular interest in a HTTP conversation as it tells
    * the response whether a response body is to be sent or not.
    * If the method is head the delimeters for the response should
    * be as they would be for a similar GET, however no body is sent.
    * 
    * @return true if the request method was a HEAD method
    */
   public boolean isHead() {
      String method = request.getMethod();
      
      if(method == null) {
         return false;
      }
      return method.equalsIgnoreCase("HEAD");
   }
   
   /**
    * This is used to set the content length for the response. If
    * the HTTP version is HTTP/1.1 then the Content-Length header is
    * used, if an earlier protocol version is used then connection
    * close semantics are also used to ensure client compatibility.
    *
    * @param length this is the length to set HTTP header to
    */ 
   public void setContentLength(int length) {
      boolean keepAlive = isKeepAlive();
      
      if(keepAlive) {   
         response.set("Connection", "keep-alive");         
      } else {
         response.set("Connection", "close");              
      }
      response.set("Content-Length", length);
   } 
   
   /**
    * This checks the protocol version used in the request to check
    * whether it supports persistent HTTP connections. By default the
    * HTTP/1.1 protocol supports persistent connnections, this can 
    * onlyy be overridden with a Connection header with the close
    * token. Earlier protocol versions are connection close.
    *
    * @return this returns true if the protocol is HTTP/1.1 or above
    */ 
   public boolean isPersistent() {
      String token = request.getValue("Connection");
      
      if(token != null) {
         return token.equalsIgnoreCase("keep-alive");
      }      
      int major = request.getMajor();
      int minor = request.getMinor();
      
      if(major >= 1) {
         return minor >= 1;
      }
      return false;     
   }
   
   /**
    * The <code>isKeepAlive</code> method is used to determine if
    * the connection semantics are set to maintain the connection. 
    * This checks to see if there is a Connection header with the
    * keep-alive token, if so then the connection is keep alive, if
    * however there is no connection header the version is used.
    *
    * @return true if the response connection is to be maintained
    */      
   public boolean isKeepAlive() {
      String token = response.getValue("Connection");
      
      if(token != null) {
         return token.equalsIgnoreCase("keep-alive");
      }
      return isPersistent();
   }
   
   /**
    * The <code>isChunkable</code> method is used to determine if
    * the client supports chunked encoding. If the client does not
    * support chunked encoding then a connection close should be used
    * instead, this allows HTTP/1.0 clients to be supported properly.
    * 
    * @return true if the client supports chunked transfer encoding
    */
   public boolean isChunkable() {
      int major = request.getMajor();
      int minor = request.getMinor();
      
      if(major >= 1) {
         return minor >= 1;
      }
      return false;  
   }
  
   /**
    * This is used when the output is encoded in the chunked encoding.
    * This should only be used if the protocol version is HTTP/1.1 or
    * above. If the protocol version supports chunked encoding then it
    * will encode the data as specified in RFC 2616 section 3.6.1.
    */     
   public void setChunkedEncoded() {
      boolean keepAlive = isKeepAlive();
      boolean chunkable = isChunkable();
      
      if(keepAlive && chunkable) {
         response.set("Transfer-Encoding", "chunked");
         response.set("Connection", "keep-alive");
      } else {    
         response.set("Connection", "close");
      }
   }  
  
   /**
    * This will remove all explicit transfer encoding headers from 
    * the response header. By default the identity encoding is used
    * for all connections, it basically means no encoding. So if the
    * response uses a Content-Length it implicitly assumes tha the
    * encoding of the response is identity encoding.
    */  
   public void setIdentityEncoded() {
      response.remove("Transfer-Encoding");
   }

   /**
    * The <code>isChunkedEncoded</code> is used to determine whether 
    * the chunked encoding scheme is desired. This is enables data to
    * be encoded in such a way that a connection can be maintained
    * without a Content-Length header. If the output is chunked then 
    * the connection is keep alive.
    *
    * @return true if the response output is chunked encoded
    */
   public boolean isChunkedEncoded() {
      String token = response.getValue("Transfer-Encoding");
      
      if(token != null) {
         return token.equalsIgnoreCase("chunked");
      }
      return false;
   }
}


