/*
 * Transfer.java February 2007
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
import java.nio.ByteBuffer;

import org.simpleframework.http.Response;

/**
 * The <code>Transfer</code> object acts as a means to determine the
 * transfer encoding for the response body. This will ensure that the
 * correct HTTP headers are used when the transfer of the body begins.
 * In order to determine what headers to use this can be provided 
 * with a content length value. If the <code>start</code> method is
 * provided with the content length then the HTTP headers will use a
 * Content-Length header as the message delimiter. If there is no
 * content length provided then the chunked encoding is used for 
 * HTTP/1.1 and connection close is used for HTTP/1.0.
 *
 * @author Niall Gallagher
 *
 * @see org.simpleframework.http.core.Producer
 */ 
class Transfer {

   /**
    * This is used to create a producer based on the HTTP headers.
    */         
   private ProducerFactory factory;

   /**
    * This is used to determine the type of transfer required.
    */ 
   private Conversation support;
   
   /**
    * This is the response message that is to be committed.
    */ 
   private Response response;
   
   /**
    * Once the header is committed this is used to produce data.
    */ 
   private Producer producer;
   
   /**
    * Constructor for the <code>Transfer</code> object, this is used
    * to create an object used to transfer a response body. This must
    * be given a <code>Conversation</code> that can be used to set
    * and get information regarding the type of transfer required. 
    * 
    * @param support this is used to determine the semantics
    * @param sender this is used to send data over the transport
    * @param monitor this is used to signal I/O events to the kernel
    */ 
   public Transfer(Conversation support, Sender sender, Monitor monitor) {
      this.factory = new ProducerFactory(support, sender, monitor);
      this.response = support.getResponse();
      this.support = support;
   }
   
   /**
    * This is used to determine if the transfer has started. It has
    * started when a producer is created and the HTTP headers have
    * been sent, or at least handed to the underlying transport.
    * Once started the semantics of the connection can not change.
    *
    * @return this returns whether the transfer has started
    */ 
   public boolean isStarted() {
      return producer != null;
   }
   
   /**
    * This starts the transfer with no specific content length set.
    * This is typically used when dynamic data is emitted ans will
    * require chunked encoding for HTTP/1.1 and connection close
    * for HTTP/1.0. Once invoked the HTTP headers are committed.
    */ 
   public void start() throws IOException {
      if(producer != null) {
         throw new TransferException("Transfer has already started");
      }
      clear();
      configure();
      commit();        
   }
   
   /**
    * This starts the transfer with a known content length. This is
    * used when there is a Content-Length header set. This will not
    * encode the content for HTTP/1.1 however, HTTP/1.0 may need
    * a connection close if it does not have keep alive semantics.
    *
    * @param length this is the length of the response body
    */ 
   public void start(int length) throws IOException {
      if(producer != null) {
         throw new TransferException("Transfer has already started");
      }
      clear();
      configure(length);
      commit();      
   }
   
   /**
    * This method is used to write content to the underlying socket.
    * This will make use of the <code>Producer</code> object to 
    * encode the response body as required. If the producer has not
    * been created then this will throw an exception.
    *
    * @param array this is the array of bytes to send to the client    
    */ 
   public void write(byte[] array) throws IOException {
      write(array, 0, array.length);
   }
   
   /**
    * This method is used to write content to the underlying socket.
    * This will make use of the <code>Producer</code> object to 
    * encode the response body as required. If the producer has not
    * been created then this will throw an exception.
    *
    * @param array this is the array of bytes to send to the client
    * @param off this is the offset within the array to send from
    * @param len this is the number of bytes that are to be sent    
    */ 
   public void write(byte[] array, int off, int len) throws IOException {
      if(producer == null) {
         throw new TransferException("Conversation details not ready");
      }
      producer.produce(array, off, len);
   }
   
   /**
    * This method is used to write content to the underlying socket.
    * This will make use of the <code>Producer</code> object to 
    * encode the response body as required. If the producer has not
    * been created then this will throw an exception.
    *
    * @param buffer this is the buffer of bytes to send to the client    
    */ 
   public void write(ByteBuffer buffer) throws IOException {
      int mark = buffer.position();
      int size = buffer.limit();
      
      if(mark > size) {
         throw new TransferException("Buffer position greater than limit");
      }
      write(buffer, 0, size - mark);
   }
   
   /**
    * This method is used to write content to the underlying socket.
    * This will make use of the <code>Producer</code> object to 
    * encode the response body as required. If the producer has not
    * been created then this will throw an exception.
    *
    * @param buffer this is the buffer of bytes to send to the client
    * @param off this is the offset within the buffer to send from
    * @param len this is the number of bytes that are to be sent    
    */ 
   public void write(ByteBuffer buffer, int off, int len) throws IOException {
      if(producer == null) {
         throw new TransferException("Conversation details not ready");
      }
      producer.produce(buffer, off, len);
   }
   
   /**
    * This method is used to flush the contents of the buffer to 
    * the client. This method will block until such time as all of
    * the data has been sent to the client. If at any point there
    * is an error sending the content an exception is thrown.    
    */   
   public void flush() throws IOException {
      if(producer == null) {
         throw new TransferException("Conversation details not ready");
      }
      producer.flush();
   }
  
   /**
    * This is used to signal to the producer that all content has 
    * been written and the user no longer needs to write. This will
    * either close the underlying transport or it will notify the
    * monitor that the response has completed and the next request
    * can begin. This ensures the content is flushed to the client.
    */      
   public void close() throws IOException {
      if(producer == null) {
         throw new TransferException("Conversation details not ready");
      }
      producer.close();
   }
   
   /**
    * This method is used to set the required HTTP headers on the
    * response. This will check the existing HTTP headers, and if
    * there is insufficient data chunked encoding will be used for
    * HTTP/1.1 and connection close will be used for HTTP/1.0.
    */ 
   private void configure() throws IOException {
      int length = support.getContentLength(); 
      boolean empty = support.isEmpty();
      
      if(empty) {
         support.setContentLength(0);
      } else if(length >= 0) {
         support.setContentLength(length);         
      } else {
         support.setChunkedEncoded();      
      }
      producer = factory.getInstance();
   }
   
   /**
    * This method is used to set the required HTTP headers on the
    * response. This will check the existing HTTP headers, and if
    * there is insufficient data chunked encoding will be used for
    * HTTP/1.1 and connection close will be used for HTTP/1.0.
    *
    * @param count this is the number of bytes to be transferred
    */ 
   private void configure(int count) throws IOException {
      int length = support.getContentLength();
      
      if(support.isHead()) { 
         if(count > 0) {
            configure(count, count);
         } else {
            configure(count, length);
         }
      } else {         
         configure(count, count);
      }
   }
   
   /**
    * This method is used to set the required HTTP headers on the
    * response. This will check the existing HTTP headers, and if
    * there is insufficient data chunked encoding will be used for
    * HTTP/1.1 and connection close will be used for HTTP/1.0.
    *
    * @param count this is the number of bytes to be transferred
    * @param length this is the actual length value to be used
    */ 
   private void configure(int count, int length) throws IOException {    
      boolean empty = support.isEmpty();
      
      if(empty) {
         support.setContentLength(0);
      } else if(length >= 0) {
         support.setContentLength(length);
      } else {
         support.setChunkedEncoded();         
      }      
      producer = factory.getInstance();
   }
 
   /**
    * This is used to clear any previous encoding that has been set
    * in the event that content length may be used instead. This is
    * used so that an override can be made to the transfer encoding
    * such that content length can be used instead.   
    */    
   private void clear() throws IOException {
      support.setIdentityEncoded();
      
   }
   
   /**
    * This is used to compose the HTTP header and send it over the
    * transport to the client. Once done the response is committed
    * and no more headers can be set, also the semantics of the
    * response have been committed and the producer is created.
    */ 
   private void commit() throws IOException {
      try {
         response.commit();
      } catch(Exception cause) {
         throw new TransferException("Unable to commit", cause);
      }
   }

}


