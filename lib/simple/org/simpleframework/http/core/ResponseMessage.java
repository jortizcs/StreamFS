/*
 * ResponseMessage.java February 2001
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

import org.simpleframework.http.ContentType;
import org.simpleframework.http.Cookie;
import org.simpleframework.http.ResponseHeader;
import org.simpleframework.http.parse.ContentParser;

/**
 * The <code>ResponseMessage</code> object represents the header used
 * for a response. This is used to get and set the headers in a case
 * insensitive manner. It is also used to manage the cookies that are
 * send and received. Also, the status code and description can also
 * be set through this object as well as the protocol version. 
 * 
 * @author Niall Gallagher
 */
class ResponseMessage extends Message implements ResponseHeader {
   
   /**
    * This is the text description used for the response status.
    */
   private String text;
   
   /**
    * This is the major protocol version used for the response.
    */
   private int major;
   
   /**
    * This is the minor protocol version used for the response.
    */
   private int minor;
   
   /**
    * This is the status code used to identify the response type.
    */
   private int code; 
   
   /**
    * Constructor for the <code>ResponseMessage</code> object. This
    * is used to create a response message with a default status code
    * of 200 and a a protocol version of HTTP/1.1. If the response is
    * a different status code or version these can be modified. 
    */
   public ResponseMessage() {
      this.text = "OK";
      this.code = 200;
      this.major = 1;
      this.minor = 1;
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
      return code;
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
      this.code = code;
   }

   /**
    * This can be used to retrieve the text of a HTTP status
    * line. This is the text description for the status code.
    * This should match the status code specified by the RFC.
    *
    * @return the message description of the response
    */ 
   public String getText() {
      return text;
   }

   /**
    * This is used to set the text of the HTTP status line.
    * This should match the status code specified by the RFC.
    *
    * @param text the descriptive text message of the status
    */ 
   public void setText(String text) {
      this.text = text;
   }

   /**
    * This can be used to get the major number from a HTTP version.
    * The major version corresponds to the major type that is the 1
    * of a HTTP/1.0 version string. 
    *
    * @return the major version number for the request message
    */ 
   public int getMajor() {
      return major;
   }

   /**
    * This can be used to set the major number from a HTTP version.
    * The major version corresponds to the major type that is the 1
    * of a HTTP/1.0 version string. 
    *
    * @param major the major version number for the request message
    */ 
   public void setMajor(int major) {
      this.major = major;
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
      return minor;
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
      this.minor = minor;
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
      String value = getValue("Content-Type");
      
      if(value == null) {
         return null; 
      }
      return new ContentParser(value);
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
      return getInteger("Content-Length");
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
      return getValue("Transfer-Encoding");
   }
   
   /**
    * This is used to convert the representation of the response to 
    * an ISO-8850-1 encoded array of bytes. The array is composed
    * from the <code>toString</code> method an then converted to a
    * byte array. 
    * 
    * @return this returns a byte array representing the header
    */
   protected byte[] getMessage() throws IOException {
      return toString().getBytes("ISO-8859-1");
   }
   
   /**
    * This is used to compose the HTTP response header. All of the
    * headers added to the response are added, as well as the cookies
    * to form the response message header. To ensure that the text
    * produces is as required the header names are in the same case
    * as they were added to the response message.
    * 
    * @return a string representation of the response message
    */
   public String toString() {   
      StringBuilder head = new StringBuilder(256);
      
      head.append("HTTP/").append(major);      
      head.append('.').append(minor);
      head.append(' ').append(code);
      head.append(' ').append(text);
      head.append("\r\n");
      
      for(String name : getNames()) {
         for(String value : getAll(name)) {
            head.append(name);
            head.append(": ");
            head.append(value);
            head.append("\r\n");
         }
      }
      for(Cookie cookie : getCookies()) {         
         head.append("Set-Cookie: ");
         head.append(cookie);
         head.append("\r\n");
      }      
      return head.append("\r\n").toString();      
   }
}
