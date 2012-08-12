/*
 * PartForm.java February 2007
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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.simpleframework.http.Form;
import org.simpleframework.http.Part;
import org.simpleframework.http.Query;

/**
 * The <code>PartForm</code> is a form that delegates to a body to
 * get any parts requested. This combines both the query and the body
 * in to a single object which can be used to acquire all data that
 * has been sent by the client in a single convenient location. All
 * changes made to this form will be made to the underlying query.
 * 
 * @author Niall Gallagher
 */
class PartForm implements Form {
      
   /**
    * This is the query used to acquire the request parameters.
    */
   private final Query query;
   
   /**
    * This is the body used to acquire the uploaded parts.
    */
   private final Body body;
   
   /**
    * Constructor for the <code>PartForm</code> object. This is used
    * to create a form using the parameters from the query and the
    * parts from the specified body. Combining both objects under a
    * single instance provides a convenient means to find data.
    * 
    * @param body this is the body that contains all the parts
    * @param query this is the query used to get parameters
    */
   public PartForm(Body body, Query query) {
      this.query = query;
      this.body = body;
   }
   
   /**
    * This extracts an integer parameter for the named value. If the 
    * named parameter does not exist this will return a zero value. 
    * If however the parameter exists but is not in the format of a 
    * decimal integer value then this will throw an exception.
    *
    * @param name the name of the parameter value to retrieve
    *
    * @return this returns the named parameter value as an integer   
    */
   public int getInteger(Object name) {
      return query.getInteger(name);
   }

   /**
    * This extracts a float parameter for the named value. If the 
    * named parameter does not exist this will return a zero value. 
    * If however the parameter exists but is not in the format of a 
    * floating point number then this will throw an exception.
    *
    * @param name the name of the parameter value to retrieve
    *
    * @return this returns the named parameter value as a float   
    */
   public float getFloat(Object name) {
      return query.getFloat(name);
   }

   /**
    * This extracts a boolean parameter for the named value. If the
    * named parameter does not exist this will return false otherwise
    * the value is evaluated. If it is either <code>true</code> or 
    * <code>false</code> then those boolean values are returned.
    * 
    * @param name the name of the parameter value to retrieve
    *
    * @return this returns the named parameter value as an float
    */
   public boolean getBoolean(Object name) {
      return query.getBoolean(name);
   }

   /**
    * This method is used to acquire a <code>Part</code> from the
    * form using a known name for the part. This is typically used 
    * when there is a file upload with a multipart POST request.
    * All parts that are not files are added to the query values
    * as strings so that they can be used in a convenient way.
    * 
    * @param name this is the name of the part to acquire
    * 
    * @return the named part or null if the part does not exist
    */   
   public Part getPart(String name) {
      return body.getPart(name);
   }
   
   /**
    * This method provides all parts for this <code>Form</code>. The
    * parts for a form can contain text parameters or files. Each file
    * part can contain headers, which take the form of HTTP headers to
    * describe the payload. Typically headers describe the content.
    * 
    * @return this returns a list of parts for this form
    */   
   public List<Part> getParts() {
      return body.getParts();
   }

   /**
    * This is used to determine whether a value representing the
    * name of a pair has been inserted into the internal map. The
    * object passed into this method should be a string, as all
    * values stored within the map will be stored as strings.
    *  
    * @param name this is the name of a pair within the map
    *
    * @return this returns true if the pair of that name exists
    */
   public boolean containsKey(Object name) {
      return query.containsKey(name);
   }
   
   /**
    * This method is used to determine whether any pair that has
    * been inserted into the internal map had the presented value.
    * If one or more pairs within the collected values contains
    * the value provided then this method will return true.
    * 
    * @param value this is the value that is to be searched for
    *
    * @return this returns true if any value is equal to this
    */
   public boolean containsValue(Object value) {
      return query.containsValue(value);
   }
   
   /**
    * This method is used to acquire the name and value pairs that
    * have currently been collected by this parser. This is used
    * to determine which values have been extracted from the 
    * source. It is useful when the values have to be gathered.
    *
    * @return this set of value pairs that have been extracted
    */
   public Set<Map.Entry<String, String>> entrySet() {
      return query.entrySet();
   }
   
   /**
    * The <code>get</code> method is used to acquire the value for
    * a named pair. So if a pair of name=value has been parsed and
    * inserted into the collection of values this will return the
    * value given the name. The value returned will be a string.
    *
    * @param name this is a string used to search for the value
    *
    * @return this is the value, as a string, that has been found 
    */
   public String get(Object name) {
      return query.get(name);
   }
   
   /**
    * This method is used to acquire a <code>List</code> for all of
    * the values that have been put in to the map. The list allows
    * all values associated with the specified key. This enables a
    * parser to collect a number of associated values.
    * 
    * @param name this is the key used to search for the value
    * 
    * @return this is the list of values associated with the key
    */
   public List<String> getAll(Object name) {
      return query.getAll(name);
   }
   
   /**
    * This method is used to determine whether the parser has any
    * values available. If the <code>size</code> is zero then the
    * parser is empty and this returns true. The is acts as a
    * proxy the the <code>isEmpty</code> of the internal map.
    * 
    * @return this is true if there are no available values
    */
   public boolean isEmpty() {
      return query.isEmpty();
   }
   
   /**
    * This is used to acquire the names for all the values that 
    * have currently been collected by this parser. This is used
    * to determine which values have been extracted from the 
    * source. It is useful when the values have to be gathered.
    *
    * @return the set of name values that have been extracted
    */
   public Set<String> keySet() {
      return query.keySet();
   }
   
   /**
    * The <code>put</code> method is used to insert the name and
    * value provided into the collection of values. Although it is
    * up to the parser to decide what values will be inserted it
    * is generally the case that the inserted values will be text.
    *
    * @param name this is the name string from a name=value pair
    * @param value this is the value string from a name=value pair
    *
    * @return this returns the previous value if there was any
    */
   public String put(String name, String value) {
      return query.put(name, value);
   }
   
   /**
    * This method is used to insert a collection of values into 
    * the parsers map. This is used when another source of values
    * is required to populate the connection currently maintained
    * within this parsers internal map. Any values that currently
    * exist with similar names will be overwritten by this.
    * 
    * @param map this is the collection of values to be added
    */
   public void putAll(Map<? extends String, ? extends String> map) {
      query.putAll(map);
   }
   
   /**
    * The <code>remove</code> method is used to remove the named
    * value pair from the collection of values. This acts like a
    * take, in that it will get the value string and remove if 
    * from the collection of values the parser has stored.
    *
    * @param name this is a string used to search for the value
    *
    * @return this is the value, as a string, that is removed
    */
   public String remove(Object name) {
      return query.remove(name);
   }
   
   /**
    * This obviously enough provides the number of values that
    * have been inserted into the internal map. This acts as
    * a proxy method for the internal map <code>size</code>.
    *
    * @return this returns the number of values are available
    */
   public int size() {
      return query.size();
   }
   
   /**
    * This method is used to acquire the value for all values that
    * have currently been collected by this parser. This is used
    * to determine which values have been extracted from the
    * source. It is useful when the values have to be gathered.
    *
    * @return the list of value strings that have been extracted
    */
   public Collection<String> values() {
      return query.values();
   }   

   /**
    * The <code>clear</code> method is used to wipe out all the
    * currently existing values from the collection. This is used
    * to recycle the parser so that it can be used to parse some
    * other source of values without any lingering state.
    */
   public void clear() {
      query.clear();      
   }
   
   /**
    * This will return all parameters represented using the HTTP
    * URL query format. The <code>x-www-form-urlencoded</code>
    * format is used to encode the attributes, see RFC 2616. 
    * <p>
    * This will also encode any special characters that appear
    * within the name and value pairs as an escaped sequence.
    * If there are no parameters an empty string is returned.
    *
    * @return returns an empty string if the is no parameters
    */    
   public String toString() {
      return query.toString();
   }
}
