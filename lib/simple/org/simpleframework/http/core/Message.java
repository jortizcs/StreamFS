/*
 * Message.java February 2007
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

import java.util.ArrayList;
import java.util.List;

import org.simpleframework.http.Cookie;
import org.simpleframework.http.parse.DateParser;
import org.simpleframework.http.parse.ValueParser;
import org.simpleframework.util.KeyMap;

/**
 * The <code>Message</code> object is used to store an retrieve the
 * headers for both a request and response. Headers are stored and
 * retrieved in a case insensitive manner according to RFC 2616. 
 * The message also allows multiple header values to be added to a
 * single header name, headers such as Cookie and Set-Cookie can be 
 * added multiple times with different values.
 * 
 * @author Niall Gallagher
 */
class Message {

   /**
    * This is used to store the cookies added to the HTTP header.
    */
   private final KeyMap<Cookie> cookies;

   /**
    * This is used to store multiple header values for a name.  
    */
   private final KeyMap<Entry> values;

   /**
    * This is used to store the individual names for the header.
    */
   private final KeyMap<String> names;

   /**
    * This is used to parse all date headers added to the message.
    */
   private final DateParser parser;

   /**
    * Constructor for the <code>Message</code> object. This is used 
    * to create a case insensitive means for storing HTTP header
    * names and values. Dates can also be added to message as a
    * long value and is converted to RFC 1123 compliant date string.
    */
   public Message() {
      this.cookies = new KeyMap<Cookie>();
      this.values = new KeyMap<Entry>();
      this.names = new KeyMap<String>();
      this.parser = new DateParser();
   }

   /**
    * This is used to acquire the names of the of the headers that
    * have been set in the response. This can be used to acquire all
    * header values by name that have been set within the response.
    * If no headers have been set this will return an empty list.
    * 
    * @return a list of strings representing the set header names
    */
   public List<String> getNames() {
      return names.getValues();
   }

   /**
    * This can be used to set a HTTP message header to this object.
    * The name and value of the HTTP message header will be used to
    * create a HTTP message header object which can be retrieved using
    * the <code>getValue</code> in combination with the get methods.
    * This will perform a <code>remove</code> using the issued header
    * name before the header value is set.       
    *
    * @param name the name of the HTTP message header to be added
    * @param value the value the HTTP message header will have
    */
   public void set(String name, String value) {
      List<String> list = getAll(name);

      if(value != null) {
         list.clear();
         list.add(value);
      }
   }

   /**
    * This can be used to set a HTTP message header to this object.
    * The name and value of the HTTP message header will be used to
    * create a HTTP message header object which can be retrieved using
    * the <code>getValue</code> in combination with the get methods.
    * This will perform a <code>remove</code> using the issued header
    * name before the header value is set.       
    *
    * @param name the name of the HTTP message header to be added
    * @param value the value the HTTP message header will have
    */
   public void set(String name, int value) {
      set(name, String.valueOf(value));
   }

   /**
    * This is used as a convenience method for adding a header that
    * needs to be parsed into a HTTP date string. This will convert
    * the date given into a date string defined in RFC 2616 sec 3.3.1.
    * This will perform a <code>remove</code> using the issued header
    * name before the header value is set.       
    *
    * @param name the name of the HTTP message header to be added
    * @param date the value constructed as an RFC 1123 date string
    */
   public void setDate(String name, long date) {
      set(name, parser.convert(date));
   }

   /**
    * This can be used to add a HTTP message header to this object.
    * The name and value of the HTTP message header will be used to
    * create a HTTP message header object which can be retrieved using
    * the <code>getValue</code> in combination with the get methods.
    *
    * @param name the name of the HTTP message header to be added
    * @param value the value the HTTP message header will have
    */
   public void add(String name, String value) {
      List<String> list = getAll(name);

      if(value != null) {
         list.add(value);
      }
   }

   /**
    * This can be used to add a HTTP message header to this object.
    * The name and value of the HTTP message header will be used to
    * create a HTTP message header object which can be retrieved using
    * the <code>getInteger</code> in combination with the get methods.
    *
    * @param name the name of the HTTP message header to be added
    * @param value the value the HTTP message header will have
    */
   public void add(String name, int value) {
      add(name, String.valueOf(value));
   }

   /**
    * This is used as a convenience method for adding a header that
    * needs to be parsed into a HTTPdate string. This will convert
    * the date given into a date string defined in RFC 2616 sec 3.3.1.
    *
    * @param name the name of the HTTP message header to be added
    * @param date the value constructed as an RFC 1123 date string
    */
   public void addDate(String name, long date) {
      add(name, parser.convert(date));
   }

   /**
    * This can be used to get the value of the first message header
    * that has the specified name. This will return the full string
    * representing the named header value. If the named header does
    * not exist then this will return a null value.
    *
    * @param name the HTTP message header to get the value from
    *
    * @return this returns the value that the HTTP message header
    */
   public String getValue(String name) {
      List<String> list = getAll(name);

      if(list.size() > 0) {
         return list.get(0);
      }
      return null;
   }

   /**
    * This can be used to get the value of the first message header
    * that has the specified name. This will return the integer
    * representing the named header value. If the named header does
    * not exist then this will return a value of minus one, -1.
    *
    * @param name the HTTP message header to get the value from
    *
    * @return this returns the value that the HTTP message header
    */
   public int getInteger(String name) {
      String value = getValue(name);

      if(value == null) {
         return -1;
      }
      return Integer.parseInt(value);
   }

   /**
    * This can be used to get the value of the first message header
    * that has the specified name. This will return the long value
    * representing the named header value. If the named header does
    * not exist then this will return a value of minus one, -1.
    *
    * @param name the HTTP message header to get the value from
    *
    * @return this returns the value that the HTTP message header
    */
   public long getDate(String name) {
      String value = getValue(name);

      if(value == null) {
         return -1;
      }
      return parser.convert(value);
   }

   /**
    * This returns the <code>Cookie</code> object stored under the
    * specified name. This is used to retrieve cookies that have been 
    * set with the <code>setCookie</code> methods. If the cookie does
    * not exist under the specified name this will return null. 
    *
    * @param name this is the name of the cookie to be retrieved
    * 
    * @return returns the <code>Cookie</code> by the given name
    */
   public Cookie getCookie(String name) {
      return cookies.get(name);
   }

   /**
    * This returns all <code>Cookie</code> objects stored under the
    * specified name. This is used to retrieve cookies that have been 
    * set with the <code>setCookie</code> methods. If there are no
    * cookies then this will return an empty list. 
    * 
    * @return returns all the <code>Cookie</code> in the response
    */
   public List<Cookie> getCookies() {
      return cookies.getValues();
   }

   /**
    * The <code>setCookie</code> method is used to set a cookie value 
    * with the cookie name. This will add a cookie to the response
    * stored under the name of the cookie, when this is committed it 
    * will be added as a Set-Cookie header to the resulting response.
    * This is a convenience method that avoids cookie creation.     
    *
    * @param name this is the cookie to be added to the response
    * @param value this is the cookie value that is to be used
    * 
    * @return returns the cookie that has been set in the response
    */
   public Cookie setCookie(String name, String value) {
      return setCookie(new Cookie(name, value, true));
   }

   /**
    * The <code>setCookie</code> method is used to set a cookie value 
    * with the cookie name. This will add a cookie to the response
    * stored under the name of the cookie, when this is committed it 
    * will be added as a Set-Cookie header to the resulting response.
    *
    * @param cookie this is the cookie to be added to the response
    * 
    * @return returns the cookie that has been set in the response
    */
   public Cookie setCookie(Cookie cookie) {
      String name = cookie.getName();

      if(name != null) {
         cookies.put(name, cookie);
      }
      return cookie;
   }

   /**
    * This can be used to get the values of HTTP message headers
    * that have the specified name. This is a convenience method that 
    * will present that values as tokens extracted from the header.
    * This has obvious performance benefits as it avoids having to 
    * deal with <code>substring</code> and <code>trim</code> calls.
    * <p>
    * The tokens returned by this method are ordered according to
    * there HTTP quality values, or "q" values, see RFC 2616 section
    * 3.9. This also strips out the quality parameter from tokens
    * returned. So "image/html; q=0.9" results in "image/html". If
    * there are no "q" values present then order is by appearance.
    * <p> 
    * The result from this is either the trimmed header value, that
    * is, the header value with no leading or trailing whitespace
    * or an array of trimmed tokens ordered with the most preferred
    * in the lower indexes, so index 0 is has highest preference.
    *
    * @param name the name of the headers that are to be retrieved
    *
    * @return ordered list of tokens extracted from the header(s)
    */
   public List<String> getValues(String name) {
      return getValues(getAll(name));
   }

   /**
    * This can be used to get the values of HTTP message headers
    * that have the specified name. This is a convenience method that 
    * will present that values as tokens extracted from the header.
    * This has obvious performance benefits as it avoids having to 
    * deal with <code>substring</code> and <code>trim</code> calls.
    * <p>
    * The tokens returned by this method are ordered according to
    * there HTTP quality values, or "q" values, see RFC 2616 section
    * 3.9. This also strips out the quality parameter from tokens
    * returned. So "image/html; q=0.9" results in "image/html". If
    * there are no "q" values present then order is by appearance.
    * <p> 
    * The result from this is either the trimmed header value, that
    * is, the header value with no leading or trailing whitespace
    * or an array of trimmed tokens ordered with the most preferred
    * in the lower indexes, so index 0 is has highest preference.
    *
    * @param list this is the list of individual header values
    *
    * @return ordered list of tokens extracted from the header(s)
    */
   public List<String> getValues(List<String> list) {
      return new ValueParser(list).list();
   }

   /**
    * This is used to acquire all the individual header values from
    * the message. The header values provided by this are unparsed
    * and represent the actual string values that have been added to
    * the message keyed by a given header name.
    * 
    * @param name the name of the header to get the values for
    * 
    * @return this returns a list of the values for the header name
    */
   public List<String> getAll(String name) {
      String token = name.toLowerCase();
      Entry entry = values.get(token);

      if(entry == null) {
         return getAll(name, token);
      }
      return entry.getValues();
   }

   /**
    * This is used to acquire all the individual header values from
    * the message. The header values provided by this are unparsed
    * and represent the actual string values that have been added to
    * the message keyed by a given header name.
    * 
    * @param name the name of the header to get the values for
    * @param token this provides a lower case version of the header
    * 
    * @return this returns a list of the values for the header name
    */
   private List<String> getAll(String name, String token) {
      Entry entry = new Entry(name);
      String value = names.get(token);

      if(value == null) {
         names.put(token, name);
      }
      values.put(token, entry);

      return entry.getValues();
   }

   /**
    * This is used to remove the named header from the response. This
    * removes all header values assigned to the specified name. If it
    * does not exist then this will return without modifying the HTTP
    * response. Headers names removed are case insensitive.
    *
    * @param name the HTTP message header to remove from the response
    */
   public void remove(String name) {
      String token = name.toLowerCase();
      String value = names.get(token);

      if(value != null) {
         names.remove(token);
      }
      values.remove(token);
   }

   /**
    * This is used to see if there is a HTTP message header with the
    * given name in this container. If there is a HTTP message header
    * with the specified name then this returns true otherwise false.
    *
    * @param name the HTTP message header to get the value from
    *
    * @return this returns true if the HTTP message header exists
    */
   public boolean contains(String name) {
      return !getAll(name).isEmpty();
   }

   /**
    * The <code>Entry</code> object is used to represent a list of
    * HTTP header value for a given name. It allows multiple values
    * to exist for a given header, such as the Cookie header. Most
    * entries will contain a single value.
    * 
    * @author Niall Gallagher
    */
   private class Entry {

      /**
       * Contains the header values that belong to the entry name. 
       */
      private List<String> value;

      /**
       * Contains the unformatted header name for this entry.
       */
      private String name;

      /**
       * Constructor for the <code>Entry</code> object. The entry is
       * created using the name of the HTTP header. Values can be
       * added to the entry list in order to build up the header.
       * 
       * @param name this is the name of the HTTP header to use
       */
      public Entry(String name) {
         this.value = new ArrayList<String>(2);
         this.name = name;
      }

      /**
       * This is used to acquire the unformatted name of the header.
       * The name provided by this method is used to compose the
       * HTTP header by appending a semicolon and the header value.
       * 
       * @return this returns the unformatted header name
       */
      public String getName() {
         return name;
      }

      /**
       * This returns the list of header values associated with the
       * header name. Each value is added as an individual header
       * prefixed by the header name and a semicolon character.
       * 
       * @return this returns the list of values for the header
       */
      public List<String> getValues() {
         return value;
      }
   }
}
