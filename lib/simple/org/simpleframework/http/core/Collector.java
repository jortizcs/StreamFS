/*
 * Collector.java October 2002
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

package org.simpleframework.http.core;

import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * The <code>Collector</code> object is used to collect bytes from
 * a channel in order to create the HTTP entity. In order to create
 * the entity this makes use of a collection of consumer objects
 * which will consume the data required to compose the header and
 * body of the entity. Once the entity has been fully collected
 * this object exposes the consumed data as typed objects, which
 * can be used to examine the request sent by the client.
 * <p>
 * Because this collects bytes in a non-blocking manner there may
 * be a situation where there are no bytes left. In such an event
 * the collector needs to be queued until such time as there are
 * more bytes ready to read from the socket. To achieve this it is
 * given a <code>Selector</code> object, which it can used to 
 * hibernate until such time as there is data ready to read.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.http.core.Channel
 */
interface Collector extends Entity {
   
   /**
    * This is used to collect the data from a <code>Channel</code>
    * which is used to compose the entity. If at any stage there
    * are no ready bytes on the socket the selector provided can be
    * used to queue the collector until such time as the socket is
    * ready to read. Also, should the entity have completed reading
    * all required content it is handed to the selector as ready,
    * which processes the entity as a new client HTTP request.
    * 
    * @param selector this is the selector used to queue this
    */
   public void collect(Selector selector) throws IOException;
   
   /**
    * This returns the socket channel that is used by the collector
    * to read content from. This is a selectable socket, in that
    * it can be registered with a Java NIO selector. This ensures
    * that the system can be notified when the socket is ready.
    * 
    * @return the socket channel used by this collector object
    */
   public SocketChannel getSocket();
}
