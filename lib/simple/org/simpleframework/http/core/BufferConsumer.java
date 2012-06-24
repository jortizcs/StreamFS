/*
 * BufferConsumer.java February 2007
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

import org.simpleframework.util.buffer.ArrayBuffer;
import org.simpleframework.util.buffer.Buffer;

/**
 * The <code>BufferConsumer</code> is used to create a consumer that
 * is used to consume data and append that data to a buffer. This is
 * done so that the buffer can act as a data source for the content
 * that has been consumed. For example, take the message body, this
 * can be consumed in such a way that the internal buffer can be 
 * used acquire an <code>InputStream</code> to read the contents. 
 * Buffering in such a way provides an efficient means to store the
 * contents of the message as only one read only copy is created.
 *
 * @author Niall Gallagher
 */ 
abstract class BufferConsumer implements Consumer {
   
   /**
    * Constructor for the <code>BufferConsumer</code> object. This
    * will create a consumer that provides methods that can be
    * used to buffer content consumed from the provided cursor.
    * Such consumers can accumulate the transport data.
    */
   protected BufferConsumer() {
      super();
   }

   /** 
    * This method is used to append the contents of the array to the
    * internal buffer. The appended bytes can be acquired from the
    * internal buffer using an <code>InputStream</code>, or the text
    * of the appended bytes can be acquired by encoding the bytes.
    *
    * @param array this is the array of bytes to be appended
    */ 
   protected void append(byte[] array) throws IOException {
      append(array, 0, array.length);
   }

   /** 
    * This method is used to append the contents of the array to the
    * internal buffer. The appended bytes can be acquired from the
    * internal buffer using an <code>InputStream</code>, or the text
    * of the appended bytes can be acquired by encoding the bytes.
    *
    * @param array this is the array of bytes to be appended
    * @param off this is the start offset in the array to read from
    * @param len this is the number of bytes to write to the buffer
    */
   protected void append(byte[] array, int off, int len) throws IOException {
      Buffer buffer = allocate();

      if(buffer != null) {
         buffer.append(array, off, len);
      }
   }

   /** 
    * This method is used to allocate the internal buffer. If there
    * has already been a call to this method the previous instance
    * is returned. If there is any issue allocating the buffer then
    * this will throw an exception.
    *
    * @return this returns the buffer to append the bytes to
    */ 
   protected Buffer allocate() throws IOException {
      return new ArrayBuffer();
   }
}


