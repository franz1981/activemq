/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.util;

import java.io.OutputStream;
import java.util.ArrayList;


/**
 * Very similar to the java.io.ByteArrayOutputStream but this version 
 * is not thread safe and the resulting data is returned in a ByteSequence
 * to avoid an extra byte[] allocation.
 */
public class ByteArrayOutputStream extends OutputStream {

    /**
     * Limits the number of bytes to copy to allow safepoint polling during a large copy.
     */
    private static final int COPY_THRESHOLD = 1024 * 1024;
    private static final int DEFAULT_CAPACITY = 1028;
    /**
     * Shared empty array instance used for empty instances.
     */
    private static final byte[] EMPTY_BUFFER = {};
    /**
     * Shared empty array instance used for default sized empty instances. We
     * distinguish this from EMPTY_ELEMENTDATA to know how much to inflate when
     * first element is added.
     */
    private static final byte[] DEFAULTCAPACITY_EMPTY_BUFFER = EMPTY_BUFFER;
    /**
     * The maximum size of array to allocate.
     * Some VMs reserve some header words in an array.
     * Attempts to allocate larger arrays may result in
     * OutOfMemoryError: Requested array size exceeds VM limit
     */
    private static final int MAX_ARRAY_SIZE = Integer.MAX_VALUE - 8;
    byte buffer[];
    int size;

    public ByteArrayOutputStream() {
        this.buffer = DEFAULTCAPACITY_EMPTY_BUFFER;
    }

    public ByteArrayOutputStream(int capacity) {
        if (capacity > 0) {
            this.buffer = new byte[capacity];
        } else if (capacity == 0) {
            this.buffer = EMPTY_BUFFER;
        } else {
            throw new IllegalArgumentException("Illegal Capacity: "+ capacity);
        }
    }

    private static void copyBytes(byte[] src, int srcPos,byte[] dest, int destPos,int length) {
        while (length > 0) {
            int size = Math.min(length, COPY_THRESHOLD);
            System.arraycopy(src,srcPos,dest,destPos,size);
            length -= size;
            srcPos += size;
            destPos += size;
        }
    }

    private static int hugeCapacity(int minCapacity) {
        if (minCapacity < 0) // overflow
            throw new OutOfMemoryError();
        return (minCapacity > MAX_ARRAY_SIZE) ?
                Integer.MAX_VALUE :
                MAX_ARRAY_SIZE;
    }

    public void write(int b) {
        final int newsize = size + 1;
        ensureCapacityInternal(newsize);
        buffer[size] = (byte) b;
        size = newsize;
    }

    public void write(byte b[], int off, int len) {
        final int newsize = size + len;
        ensureCapacityInternal(newsize);
        copyBytes(b, off, buffer, size, len);
        size = newsize;
    }

    /**
     * Increases the capacity of this <tt>ByteArrayOutputStream</tt> instance, if
     * necessary, to ensure that it can hold at least the number of bytes
     * specified by the minimum capacity argument.
     *
     * @param   minCapacity   the desired minimum capacity
     */
    public void ensureCapacity(int minCapacity) {
        final int minExpand = (buffer != DEFAULTCAPACITY_EMPTY_BUFFER)
                // any size if not default empty buffer
                ? 0
                // larger than default for default empty buffer. It's already
                // supposed to be at default size.
                : DEFAULT_CAPACITY;
        if (minCapacity > minExpand) {
            ensureExplicitCapacity(minCapacity);
        }
    }

    private void ensureCapacityInternal(int minCapacity) {
        if (buffer == DEFAULTCAPACITY_EMPTY_BUFFER) {
            minCapacity = Math.max(DEFAULT_CAPACITY, minCapacity);
        }
        ensureExplicitCapacity(minCapacity);
    }

    private void ensureExplicitCapacity(int minCapacity) {
        // overflow-conscious code
        if (minCapacity - buffer.length > 0)
            grow(minCapacity);
    }

    /**
     * Increases the capacity to ensure that it can hold at least the
     * number of elements specified by the minimum capacity argument.
     *
     * @param minCapacity the desired minimum capacity
     */
    private void grow(int minCapacity) {
        // overflow-conscious code
        final int oldCapacity = buffer.length;
        int newCapacity = oldCapacity + (oldCapacity >> 1);
        if (newCapacity - minCapacity < 0)
            newCapacity = minCapacity;
        if (newCapacity - MAX_ARRAY_SIZE > 0)
            newCapacity = hugeCapacity(minCapacity);
        final byte[] newBuffer = new byte[newCapacity];
        copyBytes(buffer,0,newBuffer,0,size);
        this.buffer = newBuffer;
    }

    public void reset() {
        size = 0;
    }

    public ByteSequence toByteSequence() {
        return new ByteSequence(buffer, 0, size);
    }

    public int writeToByteSequence(ByteSequence byteSequence) {
        //fast-path
        if(size>0 && byteSequence.length>0){
            return writeToByteArray(byteSequence.data,byteSequence.offset);
        }else{
            return 0;
        }
    }

    public int writeToByteArray(byte[] dest,int destPos) {
        if(size>0) {
            final int destRemaining = dest.length - destPos;
            final int copied = Math.min(size,destRemaining);
            if(copied>0) {
                copyBytes(buffer, 0, dest, destPos, copied);
            }
            return copied;
        }else {
            return 0;
        }
    }
    
    public byte[] toByteArray() {
        if(size>0) {
            final byte rc[] = new byte[size];
            copyBytes(buffer, 0, rc, 0, size);
            return rc;
        }else{
            return EMPTY_BUFFER;
        }
    }
    
    public int size() {
        return size;
    }

    public boolean endsWith(final byte[] array) {
        int i = 0;
        int start = size - array.length;
        if (start < 0) {
            return false;
        }
        while (start < size) {
            if (buffer[start++] != array[i++]) {
                return false;
            }
        }
        return true;
    }
}
