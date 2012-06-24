/*
 * PartFactory.java February 2007
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

import org.simpleframework.util.buffer.Allocator;

/**
 * The <code>PartFactory</code> represents a factory for creating the
 * consumers that are used to read a multipart upload message. This
 * supports two types of consumers for the multipart upload, lists
 * and bodies. A part list is basically a collection of parts and or
 * part lists. The part type is determined from the part header.
 *
 * @author Niall Gallagher
 *
 * @see org.simpleframework.http.core.PartListConsumer
 * @see org.simpleframework.http.core.PartBodyConsumer 
 */ 
class PartFactory extends ConsumerFactory {
   
   /**
    * Constructor for the <code>PartFactory</code> object. This is
    * used to create a factory using a buffer allocator, which will
    * create a buffer for accumulating the entire message body,
    * also to ensure the correct part type is created this requires
    * the header information for the part.
    *
    * @param allocator this is used to allocate the internal buffer
    * @param header this is used to determine the part type 
    */ 
   public PartFactory(Allocator allocator, Segment header) {
      super(allocator, header);
   }
  
   /**
    * This method is used to create the consumer given the list and
    * boundary for the part. In order to determine the part type 
    * this will consult the header consumed for the part. Depending
    * on whether it is a list or body a suitable consumer is created.
    *
    * @param list this is the list used to collect the parts 
    * @param boundary this is the boundary used to terminate the part
    *
    * @return this will return the consumer for the part body
    */  
   public BodyConsumer getInstance(PartList list, byte[] boundary) throws IOException {
      byte[] terminal = getBoundary(segment);
      
      if(isPart(segment)) {
         return new PartListConsumer(allocator, list, terminal); 
      }
      return new PartBodyConsumer(allocator, segment, list, boundary); 
   }
}

