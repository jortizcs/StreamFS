/*
 * DirectReactor.java February 2007
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

package org.simpleframework.transport.reactor;

import java.io.IOException;
import java.util.concurrent.Executor;

import org.simpleframework.util.thread.DirectExecutor;

/**
 * The <code>DirectReactor</code> object is used to execute the ready
 * operations of within a single synchronous thread. This is used 
 * when the I/O operations to be performed do not require much time
 * to execute and so will not block the execution thread.
 * 
 * @author Niall Gallagher
 */
public class DirectReactor implements Reactor {

   /**
    * This is used to distribute the ready operations for execution.
    */         
   private Distributor exchange;

   /**
    * This is used to execute the operations that ready to run.
    */ 
   private Executor executor;
   
   /**
    * Constructor for the <code>DirectReactor</code> object. This is
    * used to create a reactor that does not require thread pooling
    * to execute the ready operations. All I/O operations are run
    * in the selection thread and should complete quickly.
    */
   public DirectReactor() throws IOException {
      this(false);
   }
   
   /**
    * Constructor for the <code>DirectReactor</code> object. This is
    * used to create a reactor that does not require thread pooling
    * to execute the ready operations. All I/O operations are run
    * in the selection thread and should complete quickly.
    * 
    * @param cancel determines the selection key should be canceled
    */   
   public DirectReactor(boolean cancel) throws IOException {
      this.executor = new DirectExecutor();
      this.exchange = new ActionDistributor(executor, cancel);
   }   

   /**
    * This method is used to execute the provided operation without
    * the need to specifically check for I/O events. This is used if
    * the operation knows that the <code>SelectableChannel</code> is
    * ready, or if the I/O operation can be performed without knowing
    * if the channel is ready. Typically this is an efficient means
    * to perform a poll rather than a select on the channel.
    *
    * @param task this is the task to execute immediately
    */   
   public void process(Operation task) throws IOException {
      executor.execute(task);          
   }
   
   /**        
    * This method is used to execute the provided operation when there
    * is an I/O event that task is interested in. This will used the
    * operations <code>SelectableChannel</code> object to determine 
    * the events that are ready on the channel. If this reactor is
    * interested in any of the ready events then the task is executed.
    *
    * @param task this is the task to execute on interested events
    * @param require this is the bit-mask value for interested events
    */  
   public void process(Operation task, int require) throws IOException {          
      exchange.process(task, require);    
   }        

   /**
    * This is used to stop the reactor so that further requests to
    * execute operations does nothing. This will clean up all of 
    * the reactors resources and unregister any operations that are
    * currently awaiting execution. This should be used to ensure
    * any threads used by the reactor graceful stop.
    */   
   public void stop() throws IOException {
      exchange.close();
   }   
}
