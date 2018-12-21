##### 属性配置清单

| Option                                                            | Comments                                                                                                                                                                                                                                       |
| ----------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| jupiter.executor.factory.consumer.core.workers                    | Client-Executor核心工作线程数, 默认值为cpu cores * 2
| jupiter.executor.factory.provider.core.workers                    | Server-Executor核心工作线程数, 默认值为cpu cores * 2
| jupiter.executor.factory.consumer.max.workers                     | Client-Executor最大工作线程数, 默认值为32
| jupiter.executor.factory.provider.max.workers                     | Server-Executor最大工作线程数, 默认值为512
| jupiter.executor.factory.consumer.queue.type                      | Client-Executor的Queue类型, 仅在使用ThreadPoolExecutorFactory时有效, 默认值为ARRAY_BLOCKING_QUEUE
| jupiter.executor.factory.provider.queue.type                      | Server-Executor的Queue类型, 仅在使用ThreadPoolExecutorFactory时有效, 默认值为ARRAY_BLOCKING_QUEUE
| jupiter.executor.factory.consumer.queue.capacity                  | Client-Executor的Queue或者buffer的容量, 对ForkJoinPoolExecutorFactory是无效设置, 默认值为32768
| jupiter.executor.factory.provider.queue.capacity                  | Server-Executor的Queue或者buffer的容量, 对ForkJoinPoolExecutorFactory是无效设置, 默认值为32768
| jupiter.executor.factory.consumer.disruptor.wait.strategy.type    | Client-Executor-Disruptor等待策略, 仅在使用DisruptorExecutorFactory时有效, 默认值为LITE_BLOCKING_WAIT
| jupiter.executor.factory.provider.disruptor.wait.strategy.type    | Server-Executor-Disruptor等待策略, 仅在使用DisruptorExecutorFactory时有效, 默认值为LITE_BLOCKING_WAIT
| jupiter.executor.factory.consumer.thread.pool.rejected.handler    | Client-Executor饱和策略指定, 仅在使用ThreadPoolExecutorFactory时有效, 默认值为org.jupiter.common.concurrent.RejectedTaskPolicyWithReport(如果当前任务实现了RejectedRunnable接口, 那么交给用户去实现拒绝任务的逻辑, 否则以FIFO的方式抛弃队列中一部分现有任务)
| jupiter.executor.factory.provider.thread.pool.rejected.handler    | Server-Executor饱和策略指定, 仅在使用ThreadPoolExecutorFactory时有效, 默认值为org.jupiter.common.concurrent.RejectedTaskPolicyWithReport(如果当前任务实现了RejectedRunnable接口, 那么交给用户去实现拒绝任务的逻辑, 否则以FIFO的方式抛弃队列中一部分现有任务)
| jupiter.io.reader.idle.time.seconds                               | Server链路read空闲检测, 默认60秒, 60秒没读到任何数据会强制关闭连接
| jupiter.io.writer.idle.time.seconds                               | Client链路write空闲检测, 默认30秒, 30秒没有向链路中写入任何数据时Client会主动向Server发送心跳数据包
| jupiter.io.decoder.max.body.size                                  | Client/Server可接收的最大消息体大小(默认5M), 超过限制直接断开连接
| jupiter.io.decoder.composite.buf                                  | 消息解码是否使用CompositeByteBuf(netty选项)以减少内存拷贝, 默认不使用(索引计算复杂度高, 可能有较大开销)
| jupiter.rpc.invoke.timeout                                        | 远程调用默认超时时间(3000毫秒)
| jupiter.rpc.load-balancer.warm-up                                 | 一个服务发布后的默认预热时间(10分钟)
| jupiter.rpc.load-balancer.default.weight                          | Load balancer 默认权重
| jupiter.rpc.load-balancer.max.weight                              | Load balancer 最大权重
| jupiter.rpc.suggest.connection.count                              | Client对Server默认的建议连接数(cpu cores)
| jupiter.metric.needed                                             | 是否启用provider的指标度量, 默认不启用
| jupiter.metric.csv.reporter                                       | 是否启用Metrics csv reporter, 默认不启用而是打印在日志里面
| jupiter.metric.csv.reporter.directory                             | 如果启用Metrics csv, csv的文件路径
| jupiter.metric.report.period                                      | Metrics 执行周期, 默认15分钟
| jupiter.registry.zookeeper.sessionTimeoutMs                       | ZK session timeout, 默认60 * 1000毫秒
| jupiter.registry.zookeeper.connectionTimeoutMs                    | ZK连接超时设置, 默认15 * 1000毫秒
| jupiter.use.non_blocking_hash                                     | 是否使用Cliff Click的NonBlockingHashMap代替ConsurrentHashMap, 默认不使用
| jupiter.local.address                                             | 本地IP地址, 默认值为InetAddress.getLocalHost()或者是本机网卡中第一个有效IP
| jupiter.registry.impl                                             | (1.2.8之后的版本废除)注册中心选择(since 1.2.3) default / zookeeper


