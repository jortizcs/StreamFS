/*
 * ResourceProcessor.java February 2001
 *
 * Copyright (C) 2001, Niall Gallagher <niallg@users.sf.net>
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
  
package org.simpleframework.http.resource;

import org.simpleframework.http.Response;
import org.simpleframework.http.Request;
import org.simpleframework.http.core.Container;

/**
 * The <code>ResourceContainer</code> is an implementation of the
 * <code>Container</code> interface for handling an arbitrary set
 * of resources. This container will accept any HTTP transaction
 * and delegate the processing of that transation to a resource.
 * This resolves the resource to use using an implementation of 
 * the <code>ResourceEngine</code> interface. 
 * <p>
 * This provides a very simple means to manage individual targets
 * using separate resource implementations. It also provides an
 * ideal location to introduce path mapping functionality.
 *
 * @author Niall Gallagher
 */
public class ResourceContainer implements Container {   

   /**
    * The engine that resolves the resources to be used.
    */
   private final ResourceEngine engine;

   /**
    * Constructor for the <code>ResourceContainer</code> object.
    * This requires a resource engine which it uses to map the
    * request targets to a given implementation or instance. 
    * This is essentially a router to the <code>Resource</code>
    * objects that have been mapped to a given request path.
    *
    * @param engine the engine used to resolve resources
    */
   public ResourceContainer(ResourceEngine engine){
      this.engine = engine;
   }

   /**
    * This method is where most of the work is done to retrieve 
    * the <code>Resource</code> and process the HTTP request. This
    * will basically use the <code>resolve</code> method of the
    * issued <code>ResourceEngine</code> to acquire resources.
    *
    * @param req the <code>Request</code> to be processed
    * @param resp the <code>Response</code> to be processed
    */
   public void handle(Request req, Response resp){
      engine.resolve(req.getAddress()).handle(req,resp);
   }
}
