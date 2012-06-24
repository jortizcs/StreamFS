/*
 * BuilderConsumer.java February 2007
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

import org.simpleframework.transport.Cursor;
import org.simpleframework.util.buffer.Allocator;

/**
 * The <code>BuilderConsumer</code> object is used to consume data
 * from a cursor and build a request entity. Each constituent part of
 * the entity is consumed from the pipeline, and passed to a builder
 * which will use the parts to create an entry. Once all parts have
 * been consumed and given to the builder then this is finished.
 * 
 * @author Niall Gallagher
 */
class BuilderConsumer implements Consumer {
   
   /**
    * This is used to create a body consumer for the entity.
    */
   protected ConsumerFactory factory;
   
   /**
    * This is used to consume the header for the request entity. 
    */
   protected RequestConsumer header;   
   
   /**
    * This is used to consume the body for the request entity.
    */
   protected BodyConsumer body;
   
   /**
    * This is used to determine if there a continue is expected.
    */
   protected Expectation expect;
   
   /**
    * This is used to build the entity from the part and header.
    */
   protected Builder builder;
   
   /**
    * Constructor for the <code>BuilderConsumer</code> object. This
    * is used to build an entity from the constituent parts. Once
    * all of the parts have been consumed they are passed to the
    * builder and the consumer is considered finished.
    * 
    * @param allocator this is used to allocate the memory used
    * @param builder this is used to build the request entity
    * @param channel this is the channel used to send a response
    */
   public BuilderConsumer(Allocator allocator, Builder builder, Channel channel) {
      this.header = new RequestConsumer();
      this.expect = new Expectation(channel);
      this.factory = new ConsumerFactory(allocator, header);
      this.builder = builder;
   }
   
   /**
    * This consumes the header and body from the cursor. The header
    * is consumed first followed by the body if there is any. There
    * is a body of there is a Content-Length or a Transfer-Encoding
    * header present. If there is no body then a substitute body 
    * is given which has an empty input stream.
    * 
    * @param cursor used to consumed the bytes for the entity
    */
   public void consume(Cursor cursor) throws IOException {
      while(cursor.isReady()) {
         if(header.isFinished()) {
            if(body == null) {
               body = factory.getInstance();    
            }
            body.consume(cursor);            
            
            if(body.isFinished()) {
               break;
            }
         } else {
            header.consume(cursor);
         }
      }
      if(header.isFinished()) {
         if(body == null) {
            expect.execute(header);
            body = factory.getInstance();
         } 
         builder.setBody(body);
         builder.setHeader(header);
      }
   }
   
   /**
    * This is determined finished when the body has been consumed.
    * If only the header has been consumed then the body will be
    * created using the header information, the body is then read
    * from the cursor, which may read nothing for an empty body.
    * 
    * @return this returns true if the entity has been built
    */
   public boolean isFinished() {
      if(header.isFinished()) {
         if(body == null) {
            body = factory.getInstance();
         }
         return body.isFinished();
      } 
      return false;
   
   }
}
