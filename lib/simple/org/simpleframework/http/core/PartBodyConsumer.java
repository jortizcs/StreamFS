/*
 * PartBodyConsumer.java February 2007
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

import org.simpleframework.transport.Cursor;
import org.simpleframework.util.buffer.Allocator;

/**
 * The <code>PartBodyConsumer</code> object is used to consume a part
 * the contents of a multipart body. This will consume the part and
 * add it to a part list, once the part has been consumed and added
 * to the part list a terminal token is consumed, which is a carriage
 * return and line feed.
 *
 * @author Niall Gallagher
 */ 
class PartBodyConsumer extends BodyConsumer {

   /**
    * This is the token that is consumed after the content body. 
    */
   private static final byte[] LINE = { '\r', '\n' };
   
   /**
    * This is used to consume the content from the multipart upload.
    */  
   private ContentConsumer content;
   
   /**
    * This is used to consume the final terminal token from the part.
    */  
   private Consumer token;
   
   /**
    * Constructor for the <code>PartBodyConsumer</code> object. This 
    * is used to create a consumer that reads the body of a part in
    * a multipart request body. The terminal token must be provided
    * so that the end of the part body can be determined.
    *
    * @param allocator this is used to allocate the internal buffer
    * @param segment this represents the headers for the part body
    * @param boundary this is the message boundary for the body part
    */   
   public PartBodyConsumer(Allocator allocator, Segment segment, byte[] boundary) {
      this(allocator, segment, new PartList(), boundary);
   }
   
   /**
    * Constructor for the <code>PartBodyConsumer</code> object. This 
    * is used to create a consumer that reads the body of a part in
    * a multipart request body. The terminal token must be provided
    * so that the end of the part body can be determined.
    *
    * @param allocator this is used to allocate the internal buffer
    * @param segment this represents the headers for the part body
    * @param list this is the part list that this body belongs in
    * @param boundary this is the message boundary for the body part
    */    
   public PartBodyConsumer(Allocator allocator, Segment segment, PartList list, byte[] boundary) {
      this.content = new ContentConsumer(allocator, segment, list, boundary);
      this.token = new TokenConsumer(allocator, LINE);
   }
  
   /** 
    * This is used to consume the part body from the cursor. This
    * initially reads the body of the part, which represents the
    * actual payload exposed via the <code>Part</code> interface
    * once the payload has been consumed the terminal is consumed.
    *
    * @param cursor this is the cursor to consume the body from
    */ 
   public void consume(Cursor cursor) throws IOException {
      while(cursor.isReady()) {
         if(content.isFinished()) {
            if(token.isFinished()) {
               break;
            }
            token.consume(cursor);
         } else {
            content.consume(cursor);
         }
      }
   }
  
   /**
    * This is used to determine whether the part body has been read
    * from the cursor successfully. In order to determine if all of
    * the bytes have been read successfully this will check to see
    * of the terminal token had been consumed.
    *
    * @return true if the part body and terminal have been read 
    */ 
   public boolean isFinished() {
      return token.isFinished();
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
      return content.getContent();
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
      return content.getInputStream();
   }
}


