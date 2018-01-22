Jupiter release notes
------------------------

### 2018-01-22

- Add CloseableExecutor
- Add method JClient#awaitConnections(Class<?> interfaceClass, long timeoutMillis)
- Add method JClient#awaitConnections(Class<?> interfaceClass, String version, long timeoutMillis)
- 感谢 @远墨 提供的图 docs/static_files/jupiter-rpc.png

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