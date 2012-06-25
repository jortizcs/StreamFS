/*
 * Observer.java May 2007
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

package org.simpleframework.http.session;

/**
 * The <code>Observer</code> interface is used to observe the session
 * activity within the session manager. This enables tracking of all
 * sessions created and destroyed by the session manager. Once the
 * session is created the observer <code>start</code> method is 
 * invoked and when the session is canceled the <code>cancel</code>
 * method is invoked. Observations allow specialized monitoring.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.http.session.SessionManager
 */
public interface Observer<T> {
   
   /**
    * This method is called after the session has been created but
    * before it is used. Listening to invocations of this method will
    * allow the user to establish any data structures required or 
    * add any attributes to the session that are needed.
    * 
    * @param session this is the session instance that was created
    */
   public void create(Session<T> session);
   
   /**
    * This method is called after the session has been canceled or
    * expired. It allows the listener to clean up session resources
    * and attributes in a specialized manner. When finished the
    * session will no longer exist within the application.
    * 
    * @param session this is the session object that is canceled
    */
   public void cancel(Session<T> session);
}
