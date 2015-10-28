Jupiter
a rpc framework

TODO:

1.监控

  ------------------------------------------------------------------

性能报告:

小数据包同步调用qps:10w+
(详细测试代码见jupiter-example中BenchmarkClient/BenchmarkServer)

  其中一次简单的测试结果(小数据包1000w+次同步调用)
  
  ------------------------------------------------------------------
  
  测试机器:
  
  server端(一台机器)
  
       cpu型号: Intel(R) Xeon(R) CPU           X3430  @ 2.40GHz
       
       cpu cores: 4核心
       
  client端(一台机器)
  
       cpu型号: Intel(R) Xeon(R) CPU           X3430  @ 2.40GHz
       
       cpu cores: 4核心
       
  网络环境: 局域网
  
  ------------------------------------------------------------------
  
  测试结果:
  
  Request count: 12800000, time: 117 second, qps: 109401
  
  ------------------------------------------------------------------
    telnet 127.0.0.0 19999
    输入: metrics -report
  
  -- Histograms ------------------------------------------------------------------
  
  org.jupiter.rpc.provider.processor.DefaultProviderProcessor.request.sizes
  
               count = 10697193
                 min = 122
                 max = 122
                mean = 122.00
              stddev = 0.00
              median = 122.00
                75% <= 122.00
                95% <= 122.00
                98% <= 122.00
                99% <= 122.00
              99.9% <= 122.00
  org.jupiter.rpc.provider.processor.task.RecyclableTask.response.sizes
  
               count = 10697143
                 min = 17
                 max = 17
                mean = 17.00
              stddev = 0.00
              median = 17.00
                75% <= 17.00
                95% <= 17.00
                98% <= 17.00
                99% <= 17.00
              99.9% <= 17.00
  
  -- Timers ----------------------------------------------------------------------

  org.jupiter.benchmark.tcp.ServiceImpl.Jupiter-1.0.0-Service.hello
  
               count = 10697548
           mean rate = 100072.94 calls/second
       1-minute rate = 86585.73 calls/second
       5-minute rate = 34631.65 calls/second
      15-minute rate = 17167.06 calls/second
                 min = 0.00 milliseconds
                 max = 0.01 milliseconds
                mean = 0.00 milliseconds
              stddev = 0.00 milliseconds
              median = 0.00 milliseconds
                75% <= 0.00 milliseconds
                95% <= 0.00 milliseconds
                98% <= 0.00 milliseconds
                99% <= 0.00 milliseconds
              99.9% <= 0.01 milliseconds
  org.jupiter.rpc.provider.processor.task.RecyclableTask.invocation.timer
  
               count = 10698013
           mean rate = 100069.92 calls/second
       1-minute rate = 86576.08 calls/second
       5-minute rate = 34600.06 calls/second
      15-minute rate = 17128.04 calls/second
                 min = 0.00 milliseconds
                 max = 6.00 milliseconds
                mean = 0.53 milliseconds
              stddev = 0.81 milliseconds
              median = 0.00 milliseconds
                75% <= 1.00 milliseconds
                95% <= 2.00 milliseconds
                98% <= 3.00 milliseconds
                99% <= 3.71 milliseconds
              99.9% <= 6.00 milliseconds
