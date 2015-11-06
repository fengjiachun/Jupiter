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

package org.jupiter.rpc.consumer;

import org.jupiter.common.concurrent.atomic.AtomicUpdater;
import org.jupiter.common.util.Lists;
import org.jupiter.common.util.Reflects;
import org.jupiter.common.util.Strings;
import org.jupiter.common.util.internal.Recyclers;
import org.jupiter.common.util.internal.logging.InternalLogger;
import org.jupiter.common.util.internal.logging.InternalLoggerFactory;
import org.jupiter.rpc.*;
import org.jupiter.rpc.annotation.ServiceProvider;
import org.jupiter.rpc.aop.ConsumerHook;
import org.jupiter.rpc.consumer.dispatcher.DefaultBroadcastDispatcher;
import org.jupiter.rpc.consumer.dispatcher.DefaultRoundDispatcher;
import org.jupiter.rpc.consumer.dispatcher.Dispatcher;
import org.jupiter.rpc.consumer.invoker.AsyncInvoker;
import org.jupiter.rpc.consumer.invoker.SyncInvoker;
import org.jupiter.rpc.model.metadata.MessageWrapper;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import static org.jupiter.common.util.JConstants.UNKNOWN_APP_NAME;
import static org.jupiter.common.util.Preconditions.checkArgument;
import static org.jupiter.common.util.Preconditions.checkNotNull;
import static org.jupiter.rpc.AsyncMode.ASYNC_CALLBACK;
import static org.jupiter.rpc.AsyncMode.SYNC;
import static org.jupiter.rpc.DispatchMode.BROADCAST;
import static org.jupiter.rpc.DispatchMode.ROUND;

/**
 * ProxyFactory是池化的, 每次 {@link #create()} 即可, 使用完后会自动回收, 不要创建一个反复使用.
 *
 * jupiter
 * org.jupiter.rpc.consumer
 *
 * @author jiachun.fjc
 */
public class ProxyFactory {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(ProxyFactory.class);

    private static final AtomicIntegerFieldUpdater<ProxyFactory> updater;
    static {
        updater = AtomicUpdater.newAtomicIntegerFieldUpdater(ProxyFactory.class, "recycled");
    }
    // 个别使用者可能喜欢频繁创建ProxyFactory实例, 相比较AtomicBoolean, 使用AtomicIntegerFieldUpdater来更新volatile int的方式
    // 在64位虚拟机环境中会节省12(开启压缩指针的情况下)个字节的对象头大小.
    // http://hg.openjdk.java.net/jdk7u/jdk7u/hotspot/file/6e9aa487055f/src/share/vm/oops/klass.hpp
    //  [header         ] 8  byte
    //  [klass pointer  ] 8  byte (4 byte for compressed-oops)
    private volatile int recycled = 0; // 0: 可用; 1: 不可用(已被回收)

    private JClient connector;
    private List<UnresolvedAddress> addresses;
    private String appName;
    private Class<?> serviceInterface;
    private AsyncMode asyncMode = SYNC;
    private DispatchMode dispatchMode = ROUND;
    private int timeoutMills;
    private List<ConsumerHook> hooks;
    private JListener listener;

    public static ProxyFactory create() {
        ProxyFactory fac = recyclers.get();

        // 初始化数据
        fac.appName = UNKNOWN_APP_NAME;
        fac.addresses = Lists.newArrayListWithCapacity(4);
        fac.hooks = Lists.newArrayListWithCapacity(4);
        fac.hooks.add(logConsumerHook);

        // 对当前线程可见就可以了
        // 用Unsafe.putOrderedXXX()消除写volatile的write barrier, JIT以后去掉了StoreLoad, 只剩StoreStore(x86下是空操作)
        // 在x86架构cpu上, StoreLoad是一条 [lock addl $0x0,(%rsp)] 指令, 去掉这条指令会有一点性能的提升 ^_^
        updater.lazySet(fac, 0);

        return fac;
    }

    /**
     * Sets the connector.
     */
    public ProxyFactory connector(JClient connector) {
        checkValid(this);

        this.connector = connector;
        return this;
    }

    /**
     * Adds provider's addresses.
     */
    public ProxyFactory addProviderAddress(UnresolvedAddress... addresses) {
        checkValid(this);

        Collections.addAll(this.addresses, addresses);
        return this;
    }

    /**
     * Adds provider's addresses.
     */
    public ProxyFactory addProviderAddress(List<UnresolvedAddress> addresses) {
        checkValid(this);

        this.addresses.addAll(addresses);
        return this;
    }

    /**
     * Sets application's name
     */
    public ProxyFactory appName(String appName) {
        checkValid(this);

        this.appName = appName;
        return this;
    }

    /**
     * Sets the service interface type.
     */
    public <I> ProxyFactory interfaceClass(Class<I> serviceInterface) {
        checkValid(this);

        this.serviceInterface = serviceInterface;
        return this;
    }

    /**
     * Synchronous blocking or asynchronous callback, the default is synchronous.
     */
    public ProxyFactory asyncMode(AsyncMode asyncMode) {
        checkValid(this);

        this.asyncMode = asyncMode;
        return this;
    }

    /**
     * Sets the mode of dispatch, the default is {@link DispatchMode#ROUND}
     */
    public ProxyFactory dispatchMode(DispatchMode dispatchMode) {
        checkValid(this);

        this.dispatchMode = dispatchMode;
        return this;
    }

    /**
     * Timeout milliseconds.
     */
    public ProxyFactory timeoutMills(int timeoutMills) {
        checkValid(this);

        this.timeoutMills = timeoutMills;
        return this;
    }

    /**
     * Asynchronous callback listener.
     */
    public ProxyFactory listener(JListener listener) {
        checkValid(this);

        if (asyncMode != ASYNC_CALLBACK) {
            throw new UnsupportedOperationException("asyncMode should first be set to ASYNC_CALLBACK");
        }
        this.listener = listener;
        return this;
    }

    /**
     * Adds hooks.
     */
    public ProxyFactory addHook(ConsumerHook... hooks) {
        checkValid(this);

        Collections.addAll(this.hooks, hooks);
        return this;
    }

    @SuppressWarnings("unchecked")
    public <I> I newProxyInstance() {
        // stack copy
        JClient _connector = connector;
        List<UnresolvedAddress> _addresses = addresses;
        String _appName = appName;
        Class<?> _serviceInterface = serviceInterface;
        AsyncMode _asyncMode = asyncMode;
        DispatchMode _dispatchMode = dispatchMode;
        int _timeoutMills = timeoutMills;
        List<ConsumerHook> _hooks = hooks;
        JListener _listener = listener;

        if (!updater.compareAndSet(this, 0, 1)) {
            throw new IllegalStateException(
                    Reflects.simpleClassName(this) + " is used by others, you should create another one!");
        }
        try {
            // check arguments
            if (_asyncMode == SYNC && _dispatchMode == BROADCAST) {
                throw new UnsupportedOperationException("unsupported mode, SYNC & BROADCAST");
            }
            checkNotNull(_connector, "connector");
            checkNotNull(_addresses, "addresses");
            checkNotNull(_appName, "appName");
            checkNotNull(_serviceInterface, "serviceInterface");
            checkNotNull(_hooks, "hooks");
            checkArgument(_serviceInterface.isInterface(), "serviceInterface is required to be interface");
            ServiceProvider annotation = _serviceInterface.getAnnotation(ServiceProvider.class);
            checkNotNull(annotation, _serviceInterface.getClass() + " is not a ServiceProvider interface");
            String _serviceProviderName = annotation.value();
            _serviceProviderName =
                    Strings.isNotBlank(_serviceProviderName) ? _serviceProviderName : _serviceInterface.getSimpleName();
            String _group = annotation.group();
            String _version = annotation.version();
            checkNotNull(_group, "group");
            checkNotNull(_version, "version");

            // message info
            MessageWrapper message = new MessageWrapper();
            message.setAppName(_appName);
            message.setGroup(_group);
            message.setVersion(_version);
            message.setServiceProviderName(_serviceProviderName);

            for (UnresolvedAddress address : _addresses) {
                _connector.addChannelGroup(message, _connector.group(address));
            }

            // dispatcher
            Dispatcher dispatcher = null;
            switch (_dispatchMode) {
                case ROUND:
                    dispatcher = new DefaultRoundDispatcher(_connector);
                    break;
                case BROADCAST:
                    dispatcher = new DefaultBroadcastDispatcher(_connector);
                    break;
            }
            if (_timeoutMills > 0) {
                dispatcher.setTimeoutMills(_timeoutMills);
            }
            dispatcher.setHooks(_hooks);

            // invocation handler
            InvocationHandler handler = null;
            switch (_asyncMode) {
                case SYNC:
                    handler = new SyncInvoker(dispatcher, message);
                    break;
                case ASYNC_CALLBACK:
                    dispatcher.setListener(checkNotNull(_listener, "listener"));
                    handler = new AsyncInvoker(dispatcher, message);
                    break;
            }

            return (I) Proxy.newProxyInstance(
                    Reflects.getClassLoader(_serviceInterface), new Class<?>[] { _serviceInterface }, handler);
        } finally {
            recycle();
        }
    }

    private static void checkValid(ProxyFactory factory) {
        if (updater.get(factory) == 1) {
            throw new IllegalStateException(
                    Reflects.simpleClassName(factory) + " is used by others, you should create another one!");
        }
    }

    private static final ConsumerHook logConsumerHook = new ConsumerHook() {

        @Override
        public void before(Request request) {
            logger.debug("Request: [{}], {}.", request.invokeId(), request.message());
        }

        @Override
        public void after(Request request) {
            logger.debug("Request: [{}], has respond.", request.invokeId());
        }
    };

    private ProxyFactory(Recyclers.Handle<ProxyFactory> handle) {
        this.handle = handle;
    }

    private boolean recycle() {
        // help GC
        connector = null;
        addresses = null;
        appName = null;
        serviceInterface = null;
        timeoutMills = 0;
        hooks = null;
        listener = null;

        return recyclers.recycle(this, handle);
    }

    private static final Recyclers<ProxyFactory> recyclers = new Recyclers<ProxyFactory>() {

        @Override
        protected ProxyFactory newObject(Handle<ProxyFactory> handle) {
            return new ProxyFactory(handle);
        }
    };

    private transient final Recyclers.Handle<ProxyFactory> handle;
}
