/*
 * FixedProducer.java February 2007
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

/**
 * The <code>FixedProducer</code> object produces content without any
 * encoding, but limited to a fixed number of bytes. This is used if
 * the length of the content being delivered is know beforehand. It 
 * will simply count the number of bytes being send and signal the
 * server kernel that the next request is ready to read once all of
 * the bytes have been sent to the client.
 *
 * @author Niall Gallagher
 *
 * @see org.simpleframework.http.core.FixedConsumer
 */ 
class FixedProducer implements Producer{

   /**
    * This is the monitor used to notify the initiator of events.
    */ 
   private Monitor monitor;

   /**
    * This is the underlying sender used to deliver the raw data.
    */ 
   private Sender sender;

   /**
    * This is the number of bytes that have been sent so far.
    */ 
   private int count;
   
   /**
    * This is the number of bytes this producer is limited to.
    */ 
   private int limit;
   
   /**
    * Constructor for the <code>FixedProducer</code> object. This is
    * used to create a producer that will count the number of bytes
    * that are sent over the pipeline, once all bytes have been sent
    * this will signal that the next request is ready to read.
    *
    * @param sender this is used to send to the underlying transport
    * @param monitor this is used to deliver signals to the kernel
    * @param limit this is used to limit the number of bytes sent
    */ 
   public FixedProducer(Sender sender, Monitor monitor, int limit) {
      this.monitor = monitor;   
      this.sender = sender;
      this.limit = limit;
   }
  
   /**
    * This method is used to encode the provided array of bytes in
    * a HTTP/1.1 complaint format and sent it to the client. Once
    * the data has been encoded it is handed to the transport layer
    * within the server, which may choose to buffer the data if the
    * content is too small to send efficiently or if the socket is
    * not write ready.
    *
    * @param array this is the array of bytes to send to the client
    */        
   public void produce(byte[] array) throws IOException {
      produce(array, 0, array.length);
   }
   
   /**
    * This method is used to encode the provided array of bytes in
    * a HTTP/1.1 complaint format and sent it to the client. Once
    * the data has been encoded it is handed to the transport layer
    * within the server, which may choose to buffer the data if the
    * content is too small to send efficiently or if the socket is
    * not write ready.
    *
    * @param array this is the array of bytes to send to the client
    * @param off this is the offset within the array to send from
    * @param len this is the number of bytes that are to be sent
    */       
   public void produce(byte[] array, int off, int len) throws IOException {
      ByteBuffer buffer = ByteBuffer.wrap(array, off, len);
      
      if(len > 0) {
         produce(buffer);
      }  
   }
   
   /**
    * This method is used to encode the provided buffer of bytes in
    * a HTTP/1.1 compliant format and sent it to the client. Once
    * the data has been encoded it is handed to the transport layer
    * within the server, which may choose to buffer the data if the
    * content is too small to send efficiently or if the socket is
    * not write ready.
    *
    * @param buffer this is the buffer of bytes to send to the client
    */         
   public void produce(ByteBuffer buffer) throws IOException {
      int mark = buffer.position();
      int size = buffer.limit();
      
      if(mark > size) {
         throw new ProducerException("Buffer position greater than limit");
      }
      produce(buffer, 0, size - mark);
   }

   /**
    * This method is used to encode the provided buffer of bytes in
    * a HTTP/1.1 compliant format and sent it to the client. Once
    * the data has been encoded it is handed to the transport layer
    * within the server, which may choose to buffer the data if the
    * content is too small to send efficiently or if the socket is
    * not write ready.
    *
    * @param buffer this is the buffer of bytes to send to the client
    * @param off this is the offset within the buffer to send from
    * @param len this is the number of bytes that are to be sent
    */          
   public void produce(ByteBuffer buffer, int off, int len) throws IOException {
      int size = Math.min(len, limit - count);          
      
      try {
         if(monitor.isClosed()) {
            throw new ProducerException("Response content complete");
         }
         sender.send(buffer, off, size);
         
         if(count + size == limit) {
            monitor.ready(sender);
         }        
      } catch(Exception cause) {
         if(sender != null) {
            monitor.error(sender);
         }
         throw new ProducerException("Error sending response", cause);
      }
      count += size;
   }
   
   /**
    * This method is used to flush the contents of the buffer to 
    * the client. This method will block until such time as all of
    * the data has been sent to the client. If at any point there
    * is an error sending the content an exception is thrown.    
    */    
   public void flush() throws IOException {
      try {
         if(!monitor.isClosed()) {
            sender.flush();
         }
      } catch(Exception cause) {
         if(sender != null) {
            monitor.error(sender);
         }
         throw new ProducerException("Error flushing", cause);
      }
   }
   
   /**
    * This is used to signal to the producer that all content has 
    * been written and the user no longer needs to write. This will
    * either close the underlying transport or it will notify the
    * monitor that the response has completed and the next request
    * can begin. This ensures the content is flushed to the client.
    */   
   public void close() throws IOException {
      if(!monitor.isClosed()) {
         if(count < limit) {
            monitor.error(sender);
         } else {
            monitor.ready(sender);
         }
      }
   }
}
