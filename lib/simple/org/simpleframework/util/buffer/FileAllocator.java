/*
 * FileAllocator.java February 2008
 *
 * Copyright (C) 2008, Niall Gallagher <niallg@users.sf.net>
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

import java.io.File;
import java.io.IOException;

/**
 * The <code>FileAllocator</code> object is used to create buffers
 * that can be written to the file system. This creates buffers as
 * files if they are larger than the specified limit. This ensures
 * that buffers of arbitrary large size can be created. All buffer
 * sizes under the limit are created using byte arrays allocated
 * on the executing VM heap. This ensures that optimal performance
 * is maintained for buffers of reasonable size.
 * 
 * @author Niall Gallagher
 */
public class FileAllocator implements Allocator {
     
   /**
    * This is the default prefix used when none has been specified.
    */
   private static final String PREFIX = "temp";
   
   /**
    * This is the file manager used to create the buffer files.
    */
   private FileManager manager;
   
   /**
    * This is the limit up to which buffers are allocated in memory.
    */
   private int limit;
   
   /**
    * Constructor for the <code>FileAllocator</code> object. This is
    * used to create buffers in memory up to a threshold size. If a
    * buffer is required over the threshold size then the data is
    * written to a file, where it can be retrieved at a later point.
    */
   public FileAllocator() throws IOException {
      this(1048576);
   }

   /**
    * Constructor for the <code>FileAllocator</code> object. This is
    * used to create buffers in memory up to a threshold size. If a
    * buffer is required over the threshold size then the data is
    * written to a file, where it can be retrieved at a later point.
    * 
    * @param limit this is the maximum size for a heap buffer
    */
   public FileAllocator(int limit) throws IOException {
      this(PREFIX, limit);
   }
   
   /**
    * Constructor for the <code>FileAllocator</code> object. This is
    * used to create buffers in memory up to a threshold size. If a
    * buffer is required over the threshold size then the data is
    * written to a file, where it can be retrieved at a later point.
    * 
    * @param prefix this is the file prefix for the file buffers
    */
   public FileAllocator(String prefix) throws IOException {
      this(prefix, 1048576);
   }

   /**
    * Constructor for the <code>FileAllocator</code> object. This is
    * used to create buffers in memory up to a threshold size. If a
    * buffer is required over the threshold size then the data is
    * written to a file, where it can be retrieved at a later point.
    * 
    * @param prefix this is the file prefix for the file buffers
    * @param limit this is the maximum size for a heap buffer
    */
   public FileAllocator(String prefix, int limit) throws IOException {
      this.manager = new FileManager(prefix);
      this.limit = limit;
   }
   
   /**
    * This will allocate a file buffer which will write data for the
    * buffer to a file. Buffers allocated by this method can be of 
    * arbitrary size as data is appended directly to a temporary
    * file. This ensures there is no upper limit for appended data.
    * 
    * @return a buffer which will write to a temporary file
    */
   public Buffer allocate() throws IOException {
      File file = manager.create();
      
      if(!file.exists()) {
         throw new BufferException("Could not create file %s", file);
      }
      return new FileBuffer(file);
   }

   /**
    * This will allocate a file buffer which will write data for the
    * buffer to a file. Buffers allocated by this method can be of 
    * arbitrary size as data is appended directly to a temporary
    * file. This ensures there is no upper limit for appended data.
    * If the size required is less than the limit then the buffer
    * is an in memory array which provides optimal performance.
    * 
    * @param size this is the size of the buffer to be created
    * 
    * @return a buffer which will write to a created temporary file
    */
   public Buffer allocate(int size) throws IOException {
      if(size <= limit) {
         return new ArrayBuffer(size); 
      }
      return allocate();
   }
   
   /**
    * This method is used to close the allocator so that resources
    * that are occupied by the allocator can be freed. This will
    * allow the allocator to be created and closed repeatedly in
    * a single process without holding on to resources such as
    * mapped file buffers or threads.
    */
   public void close() throws IOException {
      manager.close();
   }
}
