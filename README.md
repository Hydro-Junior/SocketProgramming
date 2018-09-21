# SocketPG
本仓库共分为3个独立的部分：

1. blocking: 利用传统I/O阻塞式模型实现简单的聊天室
2. nio：介绍的nio的几大重要元素（详见tcp.java中的注解），并提供了NIO下tcp和udp网络通信的demo
3. 测试工具类，特别是TcpClientTester,用GUI实现的测试窗口，生成自定义的报文头、报文尾和数据，但目前这三项是程序中固定的，需要修改代码才能重新配置。
