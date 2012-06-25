/*
 * PartListConsumer.java February 2007
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

import org.simpleframework.http.Part;
import org.simpleframework.transport.Cursor;
import org.simpleframework.util.buffer.Allocator;
import org.simpleframework.util.buffer.BufferAllocator;

/**
 * The <code>PartListConsumer</code> object is used to consume a list
 * of parts encoded in the multipart format. This is can consume any
 * number of parts from a cursor. Each part consumed is added to an
 * internal part list which can be used to acquire the contents of the
 * upload and inspect the headers provided for each uploaded part. To
 * ensure that only a fixed number of bytes are consumed this uses a
 * content length for an internal buffer.
 * 
 * @author Niall Gallagher
 */
class PartListConsumer extends BodyConsumer {

   /**
    * This is used to consume individual parts from the part list.
    */ 
   private PartEntryConsumer consumer;

   /**
    * This is the factory that is used to create the consumers used.
    */
   private PartEntryFactory factory;
   
   /**
    * This is used to both allocate and buffer the part list body.
    */ 
   private BufferAllocator buffer;

   /**
    * This is used to accumulate all the parts of the upload.
    */ 
   private PartList list;
   
   /**
    * Constructor for the <code>PartListConsumer</code> object. This 
    * will create a consumer that is capable of breaking an upload in
    * to individual parts so that they can be accessed and used by
    * the receiver of the HTTP request message.
    *
    * @param allocator this is used to allocate the internal buffer
    * @param boundary this is the boundary used for the upload
    */ 
   public PartListConsumer(Allocator allocator, byte[] boundary) {
      this(allocator, boundary, 8192);
   }

   /**
    * Constructor for the <code>PartListConsumer</code> object. This 
    * will create a consumer that is capable of breaking an upload in
    * to individual parts so that they can be accessed and used by
    * the receiver of the HTTP request message.
    *
    * @param allocator this is used to allocate the internal buffer
    * @param boundary this is the boundary used for the upload
    * @param length this is the number of bytes the upload should be
    */    
   public PartListConsumer(Allocator allocator, byte[] boundary, int length) {
      this(allocator, new PartList(), boundary, length);
   }   

   /**
    * Constructor for the <code>PartListConsumer</code> object. This 
    * will create a consumer that is capable of breaking an upload in
    * to individual parts so that they can be accessed and used by
    * the receiver of the HTTP request message.
    *
    * @param allocator this is used to allocate the internal buffer
    * @param boundary this is the boundary used for the upload
    * @param list this is the part list used to accumulate the parts
    */    
   public PartListConsumer(Allocator allocator, PartList list, byte[] boundary) {
      this(allocator, list, boundary, 8192);
   }

   /**
    * Constructor for the <code>PartListConsumer</code> object. This 
    * will create a consumer that is capable of breaking an upload in
    * to individual parts so that they can be accessed and used by
    * the receiver of the HTTP request message.
    *
    * @param allocator this is used to allocate the internal buffer
    * @param boundary this is the boundary used for the upload
    * @param length this is the number of bytes the upload should be   
    * @param list this is the part list used to accumulate the parts   
    */    
   public PartListConsumer(Allocator allocator, PartList list, byte[] boundary, int length) {
      this.buffer = new BufferAllocator(allocator, length);
      this.consumer = new PartEntryConsumer(buffer, list, boundary);
      this.factory = new PartEntryFactory(buffer, list, boundary);
      this.list = list;
   }

   /** 
    * This is used to consume the part list from the cursor. This
    * initially reads the list of parts, which represents the
    * actual content exposed via the <code>PartList</code> object,
    * once the content has been consumed the terminal is consumed.
    *
    * @param cursor this is the cursor to consume the list from
    */ 
   public void consume(Cursor cursor) throws IOException {
      while(cursor.isReady()) { 
         if(!consumer.isFinished()) {
            consumer.consume(cursor);
         } else {
            if(!consumer.isEnd()) {
               consumer = factory.getInstance();
            } else {
               break;
            }
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
      return consumer.isEnd();
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
      return buffer.encode();
   }
   
   /**
    * This is used to acquire the content of the part as a string.
    * The encoding of the string is taken from the content type. 
    * If no content type is sent the content is decoded in the
    * standard default of ISO-8859-1.
    * 
    * @param charset this is the character encoding to be used
    *
    * @return this returns a string representing the content
    */       
   @Override
   public String getContent(String charset) throws IOException {
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
      return buffer.getInputStream();
   }
   
   /**
    * This method is used to acquire a <code>Part</code> from the
    * body using a known name for the part. This is typically used 
    * when there is a file upload with a multipart POST request.
    * All parts that are not files are added to the query values
    * as strings so that they can be used in a convenient way.
    * 
    * @param name this is the name of the part to acquire
    * 
    * @return the named part or null if the part does not exist
    */    
   @Override
   public Part getPart(String name) {
      return list.getPart(name);
   }
   
   /**
    * This method provides all parts for this body. The parts for a
    * body can contain text parameters or files. Each file part can
    * contain headers, which are the typical HTTP headers. Typically
    * headers describe the content and any encoding if required.
    * 
    * @return this returns a list of parts for this body
    */   
   @Override
   public PartList getParts() {
      return list;
   }   
}
