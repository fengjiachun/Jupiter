/*
 * Copyright (c) 2015 The Jupiter Project
 *
 * Licensed under the Apache License, version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jupiter.rpc.load.balance;

import java.util.List;

import org.jupiter.common.util.SystemClock;
import org.jupiter.transport.Directory;
import org.jupiter.transport.UnresolvedAddress;
import org.jupiter.transport.channel.JChannel;
import org.jupiter.transport.channel.JChannelGroup;

/**
 * jupiter
 * org.jupiter.rpc.load.balance
 *
 * @author jiachun.fjc
 */
public class ChannelGroup implements JChannelGroup {
    public int index;
    public int weight;

    public volatile long timestamp = SystemClock.millisClock().now();

    @Override
    public String toString() {
        return "Channel{" +
                "index=" + index +
                ", weight=" + weight +
                '}';
    }

    @Override
    public UnresolvedAddress remoteAddress() {
        return null;
    }

    @Override
    public JChannel next() {
        return null;
    }

    @Override
    public List<? extends JChannel> channels() {
        return null;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean add(JChannel channel) {
        return false;
    }

    @Override
    public boolean remove(JChannel channel) {
        return false;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public void setCapacity(int capacity) {}

    @Override
    public int getCapacity() {
        return 0;
    }

    @Override
    public boolean isConnecting() {
        return false;
    }

    @Override
    public void setConnecting(boolean connecting) {}

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public boolean waitForAvailable(long timeoutMillis) {
        return false;
    }

    @Override
    public void onAvailable(Runnable callback) {}

    @Override
    public int getWeight(Directory directory) {
        return weight;
    }

    @Override
    public void putWeight(Directory directory, int weight) {}

    @Override
    public void removeWeight(Directory directory) {}

    @Override
    public int getWarmUp() {
        return 0;
    }

    @Override
    public void setWarmUp(int warmUp) {

    }

    @Override
    public boolean isWarmUpComplete() {
        return true;
    }

    @Override
    public long timestamp() {
        return timestamp;
    }

    @Override
    public long deadlineMillis() {
        return 0;
    }
}
