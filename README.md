Jupiter

  a rpc framework

  example参见jupiter-example: org.jupiter.example.round.*

  -------------------------------------------------------------------------------------------------------
  Jupiter Architecture:

           ═ ═ ═▷ init         ━ ━ ━ ▷ async       ━━━━━━▶ sync
      ***************************************************************************************************
                                                   ┏━━━━━━━━━━━━━━┓
                                                   ┃              ┃
                                                   ┃              ┃
                                         ┌ ─ ─ ─ ─ ─ ─ ─ ─ ─      ┃
                       Subscribe═ ═ ═ ═ ▷      Registry     │◁ ═ ═┃═ ═ ═ ═ ═ Register
                                         └ ─ ─ ─ ─ ─ ─ ─ ─ ─      ┃
                        ║                          ┃              ┃              ║
                                                                  ┃
                        ║                          ┃              ┃              ║
                                                                  ┃
                        ║                          ┃              ┃              ║
            ━ ━ ━ Notify━ ━ ━ ━ ━ ━ ━ ━ ━ ━ ━ ━ ━ ━               ┃
           ┃            ║                                         ┃              ║
                                                                  ┃
           ┃            ║                                         ┃              ║
                                                                  ┃
           ┃            ║                                         ┃              ║
              ┌ ─ ─ ─ ─ ─ ─ ─ ─ ─                                 ┃    ┌ ─ ─ ─ ─ ─ ─ ─ ─ ─
           ┗ ▷      Consumer     ┣━━━━━━━━━━━━━━━━━Invoke━━━━━━━━━╋━━━▶      Provider     │
              └ ─ ─ ─ ─ ─ ─ ─ ─ ─                                 ┃    └ ─ ─ ─ ─ ─ ─ ─ ─ ─
                                                                  ┃              ┃
                                                                  ┣━━━━━━━━━━━━━━┛
                                                                  ▼
                                                        ┌ ─ ─ ─ ─ ─ ─ ─ ─ ─
                                                              Monitor      │
                                                        └ ─ ─ ─ ─ ─ ─ ─ ─ ─
      ***************************************************************************************************

  -------------------------------------------------------------------------------------------------------

性能报告:

小数据包同步调用qps:10w+
(详细测试代码见jupiter-example中BenchmarkClient/BenchmarkServer)

  其中一次简单的测试结果(小数据包2.5亿+次同步调用)
  
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
  
  Request count: 256000000, time: 2465 second, qps: 103853
  
  监控数据:
  ------------------------------------------------------------------
    telnet 127.0.0.1 19999
    输入: metrics -report
  
  
    - Histograms ------------------------------------------------------------------
    
    org.jupiter.rpc.provider.processor.DefaultProviderProcessor.request.size
                 count = 253946227
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
    org.jupiter.rpc.provider.processor.task.RecyclableTask.response.size
                 count = 253946207
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
                 count = 253946428
             mean rate = 103665.58 calls/second
         1-minute rate = 103374.81 calls/second
         5-minute rate = 103748.01 calls/second
        15-minute rate = 97171.10 calls/second
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
                 count = 253946355
             mean rate = 103665.41 calls/second
         1-minute rate = 103374.82 calls/second
         5-minute rate = 103748.06 calls/second
        15-minute rate = 97169.22 calls/second
                   min = 0.00 milliseconds
                   max = 11.00 milliseconds
                  mean = 0.62 milliseconds
                stddev = 1.00 milliseconds
                median = 0.00 milliseconds
                  75% <= 1.00 milliseconds
                  95% <= 3.00 milliseconds
                  98% <= 4.00 milliseconds
                  99% <= 4.00 milliseconds
                99.9% <= 10.88 milliseconds
    
    TOP:
    ￼------------------------------------------------------------------

    top - 00:43:39 up 220 days,  7:52,  3 users,  load average: 0.17, 0.13, 0.06
    Tasks: 171 total,   2 running, 169 sleeping,   0 stopped,   0 zombie
    Cpu0  : 34.8%us, 17.4%sy,  0.0%ni,  3.4%id,  0.0%wa,  0.0%hi, 44.4%si,  0.0%st
    Cpu1  : 44.2%us, 19.6%sy,  0.0%ni, 35.8%id,  0.4%wa,  0.0%hi,  0.0%si,  0.0%st
    Cpu2  : 45.4%us, 16.4%sy,  0.0%ni, 38.2%id,  0.0%wa,  0.0%hi,  0.0%si,  0.0%st
    Cpu3  : 45.9%us, 16.5%sy,  0.0%ni, 37.6%id,  0.0%wa,  0.0%hi,  0.0%si,  0.0%st
    Mem:  10117196k total,  7387036k used,  2730160k free,   250092k buffers
    Swap:  8191992k total,   161452k used,  8030540k free,   579820k cached
