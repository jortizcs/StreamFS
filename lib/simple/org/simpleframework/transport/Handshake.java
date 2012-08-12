/*
 * Handshake.java February 2007
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

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;
import static org.simpleframework.transport.Status.CLIENT;
import static org.simpleframework.transport.Status.DONE;
import static org.simpleframework.transport.Status.SERVER;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SocketChannel;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;

/**
 * The <code>Handshake</code> object is used to perform secure SSL
 * negotiations on a pipeline or <code>Transport</code>. This can
 * be used to perform an SSL handshake. To perform the negotiation 
 * this uses an SSL engine provided with the pipeline to direct 
 * the conversation. The SSL engine tells the negotiation what is
 * expected next, whether this is a response to the client or a 
 * message from it. During the negotiation this may need to wait 
 * for either a write ready event or a read ready event. Event 
 * notification is done using the negotiator provided.
 *
 * @author Niall Gallagher
 *
 * @see org.simpleframework.transport.Negotiator
 */
class Handshake implements Negotiation {

   /**
    * This is the negotiator used to process the secure transport.
    */
   private final Negotiator negotiator;
   
   /**
    * This is the socket channel used to read and write data to.
    */
   private final SocketChannel channel;
   
   /**
    * This is the transport dispatched when the negotiation ends.
    */
   private final Transport transport;
   
   /**
    * This is the output buffer used to generate data to.
    */
   private final ByteBuffer output;
   
   /**
    * This is the input buffer used to read data from the socket.
    */
   private final ByteBuffer input;
   
   /**
    * This is an empty byte buffer used to generate a response.
    */
   private final ByteBuffer empty;
   
   /**
    * This is the SSL engine used to direct the conversation.
    */
   private final SSLEngine engine;

   /**
    * Constructor for the <code>Negotiation</code> object. This is
    * used to create an operation capable of performing negotiations
    * for SSL connections. Typically this is used to perform request
    * response negotiations, such as a handshake or termination.
    *
    * @param negotiator the negotiator used to check socket events
    * @param transport the transport to perform the negotiation for
    */
   public Handshake(Transport transport, Negotiator negotiator) {
      this(transport, negotiator, 20480);           
   }
  
   /**
    * Constructor for the <code>Negotiation</code> object. This is
    * used to create an operation capable of performing negotiations
    * for SSL connections. Typically this is used to perform request
    * response negotiations, such as a handshake or termination.
    *
    * @param negotiator the negotiator used to check socket events
    * @param transport the transport to perform the negotiation for
    * @param size the size of the buffers used for the negotiation
    */
   public Handshake(Transport transport, Negotiator negotiator, int size) {
      this.output = ByteBuffer.allocate(size);
      this.input = ByteBuffer.allocate(size);
      this.channel = transport.getChannel();    
      this.engine = transport.getEngine();
      this.empty = ByteBuffer.allocate(0);
      this.negotiator = negotiator;
      this.transport = transport;
   }
   
   /**
    * This returns the socket channel for the connected pipeline. It
    * is this channel that is used to determine if there are bytes
    * that can be read. When closed this is no longer selectable.
    *
    * @return this returns the connected channel for the pipeline
    */
   public SelectableChannel getChannel() {
      return channel;
   }   
   
   /**
    * This is used to start the negotiation. Once started this will
    * send a message to the client, once sent the negotiation reads
    * the response. However if the response is not yet ready this 
    * will schedule the negotiation for a selectable operation 
    * ensuring that it can resume execution when ready.
    */
   public void run() {      
      if(engine != null) {
         engine.setUseClientMode(false);         
         input.flip();
      }         
      begin();
    
   }

   /**
    * This is used to terminate the negotiation. This is excecuted
    * when the negotiation times out. When the negotiation expires it
    * is rejected by the negotiator and must be canceled. Canceling
    * is basically termination of the connection to free resources.
    */
   public void cancel() {
      try {
         transport.close();
      } catch(Exception e) { 
         return;
      }
   }
   
   /**
    * This is used to start the negotation. Once started this will
    * send a message to the client, once sent the negotiation reads
    * the response. However if the response is not yet ready this 
    * will schedule the negotiation for a selectable operation 
    * ensuring that it can resume execution when ready.
    */
   private void begin() {
      try {
         resume();
      } catch(Exception e) {
         return;
      }
   }
   
   /**
    * This is the main point of execution within the negotiation. It
    * is where the negotiation is performed. Negotiations are done
    * by performing a request response flow, governed by the SSL
    * engine associated with the pipeline. Typically the client is
    * the one to initiate the handshake and the server initiates the
    * termination sequence. This may be executed several times 
    * depending on whether reading or writing blocks.
    */
   public void resume() throws IOException {
      Runnable task = process();
      
      if(task != null) {
         task.run();
      }
   }
   
   /**
    * This is the main point of execution within the negotiation. It
    * is where the negotiation is performed. Negotiations are done
    * by performing a request response flow, governed by the SSL
    * engine associated with the pipeline. Typically the client is
    * the one to initiate the handshake and the server initiates the
    * termination sequence. This may be executed several times 
    * depending on whether reading or writing blocks.
    * 
    * @return this returns a task used to execute the next phase
    */
   private Runnable process() throws IOException {
      Status require = exchange();
      
      if(require == CLIENT) {
         return new Client(this);
      } 
      if(require == SERVER) {
         return new Server(this);
      } 
      return new Done(this);
   }
   
   /**
    * This is the main point of execution within the negotiation. It
    * is where the negotiation is performed. Negotiations are done
    * by performing a request response flow, governed by the SSL
    * engine associated with the pipeline. Typically the client is
    * the one to initiate the handshake and the server initiates the
    * termination sequence. This may be executed several times 
    * depending on whether reading or writing blocks.
    * 
    * @return this returns what is expected next in the negotiation
    */
   private Status exchange() throws IOException {
      HandshakeStatus status = engine.getHandshakeStatus();
      
      switch(status){
      case NEED_WRAP:
         return write();
      case NOT_HANDSHAKING:
      case NEED_UNWRAP:
         return read();
      }      
      return DONE;
   }
   
   /**
    * This is used to perform the read part of the negotiation. The
    * read part is where the client sends information to the server
    * and the server consumes the data and determines what action 
    * to take. Typically it is the SSL engine that determines what
    * action is to be taken depending on the client data.
    *
    * @return the next action that should be taken by the handshake
    */
   private Status read() throws IOException {
      return read(5);
   }

   /**
    * This is used to perform the read part of the negotiation. The
    * read part is where the client sends information to the server
    * and the server consumes the data and determines what action 
    * to take. Typically it is the SSL engine that determines what
    * action is to be taken depending on the client data.
    *
    * @param count this is the number of times a read can repeat
    *
    * @return the next action that should be taken by the handshake
    */
   private Status read(int count) throws IOException {
      while(count > 0) {
         SSLEngineResult result = engine.unwrap(input, output); 
         HandshakeStatus status = result.getHandshakeStatus();
       
         switch(status) {
         case NOT_HANDSHAKING:
            return DONE;
         case NEED_WRAP:
            return SERVER;
         case FINISHED:
         case NEED_UNWRAP:
            return read(count-1);
         case NEED_TASK:
            execute(); 
         }      
      }
      return CLIENT;
   }
   
   /**
    * This is used to perform the write part of the negotiation. The
    * read part is where the server sends information to the client
    * and the client interprets the data and determines what action 
    * to take. After a write the negotiation typically completes or
    * waits for the next response from the client.
    *
    * @return the next action that should be taken by the handshake
    */
   private Status write() throws IOException {   
      return write(5);
   }
   
   /**
    * This is used to perform the write part of the negotiation. The
    * read part is where the server sends information to the client
    * and the client interprets the data and determines what action 
    * to take. After a write the negotiation typically completes or
    * waits for the next response from the client.
    *
    * @param count this is the number of times a read can repeat
    *
    * @return the next action that should be taken by the handshake
    */
   private Status write(int count) throws IOException {
      while(count > 0) {
         SSLEngineResult result = engine.wrap(empty, output);
         HandshakeStatus status = result.getHandshakeStatus();

         switch(status) {
         case NOT_HANDSHAKING:
         case FINISHED:
         case NEED_UNWRAP:
            return SERVER;
         case NEED_WRAP:
            return write(count-1);
         case NEED_TASK:
            execute();
         }
      }
      return SERVER;
   }  
   
   /**
    * This is used to execute the delegated tasks. These tasks are
    * used to digest the information received from the client in
    * order to generate a response. This may need to execute several
    * tasks from the associated SSL engine.
    */
   private void execute() throws IOException {
      while(true) {
         Runnable task = engine.getDelegatedTask();
         
         if(task == null) {
            break;
         }
         task.run();
      }
   }
   
   /**
    * This is used to receive data from the client. If at any
    * point during the negotiation a message is required that
    * can not be read immediately this is used to asynchronously
    * read the data when a select operation is signaled.
    *  
    * @return this returns true when the message has been read
    */
   public boolean receive() throws IOException {
      int count = input.capacity();
      
      if(count > 0) {
         input.compact();
      }
      int size = channel.read(input); 
      
      if(size < 0) {
         throw new TransportException("Client closed connection");
      }
      if(count > 0) {
         input.flip(); 
      }
      return size > 0;
   }

   /**
    * Here we attempt to send all data within the output buffer. If
    * all of the data is delivered to the client then this will
    * return true. If however there is content yet to be sent to
    * the client then this returns false, telling the negotiation
    * that in order to resume it must attempt to send the content
    * again after a write ready operation on the underlying socket.
    * 
    * @return this returns true if all of the content is delivered
    */
   public boolean send() throws IOException {
      int require = output.position();
      int count = 0;
      
      if(require > 0) { 
         output.flip();
      }
      while(count < require) { 
         int size = channel.write(output);

         if(size <= 0) {
            break;
         }
         count += size;
      }
      if(require > 0) {
         output.compact(); 
      }
      return count == require;
   }

   /**
    * This method is invoked when the negotiation is done and the
    * next phase of the connection is to take place. This will
    * be invoked when the SSL handshake has completed and the new
    * secure transport is to be handed to the processor.
    */
   public void commit() throws IOException {
      Transport secure = new SecureTransport(transport, output, input);

      if(negotiator != null) {
         negotiator.process(secure);
      }
   }
   
   /**
    * The <code>Done</code> task is used to transfer the transport
    * created to the negotiator. This is executed when the SSL
    * handshake is completed. It allows the transporter to use the
    * newly created transport to read and write in plain text and
    * to have the SSL transport encrypt and decrypt transparently.
    * 
    * @author Niall Gallagher
    */
   private class Done extends Task {
      
      /**
       * Constructor for the <code>Done</code> task. This is used to
       * pass the transport object object to the negotiator when the
       * SSL handshake has completed. 
       * 
       * @param state this is the underlying negotiation to use
       */
      public Done(Negotiation state) {
         super(state, negotiator, OP_READ);
      }

      /**
       * This is used to execute the task. It is up to the specific
       * task implementation to decide what to do when executed. If
       * the task needs to read or write data then it can attempt
       * to perform the read or write, if it incomplete the it can
       * be scheduled for execution with the reactor.
       */
      @Override
      public void execute() throws IOException{
         state.commit();
      }
   }
   
   /**
    * The <code>Client</code> task is used to schedule the negotiation
    * for a read operation. This allows the negotiation to receive any
    * messages generated by the client asynchronously. Once this has 
    * completed then it will resume the negotiation.
    * 
    * @author Niall Gallagher
    */
   private class Client extends Task {
      
      /**
       * Constructor for the <code>Client</code> task. This is used 
       * to create a task which will schedule a read operation for 
       * the negotiation. When the operation completes this will 
       * resume the negotiation.
       * 
       * @param state this is the negotiation object that is used
       */
      public Client(Negotiation state) {
         super(state, negotiator, OP_READ);
      }
      
      /**
       * This method is used to determine if the task is ready. This 
       * is executed when the select operation is signaled. When this 
       * is true the the task completes. If not then this will 
       * schedule the task again for the specified select operation.
       * 
       * @return this returns true when the task has completed
       */
      @Override
      protected boolean ready() throws IOException {
         return state.receive();
      }
   }
   
   /**
    * The <code>Server</code> is used to schedule the negotiation
    * for a write operation. This allows the negotiation to send any
    * messages generated during the negotiation asynchronously. Once
    * this has completed then it will resume the negotiation.
    * 
    * @author Niall Gallagher
    */
   private class Server extends Task {
      
      /**
       * Constructor for the <code>Server</code> task. This is used 
       * to create a task which will schedule a write operation for 
       * the negotiation. When the operation completes this will 
       * resume the negotiation.
       * 
       * @param state this is the negotiation object that is used
       */
      public Server(Negotiation state) {
         super(state, negotiator, OP_WRITE);
      }
      
      /**
       * This method is used to determine if the task is ready. This 
       * is executed when the select operation is signaled. When this 
       * is true the the task completes. If not then this will 
       * schedule the task again for the specified select operation.
       * 
       * @return this returns true when the task has completed
       */
      @Override
      protected boolean ready() throws IOException {
         return state.send();
      }
   }
}
