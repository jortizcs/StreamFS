/*
 * FixedConsumer.java February 2007
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
import java.io.InputStream;

import org.simpleframework.util.buffer.Allocator;
import org.simpleframework.util.buffer.Buffer;

/**
 * The <code>FixedConsumer</code> object reads a fixed number of bytes
 * from the cursor. This is typically used when the Content-Length
 * header is used as the body delimiter. In order to determine when
 * the full body has been consumed this counts the bytes read. Once
 * all the bytes have been read any overflow will be reset. All of the
 * bytes read are appended to the internal buffer so they can be read.
 *
 * @author Niall Gallagher
 */ 
class FixedConsumer extends UpdateConsumer {
   
   /**
    * This is the allocator used to allocate the buffer used.
    */         
   private Allocator allocator;
   
   /**
    * This is the internal buffer used to accumulate the body.
    */ 
   private Buffer buffer;
   
   /**
    * This is the number of bytes to be consumed from the cursor.
    */ 
   private int limit;
   
   /**
    * Constructor for the <code>FixedConsumer</code> object. This is
    * used to create a consumer that reads a fixed number of bytes
    * from the cursor and accumulates those bytes in an internal 
    * buffer so that it can be read at a later stage.
    *
    * @param allocator this is used to allocate the internal buffer
    * @param limit this is the number of bytes that are to be read
    */ 
   public FixedConsumer(Allocator allocator, int limit) {
      this.allocator = allocator;
      this.limit = limit;
   }
   
   /**
    * This will acquire the contents of the body in UTF-8. If there
    * is no content encoding and the user of the request wants to
    * deal with the body as a string then this method can be used.
    * It will simply create a UTF-8 string using the body bytes.
    *
    * @return returns a UTF-8 string representation of the body
    */       
   @Override
   public String getContent() throws IOException {
      if(buffer == null) {
         return new String();
      }
      return buffer.encode();
   }

   /**
    * This will acquire the contents of the body in the specified
    * charset. Typically this will be given the charset as taken 
    * from the HTTP Content-Type header. Although any encoding can
    * be specified to convert the body to a string representation.
    *
    * @param charset this is the charset encoding to be used
    *
    * @return returns an encoded string representation of the body
    */    
   @Override
   public String getContent(String charset) throws IOException {
      if(buffer == null) {
         return new String();
      }
      return buffer.encode(charset);
   } 
  
   /**
    * This is used to acquire the contents of the body as a stream.
    * Each time this method is invoked a new stream is created that
    * will read the contents of the body from the first byte. This
    * ensures that the stream can be acquired several times without
    * any issues arising from previous reads.
    *
    * @return this returns a new string used to read the body
    */        
   @Override
   public InputStream getInputStream() throws IOException {
      if(buffer == null) {
         return new EmptyInputStream();
      }
      return buffer.getInputStream();
   }

   /**
    * This is used to process the bytes that have been read from the
    * cursor. This will count the number of bytes read, once all of 
    * the bytes that form the body have been read this returns the 
    * number of bytes that represent the overflow.
    *
    * @param array this is a chunk read from the cursor
    * @param off this is the offset within the array the chunk starts
    * @param count this is the number of bytes within the array
    *
    * @return this returns the number of bytes overflow that is read
    */       
   @Override
   protected int update(byte[] array, int off, int count) throws IOException {
      int mark = limit;
      
      if(count >= limit) {
         append(array, off, mark);
         finished = true;
         limit = 0;
         return count - mark;
      } 
      if(count > 0) {
         append(array, off, count);
         limit -= count;         
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
         buffer = allocator.allocate(limit);
      }
      return buffer;
   }
}


