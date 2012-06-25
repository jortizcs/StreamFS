/*
 * Wrapper.java February 2008
 *
 * Copyright (C) 2008, Niall Gallagher <niallg@users.sf.net>
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

package org.simpleframework.transport;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ByteChannel;
import java.nio.charset.Charset;

/**
 * The <code>Wrapper</code> object represents a packet that wraps an
 * unmodifiable buffer. This ensures that the contents of the buffer
 * are not modified during the use of the packet. To ensure that the
 * buffer can not be modified the <code>append</code> methods will
 * always append zero bytes, also the <code>write</code> methods do 
 * no compact the buffers when some content has been written. 
 * 
 * @author Niall Gallagher
 */
class Wrapper implements Packet {

   /**
    * This is the ready only byte buffer that this packet wraps.
    */
   private ByteBuffer buffer;
   
   /**
    * This is the unique sequence number for this packet.
    */
   private long sequence;
   
   /**
    * This determines if the packet has already been closed.
    */
   private boolean closed;
   
   /**
    * This determines if this packet represents a shared one.
    */
   private boolean shared;
   
   /**
    * Constructor for the <code>Wrapper</code> object. This will
    * create a wrapper for the provided buffer which will enable
    * the buffer to be written to a byte channel without being
    * modified by the write process.
    * 
    * @param buffer this is the buffer that is to be wrapped
    * @param sequence this is the sequence number for this packet
    */
   public Wrapper(ByteBuffer buffer, long sequence) {
      this(buffer, sequence, true);
   }
   
   /**
    * Constructor for the <code>Wrapper</code> object. This will
    * create a wrapper for the provided buffer which will enable
    * the buffer to be written to a byte channel without being
    * modified by the write process.
    * 
    * @param buffer this is the buffer that is to be wrapped
    * @param sequence this is the sequence number for this packet
    */
   public Wrapper(ByteBuffer buffer, long sequence, boolean shared) {
      this.sequence = sequence;
      this.buffer = buffer;
      this.shared = shared;
   }

   /**
    * The sequence number represents the order with which this is
    * to be delivered to the underlying network. This allows safer
    * transfer of packets in an asynchronous environment where it 
    * may be possible for a packet to be written out of sequence. 
    * The sequence number also determines the order of closure.
    *
    * @return this returns an increasing packet sequence number
    */ 
   public long sequence() {
      return sequence;
   }

   /**
    * This is used to determine how much space is left to append 
    * data to this packet. This is typically equivilant to capacity
    * minus the length. However in the event that the packet uses 
    * a private memory store that can not be written to then this
    * can return zero regardless of the capacity and length.
    *
    * @return the space left within the buffer to append data to
    */   
   public int space() {
      return 0;
   }

   /**
    * This represents the capacity of the backing store. The buffer
    * is full when length is equal to capacity and it can typically
    * be appended to when the length is less than the capacity. The
    * only exception is when <code>space</code> returns zero, which
    * means that the packet can not have bytes appended to it.
    *
    * @return this is the capacity of other backing byte storage
    */    
   public int capacity() {
      return length();
   }

   /**        
    * This is used to determine how many bytes remain within this
    * packet. It represents the number of write ready bytes, so if
    * the length is greater than zero the packet can be written to
    * a byte channel. When length is zero the packet can be closed.
    * 
    * @return this is the number of bytes remaining in this packet
    */
   public int length() {
      int offset = buffer.position();
      int limit = buffer.limit();
     
      if(closed) {
         return 0;              
      } 
      return limit - offset;
   }
   
   /**
    * This is used to that packets can be entered in to a priority
    * queue such that they are ordered based on their sequence
    * numbers. Ordering based on sequence numbers ensures that
    * packets can be remove and inserted back in to the equeue 
    * without concern for othe order of their insertion.
    * 
    * @param packet this is the packet that is to be compared
    * 
    * @return this is negative is less than otherwise its positive 
    */
   public int compareTo(Packet packet) {
      long other = packet.sequence();
      
      if(other > sequence) {
         return -1;
      } 
      if(sequence > other) {
         return 1;
      }
      return 0;
   }
   
   /**
    * This method is used to extract the contents of the packet in
    * to a duplicate packet. The purpose of this is to ensure that
    * when a packet wraps a shared buffer the contents of that
    * buffer can be drained in to an allocated buffer, resulting
    * in a packet that can be used without read write conflicts.
    *  
    * @return this returns the packets contents in a new buffer
    */
   public Packet extract() throws IOException {
      int length = length();
      
      if(length <= 0) {
         throw new PacketException("Buffer is empty");
      }
      if(!shared) {
         return this;
      }
      return extract(length);
   }
   
   /**
    * This method is used to extract the contents of the packet in
    * to a duplicate packet. The purpose of this is to ensure that
    * when a packet wraps a shared buffer the contents of that
    * buffer can be drained in to an allocated buffer, resulting
    * in a packet that can be used without read write conflicts.
    * 
    * @param size this is the size of the buffer to be extracted
    *  
    * @return this returns the packets contents in a new buffer
    */
   private Packet extract(int size) throws IOException {
      ByteBuffer data = ByteBuffer.allocate(size);
      
      if(size > 0) {
         data.put(buffer);
         data.position(0);
      }
      return new Wrapper(data, sequence, false);
   }

   /**
    * This is used to encode the underlying byte sequence to text.
    * Converting the byte sequence to text can be useful when either
    * debugging what exactly is being sent. Also, for transports 
    * that require string delivery of packets this can be used. 
    *
    * @return this returns the bytes sequence as a string object
    */  
   public String encode() throws IOException {
      return encode("ISO-8859-1"); 
   }
   
   /**
    * This is used to encode the underlying byte sequence to text.
    * Converting the byte sequence to text can be useful when either
    * debugging what exactly is being sent. Also, for transports 
    * that require string delivery of packets this can be used. 
    *
    * @param encoding this is the character set to use for encoding
    *
    * @return this returns the bytes sequence as a string object
    */   
   public String encode(String encoding) throws IOException {
      ByteBuffer segment = buffer.duplicate();
      
      if(segment == null) {
         return new String();
      }
      return encode(encoding, segment);
   }
   
   /**
    * This is used to encode the underlying byte sequence to text.
    * Converting the byte sequence to text can be useful when either
    * debugging what exactly is being sent. Also, for transports 
    * that require string delivery of packets this can be used. 
    *
    * @param encoding this is the character set to use for encoding
    * @param buffer this is the buffer that will be encoded
    *
    * @return this returns the bytes sequence as a string object
    */   
   private String encode(String encoding, ByteBuffer buffer) throws IOException {
      Charset charset = Charset.forName(encoding); 
      CharBuffer text = charset.decode(buffer);

      return text.toString();
   }

   /**
    * This will not append any bytes to the packet. Because this is
    * an immutable implementation of the <code>Packet</code> it can
    * not modify the underlying buffer. So this will simply return
    * having made no changes to either the buffer of the packet.
    *
    * @param buffer this is the buffer containing the bytes
    *
    * @return returns the number of bytes that have been moved
    */    
   public int append(ByteBuffer buffer) throws IOException {
      return append(buffer, 0);
   }

   /**
    * This will not append any bytes to the packet. Because this is
    * an immutable implementation of the <code>Packet</code> it can
    * not modify the underlying buffer. So this will simply return
    * having made no changes to either the buffer of the packet.
    *
    * @param buffer this is the buffer containing the bytes
    * @param count this is the number of bytes that should be used
    *
    * @return returns the number of bytes that have been moved
    */    
   public int append(ByteBuffer buffer, int count) throws IOException {
      if(closed) {
        throw new PacketException("Packet is closed");              
      }
      return 0;
   }
   
   /**
    * This write method will write the contents of the packet to the
    * provided byte channel. If the whole packet can be be written
    * then this will simply return the number of bytes that have. 
    * The number of bytes remaining within the packet after a write
    * can be acquired from the <code>length</code> method. Once all
    * of the bytes are written the packet must be closed.
    *
    * @param channel this is the channel to write the packet to
    *
    * @return this returns the number of bytes that were written
    */ 
   public int write(ByteChannel channel) throws IOException {
      int size = length();     

      if(closed) {
         throw new PacketException("Packet is closed");              
      }
      if(size <= 0) { 
         return 0;
      }
      return write(channel, size);
   }

   /**
    * This write method will write the contents of the packet to the
    * provided byte channel. If the whole packet can be be written
    * then this will simply return the number of bytes that have. 
    * The number of bytes remaining within the packet after a write
    * can be acquired from the <code>length</code> method. Once all
    * of the bytes are written the packet must be closed.
    *
    * @param channel this is the channel to write the packet to
    * @param count the number of bytes to write to the channel
    *
    * @return this returns the number of bytes that were written
    */ 
   public int write(ByteChannel channel, int count) throws IOException {
      if(closed) {
         throw new PacketException("Packet is closed");              
      }           
      return write(channel, buffer);
   }

   /**
    * This write method will write the contents of the packet to the
    * provided byte channel. If the whole packet can be be written
    * then this will simply return the number of bytes that have. 
    * The number of bytes remaining within the packet after a write
    * can be acquired from the <code>length</code> method. Once all
    * of the bytes are written the packet must be closed.
    *
    * @param channel this is the channel to write the packet to
    * @param segment this is the segment that is to be written
    *
    * @return this returns the number of bytes that were written
    */ 
   private int write(ByteChannel channel, ByteBuffer segment) throws IOException {
      int require = segment.remaining();
      int count = 0;
      
      while(count < require) { 
         int size = channel.write(segment);

         if(size <= 0) {
            break;
         }
         count += size;
      }
      return count;

   }
   
   /**
    * This method is used to determine if the buffer is shared with
    * another thread or service. It is important to know whether a
    * packet is shared as it tells the writer whether it needs to
    * block the writing thread whilst the packet is pending a write
    * to the socket channel.
    * 
    * @return true if the buffer is shared with another service
    */
   public boolean isReference() {
      return shared;
   }

   /**
    * The <code>close</code> method for the packet is used to ensure
    * that any resources occupied by the packet are released. This
    * can be subclassed to introduce such functionality, however the
    * current implementation does not hold any releasable resources.
    */    
   public void close() throws IOException {
      closed = true;
   }
   
   /**
    * Provides a string representation of the state of the packet. 
    * This can be useful for debugging the state transitions that a
    * packet will go through when being written and appended to.
    *
    * @return this returns a string representation for the packet
    */    
   @Override
   public String toString() {
      return String.format("%s %s", sequence, buffer);
   }

}
