/*
 * ThreadNamer.java February 2009
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

import java.util.concurrent.atomic.AtomicInteger;

import org.simpleframework.util.KeyMap;

/**
 * The <code>ThreadNamer</code> object is used to name threads using
 * sequence numbers. Each thread with the same name will be given a
 * unique sequence number which is appended to the end of the name.
 * This is similar to the Java thread naming convention used by the
 * standard thread and thread pool implementations.
 * 
 * @author Niall Gallagher
 */
class ThreadNamer {
   
   /**
    * This is the singleton sequencer that is used to track names.
    */
   private static final Sequencer SEQUENCER;
   
   static {
      SEQUENCER = new Sequencer();
   }
   
   /**
    * This will create a thread name that is unique. The thread name
    * is a combination of the provided name and a sequence number
    * which is appended to the end of the name. This ensures that
    * each thread within the system has a unique name.
    * 
    * @param name this is the prefix for the thread name produced
    * 
    * @return this will return the name of the thread produced
    */
   public static String getName(String name) {
      int count = SEQUENCER.next(name);

      if(name == null) {
         return null;
      }
      return String.format("%s-%s", name, count);
   }
   
   /**
    * The <code>Sequencer</code> is used to create sequence numbers
    * for the threads that are to be named. This basically uses a
    * hash map of strings to atomic integers. When a name is used
    * the integer is incremented and returned.
    * 
    * @author Niall Gallagher
    */
   private static class Sequencer {
      
      /**
       * This is the map of atomic integers that are referenced.
       */
      private final KeyMap<AtomicInteger> map;
      
      /**
       * Constructor for the <code>Sequencer</code> object. This is
       * used to keep track of the sequence numbers used for the
       * threads in the system, so that they are all unique.
       */
      public Sequencer() {
         this.map = new KeyMap<AtomicInteger>();
      }
      
      /**
       * This is used to get the next sequence number for the name
       * provided. This allows the thread namer to construct a 
       * unique sequence number for threads with the same name.
       * 
       * @param name this is the name of the thread to sequence
       * 
       * @return this is the sequence number that has been retrieved
       */
      public synchronized int next(String name) {
         AtomicInteger count = map.get(name);
         
         if(count == null) {
            count = new AtomicInteger();
            map.put(name, count);
         }
         return count.getAndIncrement();
      }
   }
}
