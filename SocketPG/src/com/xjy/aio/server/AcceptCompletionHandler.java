package com.xjy.aio.server;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

/**
 * @Author: Mr.Xu
 * @Date: Created in 14:45 2018/9/22
 * @Description:注意java提供的accept() read() write()方法的参数特点,往往牵涉到一个result（与方法相关）、一个attachment（自定义）
 */
public class AcceptCompletionHandler implements CompletionHandler<AsynchronousSocketChannel,AsyncTimeServerHandler> {

    @Override
    public void completed(AsynchronousSocketChannel result, AsyncTimeServerHandler attachment) {
        attachment.asynchronousServerSocketChannel.accept(attachment,this);
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        result.read(buffer,buffer,new ReadCompletionHandler(result));//read方法3个参数 ByteBuffer数据包 异步channel携带的附件（回调时的入参，可以为空，但一般就是buffer）接收通知回调的业务Handler
    }

    @Override
    public void failed(Throwable exc, AsyncTimeServerHandler attachment) {
        exc.printStackTrace();
        attachment.latch.countDown();
    }
}
