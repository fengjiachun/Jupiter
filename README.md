[![Build Status](https://travis-ci.org/fengjiachun/Jupiter.svg?branch=master)](https://travis-ci.org/fengjiachun/Jupiter)
[![Maven Central](https://img.shields.io/maven-central/v/org.jupiter-rpc/jupiter.svg?label=Maven Central)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.jupiter-rpc%22%20AND%20jupiter)

####Jupiter:
- Jupiter是一款性能非常不错的, 轻量级的分布式服务框架

####Jupiter Architecture:

           ═ ═ ═▷ init         ─ ─ ─ ▷ async       ──────▶ sync
    ----------------------------------------------------------------------------------------

                                ┌ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─
                                           ┌ ─ ─ ─ ┐ │
               ─ ─ ─ ─ ─ ─ ─ ─ ─│ Registry  Monitor ───────────────────────────┐
              │                            └ ─ ─ ─ ┘ │                         │
                                └ ─ ─△─ ─ ─ ─ ─△─ ─ ─                          ▼
              │                                                           ┌ ─ ─ ─ ─
            Notify                   ║         ║                            Telnet │
              │         ═ ═ ═ ═ ═ ═ ═           ═ ═ ═ ═ ═ ═ ═ ═ ═         └ ─ ─ ─ ─
                       ║                                         ║             ▲
              │    Subscribe                                  Register         │
                       ║                                         ║             │
              │  ┌ ─ ─ ─ ─ ─                          ┌ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─    │
                            │─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ▷           ┌ ─ ─ ─ ┐ │   │
              └ ▷│ Consumer           Invoke          │ Provider  Monitor ─────┘
                            │────────────────────────▶           └ ─ ─ ─ ┘ │
                 └ ─ ─ ─ ─ ─                          └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─

    ---------------------------------------------------------------------------------------

####性能:
- 小数据包请求(不带业务)在四核刀片服务器上可达到17w+的tps, 详情见 [Benchmark](https://github.com/fengjiachun/Jupiter/wiki/Benchmark)

####文档:
- [Wiki](https://github.com/fengjiachun/Jupiter/wiki)



####快速入门:

#####工程依赖:
+ JDK1.7或更高版本
+ 依赖管理工具: Maven3.x版本

#####[最新版本OSS下载](https://oss.sonatype.org/#nexus-search;quick~org.jupiter-rpc)
#####[最新版本Maven中心仓库下载](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.jupiter-rpc%22%20AND%20jupiter)
#####Maven依赖:

    <properties>
        <jupiter.version>1.2.3</jupiter.version>
    </properties>

    <dependency>
       <groupId>org.jupiter-rpc</groupId>
       <artifactId>jupiter-rpc</artifactId>
       <version>${jupiter.version}</version>
    </dependency>
    <!-- 传输层 -->
    <dependency>
       <groupId>org.jupiter-rpc</groupId>
       <artifactId>jupiter-transport-netty</artifactId>
       <version>${jupiter.version}</version>
    </dependency>
    <!-- 序列化/反序列化, 可选择只其中依赖一种或者同时依赖多种 -->
    <dependency>
       <groupId>org.jupiter-rpc</groupId>
       <artifactId>jupiter-serialization-hessian</artifactId>
       <version>${jupiter.version}</version>
    </dependency>
    <dependency>
       <groupId>org.jupiter-rpc</groupId>
       <artifactId>jupiter-serialization-java</artifactId>
       <version>${jupiter.version}</version>
    </dependency>
    <dependency>
       <groupId>org.jupiter-rpc</groupId>
       <artifactId>jupiter-serialization-kryo</artifactId>
       <version>${jupiter.version}</version>
    </dependency>
    <dependency>
       <groupId>org.jupiter-rpc</groupId>
       <artifactId>jupiter-serialization-protostuff</artifactId>
       <version>${jupiter.version}</version>
    </dependency>
    <!-- 注册中心, jupiter-registry-zookeeper/jupiter-registry-default二选一 -->
    <dependency>
       <groupId>org.jupiter-rpc</groupId>
       <artifactId>jupiter-registry-default</artifactId>
       <version>${jupiter.version}</version>
    </dependency>
    <!-- 集成Spring支持, 如不集成Spring可不依赖 -->
    <dependency>
       <groupId>org.jupiter-rpc</groupId>
       <artifactId>jupiter-spring-support</artifactId>
       <version>${jupiter.version}</version>
    </dependency>
    <dependency>
       <groupId>org.springframework</groupId>
       <artifactId>spring-context</artifactId>
       <version>4.3.0.RELEASE</version>
    </dependency>
    <!-- telnet监控模块(可选) -->
    <dependency>
       <groupId>org.jupiter-rpc</groupId>
       <artifactId>jupiter-monitor</artifactId>
       <version>${jupiter.version}</version>
    </dependency>
    <!-- flightexec(可选) -->
    <dependency>
       <groupId>org.jupiter-rpc</groupId>
       <artifactId>jupiter-flightexec</artifactId>
       <version>${jupiter.version}</version>
    </dependency>

#####简单调用示例:
1. 创建服务接口:
>     @ServiceProvider(group = "test", name = "serviceTest")
>     public interface ServiceTest {
>         String sayHelloString();
>     }
>
> @ServiceProvider:
>    - 建议每个服务接口通过此注解来指定服务信息, 如不希望业务代码对jupiter依赖也可以不使用此注解而手动去设置服务信息
>        + group: 服务组别(选填, 默认组别为'Jupiter')
>        + name: 服务名称(选填, 默认名称为接口全限定名称)

2. 创建服务实现:
>     @ServiceProviderImpl(version = "1.0.0")
>     public class ServiceTestImpl implements ServiceTest {
>
>         @Override
>         public String sayHelloString() {
>             return "Hello jupiter";
>         }
>     }
>
> @ServiceProviderImpl:
>    - 建议每个服务实现通过此注解来指定服务版本信息, 如不希望业务代码对jupiter依赖也可以不使用此注解而手动去设置版本信息
>        + version: 服务版本号(选填, 默认版本号为'1.0.0')

3. 启动注册中心:
> 选择1: 使用jupiter默认的注册中心:
>
>     public class HelloJupiterRegistryServer {
>
>         public static void main(String[] args) {
>             // 注册中心
>             RegistryServer registryServer = RegistryServer.Default.createRegistryServer(20001, 1);
>             try {
>                 registryServer.startRegistryServer();
>             } catch (InterruptedException e) {
>                 e.printStackTrace();
>             }
>         }
>     }
>
> 选择2: 使用[zookeeper](https://zookeeper.apache.org/doc/trunk/zookeeperStarted.html)作为注册中心:
>
>     默认注册中心只建议在测试环境使用, 线上建议使用zookeeper实现
>     在server和client中配置jupiter-registry-zookeeper依赖并去除jupiter-registry-default依赖即可, 无需其他改动
>
>     <dependency>
>         <groupId>org.jupiter-rpc</groupId>
>         <artifactId>jupiter-registry-zookeeper</artifactId>
>         <version>${jupiter.version}</version>
>     </dependency>

4. 启动服务提供(Server):
>     public class HelloJupiterServer {
>
>         public static void main(String[] args) throws Exception {
>             JServer server = new DefaultServer().withAcceptor(new JNettyTcpAcceptor(18090));
>             // provider
>             ServiceTestImpl service = new ServiceTestImpl();
>             // 本地注册
>             ServiceWrapper provider = server.serviceRegistry()
>                     .provider(service)
>                     .register();
>             // 连接注册中心
>             server.connectToRegistryServer("127.0.0.1:20001");
>             // 向注册中心发布服务
>             server.publish(provider);
>             // 启动server
>             server.start();
>         }
>     }

5. 启动服务消费者(Client)
>     public class HelloJupiterClient {
>
>         public static void main(String[] args) {
>             JClient client = new DefaultClient().withConnector(new JNettyTcpConnector());
>             // 连接RegistryServer
>             client.connectToRegistryServer("127.0.0.1:20001");
>             // 自动管理可用连接
>             JConnector.ConnectionWatcher watcher = client.watchConnections(ServiceTest.class);
>             // 等待连接可用
>             if (!watcher.waitForAvailable(3000)) {
>                 throw new ConnectFailedException();
>             }
>
>             ServiceTest service = ProxyFactory.factory(ServiceTest.class)
>                     .version("1.0.0")
>                     .client(client)
>                     .newProxyInstance();
>
>             service.sayHelloString();
>         }
>     }

#####结合Spring使用示例:
1. [Server端配置](https://github.com/fengjiachun/Jupiter/blob/master/jupiter-example/src/main/resources/spring-provider.xml):
>
>     <!-- netty的网络层实现(建议单例) -->
>     <bean id="nettyTcpAcceptor" class="org.jupiter.transport.netty.JNettyTcpAcceptor">
>         <constructor-arg index="0" value="18090" />
>     </bean>
>
>     <!-- jupiter server(建议单例) -->
>     <bean id="jupiterServer" class="org.jupiter.rpc.DefaultServer">
>         <property name="acceptor" ref="nettyTcpAcceptor" />
>     </bean>
>
>     <bean id="server" class="org.jupiter.spring.support.JupiterSpringServer">
>         <property name="server" ref="jupiterServer" />
>         <!-- 注册中心地址, 逗号分隔 -->
>         <property name="registryServerAddresses" value="127.0.0.1:20001" />
>     </bean>
>
>     <!-- provider -->
>     <bean id="serviceTest" class="org.jupiter.example.ServiceTestImpl" />
>     <bean class="org.jupiter.spring.support.JupiterSpringProviderBean">
>         <property name="server" ref="server" />
>         <property name="providerImpl" ref="serviceTest" />
>     </bean>

2. [Client端配置](https://github.com/fengjiachun/Jupiter/blob/master/jupiter-example/src/main/resources/spring-consumer.xml):
>
>     <!-- netty的网络层实现(建议单例) -->
>     <bean id="nettyTcpConnector" class="org.jupiter.transport.netty.JNettyTcpConnector" />
>
>     <!-- jupiter client(建议单例) -->
>     <bean id="jupiterClient" class="org.jupiter.rpc.DefaultClient">
>         <property name="connector" ref="nettyTcpConnector" />
>     </bean>
>
>     <bean id="client" class="org.jupiter.spring.support.JupiterSpringClient">
>         <property name="client" ref="jupiterClient" />
>         <!-- 注册中心地址, 逗号分隔 -->
>         <property name="registryServerAddresses" value="127.0.0.1:20001" />
>     </bean>
>
>     <!-- consumer -->
>     <bean id="serviceTest" class="org.jupiter.spring.support.JupiterSpringConsumerBean">
>         <property name="client" ref="client" />
>         <property name="interfaceClass" value="org.jupiter.example.ServiceTest" />
>
>         <!-- 以下都选项可不填 -->
>
>         <!-- 服务版本号, 通常在接口不兼容时版本号才需要升级 -->
>         <property name="version" value="1.0.0" />
>         <property name="serializerType" value="proto_stuff" />
>     </bean>
[Server/Client代码示例](https://github.com/fengjiachun/Jupiter/tree/master/jupiter-example/src/main/java/org/jupiter/example/spring)

#####[更多示例代码](https://github.com/fengjiachun/Jupiter/tree/master/jupiter-example/src/main/java/org/jupiter/example)


####其他
- qq交流群: 397633380
- 邮件交流: jiachun_fjc@163.com

