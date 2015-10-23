Jupiter
a rpc framework

TODO:

1.监控
性能报告:

小数据包同步调用qps:10w+
(详细测试代码见jupiter-example中BenchmarkClient/BenchmarkServer)

* 其中一次简单的测试结果(小数据包1000w+次同步调用)
* ------------------------------------------------------------------
* 测试机器:
* server端(一台机器)
*      cpu型号: Intel(R) Xeon(R) CPU           X3430  @ 2.40GHz
*      cpu cores: 4核心
*
* client端(一台机器)
*      cpu型号: Intel(R) Xeon(R) CPU           X3430  @ 2.40GHz
*      cpu cores: 4核心
*
* 网络环境: 局域网
* ------------------------------------------------------------------
* 测试结果:
* Request count: 12800000, time: 117 second, qps: 109401