/*
 * SocketTransport.java February 2007
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

package org.simpleframework.transport;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Map;

import javax.net.ssl.SSLEngine;

import org.simpleframework.transport.reactor.Reactor;

/**
 * The <code>SocketTransport</code> object offers a transport that can
 * send and receive bytes in a non-blocking manner. The contract of
 * the <code>Transport</code> is that it must either write the data
 * it is asked to write or it must queue that data for delivery. For
 * the vast majority of cases data is written directly to the socket
 * without any need for queuing or selection for write ready events.
 * <p>
 * In the event that the client TCP window is full and writing would
 * block this makes use of a queue of buffers which can be used to 
 * append data to. The buffers are lazily instantiated so the memory
 * required is created only in the rare case that they are needed.
 * Once a buffer is full it is queued to an asynchronous thread where
 * the buffer queue is drained and sent to the client when the TCP
 * window of the client is capable of accepting it.
 * <p>
 * In order to improve the network performance of this transport the
 * default packet size sent to the TCP stack is four kilobytes. This
 * ensures that the fragments of response delivered to the TCP layer
 * are sufficiently large for optimum network performance.
 *
 * @author Niall Gallagher
 */
class SocketTransport implements Transport {
   
   /**
    * This creates packets with increasing sequence numbers.
    */
   private PacketBuilder builder;

   /**
    * This is the underlying byte channel used to send the  data.
    */         
   private SocketChannel channel;    
   
   /**
    * This is the writer that is used to flush the buffer queue.
    */
   private Controller writer;
   
   /**
    * This is the socket that this transport is representing.
    */
   private Socket socket;
  
   /**
    * This is used to determine if the transport has been closed.
    */
   private boolean closed;
   
   /**
    * Constructor for the <code>SocketTransport</code> object. This 
    * requires a reactor to perform asynchronous writes and also the
    * pipeline which is used to read and write data. This transport
    * will use a queue of buffers which are lazily initialized so as
    * to only allocate the memory on demand.
    *
    * @param socket this is used to read and write the data
    * @param reactor this is used to perform asynchronous writes
    */
   public SocketTransport(Socket socket, Reactor reactor) throws IOException {
      this(socket, reactor, 20480);
   }

   /**
    * Constructor for the <code>SocketTransport</code> object. This 
    * requires a reactor to perform asynchronous writes and also the
    * pipeline which is used to read and write data. This transport
    * will use a queue of buffers which are lazily initialized so as
    * to only allocate the memory on demand.
    *
    * @param socket this is used to read and write the data
    * @param reactor this is used to perform asynchronous writes
    * @param limit this is the threshold for asynchronous buffers
    */
   public SocketTransport(Socket socket, Reactor reactor, int limit) throws IOException {
      this(socket, reactor, limit, 3);
   }

   /**
    * Constructor for the <code>SocketTransport</code> object. This 
    * requires a reactor to perform asynchronous writes and also the
    * pipeline which is used to read and write data. This transport
    * will use a queue of buffers which are lazily initialized so as
    * to only allocate the memory on demand.
    *
    * @param socket this is used to read and write the data
    * @param reactor this is used to perform asynchronous writes
    * @param limit this is the threshold for asynchronous buffers  
    * @param queue this is the queue size for asynchronous writes
    */
   public SocketTransport(Socket socket, Reactor reactor, int limit, int queue) throws IOException {
     this.writer = new SocketController(socket, reactor, limit);
     this.builder = new PacketBuilder(queue);
     this.channel = socket.getChannel();
     this.socket = socket;
   }   
   
   /**
    * This method is used to get the <code>Map</code> of attributes 
    * by this pipeline. The attributes map is used to maintain details
    * about the connection. Information such as security credentials
    * to client details can be placed within the attribute map.
    *
    * @return this returns the map of attributes for this pipeline
    */   
   public Map getAttributes() {     
      return socket.getAttributes();
   }

   /**
    * This is used to acquire the SSL engine used for https. If the
    * pipeline is connected to an SSL transport this returns an SSL
    * engine which can be used to establish the secure connection
    * and send and receive content over that connection. If this is
    * null then the pipeline represents a normal transport. 
    *  
    * @return the SSL engine used to establish a secure transport
    */   
   public SSLEngine getEngine() {     
      return socket.getEngine();
   }

   /**
    * This method is used to acquire the <code>SocketChannel</code>
    * for the connection. This allows the server to acquire the input
    * and output streams with which to communicate. It can also be 
    * used to configure the connection and perform various network 
    * operations that could otherwise not be performed.
    *
    * @return this returns the socket used by this HTTP pipeline
    */      
   public SocketChannel getChannel() {      
      return socket.getChannel();
   }   
   
   /**
    * This is used to perform a non-blocking read on the transport.
    * If there are no bytes available on the input buffers then
    * this method will return zero and the buffer will remain the
    * same. If there is data and the buffer can be filled then this
    * will return the number of bytes read. Finally if the socket
    * is closed this will return a -1 value.
    *
    * @param data this is the buffer to append the bytes to
    *
    * @return this returns the number of bytes that were read 
    */ 
   public int read(ByteBuffer data) throws IOException {
      if(closed) {
         throw new TransportException("Transport is closed");         
      }      
      return channel.read(data);
   }
   
   /**
    * This method is used to deliver the provided buffer of bytes to
    * the underlying transport. Depending on the connection type the
    * array may be encoded for SSL transport or send directly. This
    * will buffer the bytes within the internal buffer to ensure 
    * that the response fragments are sufficiently large for the
    * network. Smaller packets result poorer performance.
    *
    * @param data this is the array of bytes to send to the client
    */   
   public  void write(ByteBuffer data) throws IOException{  
      if(closed) {
         throw new TransportException("Transport is closed");
      }     
      Packet packet = builder.build(data);
      
      while(packet != null) {
         if(!closed) {
            writer.write(packet);
         }
         packet = builder.build(data);
      }
   }    
   
   /**
    * This is used to flush the internal buffer to the underlying
    * socket. Flushing with this method is always non-blocking, so
    * if the socket is not write ready and the buffer can be queued
    * it will be queued and the calling thread will return.
    */
   public void flush() throws IOException {      
      if(closed) {
         throw new TransportException("Transport is closed");
      }
      Packet packet = builder.build();

      if(packet != null) {
         writer.write(packet);
      }
   }
   
   /**
    * This method is used to flush the internal buffer and close
    * the underlying socket. This method will not complete until
    * all buffered data is written and the underlying socket is
    * closed at which point this can be disposed of.
    */
   public void close() throws IOException {
      if(!closed) {              
         Packet packet = builder.build();
         
         if(packet != null) {
            writer.write(packet);
         }
         writer.close();
         closed = true;
      }
   }
}
