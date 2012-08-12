/*
 * Listener.java October 2002
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

import static java.nio.channels.SelectionKey.OP_ACCEPT;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketAddress;

import javax.net.ssl.SSLContext;

import org.simpleframework.transport.Server;
import org.simpleframework.transport.reactor.DirectReactor;
import org.simpleframework.transport.reactor.Reactor;

/**
 * The <code>Listener</code> object is represents the interface to
 * the server that the clients can connect to. This is responsible
 * for making call backs to the <code>Acceptor</code> when there
 * is a new connection waiting to be accepted. When the connection
 * is to be closed the interface object can be closed.
 * 
 * @author Niall Gallagher 
 */
class Listener implements Closeable {      
   
   /**
    * This is the acceptor that is used to accept the connections.
    */
   private final Acceptor acceptor;
   
   /**
    * This is the reactor used to notify the acceptor of sockets.
    */
   private final Reactor reactor;
   
   /**
    * Constructor for the <code>Listener</code> object. This needs
    * a socket address and a processor to hand created sockets
    * to. This creates a <code>Reactor</code> which will notify the
    * acceptor when there is a new connection waiting to be accepted.
    * 
    * @param address this is the address to listen for new sockets
    * @param processor this is where pipelines are handed to
    */
   public Listener(SocketAddress address, Server processor) throws IOException {
      this(address, null, processor);
   }
   
   /**
    * Constructor for the <code>Listener</code> object. This needs
    * a socket address and a processor to hand created sockets
    * to. This creates a <code>Reactor</code> which will notify the
    * acceptor when there is a new connection waiting to be accepted.
    * 
    * @param address this is the address to listen for new sockets
    * @param context this is the SSL context used for secure HTTPS
    * @param processor this is where pipelines are handed to
    */
   public Listener(SocketAddress address, SSLContext context, Server processor) throws IOException {
      this.acceptor = new Acceptor(address, context, processor);
      this.reactor = new DirectReactor();
      this.process();
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
      return acceptor.getAddress();
   }
   
   /**
    * This is used to register the <code>Acceptor</code> to listen
    * for new connections that are ready to be accepted. Once this
    * is registered it will remain registered until the interface
    * is closed, at which point the socket is closed.
    * 
    * @throws IOException thrown if there is a problem registering
    */
   private void process() throws IOException {
      try {
         reactor.process(acceptor, OP_ACCEPT);
      } catch(Exception e) {
         throw new ConnectionException("Listen error", e);
      }
   }
   
   /**
    * This is used to close the connection and the server socket
    * used to accept connections. This will perform a close of the
    * connected server socket that have been created from using
    * the <code>Acceptor</code> object. 
    * 
    * @throws IOException thrown if there is a problem closing
    */   
   public void close() throws IOException {
      try {
         acceptor.close();
         reactor.stop();
      } catch(Exception e) {
         throw new ConnectionException("Close error", e);
      }
   }
}
