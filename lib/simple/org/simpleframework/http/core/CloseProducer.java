/*
 * CloseProducer.java February 2007
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
import java.nio.ByteBuffer;

/**
 * The <code>CloseProducer</code> is used to close a connection once
 * all of the content has been produced. This is typically used if
 * the connected client supports the HTTP/1.0 protocol and there is
 * no Connection header with the keep-alive token. For reasons of
 * performance this should not be used for HTTP/1.1 clients.
 *
 * @author Niall Gallagher
 */ 
class CloseProducer implements Producer {

   /**
    * This is the monitor used to notify the initiator of events.
    */ 
   private final Monitor monitor;

   /**
    * This is the underlying sender used to deliver the raw data.
    */ 
   private final Sender sender;
  
   /** 
    * Constructor for the <code>CloseProducer</code> object. This is
    * used to create a producer that will close the underlying socket
    * as a means to signal that the response is fully sent. This is
    * typically used with HTTP/1.0 connections.
    *
    * @param sender this is used to send to the underlying transport
    * @param monitor this is used to deliver signals to the kernel    
    */ 
   public CloseProducer(Sender sender, Monitor monitor) {
      this.monitor = monitor;   
      this.sender = sender;
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
      if(monitor.isClosed()) {
         throw new ProducerException("Stream has been closed");
      }
      try {
         sender.send(buffer, off, len);
      } catch(Exception cause) {
         if(sender != null) {
            monitor.error(sender);
         }
         throw new ProducerException("Error sending response", cause);
      }
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
         throw new ProducerException("Error sending response", cause);
      }
   }
   
   /**
    * This is used to signal to the producer that all content has 
    * been written and the user no longer needs to write. This will
    * close the underlying transport which tells the client that 
    * all of the content has been sent over the connection.
    */      
   public void close() throws IOException {
      try {
         if(!monitor.isClosed()) {
            monitor.close(sender);
            sender.close();
         }
      } catch(Exception cause) {
         if(sender != null) {
            monitor.error(sender);
         }
         throw new ProducerException("Error sending response", cause);
      }
   }
}
