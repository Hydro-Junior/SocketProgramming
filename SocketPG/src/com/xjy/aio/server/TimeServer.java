package com.xjy.aio.server;

import java.io.IOException;

/**
 * @Author: Mr.Xu
 * @Date: Created in 14:34 2018/9/22
 * @Description:利用AIO编程模型实现的时间查询服务器
 * @Reference: Netty权威指南（第二版）
 */
public class TimeServer {
    public static  void main(String[] args) throws IOException{
        int port = 8086;
        AsyncTimeServerHandler timeServerHandler = new AsyncTimeServerHandler(port);
        new Thread(timeServerHandler, "AIO-AsyncTimeServerHandler-001").start();
    }
}
