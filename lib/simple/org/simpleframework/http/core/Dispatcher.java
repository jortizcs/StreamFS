/*
 * Dispatcher.java February 2007
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

import org.simpleframework.http.Request;
import org.simpleframework.http.Response;

/**
 * The <code>Dispatcher</code> object is  used to dispatch a request
 * and response to the container. This is the root task that executes
 * all transactions. A transaction is dispatched to the container
 * which can deal with it asynchronously, however as a safeguard the
 * dispatcher will catch any throwables from the contains and close
 * the connection if required. Closing the connection if an exception
 * is thrown ensures that CLOSE_WAIT issues do not arise with open
 * connections that can not be closed within the container.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.http.core.Container
 */
class Dispatcher implements Runnable {
   
   /**
    * This is the container that is used to handle the transactions.
    */
   private final Container container;
   
   /**
    * This is the response object used to response to the request.
    */
   private final Response response;
   
   /**
    * This is the request object which contains the request entity.
    */
   private final Request request;
   
   /**
    * This is the monitor object used to signal completion events.
    */
   private final Monitor monitor; 
   
   /**
    * This is the entity containing the raw request details.
    */
   private final Entity entity;

   /**
    * Constructor for the <code>Dispatcher</code> object. This creates
    * a request and response object using the provided entity, these
    * can then be passed to the container to handle the transaction. 
    * 
    * @param container this is the container to handle the request
    * @param reactor the reactor used to handle the next request
    * @param entity this contains the current request entity
    */
   public Dispatcher(Container container, Initiator reactor, Entity entity) {
      this.monitor = new FlushMonitor(reactor, entity);
      this.request = new RequestEntity(entity, monitor);
      this.response = new ResponseEntity(request, entity, monitor);
      this.container = container;
      this.entity = entity;
   }

   /**
    * This <code>run</code> method will dispatch the created request
    * and response objects to the container. This will interpret the
    * target and semantics from the request object and compose a
    * response for the request which is sent to the connected client. 
    */
   public void run() {
      try {
         dispatch();
      } catch(Exception e) {
         return;
      }
   }
   
   /**
    * This <code>dispatch</code> method will dispatch the request
    * and response objects to the container. This will interpret the
    * target and semantics from the request object and compose a
    * response for the request which is sent to the connected client.
    * If there is an exception this will close the socket channel. 
    */   
   private void dispatch() throws Exception {
      Channel channel = entity.getChannel();

      try {
         container.handle(request, response);
      } catch(Throwable e) {
         channel.close();
      }
   }   
}

