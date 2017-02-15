[![Build Status](https://travis-ci.org/fengjiachun/Jupiter.svg?branch=master)](https://travis-ci.org/fengjiachun/Jupiter)

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

####Maven依赖:

       <dependency>
           <groupId>org.jupiter-rpc</groupId>
           <artifactId>jupiter-rpc</artifactId>
           <version>1.2.0</version>
       </dependency>
       <!-- 传输层 -->
       <dependency>
           <groupId>org.jupiter-rpc</groupId>
           <artifactId>jupiter-transport-netty</artifactId>
           <version>1.2.0</version>
       </dependency>
       <!-- 序列化/反序列化, 可选择只其中依赖一种或者同时依赖多种 -->
       <dependency>
           <groupId>org.jupiter-rpc</groupId>
           <artifactId>jupiter-serialization-hessian</artifactId>
           <version>1.2.0</version>
       </dependency>
       <dependency>
           <groupId>org.jupiter-rpc</groupId>
           <artifactId>jupiter-serialization-java</artifactId>
           <version>1.2.0</version>
       </dependency>
       <dependency>
           <groupId>org.jupiter-rpc</groupId>
           <artifactId>jupiter-serialization-kryo</artifactId>
           <version>1.2.0</version>
       </dependency>
       <dependency>
           <groupId>org.jupiter-rpc</groupId>
           <artifactId>jupiter-serialization-protostuff</artifactId>
           <version>1.2.0</version>
       </dependency>
       <!-- 注册中心 -->
       <dependency>
           <groupId>org.jupiter-rpc</groupId>
           <artifactId>jupiter-registry-zookeeper</artifactId>
           <version>1.2.0</version>
       </dependency>
       <!-- 集成Spring支持, 如不集成Spring可不依赖 -->
       <dependency>
           <groupId>org.jupiter-rpc</groupId>
           <artifactId>jupiter-spring-support</artifactId>
           <version>1.2.0</version>
       </dependency>
       <dependency>
           <groupId>org.springframework</groupId>
           <artifactId>spring-context</artifactId>
           <version>4.3.0.RELEASE</version>
       </dependency>


####其他
- qq交流群: 397633380
- 邮件交流: jiachun_fjc@163.com

