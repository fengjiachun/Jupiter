Jupiter release notes
------------------------

### 

### 2018-09-11: version 1.2.26
- Bug fix: [Object[] 包含 null 时序列化/反序列化错误] (https://github.com/fengjiachun/Jupiter/issues/73#issuecomment-420119074)

### 2018-08-14: version 1.2.25
- [LoadBalancer扩展SPI支持](https://github.com/fengjiachun/Jupiter/issues/69)

### 2018-07-28: version 1.2.24
- Bug fix: [TCP_FASTOPEN_CONNECT报错](https://github.com/fengjiachun/Jupiter/issues/68)

### 2018-07-13: version 1.2.23
- Bug fix: [LowCopyProtocolEncoder throw NPE](https://github.com/fengjiachun/Jupiter/issues/67)

### 2018-07-01: version 1.2.22
- [增加针对Linux平台的一些TCP参数](https://github.com/fengjiachun/Jupiter/issues/66)
- [Unix域套接字支持](https://en.wikipedia.org/wiki/Unix_domain_socket)
- DefaultInvokeFuture#roundFutures初始容量配置, 避免频繁扩容
- [DefaultInvokeFuture#TimeoutScanner优化](https://github.com/fengjiachun/Jupiter/issues/62)

### 2018-04-13: version 1.2.21
- 个别代码重新整理

### 2018-03-28: version 1.2.20
- writeVarInt优化

### 2018-03-18: version 1.2.19
- CopyOnWriteGroupList代码重构
- Serialization模块重构
- Bug fix: [WeightArray#computeWeightArray](/jupiter-rpc/src/main/java/org/jupiter/rpc/load/balance/WeightArray.java)

### 2018-03-13: version 1.2.18
- 负载均衡代码优化
- 优化[NettyChannelGroup](/jupiter-transport/jupiter-transport-netty/src/main/java/org/jupiter/transport/netty/channel/NettyChannelGroup.java)中的index
- 移除对netty ByteBufAllocator 的配置依赖, 应直接使用netty的配置方式
- 优化String encode/decode[UnsafeUtf8Util](/jupiter-common/src/main/java/org/jupiter/common/util/internal/UnsafeUtf8Util.java)
- 优化[SystemClock](/jupiter-common/src/main/java/org/jupiter/common/util/SystemClock.java)
- Bug fix: 修复并发建立过多连接
- 移除[org.objenesis:objenesis]依赖
- [优化序列化/反序列化过程中的memory-copy](https://github.com/fengjiachun/Jupiter/issues/51)

### 2018-02-07: version 1.2.17
- Bug fix: [jupiter-all没引入opentracing](https://github.com/fengjiachun/Jupiter/issues/52)
- 删除没必要的ProtoStuffSerializer的外层schema缓存
- Bug fix: [AbstractFuture isDone 的问题](https://github.com/fengjiachun/Jupiter/issues/55)
- Enhancement: [日志信息缺少关键的错误地址信息](https://github.com/fengjiachun/Jupiter/issues/54)
- Update [protostuff](https://github.com/protostuff/protostuff) version: 1.5.2 --> 1.6.0
- [Spring环境中开放JOption参数配置](https://github.com/fengjiachun/Jupiter/issues/50)
- Add [LongSequence 序号生成器](/jupiter-common/src/main/java/org/jupiter/common/util/LongSequence.java)
- Bug fix: ProviderInterceptor 参数 {Object result, Throwable failCause} 始终为null
- Bug fix: Spring schema 数组属性注入失败
- Add JupiterSpringClient#ConsumerInterceptor[] consumer端全局拦截器
- ConsumerHook --> ConsumerInterceptor
- Update [open-tracing](https://github.com/opentracing/opentracing-java) version: 0.31.0-RC1 --> 0.31.0
- Update [metrics-core](https://github.com/dropwizard/metrics) version: 3.1.2 --> 4.0.2
- Update [affinity](https://github.com/OpenHFT/Java-Thread-Affinity) version: 3.0.6 --> 3.1.7
- Update [asm](http://asm.ow2.org) version: 5.2 --> 6.0
- Update [byte-buddy](https://github.com/raphw/byte-buddy) version: 1.6.3 --> 1.7.9
- Update [disruptor](https://github.com/LMAX-Exchange/disruptor) version: 3.3.6 --> 3.3.7
- Add [CallerRunsExecutorFactory](/jupiter-rpc/src/main/java/org/jupiter/rpc/executor/CallerRunsExecutorFactory.java)

### 2018-01-22: version 1.2.16
- Add [CloseableExecutor](/jupiter-rpc/src/main/java/org/jupiter/rpc/executor/CloseableExecutor.java)
- Add method JClient#awaitConnections(Class<?> interfaceClass, long timeoutMillis)
- Add method JClient#awaitConnections(Class<?> interfaceClass, String version, long timeoutMillis)
- [感谢 @远墨 提供的图](/docs/static_files/jupiter-rpc.png)

### 2018-01-17: version 1.2.15
- [AbstractFuture#awaitDone 优化](https://github.com/fengjiachun/Jupiter/issues/44)
- [关闭jupiter client/server](https://github.com/fengjiachun/Jupiter/issues/43)
- [dump文件名在windows系统不能有 ":"](https://github.com/fengjiachun/Jupiter/pull/42)

### 2017-12-19: version 1.2.14
- 升级Netty版本到4.1.19.Final, Netty v4.1.18.Final有[严重bug会导致core-dump](https://github.com/netty/netty/pull/7507)

### 2017-12-16: version 1.2.13
- 移除javassist依赖([netty老版本的TypeParameterMatcher用之](https://github.com/netty/netty/commit/7d08b4fc357e12ee2487e87d8fdcbeee1152e5a0))
- 升级Netty版本到4.1.18.Final
- JServiceLoader优化

### 2017-12-12: version 1.2.12
- OpenTracing支持 [\#33](https://github.com/fengjiachun/Jupiter/issues/33)
- Bug fix: backlog设置无效
- 增加jupiter-extension模块
- JFilter扩展

### 2017-09-30: version 1.2.11
- 优化: 注册失败的RegisterMeta间隔一段时间再重新入队(AbstractRegistryService)
- 升级Netty版本到4.1.15.Final
- Bug fix: ZookeeperRegistryService#lookup()方法中directory顺序错误, 修正为: group,providerName,version

### 2017-08-28: version 1.2.10
- Monitor增加ls命令(本地查询发布和订阅的服务信息)
- 防止好不容易画的注释图被格式化毁了
- 升级Netty版本到4.1.14.Final
- Bug fix: zk server闪断导致服务信息丢失, 重新建立连接后无法再次发布该服务 [\#31](https://github.com/fengjiachun/Jupiter/issues/31)
- 优化 AbstractRegistryService.notify()锁粒度

### 2017-07-19: version 1.2.9
- Spring.schema支持 注册中心类型(实现)配置 [\#30](https://github.com/fengjiachun/Jupiter/issues/30)
- 升级Netty版本到4.1.13.Final
- Spring.schema支持JAcceptor/JConnector配置 [\#29](https://github.com/fengjiachun/Jupiter/issues/29)
- Bug fix: 修复spring-support中<jupiter:property serializerType="xxx" />配置serializerType无效的问题 [\#27](https://github.com/fengjiachun/Jupiter/issues/27#event-1150209875)

### 2017-06-29: version 1.2.8
- 新增模块 jupiter-all [\#19](https://github.com/fengjiachun/Jupiter/issues/19)
- Bug fix: JConnector.ConnectionWatcher#waitForAvailable()返回值不准确

### 2017-06-15: version 1.2.7
- 升级[Disruptor](https://github.com/LMAX-Exchange/disruptor)到v3.3.6
- 传输层默认使用java nio, 而不使用netty提供的native epoll/kqueue [netty issues#6837](https://github.com/netty/netty/issues/6837)

### 2017-05-24: version 1.2.6
- 集群容错策略的配置细化到方法粒度 [\#22](https://github.com/fengjiachun/Jupiter/issues/22)
- Spring.scheme支持 [\#21](https://github.com/fengjiachun/Jupiter/issues/21)
- 线程亲和性支持 [Thread Affinity](https://github.com/OpenHFT/Java-Thread-Affinity)

### 2017-05-19: version 1.2.5
- 存在addAndGet场景时, 不使用jupiter自带的Atomic*FieldUpdater, 使用jdk默认的实现, 1.8后续版本jdk使用了intrinsics后更快
- 移除org.jupiter.rpc.model.metadata.ServiceWrapper#connCount, connCount修正为JServer粒度的参数
- 修复权重问题
- 升级Netty版本到4.1.11.Final
- 新增netty-transport-native-kqueue支持 [\#20](https://github.com/fengjiachun/Jupiter/issues/20)

### 2017-04-18: version 1.2.4
- 升级Netty版本到4.1.9.Final
- 默认注册中心实现jupiter-registry-default序列化/反序列化方式改为可配置的方式
- 不再支持udt
- 简化spring配置 [\#14](https://github.com/fengjiachun/Jupiter/issues/14)

### 2017-03-11: version 1.2.3
- 优化JServiceLoader实现, 使其可按名字查找实现类