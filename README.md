Jupiter

    A rpc framework

  -------------------------------------------------------------------------------------------------------

    Example: jupiter-example#org.jupiter.example.round.*

  -------------------------------------------------------------------------------------------------------

    Jupiter Architecture:

           ═ ═ ═▷ init         ─ ─ ─ ▷ async       ──────▶ sync
      ***********************************************************************************
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
      ***********************************************************************************

  -------------------------------------------------------------------------------------------------------

                                            Protocol
    ┌ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┐
         2   │   1   │    1   │     8     │      4      │
    ├ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┤
             │       │        │           │             │
    │  MAGIC   Sign    Status   Invoke Id   Body Length                Body Content             │
             │       │        │           │             │
    └ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ┘

  -------------------------------------------------------------------------------------------------------

    性能报告:

        1. 小数据包同步调用qps: 10w+ (测试代码见jupiter-example[BenchmarkClient/BenchmarkServer])
        2. 客户端发送 1K String, 服务端原样返回, 同步调用qps: 7w+ (测试代码见jupiter-example[BenchmarkClient_1KString/BenchmarkServer_1KString])

        测试前最好将logger设置为warn级别, info级别会打印traceId导致log文件会很大(一千万次调用大概3G日志), 再一个是logger性能较差, 会影响测试结果.

  -------------------------------------------------------------------------------------------------------

    测试机器:
    ------------------------------------------------------------------
      server端(一台机器)
           cpu型号: Intel(R) Xeon(R) CPU           X3430  @ 2.40GHz
           cpu cores: 4核心

      client端(一台机器)
           cpu型号: Intel(R) Xeon(R) CPU           X3430  @ 2.40GHz
           cpu cores: 4核心

      网络环境: 局域网
    ------------------------------------------------------------------

  ------------------------------------------------------------------------------------------------------

2016-1-9的最新一次测试结果(小数据包1亿+次同步调用):

  ------------------------------------------------------------------------------------------------------

    测试结果:

        2016-01-09 01:46:38.279 WARN  [main] [BenchmarkClient] - count=128000000
        2016-01-09 01:46:38.279 WARN  [main] [BenchmarkClient] - Request count: 128000000, time: 1089 second, qps: 117539

  ------------------------------------------------------------------------------------------------------

    监控数据:
    ------------------------------------------------------------------
        telnet 127.0.0.1 19999
        >: auth 123456
        >: metrics -report
    ------------------------------------------------------------------

        2016-1-9 1:43:54 =================================================================

        -- Histograms ------------------------------------------------------------------
        request.size [请求数据大小(byte)统计(不包括Jupiter协议头的16个字节)]
                     count = 105222780
                       min = 101
                       max = 101
                      mean = 101.00
                    stddev = 0.00
                    median = 101.00
                      75% <= 101.00
                      95% <= 101.00
                      98% <= 101.00
                      99% <= 101.00
                    99.9% <= 101.00
        response.size [响应数据大小(byte)统计(不包括Jupiter协议头的16个字节)]
                     count = 105222780
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

        -- Meters ----------------------------------------------------------------------
        rejection [请求被拒绝次数统计]
                     count = 0
                 mean rate = 0.00 events/second
             1-minute rate = 0.00 events/second
             5-minute rate = 0.00 events/second
            15-minute rate = 0.00 events/second

        -- Timers ----------------------------------------------------------------------
        Jupiter-1.0.0-Service#hello [参与此次测试的provider方法执行时间统计]
                     count = 105222780
                 mean rate = 116923.98 calls/second
             1-minute rate = 117567.98 calls/second
             5-minute rate = 111719.59 calls/second
            15-minute rate = 75550.32 calls/second
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
        processing [请求处理耗时统计(从request被解码开始, 到response数据被刷到OS内核缓冲区为止)]
                     count = 105222780
                 mean rate = 116914.57 calls/second
             1-minute rate = 117567.89 calls/second
             5-minute rate = 111678.29 calls/second
            15-minute rate = 75253.79 calls/second
                       min = 0.00 milliseconds
                       max = 4.00 milliseconds
                      mean = 0.24 milliseconds
                    stddev = 0.53 milliseconds
                    median = 0.00 milliseconds
                      75% <= 0.00 milliseconds
                      95% <= 1.00 milliseconds
                      98% <= 2.00 milliseconds
                      99% <= 2.00 milliseconds
                    99.9% <= 4.00 milliseconds
  ------------------------------------------------------------------------------------------------------

2015-11-15的一次测试结果(测试时间3.5小时左右, 小数据包12亿+次同步调用):

  ------------------------------------------------------------------------------------------------------

    测试结果:

        2015-11-15 02:43:10.691 WARN  [main] [BenchmarkClient] - count=1280000000
        Request count: 1280000000, time: 12070 second, qps: 106048

  ------------------------------------------------------------------------------------------------------

    监控数据:
    ------------------------------------------------------------------
        telnet 127.0.0.1 19999
        >: auth 123456
        >: metrics -report
    ------------------------------------------------------------------

        15-11-15 2:38:20 ===============================================================

        -- Histograms ------------------------------------------------------------------
        request.size [请求数据大小(byte)统计(不包括Jupiter协议头的16个字节)]
                     count = 1246287634
                       min = 145
                       max = 145
                      mean = 145.00
                    stddev = 0.00
                    median = 145.00
                      75% <= 145.00
                      95% <= 145.00
                      98% <= 145.00
                      99% <= 145.00
                    99.9% <= 145.00
        response.size [响应数据大小(byte)统计(不包括Jupiter协议头的16个字节)]
                     count = 1246287634
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

        -- Meters ----------------------------------------------------------------------
        rejection [请求被拒绝次数统计]
                     count = 0
                 mean rate = 0.00 events/second
             1-minute rate = 0.00 events/second
             5-minute rate = 0.00 events/second
            15-minute rate = 0.00 events/second

        -- Timers ----------------------------------------------------------------------
        Jupiter-1.0.0-Service#hello [参与此次测试的provider方法执行时间统计]
                     count = 1246287634
                 mean rate = 105977.57 calls/second
             1-minute rate = 107930.46 calls/second
             5-minute rate = 107646.03 calls/second
            15-minute rate = 107220.85 calls/second
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
        processing [请求处理耗时统计(从request被解码开始, 到response数据被刷到OS内核缓冲区为止)]
                     count = 1246287634
                 mean rate = 105976.89 calls/second
             1-minute rate = 107928.26 calls/second
             5-minute rate = 107645.48 calls/second
            15-minute rate = 107220.61 calls/second
                       min = 0.00 milliseconds
                       max = 5.00 milliseconds
                      mean = 0.39 milliseconds
                    stddev = 0.72 milliseconds
                    median = 0.00 milliseconds
                      75% <= 1.00 milliseconds
                      95% <= 2.00 milliseconds
                      98% <= 3.00 milliseconds
                      99% <= 3.00 milliseconds
                    99.9% <= 4.97 milliseconds


        一些系统指标:
        ￼------------------------------------------------------------------
        TOP: (top时间点在程序启动后3个小时左右, load较低且比较平稳)

        top - 02:34:22 up 235 days,  9:43,  1 user,  load average: 0.00, 0.02, 0.00
        Tasks: 167 total,   2 running, 165 sleeping,   0 stopped,   0 zombie
        Cpu0  : 28.4%us, 22.1%sy,  0.0%ni,  6.2%id,  0.0%wa,  0.0%hi, 43.3%si,  0.0%st
        Cpu1  : 41.4%us, 18.4%sy,  0.0%ni, 40.2%id,  0.0%wa,  0.0%hi,  0.0%si,  0.0%st
        Cpu2  : 42.9%us, 16.8%sy,  0.0%ni, 40.3%id,  0.0%wa,  0.0%hi,  0.0%si,  0.0%st
        Cpu3  : 40.9%us, 16.7%sy,  0.0%ni, 42.4%id,  0.0%wa,  0.0%hi,  0.0%si,  0.0%st
        Mem:  10117196k total,  9384396k used,   732800k free,   267816k buffers
        Swap:  8191992k total,   163432k used,  8028560k free,  2397504k cached

          PID USER      PR  NI  VIRT  RES  SHR S %CPU %MEM    TIME+  COMMAND
        11616 fengjc    20   0 5667m 1.1g  12m S 253.3 11.0 485:39.31 java

        GC: (无老年代GC, 新生代GC大概4秒一次)

        2015-11-15T02:43:12.321+0800: 12078.036: [GC (Allocation Failure) 12078.036: [ParNew: 840695K->1264K(943744K), 0.0106041 secs] 926524K->87117K(1992320K), 0.0106820 secs] [Times: user=0.04 sys=0.00, real=0.01 secs]
        2015-11-15T02:43:16.021+0800: 12081.735: [GC (Allocation Failure) 12081.735: [ParNew: 840176K->1632K(943744K), 0.0105246 secs] 926029K->87507K(1992320K), 0.0106068 secs] [Times: user=0.04 sys=0.00, real=0.01 secs]
        2015-11-15T02:43:19.854+0800: 12085.568: [GC (Allocation Failure) 12085.568: [ParNew: 840544K->1713K(943744K), 0.0105379 secs] 926419K->87609K(1992320K), 0.0106154 secs] [Times: user=0.04 sys=0.00, real=0.01 secs]
        2015-11-15T02:43:23.629+0800: 12089.344: [GC (Allocation Failure) 12089.344: [ParNew: 840625K->1796K(943744K), 0.0108642 secs] 926521K->87712K(1992320K), 0.0110592 secs] [Times: user=0.04 sys=0.00, real=0.01 secs]
        2015-11-15T02:43:27.435+0800: 12093.150: [GC (Allocation Failure) 12093.150: [ParNew: 840708K->1110K(943744K), 0.0107892 secs] 926624K->87048K(1992320K), 0.0108655 secs] [Times: user=0.04 sys=0.00, real=0.01 secs]
        2015-11-15T02:43:31.499+0800: 12097.213: [GC (Allocation Failure) 12097.213: [ParNew: 840022K->1582K(943744K), 0.0106472 secs] 925960K->87542K(1992320K), 0.0107275 secs] [Times: user=0.04 sys=0.00, real=0.01 secs]
        Heap
         par new generation   total 943744K, used 633439K [0x0000000080000000, 0x00000000c0000000, 0x00000000c0000000)
          eden space 838912K,  75% used [0x0000000080000000, 0x00000000a690c338, 0x00000000b3340000)
          from space 104832K,   1% used [0x00000000b99a0000, 0x00000000b9b2bbb8, 0x00000000c0000000)
          to   space 104832K,   0% used [0x00000000b3340000, 0x00000000b3340000, 0x00000000b99a0000)
         concurrent mark-sweep generation total 1048576K, used 85959K [0x00000000c0000000, 0x0000000100000000, 0x0000000100000000)
         Metaspace       used 14240K, capacity 14400K, committed 14592K, reserved 1062912K
          class space    used 1690K, capacity 1726K, committed 1792K, reserved 1048576K

  -------------------------------------------------------------------------------------------------------

2015-11-15的一次测试结果(1K String数据, 1000w+次同步调用):

  ------------------------------------------------------------------------------------------------------

    测试结果:

        2015-11-15 16:02:59.644 WARN  [main] [BenchmarkClient] - count=12800000
        Request count: 12800000, time: 168 second, qps: 76190

  ------------------------------------------------------------------------------------------------------

    监控数据:
    ------------------------------------------------------------------
        telnet 127.0.0.1 19999
        >: auth 123456
        >: metrics -report
    ------------------------------------------------------------------

        15-11-15 16:02:30 ==============================================================

        -- Histograms ------------------------------------------------------------------
        request.size  [请求数据大小(byte)统计(不包括Jupiter协议头的16个字节)]
                     count = 8739966
                       min = 1139
                       max = 1139
                      mean = 1139.00
                    stddev = 0.00
                    median = 1139.00
                      75% <= 1139.00
                      95% <= 1139.00
                      98% <= 1139.00
                      99% <= 1139.00
                    99.9% <= 1139.00
        response.size [响应数据大小(byte)统计(不包括Jupiter协议头的16个字节)]
                     count = 8739964
                       min = 1005
                       max = 1005
                      mean = 1005.00
                    stddev = 0.00
                    median = 1005.00
                      75% <= 1005.00
                      95% <= 1005.00
                      98% <= 1005.00
                      99% <= 1005.00
                    99.9% <= 1005.00

        -- Meters ----------------------------------------------------------------------
        rejection [请求被拒绝次数统计]
                     count = 0
                 mean rate = 0.00 events/second
             1-minute rate = 0.00 events/second
             5-minute rate = 0.00 events/second
            15-minute rate = 0.00 events/second

        -- Timers ----------------------------------------------------------------------
        Jupiter-1.0.0-Service#hello [参与此次测试的provider方法执行时间统计]
                     count = 8740185
                 mean rate = 72881.64 calls/second
             1-minute rate = 64381.05 calls/second
             5-minute rate = 24889.29 calls/second
            15-minute rate = 10723.75 calls/second
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
        processing [请求处理耗时统计(从request被解码开始, 到response数据被刷到OS内核缓冲区为止)]
                     count = 8740389
                 mean rate = 72835.88 calls/second
             1-minute rate = 65256.92 calls/second
             5-minute rate = 25577.15 calls/second
            15-minute rate = 10893.93 calls/second
                       min = 0.00 milliseconds
                       max = 6.00 milliseconds
                      mean = 0.54 milliseconds
                    stddev = 1.03 milliseconds
                    median = 0.00 milliseconds
                      75% <= 1.00 milliseconds
                      95% <= 3.00 milliseconds
                      98% <= 4.00 milliseconds
                      99% <= 4.00 milliseconds
                    99.9% <= 6.00 milliseconds

        一些系统指标:
        ￼------------------------------------------------------------------
        GC: (无老年代GC, 新生代GC大概2秒一次)

        2015-11-15T16:03:13.643+0800: 169.466: [GC (Allocation Failure) 169.466: [ParNew: 839181K->336K(943744K), 0.0047825 secs] 850025K->11195K(1992320K), 0.0048617 secs] [Times: user=0.01 sys=0.00, real=0.01 secs]
        2015-11-15T16:03:15.373+0800: 171.196: [GC (Allocation Failure) 171.196: [ParNew: 839248K->479K(943744K), 0.0046315 secs] 850107K->11354K(1992320K), 0.0047053 secs] [Times: user=0.02 sys=0.00, real=0.01 secs]
        2015-11-15T16:03:17.161+0800: 172.984: [GC (Allocation Failure) 172.984: [ParNew: 839391K->322K(943744K), 0.0048379 secs] 850266K->11214K(1992320K), 0.0049344 secs] [Times: user=0.02 sys=0.00, real=0.00 secs]
        2015-11-15T16:03:18.860+0800: 174.683: [GC (Allocation Failure) 174.683: [ParNew: 839234K->295K(943744K), 0.0045733 secs] 850126K->11202K(1992320K), 0.0046447 secs] [Times: user=0.02 sys=0.00, real=0.00 secs]
        2015-11-15T16:03:20.552+0800: 176.375: [GC (Allocation Failure) 176.375: [ParNew: 839207K->252K(943744K), 0.0051823 secs] 850114K->11175K(1992320K), 0.0053021 secs] [Times: user=0.02 sys=0.00, real=0.00 secs]
        2015-11-15T16:03:22.222+0800: 178.045: [GC (Allocation Failure) 178.045: [ParNew: 839164K->258K(943744K), 0.0045260 secs] 850087K->11196K(1992320K), 0.0045989 secs] [Times: user=0.01 sys=0.00, real=0.00 secs]
        2015-11-15T16:03:23.994+0800: 179.818: [GC (Allocation Failure) 179.818: [ParNew: 839170K->237K(943744K), 0.0046721 secs] 850108K->11189K(1992320K), 0.0047435 secs] [Times: user=0.02 sys=0.00, real=0.01 secs]
        Heap
         par new generation   total 943744K, used 69965K [0x0000000080000000, 0x00000000c0000000, 0x00000000c0000000)
          eden space 838912K,   8% used [0x0000000080000000, 0x0000000084417ed0, 0x00000000b3340000)
          from space 104832K,   0% used [0x00000000b99a0000, 0x00000000b99db588, 0x00000000c0000000)
          to   space 104832K,   0% used [0x00000000b3340000, 0x00000000b3340000, 0x00000000b99a0000)
         concurrent mark-sweep generation total 1048576K, used 10952K [0x00000000c0000000, 0x0000000100000000, 0x0000000100000000)
         Metaspace       used 14172K, capacity 14336K, committed 14592K, reserved 1062912K
          class space    used 1689K, capacity 1726K, committed 1792K, reserved 1048576K