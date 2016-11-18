[![Build Status](https://travis-ci.org/fengjiachun/Jupiter.svg?branch=master)](https://travis-ci.org/fengjiachun/Jupiter)

###Jupiter
    Jupiter是一个分布式服务框架

######版本变更:
- v1.0 服务不能单独指定或切换序列化/反序列化的方式, 这不太合理
- v1.1 为了同时支持多种序列化方式, 协议头发生变更, Sign(一个byte)的高地址4位用来标记序列化/反序列化方式, 低地址4位意义不变(暂时没有机器做性能测试和疲劳测试了, 相比v1.0可能不够稳定)

######文档:
- [Wiki](https://github.com/fengjiachun/Jupiter/wiki)

######其他:
- 闲暇时间写着玩的, 娱乐性质, 不过如有人希望尝试使用, 我承诺会一直维护
     1. qq交流: 71287116
     2. 邮件交流: jiachun_fjc@163.com


