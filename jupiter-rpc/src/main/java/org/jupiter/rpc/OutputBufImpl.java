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

package org.jupiter.rpc;

import org.jupiter.serialization.OutputBuf;
import org.jupiter.transport.channel.JChannel;

import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * jupiter
 * org.jupiter.rpc
 *
 * @author jiachun.fjc
 */
public class OutputBufImpl implements OutputBuf {

    private final JChannel.ChannelOutput output;

    public OutputBufImpl(JChannel.ChannelOutput output) {
        this.output = output;
    }

    @Override
    public OutputStream outputStream() {
        return output.outputStream();
    }

    @Override
    public ByteBuffer nioByteBuffer(int minWritableBytes) {
        return output.nioByteBuffer(minWritableBytes);
    }

    @Override
    public Object attach() {
        return output.attach();
    }

    @Override
    public int size() {
        return output.size();
    }
}
