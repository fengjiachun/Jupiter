#### 飞行中调试, 客户端编译, 服务端执行, 以java的方式, 留一个方便线上调试的口子, 注意System.out会被重定向回客户端输出.
 
#### 实现虽简单, 但使用很灵活, 除了线上调试, 还有很多使用方式:
- 比如你可以在不重启线上server的前提下临时写一个业务provider直接推送到对应server(需要拿到JServer实例)上以提供服务.
 
- 又比如某个provider阻塞时间很长, 严重占用全局的线程池, 你也可以临时写一个线程池的实现并通过ServiceRegistry#executor()将线程池注册到该provider上供其单独使用(需重新调用register).
 
- 使用方式([参见样例代码](/jupiter-example/src/main/java/org/jupiter/example/flight/exec)):
    + 服务端注册[JavaClassExecProvider](/jupiter-flightexec/src/main/java/org/jupiter/flight/exec/JavaClassExecProvider.java)作为一个provider.
    + 客户端使用[JavaCompiler](/jupiter-flightexec/src/main/java/org/jupiter/flight/exec/JavaCompiler.java)编译需要执行的类, 将编译返回的字节码byte数组作为consumer的参数, 最后再以RPC的方式调用[JavaClassExec#exec(byte[])](/jupiter-flightexec/src/main/java/org/jupiter/flight/exec/JavaClassExecProvider.java).