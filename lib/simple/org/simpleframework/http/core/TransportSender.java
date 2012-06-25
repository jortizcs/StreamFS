/*
 * TransportSender.java February 2007
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

import org.simpleframework.transport.Transport;

/**
 * The <code>TransportSender</code> object is used to send bytes to
 * and underlying transport. This is essentially an adapter between
 * an <code>OutputStream</code> and the underlying transport. Each
 * byte array segment written to the underlying transport is wrapped
 * in a bytes buffer so that it can be sent by the transport layer.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.transport.Transport
 */
class TransportSender implements Sender {

   /**
    * This is the underlying transport to write the bytes to. 
    */   
   private Transport transport;

   /**
    * This is used to determine if the transport has been closed.
    */ 
   private volatile boolean closed;
   
   /**
    * Constructor for the <code>TransportSender</code> object. This
    * is used to create an adapter for the transport such that a
    * byte array can be used to write bytes to the array.
    * 
    * @param transport the underlying transport to send bytes to
    */
   public TransportSender(Transport transport) {
      this.transport = transport;
   }

   /**
    * This method is used to deliver the provided array of bytes to
    * the underlying transport. Depending on the connection type the
    * array may be encoded for SSL transport or send directly. Any
    * implementation may choose to buffer the bytes for performance.
    *
    * @param array this is the array of bytes to send to the client
    */    
   public void send(byte[] array) throws IOException {
      send(array, 0, array.length);      
   }

   /**
    * This method is used to deliver the provided array of bytes to
    * the underlying transport. Depending on the connection type the
    * array may be encoded for SSL transport or send directly. Any
    * implementation may choose to buffer the bytes for performance.
    *
    * @param array this is the array of bytes to send to the client
    * @param off this is the offset within the array to send from
    * @param len this is the number of bytes that are to be sent
    */    
   public void send(byte[] array, int off, int len) throws IOException {
      ByteBuffer buffer = ByteBuffer.wrap(array, off, len);
      
      if(len > 0) {
         send(buffer);
      }      
   }
   
   /**
    * This method is used to deliver the provided buffer of bytes to
    * the underlying transport. Depending on the connection type the
    * array may be encoded for SSL transport or send directly. Any
    * implementation may choose to buffer the bytes for performance.
    *
    * @param buffer this is the buffer of bytes to send to the client
    */             
   public void send(ByteBuffer buffer) throws IOException {
      int mark = buffer.position();
      int size = buffer.limit();
      
      if(mark > size) {
         throw new IOException("Buffer position greater than limit");
      }
      send(buffer, 0, size - mark);
   }
   
   /**
    * This method is used to deliver the provided buffer of bytes to
    * the underlying transport. Depending on the connection type the
    * array may be encoded for SSL transport or send directly. Any
    * implementation may choose to buffer the bytes for performance.
    *
    * @param buffer this is the buffer of bytes to send to the client
    * @param off this is the offset within the buffer to send from
    * @param len this is the number of bytes that are to be sent
    */    
   public void send(ByteBuffer buffer, int off, int len) throws IOException {
      int mark = buffer.position();
      int limit = buffer.limit();
      
      if(limit - mark > len) {
         buffer.limit(mark + len); // reduce usable size
      }
      transport.write(buffer);
      buffer.limit(limit);
   }

   /**
    * This method is used to flush the contents of the buffer to 
    * the client. This method will block until such time as all of
    * the data has been sent to the client. If at any point there
    * is an error sending the content an exception is thrown.    
    */     
   public void flush() throws IOException {
      transport.flush();      
   }
   
   /**
    * This is used to close the sender and the underlying transport.
    * If a close is performed on the sender then no more bytes can
    * be read from or written to the transport and the client will
    * received a connection close on their side.
    */     
   public void close() throws IOException {
      if(!closed) {           
        transport.close();
        closed = true;
      }
   }
}



