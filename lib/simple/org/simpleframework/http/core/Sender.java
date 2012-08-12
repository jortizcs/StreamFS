/*
 * Sender.java February 2007
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
 * The <code>Sender</code> object is used to send data over the TCP
 * transport. This provides direct contact with the connected socket.
 * Delivery over a sender implementation can be either SSL based or
 * direct as with plain HTTP connections. It is the responsibility 
 * of the implementation to provide such behavior as required.
 *
 * @author Niall Gallagher
 */ 
interface Sender {
  
   /**
    * This method is used to deliver the provided array of bytes to
    * the underlying transport. Depending on the connection type the
    * array may be encoded for SSL transport or send directly. Any
    * implementation may choose to buffer the bytes for performance.
    *
    * @param array this is the array of bytes to send to the client
    */             
   public void send(byte[] array) throws IOException;
   
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
   public void send(byte[] array, int off, int len) throws IOException;
   
   /**
    * This method is used to deliver the provided buffer of bytes to
    * the underlying transport. Depending on the connection type the
    * array may be encoded for SSL transport or send directly. Any
    * implementation may choose to buffer the bytes for performance.
    *
    * @param buffer this is the buffer of bytes to send to the client
    */             
   public void send(ByteBuffer buffer) throws IOException;
   
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
   public void send(ByteBuffer buffer, int off, int len) throws IOException;
   
   /**
    * This method is used to flush the contents of the buffer to 
    * the client. This method will block until such time as all of
    * the data has been sent to the client. If at any point there
    * is an error sending the content an exception is thrown.    
    */    
   public void flush() throws IOException;
   
   /**
    * This is used to close the sender and the underlying transport.
    * If a close is performed on the sender then no more bytes can
    * be read from or written to the transport and the client will
    * received a connection close on their side.
    */      
   public void close() throws IOException;

}




