/*
 * Buffer.java February 2001
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

package org.simpleframework.util.buffer;

import java.io.IOException;

/**
 * The <code>Buffer</code> interface represents a collection of bytes
 * that can be written to and later read. This is used to provide a
 * region of memory is such a way that the underlying representation 
 * of that memory is independent of its use. Typically buffers are 
 * implemented as either allocated byte arrays or files.
 *
 * @author Niall Gallagher
 *
 * @see org.simpleframework.util.buffer.Allocator
 */ 
public interface Buffer extends Stream {

   /**
    * This method is used to allocate a segment of this buffer as a
    * separate buffer object. This allows the buffer to be sliced in
    * to several smaller independent buffers, while still allowing the
    * parent buffer to manage a single buffer. This is useful if the
    * parent is split in to logically smaller segments.
    *
    * @return this returns a buffer which is a segment of this buffer
    */
   public Buffer allocate() throws IOException;   

   /**
    * This method is used to acquire the buffered bytes as a string.
    * This is useful if the contents need to be manipulated as a
    * string or transferred into another encoding. If the UTF-8
    * content encoding is not supported the platform default is 
    * used, however this is unlikely as UTF-8 should be supported.
    *
    * @return this returns a UTF-8 encoding of the buffer contents
    */    
   public String encode() throws IOException;        

   /**
    * This method is used to acquire the buffered bytes as a string.
    * This is useful if the contents need to be manipulated as a
    * string or transferred into another encoding. This will convert
    * the bytes using the specified character encoding format.
    * 
    * @param charset this is the charset to encode the data with
    *
    * @return this returns the encoding of the buffer contents
    */   
   public String encode(String charset) throws IOException;

   /**
    * This method is used to append bytes to the end of the buffer. 
    * This will expand the capacity of the buffer if there is not
    * enough space to accommodate the extra bytes.
    *
    * @param array this is the byte array to append to this buffer
    *
    * @return this returns this buffer for another operation
    */     
   public Buffer append(byte[] array) throws IOException;

   /**
    * This method is used to append bytes to the end of the buffer. 
    * This will expand the capacity of the buffer if there is not
    * enough space to accommodate the extra bytes.
    *
    * @param array this is the byte array to append to this buffer
    * @param len the number of bytes to be read from the array
    * @param off this is the offset to begin reading the bytes from
    *
    * @return this returns this buffer for another operation    
    */    
   public Buffer append(byte[] array, int off, int len) throws IOException;  

   /**
    * This will clear all data from the buffer. This simply sets the
    * count to be zero, it will not clear the memory occupied by the
    * instance as the internal buffer will remain. This allows the
    * memory occupied to be reused as many times as is required.
    */    
   public void clear() throws IOException;   

   /**
    * This method is used to ensure the buffer can be closed. Once
    * the buffer is closed it is an immutable collection of bytes and
    * can not longer be modified. This ensures that it can be passed
    * by value without the risk of modification of the bytes.
    */   
   public void close() throws IOException;
}
