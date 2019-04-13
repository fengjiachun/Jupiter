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
package org.jupiter.benchmark.serialization;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.util.internal.PlatformDependent;

import org.jupiter.common.util.Lists;
import org.jupiter.serialization.Serializer;
import org.jupiter.serialization.SerializerFactory;
import org.jupiter.serialization.SerializerType;
import org.jupiter.serialization.io.InputBuf;
import org.jupiter.serialization.io.OutputBuf;
import org.jupiter.transport.netty.alloc.AdaptiveOutputBufAllocator;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class SerializationBenchmark {

    /*
        Benchmark                                     Mode  Cnt    Score    Error   Units
        SerializationBenchmark.hessianByteBuffer     thrpt   10  157.109 ±  7.713  ops/ms
        SerializationBenchmark.hessianBytesArray     thrpt   10  141.974 ± 20.391  ops/ms
        SerializationBenchmark.javaByteBuffer        thrpt   10   25.612 ±  2.624  ops/ms
        SerializationBenchmark.javaBytesArray        thrpt   10   26.867 ±  1.192  ops/ms
        SerializationBenchmark.kryoByteBuffer        thrpt   10  335.027 ± 12.766  ops/ms
        SerializationBenchmark.kryoBytesArray        thrpt   10  445.021 ± 15.940  ops/ms
        SerializationBenchmark.protoStuffByteBuffer  thrpt   10  889.150 ± 20.713  ops/ms
        SerializationBenchmark.protoStuffBytesArray  thrpt   10  727.444 ± 27.414  ops/ms
        SerializationBenchmark.hessianByteBuffer      avgt   10    0.006 ±  0.001   ms/op
        SerializationBenchmark.hessianBytesArray      avgt   10    0.006 ±  0.001   ms/op
        SerializationBenchmark.javaByteBuffer         avgt   10    0.036 ±  0.001   ms/op
        SerializationBenchmark.javaBytesArray         avgt   10    0.036 ±  0.002   ms/op
        SerializationBenchmark.kryoByteBuffer         avgt   10    0.003 ±  0.001   ms/op
        SerializationBenchmark.kryoBytesArray         avgt   10    0.002 ±  0.001   ms/op
        SerializationBenchmark.protoStuffByteBuffer   avgt   10    0.001 ±  0.001   ms/op
        SerializationBenchmark.protoStuffBytesArray   avgt   10    0.001 ±  0.001   ms/op
     */

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(SerializationBenchmark.class.getSimpleName())
                .build();

        new Runner(opt).run();
    }

    private static final Serializer javaSerializer = SerializerFactory.getSerializer(SerializerType.JAVA.value());
    private static final Serializer hessianSerializer = SerializerFactory.getSerializer(SerializerType.HESSIAN.value());
    private static final Serializer protoStuffSerializer = SerializerFactory.getSerializer(SerializerType.PROTO_STUFF.value());
    private static final Serializer kryoSerializer = SerializerFactory.getSerializer(SerializerType.KRYO.value());

    private static final AdaptiveOutputBufAllocator.Handle allocHandle = AdaptiveOutputBufAllocator.DEFAULT.newHandle();
    private static final ByteBufAllocator allocator = new PooledByteBufAllocator(PlatformDependent.directBufferPreferred());

    static int USER_COUNT = 1;

    @Benchmark
    public void javaBytesArray() {
        // 写入
        byte[] bytes = javaSerializer.writeObject(createUsers(USER_COUNT));
        ByteBuf byteBuf = allocator.buffer(bytes.length);
        byteBuf.writeBytes(bytes);

        // 网络传输都是相同的条件

        // 读出
        bytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes);
        javaSerializer.readObject(bytes, Users.class);

        // 释放
        byteBuf.release();
    }

    @Benchmark
    public void javaByteBuffer() {
        // 写入
        OutputBuf outputBuf = javaSerializer.writeObject(new NettyOutputBuf(allocHandle, allocator), createUsers(USER_COUNT));

        // 网络传输都是相同的条件

        // 读出
        InputBuf inputBuf = new NettyInputBuf((ByteBuf) outputBuf.backingObject());
        javaSerializer.readObject(inputBuf, Users.class);
    }

    @Benchmark
    public void hessianBytesArray() {
        // 写入
        byte[] bytes = hessianSerializer.writeObject(createUsers(USER_COUNT));
        ByteBuf byteBuf = allocator.buffer(bytes.length);
        byteBuf.writeBytes(bytes);

        // 网络传输都是相同的条件

        // 读出
        bytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes);
        hessianSerializer.readObject(bytes, Users.class);

        // 释放
        byteBuf.release();
    }

    @Benchmark
    public void hessianByteBuffer() {
        // 写入
        OutputBuf outputBuf = hessianSerializer.writeObject(new NettyOutputBuf(allocHandle, allocator), createUsers(USER_COUNT));

        // 网络传输都是相同的条件

        // 读出
        InputBuf inputBuf = new NettyInputBuf((ByteBuf) outputBuf.backingObject());
        hessianSerializer.readObject(inputBuf, Users.class);
    }

    @Benchmark
    public void protoStuffBytesArray() {
        // 写入
        byte[] bytes = protoStuffSerializer.writeObject(createUsers(USER_COUNT));
        ByteBuf byteBuf = allocator.buffer(bytes.length);
        byteBuf.writeBytes(bytes);

        // 网络传输都是相同的条件

        // 读出
        bytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes);
        protoStuffSerializer.readObject(bytes, Users.class);

        // 释放
        byteBuf.release();
    }

    @Benchmark
    public void protoStuffByteBuffer() {
        // 写入
        OutputBuf outputBuf = protoStuffSerializer.writeObject(new NettyOutputBuf(allocHandle, allocator), createUsers(USER_COUNT));

        // 网络传输都是相同的条件

        // 读出
        InputBuf inputBuf = new NettyInputBuf((ByteBuf) outputBuf.backingObject());
        protoStuffSerializer.readObject(inputBuf, Users.class);
    }

    @Benchmark
    public void kryoBytesArray() {
        // 写入
        byte[] bytes = kryoSerializer.writeObject(createUsers(USER_COUNT));
        ByteBuf byteBuf = allocator.buffer(bytes.length);
        byteBuf.writeBytes(bytes);

        // 网络传输都是相同的条件

        // 读出
        bytes = new byte[byteBuf.readableBytes()];
        byteBuf.readBytes(bytes);
        kryoSerializer.readObject(bytes, Users.class);

        // 释放
        byteBuf.release();
    }

    @Benchmark
    public void kryoByteBuffer() {
        // 写入
        OutputBuf outputBuf = kryoSerializer.writeObject(new NettyOutputBuf(allocHandle, allocator), createUsers(USER_COUNT));

        // 读出
        InputBuf inputBuf = new NettyInputBuf((ByteBuf) outputBuf.backingObject());
        kryoSerializer.readObject(inputBuf, Users.class);
    }

    static final class NettyInputBuf implements InputBuf {

        private final ByteBuf byteBuf;

        NettyInputBuf(ByteBuf byteBuf) {
            this.byteBuf = byteBuf;
        }

        @Override
        public InputStream inputStream() {
            return new ByteBufInputStream(byteBuf); // should not be called more than once
        }

        @Override
        public ByteBuffer nioByteBuffer() {
            return byteBuf.nioBuffer(); // should not be called more than once
        }

        @Override
        public int size() {
            return byteBuf.readableBytes();
        }

        @Override
        public boolean hasMemoryAddress() {
            return byteBuf.hasMemoryAddress();
        }

        @Override
        public boolean release() {
            return byteBuf.release();
        }
    }

    static final class NettyOutputBuf implements OutputBuf {

        private final AdaptiveOutputBufAllocator.Handle allocHandle;
        private final ByteBuf byteBuf;
        private ByteBuffer nioByteBuffer;

        public NettyOutputBuf(AdaptiveOutputBufAllocator.Handle allocHandle, ByteBufAllocator alloc) {
            this.allocHandle = allocHandle;
            byteBuf = allocHandle.allocate(alloc);
        }

        @Override
        public OutputStream outputStream() {
            return new ByteBufOutputStream(byteBuf); // should not be called more than once
        }

        @Override
        public ByteBuffer nioByteBuffer(int minWritableBytes) {
            if (minWritableBytes < 0) {
                minWritableBytes = byteBuf.writableBytes();
            }

            if (nioByteBuffer == null) {
                nioByteBuffer = newNioByteBuffer(byteBuf, minWritableBytes);
            }

            if (nioByteBuffer.remaining() >= minWritableBytes) {
                return nioByteBuffer;
            }

            int position = nioByteBuffer.position();

            nioByteBuffer = newNioByteBuffer(byteBuf, position + minWritableBytes);

            nioByteBuffer.position(position);

            return nioByteBuffer;
        }

        @Override
        public int size() {
            if (nioByteBuffer == null) {
                return byteBuf.readableBytes();
            }
            return Math.max(byteBuf.readableBytes(), nioByteBuffer.position());
        }

        @Override
        public boolean hasMemoryAddress() {
            return byteBuf.hasMemoryAddress();
        }

        @Override
        public Object backingObject() {
            int actualWriteBytes = byteBuf.writerIndex();
            if (nioByteBuffer != null) {
                actualWriteBytes += nioByteBuffer.position();
            }

            allocHandle.record(actualWriteBytes);

            return byteBuf.writerIndex(actualWriteBytes);
        }

        private static ByteBuffer newNioByteBuffer(ByteBuf byteBuf, int writableBytes) {
            return byteBuf
                    .ensureWritable(writableBytes)
                    .nioBuffer(byteBuf.writerIndex(), byteBuf.writableBytes());
        }
    }

    static Users createUsers(int count) {
        List<User> userList = Lists.newArrayListWithCapacity(count);
        for (int i = 0; i < count; i++) {
            userList.add(createUser());
        }
        Users users = new Users();
        users.setUsers(userList);
        return users;
    }

    static User createUser() {
        User user = new User();
        user.setId(1L);
        user.setName("block");
        user.setSex(0);
        user.setBirthday(new Date());
        user.setEmail("xxx@alibaba-inc.con");
        user.setMobile("18325038521");
        user.setAddress("浙江省 杭州市 文一西路969号");
        List<Integer> permsList = Lists.newArrayList(
                1, 12, 123
//                , Integer.MAX_VALUE, Integer.MAX_VALUE - 1, Integer.MAX_VALUE - 2
//                , Integer.MIN_VALUE, Integer.MIN_VALUE + 1, Integer.MIN_VALUE + 2
        );
        user.setPermissions(permsList);
        user.setStatus(1);
        user.setCreateTime(new Date());
        user.setUpdateTime(new Date());
        return user;
    }

    static class Users implements Serializable {

        private List<User> users;

        public List<User> getUsers() {
            return users;
        }

        public void setUsers(List<User> users) {
            this.users = users;
        }
    }

    static class User implements Serializable {

        private long id;
        private String name;
        private int sex;
        private Date birthday;
        private String email;
        private String mobile;
        private String address;
        private List<Integer> permissions;
        private int status;
        private Date createTime;
        private Date updateTime;

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getSex() {
            return sex;
        }

        public void setSex(int sex) {
            this.sex = sex;
        }

        public Date getBirthday() {
            return birthday;
        }

        public void setBirthday(Date birthday) {
            this.birthday = birthday;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getMobile() {
            return mobile;
        }

        public void setMobile(String mobile) {
            this.mobile = mobile;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public List<Integer> getPermissions() {
            return permissions;
        }

        public void setPermissions(List<Integer> permissions) {
            this.permissions = permissions;
        }

        public int getStatus() {
            return status;
        }

        public void setStatus(int status) {
            this.status = status;
        }

        public Date getCreateTime() {
            return createTime;
        }

        public void setCreateTime(Date createTime) {
            this.createTime = createTime;
        }

        public Date getUpdateTime() {
            return updateTime;
        }

        public void setUpdateTime(Date updateTime) {
            this.updateTime = updateTime;
        }

        @Override
        public String toString() {
            return "User{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    ", sex=" + sex +
                    ", birthday=" + birthday +
                    ", email='" + email + '\'' +
                    ", mobile='" + mobile + '\'' +
                    ", address='" + address + '\'' +
                    ", permissions=" + permissions +
                    ", status=" + status +
                    ", createTime=" + createTime +
                    ", updateTime=" + updateTime +
                    '}';
        }
    }
}
