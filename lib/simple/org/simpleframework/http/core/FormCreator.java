/*
 * FormCreator.java October 2002
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

import org.simpleframework.http.ContentType;
import org.simpleframework.http.Form;
import org.simpleframework.http.Part;
import org.simpleframework.http.Query;
import org.simpleframework.http.Request;

/**
 * The <code>FormCreator</code> object is used to create the form. 
 * It is created using the request URI query and a form post body if
 * sent, also if the request is a multipart upload the text parts
 * are added to the form. Building a single object form multiple
 * sources within the request ensures there is a single convenient
 * means to access the data from the request.
 * 
 * @author Niall Gallagher
 * 
 * @see org.simpleframework.http.Form
 */
class FormCreator {
   
   /**
    * This is the request that is used to acquire the data.
    */
   private final Request request;
   
   /**
    * This is used to acquire the content from the request body.
    */
   private final Body body;

   /**
    * Constructor for the <code>FormCreator</code> object. This will
    * create an object that can be used to construct a single form
    * from the multiple sources of data within the request entity.
    * 
    * @param request this is the request used to acquire the data
    * @param entity this is the entity that contains the data
    */
   public FormCreator(Request request, Entity entity) {
      this.body = entity.getBody();
      this.request = request;     
   } 
   
   /**
    * This is used to acquire a <code>Form</code> using all of the
    * parameters from the request URI as well as a form post if
    * it exists, finally all the text parts from an upload are
    * added to the form. This ensures that all data is accessible.
    * 
    * @return this returns the form created from the request
    */ 
   public Form getInstance() throws IOException {      
      PartList list = getParts();
      Query form = getQuery();
      
      for(Part part : list) {
         String name = part.getName();
         String value = part.getContent();
         
         form.put(name, value);       
      }      
      return new PartForm(body, form);      
   }
   
   
   /**
    * This method is used to acquire the query part from the HTTP 
    * request URI target and a form post if it exists. Both the 
    * query and the form post are merge together in a single query.
    * 
    * @return the query associated with the HTTP target URI
    */   
   private Query getQuery() throws IOException {
      Query query = request.getQuery();
      
      if(!isFormPost()) {
         return query;
      }
      return getQuery(query); // only get if form
   }
   
   /**
    * This method is used to acquire the query part from the HTTP 
    * request URI target and a form post if it exists. Both the 
    * query and the form post are merge together in a single query.
    * 
    * @param query this is the URI query string to be used
    * 
    * @return the query associated with the HTTP target URI
    */   
   private Query getQuery(Query query) throws IOException {
      String body = request.getContent(); 
      
      if(body == null) {
         return query;
      }
      return new QueryForm(query, body);
   }
   
   /**
    * This method provides all parts for this body. The parts for a
    * body can contain text parameters or files. Each file part can
    * contain headers, which are the typical HTTP headers. Typically
    * headers describe the content and any encoding if required.
    * 
    * @return this returns a list of parts for this body
    */   
   private PartList getParts() {
      PartList list = body.getParts();
      
      if(list.isEmpty()) {
         return list;
      }
      return getParts(list);         
   }
   
   /**
    * This method provides all parts for this body. The parts for a
    * body can contain text parameters or files. Each file part can
    * contain headers, which are the typical HTTP headers. Typically
    * headers describe the content and any encoding if required.
    * 
    * @param body this is the part list taken from the body
    * 
    * @return this returns a list of parts for this body
    */   
   private PartList getParts(PartList body) {
      PartList list = new PartList();
      
      for(Part part : body) {
         if(!part.isFile()) {
            list.add(part);
         }
      }
      return list;      
   }
   
   /**
    * This is used to determine if the content type is a form POST
    * of type application/x-www-form-urlencoded. Such a type is
    * used when a HTML form is used to post data to the server.
    * 
    * @return this returns true if content type is a form post
    */   
   private boolean isFormPost() {
      ContentType type = request.getContentType();
      
      if(type == null) {
         return false;
      }
      return isFormPost(type);
   }
   
   /**
    * This is used to determine if the content type is a form POST
    * of type application/x-www-form-urlencoded. Such a type is
    * used when a HTML form is used to post data to the server.  
    * 
    * @param type the type to determine if its a form post
    * 
    * @return this returns true if content type is a form post
    */
   private boolean isFormPost(ContentType type) {   
      String primary = type.getPrimary();
      String secondary = type.getSecondary();
      
      if(!primary.equals("application")) {
         return false;
      }
      return secondary.equals("x-www-form-urlencoded");
   }
}
