/*
 * Monitor.java February 2007
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

/**
 * The <code>Monitor</code> object is core to how the requests are
 * processed from a pipeline. This monitors the progress of the
 * response streams as they are written to the underlying transport
 * which is typically TCP. If at any point there is an error in 
 * the delivery of the response the monitor is notified. It can
 * then shutdown the connection, as RFC 2616 suggests on errors. 
 * <p>
 * If however the response is delivered successfully the monitor is
 * notified of this event. On successful delivery the monitor will
 * hand the <code>Channel</code> back to the server kernel so that
 * the next request can be processed. This ensures ordering of the
 * responses matches ordering of the requests.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.http.core.Initiator
 */
interface Monitor {

   /**
    * This is used to close the underlying transport. A closure is
    * typically done when the response is to a HTTP/1.0 client
    * that does not require a keep alive connection. Also, if the
    * container requests an explicit closure this is used when all
    * of the content for the response has been sent.
    * 
    * @param sender this is the sender used to send the response
    */
   public void close(Sender sender);

   /**
    * This is used when there is an error sending the response. On
    * error RFC 2616 suggests a connection closure is the best
    * means to handle the condition, and the one clients should be
    * expecting and support. All errors result in closure of the
    * underlying transport and no more requests are processed.
    * 
    * @param sender this is the sender used to send the response
    */
   public void error(Sender sender);

   /**
    * This is used when the response has been sent correctly and
    * the connection supports persisted HTTP. When ready the channel
    * is handed back in to the server kernel where the next request
    * on the pipeline is read and used to compose the next entity.
    * 
    * @param sender this is the sender used to send the response
    */
   public void ready(Sender sender);

   /**
    * This is used to determine if the response has completed or
    * if there has been an error. This basically allows the sender
    * of the response to take action on certain I/O events.
    * 
    * @return this returns true if there was an error or close
    */
   public boolean isClosed();

   /**
    * This is used to determine if the response was in error. If
    * the response was in error this allows the sender to throw an
    * exception indicating that there was a problem responding.
    * 
    * @return this returns true if there was a response error
    */
   public boolean isError();
}