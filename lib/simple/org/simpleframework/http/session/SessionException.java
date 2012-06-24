/*
 * SessionException.java May 2007
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

import org.simpleframework.util.lease.LeaseException;

/**
 * The <code>SessionException</code> is used to indicate that some
 * operation failed when trying to access a session or when trying
 * to perform some operation on an existing session. Typically this
 * is thrown when a session no longer exists.
 *
 * @author Niall Gallagher
 */
public class SessionException extends LeaseException {
   
   /**
    * This constructor is used if there is a description of the 
    * event that caused the exception required. This can be given
    * a message used to describe the situation for the exception.
    * 
    * @param template this is a description of the exception
    * @param list this is the list of arguments that can be used
    */
   public SessionException(String template, Object... list) {
      super(template, list);
   }

}
