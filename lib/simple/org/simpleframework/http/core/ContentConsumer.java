/*
 * ContentConsumer.java February 2007
 *
 * Copyright (C) 2007, Niall Gallagher <niallg@users.sf.net>
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
import java.io.InputStream;

import org.simpleframework.http.ContentType;
import org.simpleframework.http.Part;
import org.simpleframework.transport.Cursor;
import org.simpleframework.util.buffer.Allocator;
import org.simpleframework.util.buffer.Buffer;

/**
 * The <code>ContentConsumer</code> object represents a consumer for
 * a multipart body part. This will read the contents of the cursor
 * until such time as it reads the terminal boundary token, which is
 * used to frame the content. Once the boundary token has been read
 * this will add itself as a part to a part list. This part list can
 * then be used with the HTTP request to examine and use the part. 
 * 
 * @author Niall Gallagher
 *
 * @see org.simpleframework.http.core.PartConsumer
 */
class ContentConsumer extends UpdateConsumer implements Part {
    
   /**
    * This represents the start of the boundary token for the body.
    */      
   private static final byte[] START = { '\r', '\n', '-', '-' };   
   
   /**
    * This is used to allocate the internal buffer when required.
    */ 
   private Allocator allocator;
   
   /**
    * This is the internal buffer used to house the part body.
    */ 
   private Buffer buffer;
   
   /**
    * Represents the HTTP headers that were provided for the part.
    */ 
   private Segment segment;   
   
   /**
    * This is the part list that this part is to be added to.
    */ 
   private PartList list;
   
   /**
    * Represents the message boundary that terminates the part body.
    */ 
   private byte[] boundary;
   
   /**
    * This is used to determine if the start token had been read.
    */
   private int start;
   
   /**
    * This is used to determine how many boundary tokens are read.
    */ 
   private int seek;
  
   /**
    * Constructor for the <code>ContentConsumer</code> object. This 
    * is used to create a consumer that reads the body of a part in
    * a multipart request body. The terminal token must be provided
    * so that the end of the part body can be determined.
    *
    * @param allocator this is used to allocate the internal buffer
    * @param segment this represents the headers for the part body
    * @param list this is the part list that this body belongs in
    * @param boundary this is the message boundary for the body part
    */  
   public ContentConsumer(Allocator allocator, Segment segment, PartList list, byte[] boundary) {
      this.allocator = allocator;
      this.boundary = boundary; 
      this.segment = segment;      
      this.list = list;
   }
   
   /**
    * This method is used to determine the type of a part. Typically
    * a part is either a text parameter or a file. If this is true
    * then the content represented by the associated part is a file.
    *
    * @return this returns true if the associated part is a file
    */   
   public boolean isFile() {
      return segment.isFile();
   }
   
   /**
    * This method is used to acquire the name of the part. Typically
    * this is used when the part represents a text parameter rather
    * than a file. However, this can also be used with a file part.
    * 
    * @return this returns the name of the associated part
    */
   public String getName() {
      return segment.getName();
   }
   
   /**
    * This method is used to acquire the file name of the part. This
    * is used when the part represents a text parameter rather than 
    * a file. However, this can also be used with a file part.
    *
    * @return this returns the file name of the associated part
    */   
   public String getFileName() {
      return segment.getFileName();
   }
   
   /**
    * This is used to acquire the header value for the specified 
    * header name. Providing the header values through this method
    * ensures any special processing for a know content type can be
    * handled by an application.
    * 
    * @param name the name of the header to get the value for
    * 
    * @return value of the header mapped to the specified name
    */   
   public String getHeader(String name) {
      return segment.getValue(name);
   }
   
   /**
    * This is used to acquire the content type for this part. This
    * is typically the type of content for a file part, as provided
    * by a MIME type from the HTTP "Content-Type" header.
    * 
    * @return this returns the content type for the part object
    */   
   public ContentType getContentType() {
      return segment.getContentType();
   }
   
   /**
    * This is used to acquire the content of the part as a string.
    * The encoding of the string is taken from the content type. 
    * If no content type is sent the content is decoded in the
    * standard default of ISO-8859-1.
    * 
    * @return this returns a string representing the content
    */   
   @Override
   public String getContent() throws IOException {
      return getContent("ISO-8859-1");
   }
   
   /**
    * This is used to acquire the content of the part as a string.
    * The encoding of the string is taken from the content type. 
    * If no content type is sent the content is decoded in the
    * standard default of ISO-8859-1.
    * 
    * @param charset this is the charset encoding to be used
    * 
    * @return this returns a string representing the content
    */   
   @Override
   public String getContent(String charset) throws IOException {
      if(buffer == null) {
         return new String();
      }
      return buffer.encode(charset);
   }
   
   /**
    * This is used to acquire an <code>InputStream</code> for the
    * part. Acquiring the stream allows the content of the part to
    * be consumed by reading the stream. Each invocation of this
    * method will produce a new stream starting from the first byte.
    * 
    * @return this returns the stream for this part object
    */   
   @Override
   public InputStream getInputStream() throws IOException {
      if(buffer == null) {
         return new EmptyInputStream();
      }
      return buffer.getInputStream();
   }
   
   /**
    * This is used to push the start and boundary back on to the
    * cursor. Pushing the boundary back on to the cursor is required
    * to ensure that the next consumer will have valid data to 
    * read from it. Simply resetting the boundary is not enough as
    * this can cause an infinite loop if the connection is bad.
    * 
    * @param cursor this is the cursor used by this consumer
    */
   @Override
   protected void commit(Cursor cursor) throws IOException {
      cursor.push(boundary);
      cursor.push(START);
   }
   
   /**
    * This is used to process the bytes that have been read from the
    * cursor. This will search for the boundary token within the body
    * of the message part, when it is found this will returns the 
    * number of bytes that represent the overflow.
    *
    * @param array this is a chunk read from the cursor
    * @param off this is the offset within the array the chunk starts
    * @param size this is the number of bytes within the array
    *
    * @return this returns the number of bytes overflow that is read
    */        
   @Override
   protected int update(byte[] array, int off, int size) throws IOException {
      int skip = start + seek; // did we skip previously
      int last = off + size;
      int next = start;
      int mark = off;      
      
      while(off < last) {
         if(start == START.length) { // search for boundary      
            if(array[off++] != boundary[seek++]) { // boundary not found
               if(skip > 0) {
                  append(START, 0, next); // write skipped start
                  append(boundary, 0, skip - next); // write skipped boundary
               }
               skip = start = seek = 0; // reset scan position
            }         
            if(seek == boundary.length) { // boundary found
               int excess = seek + start; // boundary bytes read
               int total = off - mark; // total bytes read
               int valid = total - excess; // body bytes read
               
               finished = true;
               list.add(this);      
               
               if(valid > 0) {
                  append(array, mark, valid);
               }
               return size - total; // remaining excluding boundary
            }
         } else {
            byte octet = array[off++]; // current
            
            if(octet != START[start++]) {               
               if(skip > 0) {
                  append(START, 0, next); // write skipped start
               }   
               skip = start = 0; // reset
               
               if(octet == START[0]) { // is previous byte the start
                  start++;
               }               
            }
         }
      }      
      int excess = seek + start; // boundary bytes read
      int total = off - mark; // total bytes read
      int valid = total - excess; // body bytes read
      
      if(valid > 0) { // can we append processed data
         append(array, mark, valid);
      }
      return 0;
   } 
   
   /** 
    * This method is used to allocate the internal buffer. If there
    * has already been a call to this method the previous instance
    * is returned. If there is any issue allocating the buffer then
    * this will throw an exception.
    *
    * @return this returns the buffer to append the bytes to
    */     
   @Override
   protected Buffer allocate() throws IOException {
      if(buffer == null) {
         buffer = allocator.allocate();
      }
      return buffer;
   }
   
   /**
    * This is used to provide a string representation of the header
    * read. Providing a string representation of the header is used
    * so that on debugging the contents of the delivered header can
    * be inspected in order to determine a cause of error.
    *
    * @return this returns a string representation of the header
    */ 
   @Override
   public String toString() {
      return segment.toString();
   }
}
