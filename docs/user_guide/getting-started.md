### 快速入门

#### 工程依赖:
+ JDK1.7或更高版本
+ 依赖管理工具: Maven3.x版本

[最新版本](http://search.maven.org/#search%7Cga%7C1%7Corg.jupiter-rpc)

#### Maven依赖:

```xml
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
```

#### 简单调用示例:
##### 1. 创建服务接口:

```java
@ServiceProvider(group = "test", name = "serviceTest")
public interface ServiceTest {
    String sayHelloString();
}

@ServiceProvider:
    - 建议每个服务接口通过此注解来指定服务信息, 如不希望业务代码对jupiter依赖也可以不使用此注解而手动去设置服务信息
        + group: 服务组别(选填, 默认组别为'Jupiter')
        + name: 服务名称(选填, 默认名称为接口全限定名称)
```

##### 2. 创建服务实现:

```java
@ServiceProviderImpl(version = "1.0.0")
public class ServiceTestImpl implements ServiceTest {

    @Override
    public String sayHelloString() {
        return "Hello jupiter";
    }
}

@ServiceProviderImpl:
    - 建议每个服务实现通过此注解来指定服务版本信息, 如不希望业务代码对jupiter依赖也可以不使用此注解而手动去设置版本信息
        + version: 服务版本号(选填, 默认版本号为'1.0.0')
```

##### 3. 启动注册中心:
###### - 选择1: 使用jupiter默认的注册中心:

```java
public class HelloJupiterRegistryServer {

    public static void main(String[] args) {
        // 注册中心
        RegistryServer registryServer = RegistryServer.Default.createRegistryServer(20001, 1);
        try {
            registryServer.startRegistryServer();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
```

###### - 选择2: 使用[zookeeper](https://zookeeper.apache.org/doc/trunk/zookeeperStarted.html)作为注册中心:

```xml
默认注册中心只建议在测试环境使用, 线上建议使用zookeeper实现

// 设置使用zookeeper作为注册中心
JServer server = new DefaultServer(RegistryService.RegistryType.ZOOKEEPER)
JClient client = new DefaultClient(RegistryService.RegistryType.ZOOKEEPER)

在server和client中配置jupiter-registry-zookeeper依赖(jupiter-all包含jupiter-registry-zookeeper)

<dependency>
    <groupId>org.jupiter-rpc</groupId>
    <artifactId>jupiter-registry-zookeeper</artifactId>
    <version>${jupiter.version}</version>
</dependency>
```

##### 4. 启动服务提供(Server):

```java
public class HelloJupiterServer {

    public static void main(String[] args) throws Exception {
        JServer server = new DefaultServer().withAcceptor(new JNettyTcpAcceptor(18090));
        // provider
        ServiceTestImpl service = new ServiceTestImpl();
        // 本地注册
        ServiceWrapper provider = server.serviceRegistry()
                .provider(service)
                .register();
        // 连接注册中心
        server.connectToRegistryServer("127.0.0.1:20001");
        // 向注册中心发布服务
        server.publish(provider);
        // 启动server
        server.start();
    }
}
```

##### 5. 启动服务消费者(Client)

```java
public class HelloJupiterClient {

    public static void main(String[] args) {
        JClient client = new DefaultClient().withConnector(new JNettyTcpConnector());
        // 连接RegistryServer
        client.connectToRegistryServer("127.0.0.1:20001");
        // 自动管理可用连接
        JConnector.ConnectionWatcher watcher = client.watchConnections(ServiceTest.class);
        // 等待连接可用
        if (!watcher.waitForAvailable(3000)) {
            throw new ConnectFailedException();
        }

        ServiceTest service = ProxyFactory.factory(ServiceTest.class)
                .version("1.0.0")
                .client(client)
                .newProxyInstance();

        service.sayHelloString();
    }
}
```

#### 结合Spring使用示例:
1. [Server端配置](/jupiter-example/src/main/resources/spring-provider.xml)

2. [Client端配置](/jupiter-example/src/main/resources/spring-consumer.xml)

[Server/Client代码示例](/jupiter-example/src/main/java/org/jupiter/example/spring)

#### [更多示例代码](/jupiter-example/src/main/java/org/jupiter/example)
