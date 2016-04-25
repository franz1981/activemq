/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.activemq.transport.mqtt;

import org.apache.activemq.util.ByteArrayInputStream;
import org.apache.activemq.util.ByteArrayOutputStream;
import org.apache.activemq.util.ByteSequence;
import org.apache.activemq.wireformat.WireFormat;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.mqtt.codec.MQTTFrame;

import java.io.*;

/**
 * Implements marshalling and unmarsalling the <a
 * href="http://mqtt.org/">MQTT</a> protocol.
 */
public class MQTTWireFormat implements WireFormat {

    static final int MAX_MESSAGE_LENGTH = 1024 * 1024 * 256;
    static final long DEFAULT_CONNECTION_TIMEOUT = 30000L;

    private int version = 1;

    private int maxFrameSize = MAX_MESSAGE_LENGTH;
    private long connectAttemptTimeout = MQTTWireFormat.DEFAULT_CONNECTION_TIMEOUT;

    @Override
    public ByteSequence marshal(Object command) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        marshal(command, dos);
        dos.close();
        return baos.toByteSequence();
    }

    @Override
    public Object unmarshal(ByteSequence packet) throws IOException {
        ByteArrayInputStream stream = new ByteArrayInputStream(packet);
        DataInputStream dis = new DataInputStream(stream);
        return unmarshal(dis);
    }

    private static void writeLength(int length,DataOutput dataOut) throws IOException {
        do {
            //masks the first 7 LSB of remaining -> digit>=0
            byte digit = (byte) (length & 0x7F);
            //skip the masked bits
            length >>>= 7;
            //need at least 1 more byte to represents the length
            if (length > 0) {
                //add the length continuation bit to the actual digit
                digit |= 0x80;
            }
            dataOut.write(digit);
        } while (length > 0);
    }

    @Override
    public void marshal(Object command, DataOutput dataOut) throws IOException {
        MQTTFrame frame = (MQTTFrame) command;
        dataOut.write(frame.header());

        int remaining = 0;
        for (Buffer buffer : frame.buffers) {
            remaining += buffer.length;
        }
        writeLength(remaining,dataOut);
        for (Buffer buffer : frame.buffers) {
            dataOut.write(buffer.data, buffer.offset, buffer.length);
        }
    }

    private static int readLength(DataInput dataIn) throws IOException {
        byte digit;
        int length = 0;
        int shift = 0;
        do {
            digit = dataIn.readByte();
            //digit & 0x7F === use only the first 7 LSB of digit -> EVER POSITIVE!!!
            //<<shift === multiply by (1<<shift)
            length += ((digit & 0x7F) << shift);
            shift+=7;
        }while (digit > 0);
        return length;
    }

    @Override
    public Object unmarshal(DataInput dataIn) throws IOException {
        final byte header = dataIn.readByte();
        final int length = readLength(dataIn);
        if (length >= 0) {
            if (length > getMaxFrameSize()) {
                throw new IOException("The maximum message length was exceeded");
            }
            if (length > 0) {
                final byte[] data = new byte[length];
                dataIn.readFully(data);
                final Buffer body = new Buffer(data);
                return new MQTTFrame(body).header(header);
            } else {
                return new MQTTFrame().header(header);
            }
        }
        return null;
    }

    /**
     * @return the version of the wire format
     */
    @Override
    public int getVersion() {
        return this.version;
    }

    /**
     * @param version the version of the wire format
     */
    @Override
    public void setVersion(int version) {
        this.version = version;
    }

    /**
     * @return the maximum number of bytes a single MQTT message frame is allowed to be.
     */
    public int getMaxFrameSize() {
        return maxFrameSize;
    }

    /**
     * Sets the maximum frame size for an incoming MQTT frame.  The protocl limit is
     * 256 megabytes and this value cannot be set higher.
     *
     * @param maxFrameSize the maximum allowed frame size for a single MQTT frame.
     */
    public void setMaxFrameSize(int maxFrameSize) {
        this.maxFrameSize = Math.min(MAX_MESSAGE_LENGTH, maxFrameSize);
    }

    /**
     * @return the timeout value used to fail a connection if no CONNECT frame read.
     */
    public long getConnectAttemptTimeout() {
        return connectAttemptTimeout;
    }

    /**
     * Sets the timeout value used to fail a connection if no CONNECT frame is read
     * in the given interval.
     *
     * @param connectTimeout the connection frame received timeout value.
     */
    public void setConnectAttemptTimeout(long connectTimeout) {
        this.connectAttemptTimeout = connectTimeout;
    }
}
