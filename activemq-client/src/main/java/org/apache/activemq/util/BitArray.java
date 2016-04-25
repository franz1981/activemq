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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;

/**
 * Simple BitArray to enable setting multiple boolean values efficently Used
 * instead of BitSet because BitSet does not allow for efficent serialization.
 * Will store up to 64 boolean values
 * 
 * 
 */
public class BitArray implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    static final int LONG_SIZE = 64;
    static final int INT_SIZE = 32;
    static final int SHORT_SIZE = 16;
    static final int BYTE_SIZE = 8;

    private long bits;
    private int length;

    /**
     * @return the length of bits set
     */
    public int length() {
        return length;
    }

    /**
     * @return the long containing the bits
     */
    public long getBits() {
        return bits;
    }

    /**
     * set the boolean value at the index
     * 
     * @param index
     * @param flag
     * @return the old value held at this index
     */
    public boolean set(int index, boolean flag) {
        length = Math.max(length, index + 1);
        final long indexMask = 1L<<index;
        boolean oldValue = (bits & indexMask) != 0;
        if (flag) {
            bits |= indexMask;
        } else if (oldValue) {
            bits &= ~(indexMask);
        }
        return oldValue;
    }

    /**
     * @param index
     * @return the boolean value at this index
     */
    public boolean get(int index) {
        final long indexMask = 1L<<index;
        return (bits & indexMask) != 0;
    }

    /**
     * reset all the bit values to false
     */
    public void reset() {
        bits = 0;
    }

    /**
     * reset all the bits to the value supplied
     * 
     * @param bits
     */
    public void reset(long bits) {
        this.bits = bits;
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        writeToStream(out);
    }
    
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        readFromStream(in);
    }
    
    /**
     * write the bits to an output stream
     * 
     * @param dataOut
     * @throws IOException
     */
    public void writeToStream(DataOutput dataOut) throws IOException {
        dataOut.writeByte(length);
        if (length <= BYTE_SIZE) {
            dataOut.writeByte((byte)bits);
        } else if (length <= SHORT_SIZE) {
            dataOut.writeShort((short)bits);
        } else if (length <= INT_SIZE) {
            dataOut.writeInt((int)bits);
        } else {
            dataOut.writeLong(bits);
        }
    }

    /**
     * read the bits from an input stream
     * 
     * @param dataIn
     * @throws IOException
     */
    public void readFromStream(DataInput dataIn) throws IOException {
        length = dataIn.readByte();
        if (length <= BYTE_SIZE) {
            bits = dataIn.readByte();
        } else if (length <= SHORT_SIZE) {
            bits = dataIn.readShort();
        } else if (length <= INT_SIZE) {
            bits = dataIn.readInt();
        } else {
            bits = dataIn.readLong();
        }
    }
}
