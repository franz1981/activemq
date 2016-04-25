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

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

public class IntSequenceGenerator {

    //note: the updater is preferrable to an AtomicInteger due to the reduced generation of instances
    private static final AtomicIntegerFieldUpdater<IntSequenceGenerator> LAST_SEQUENCE_ID_UPDATER = AtomicIntegerFieldUpdater.newUpdater(IntSequenceGenerator.class,"lastSequenceId");

    private volatile int lastSequenceId;

    public int getNextSequenceId() {
        return LAST_SEQUENCE_ID_UPDATER.incrementAndGet(this);
    }

    public int getLastSequenceId() {
        return LAST_SEQUENCE_ID_UPDATER.get(this);
    }

    public void setLastSequenceId(int l) {
        LAST_SEQUENCE_ID_UPDATER.set(this,l);
    }
}
