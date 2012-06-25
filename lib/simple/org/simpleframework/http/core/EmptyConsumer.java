/*
 * EmptyConsumer.java February 2007
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

import org.simpleframework.transport.Cursor;

/**
 * The <code>EmptyConsumer</code> object is used to represent a body
 * of zero length. This is the most common body consumer created as 
 * it represents the body for GET messages that have nothing within
 * the body part.
 *
 * @author Niall Gallagher
 */ 
class EmptyConsumer extends BodyConsumer {

   /**
    * This method will not consume any bytes from the cursor. This 
    * ensures that the next byte read from the stream is the first
    * character of the next HTTP message within the pipeline.
    *
    * @param cursor this is the cursor which will not be read from
    */         
   public void consume(Cursor cursor) {
      return;
   }
 
   /**
    * This will return true immediately. Because the empty consumer
    * represents a zero length body and no bytes are read from the
    * cursor, this should not be processed and return finished.  
    *
    * @return this will always return true for the zero length body
    */
   public boolean isFinished() {
      return true;
   }
}


