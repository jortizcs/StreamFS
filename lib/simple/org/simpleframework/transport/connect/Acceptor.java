/*
 * Acceptor.java October 2002
 *
 * Copyright (C) 2002, Niall Gallagher <niallg@users.sf.net>
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

package org.simpleframework.transport.connect;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.simpleframework.transport.Server;
import org.simpleframework.transport.Socket;
import org.simpleframework.transport.reactor.Operation;

/**
 * The <code>Acceptor</code> object is used to accept incoming TCP
 * connections from a specified socket address. This is used by the
 * <code>Connection</code> object as a background process to accept
 * the connections, transform the connected sockets to pipelines, 
 * and hand the pipelines off to a <code>Server</code>. 
 * <p>
 * This is capable of processing SSL connections created by the
 * internal server socket. All SSL connections are forced to finish
 * the SSL handshake before being dispatched to the server. This
 * ensures that there are no problems with reading the request.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.transport.connect.Connection
 */
class Acceptor implements Operation {

   /**
    * This is the server socket channel used to accept connections.
    */
   private final ServerSocketChannel server;

   /**
    * This is the server socket to bind the socket address to.
    */
   private final ServerSocket socket;

   /**
    * If provided the SSL context is used to create SSL engines.
    */
   private final SSLContext context;

   /** 
    * The handler that manages the incoming HTTP connections.
    */
   private final Server handler;

   /**
    * Constructor for the <code>Acceptor</code> object. This accepts
    * new TCP connections from the specified server socket. Each of
    * the connections that is accepted is configured for performance
    * for HTTP applications, also SSL connection can also be handled.
    *
    * @param address this is the address to accept connections from
    * @param context this is the SSL context used for secure HTTPS 
    * @param handler this is used to initiate the HTTP processing
    */
   public Acceptor(SocketAddress address, SSLContext context, Server handler) throws IOException {
      this.server = ServerSocketChannel.open();
      this.socket = server.socket();
      this.handler = handler;
      this.context = context;
      this.bind(address);
   }

   /**
    * This is used to acquire the local socket address that this is
    * listening to. This required in case the socket address that
    * is specified is an emphemeral address, that is an address that
    * is assigned dynamically when a port of 0 is specified.
    * 
    * @return this returns the address for the listening address
    */
   public SocketAddress getAddress() {
      return socket.getLocalSocketAddress();
   }
   
   /**
    * This is the <code>SelectableChannel</code> which is used to 
    * determine if the operation should be executed. If the channel   
    * is ready for a given I/O event it can be run. For instance if
    * the operation is used to perform some form of read operation
    * it can be executed when ready to read data from the channel.
    *
    * @return this returns the channel used to govern execution
    */
   public SelectableChannel getChannel() {
      return server;
   }

   /**
    * This is used to accept a new TCP connections. When the socket
    * is ready to accept a connection this method is invoked. It will
    * then create a HTTP pipeline object using the accepted socket
    * and if provided with an <code>SSLContext</code> it will also
    * provide an <code>SSLEngine</code> which is handed to the
    * processor to handle the HTTP requests. 
    */
   public void run() {
      try {
         accept();
      } catch(Exception e) {
         pause();
      }
   }
   
   /**
    * This is used to throttle the acceptor when there is an error
    * such as exhaustion of file descriptors. This will prevent the
    * CPU from being hogged by the acceptor on such occasions. If
    * the thread can not be put to sleep then this will freeze.
    */
   private void pause() {
      try {
         Thread.sleep(10);
      } catch(Exception e) {
         return;
      }
   }

   /**
    * This is used to cancel the operation if the reactor decides to
    * reject it for some reason. Typically this method will never be
    * invoked as this operation never times out. However, should the
    * reactor cancel the operation this will close the socket.     
    */
   public void cancel() {
      try {
         close();
      } catch(Throwable e) {
         return;
      }
   }

   /**
    * This is used to configure the server socket for non-blocking
    * mode. It will also bind the server socket to the socket port
    * specified in the <code>SocketAddress</code> object. Once done
    * the acceptor is ready to accept newly arriving connections.
    * 
    * @param address this is the server socket address to bind to
    */
   private void bind(SocketAddress address) throws IOException {
      server.configureBlocking(false);
      socket.setReuseAddress(true);
      socket.bind(address, 100);
   }

   /**
    * The main processing done by this object is done using a thread
    * calling the <code>run</code> method. Here the TCP connections 
    * are accepted from the <code>ServerSocketChannel</code> which 
    * creates the socket objects. Each socket is then encapsulated in
    * to a pipeline and dispatched to the processor for processing. 
    * 
    * @throws IOException if there is a problem accepting the socket
    */
   private void accept() throws IOException {
      SocketChannel channel = server.accept();

      while(channel != null) {
         configure(channel);
         
         if(context == null) {
            process(channel, null);
         } else {
            process(channel);
         }         
         channel = server.accept();
      }
   }
   
   /**
    * This method is used to configure the accepted channel. This 
    * will disable Nagles algorithm to improve the performance of the
    * channel, also this will ensure the accepted channel disables
    * blocking to ensure that it works within the processor object.
    * 
    * @param channel this is the channel that is to be configured
    */
   private void configure(SocketChannel channel) throws IOException {
      channel.socket().setTcpNoDelay(true);   
      channel.configureBlocking(false);
   }

   /**
    * This method is used to dispatch the socket for processing. The 
    * socket will be configured and connected to the client, this 
    * will hand processing to the <code>Server</code> which will
    * create the pipeline instance used to wrap the socket object.
    *
    * @param channel this is the connected socket to be processed
    */
   private void process(SocketChannel channel) throws IOException {
      SSLEngine engine = context.createSSLEngine();

      try {
         process(channel, engine);
      } catch(Exception e) {
         channel.close();
      }
   }
   
   /**
    * This method is used to dispatch the socket for processing. The 
    * socket will be configured and connected to the client, this 
    * will hand processing to the <code>Server</code> which will
    * create the pipeline instance used to wrap the socket object.
    *
    * @param channel this is the connected socket to be processed
    * @param engine this is the SSL engine used for secure HTTPS
    */
   private void process(SocketChannel channel, SSLEngine engine) throws IOException {
      Socket socket = new Subscription(channel, engine);
      
      try {
         handler.process(socket);
      } catch(Exception e) {
         channel.close();
      }
   }

   /**
    * This is used to close the server socket channel so that the
    * port that it is bound to is released. This allows the acceptor
    * to close off the interface to the server. Ensuring the socket
    * is closed allows it to be recreated at a later point.
    * 
    * @throws IOException thrown if the socket can not be closed
    */
   public void close() throws IOException {
      server.close();
   }
}
