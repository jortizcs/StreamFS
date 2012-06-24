/*
 * Daemon.java February 2009
 *
 * Copyright (C) 2009, Niall Gallagher <niallg@users.sf.net>
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

package org.simpleframework.util.thread;

import static java.lang.Thread.State.NEW;

/**
 * The <code>Daemon</code> object provides a named daemon thread
 * which will execute the <code>run</code> method when started. 
 * This offers some convenience in that it hides the normal thread
 * methods and also allows the object extending this to provide
 * the name of the internal thread, which is given an incrementing
 * sequence number appended to the name provided.
 * 
 * @author Niall Gallagher
 */
public abstract class Daemon implements Runnable {
   
   /**
    * This is the internal thread used by this daemon instance.
    */
   private Thread thread;
   
   /**
    * Constructor for the <code>Daemon</code> object. This will 
    * create the internal thread and ensure it is a daemon. When it
    * is started the name of the internal thread is set using the
    * name of the instance as taken from <code>getName</code>. If
    * the name provided is null then no name is set for the thread.
    */
   protected Daemon() {
      this.thread = new Thread(this);
   }
   
   /**
    * This is used to start the internal thread. Once started the
    * internal thread will execute the <code>run</code> method of
    * this instance. Aside from starting the thread this will also
    * ensure the internal thread has a unique name.
    */
   public void start() {
      String prefix = getName();
      String name = ThreadNamer.getName(prefix);

      if(!isStarted()) {
         thread.setName(name);
         thread.start();
      }
   }
   
   /**
    * This is used to determine if the daemon has already started.
    * Once started it can not be started again. This ensures that
    * when dead it remains dead. The contract of this method is
    * that if the <code>start</code> method is invoked at any
    * point this method will always return true. 
    * 
    * @return true if the daemon has already been started
    */
   public boolean isStarted() {
      return thread.getState() != NEW;
   }
   
   /**
    * This is used to interrupt the internal thread. This is used
    * when there is a need to wake the thread from a sleeping or
    * waiting state so that some other operation can be performed.
    * Typically this is required when killing the thread.
    */
   public void interrupt() {
      thread.interrupt();
   }
   
   /**
    * This is used to join with the internal thread of this daemon.
    * Rather than exposing the internal thread a <code>join</code>
    * method is provided. This allows asynchronous threads to wait
    * for the daemon to complete simulating synchronous action.
    * 
    * @throws InterruptedException if the thread is interrupted
    */
   public void join() throws InterruptedException {
      thread.join();
   }
   
   /**
    * This is used to acquire the name of the thread. This will be
    * overridden by instances that wish to provide a descriptive
    * name for the thread. If this is not overridden then the name
    * of the thread is the simple name of the implementation. 
    * 
    * @return the name of the internal thread executed
    */
   public String getName() {
      return getClass().getSimpleName();
   }
}
