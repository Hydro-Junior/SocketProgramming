package com.xjy.nio;

import org.junit.Test;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;

/**
 * @Author: Mr.Xu
 * @Date: Created in 8:45 2018/3/21
 * @Description:
 * NIO支持面向缓冲区，基于通道的IO操作。（传统IO：面向单向字节流的阻塞式IO）
 * 三要素：缓冲区、通道、选择器
 * （1）缓冲区 Buffer
 * 1. 基本数据类型都有对应的缓冲区（boolean除外），常用的是ByteBuffer
 * 2. 缓冲区的几个主要方法和属性：
 * ByteBuffer.allocate(capacity)   put()  get()  flip()<存完数据需要读取之前调用，也就是写模式切到读模式时使用，本质是position归零，limit前移到原来position的位置，取消mark标记>
 *  rewind() <重新读写数据,读写模式写都可以使用，本质上是limit不变，position归零，取消mark标记>  mark() 记录当前position的位置，通过reset()恢复
 *  compact()：将未读数据拷贝到Buffer的起始位置，position位置在最后一个未读数据之后； clear():重置缓冲区，遗忘所有数据
 *  mark <= position <= limit <= capacity
 * 3. 两种缓冲区模式
 *  非直接缓冲区 ：allocate() 建立于JVM的内存中
 *  直接缓冲区：allocateDirect() 建立在物理内存中<内存映射文件>
 *
 * （2）通道 Channel
 *  1. 底层原理
 *  传统DMA方式的IO：CPU和DMA（直接存储器访问）交替获得总线控制权
 *  通道：能执行有限通道指令的I/O控制器，代替CPU管理控制外设。与DMA相比，两者都能在I/O设备和主存之间建立数据直传通路，但通道有自己的指令和程序，有更强的独立处理数据
 *  输入和输出的能力，相当于通道是在硬件的基础上添加了软件手段实现对I/O的控制和传送，更适用于I/O操作频繁的场景。
 *  2. 几个通道实现类
 *  FileChannel SocketChannel ServerSocketChannel DatagramChannel
 *  3. 获取通道
 *  本地：FileInputStream/FileOutputStream RandomAccessFile Socket ServerSocket DatagramSocket--> getChannel
 *  通道的静态方法 open()    Files.newByteChannel()
 *
 *  (3)选择器 Selector
 *  实现非阻塞式通信的关键，把通道注册于选择器，用选择器监控通道的I/O状况。
 *  同步阻塞式：一直等待，就绪(读到数据时)时处理。
 *  同步非阻塞式NIO：通知已就绪，再处理。（Reactor："能读了告诉我"）
 * （异步非阻塞式AIO：属于NIO的更进一步，把读取操作交给操作系统，ProActor模式,"读好了再叫我"。）
 *
 *  多提一句，这里没有展示服务端handleWrite回写给客户端的情况，可能还会用到的Selectionkey的interestOps方法
 */
public class Tcp {
    private static final int TIMEOUT = 6000;
    private static final int BUFFSIZE = 1024;
    @Test
    public void client() throws IOException{
        SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress("127.0.0.1",9796));//获取通道
        socketChannel.configureBlocking(false);//非阻塞

        ByteBuffer writeBuf = ByteBuffer.allocate(1024);
        ByteBuffer readBuf = ByteBuffer.allocate(1024);
        Random r = new Random();
        int count = 0;
        try{
            while(true) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                writeBuf.put((new Date().toString() + "\n" +"某个随机整数:"+ r.nextInt(10)).getBytes());
                writeBuf.flip();
                socketChannel.write(writeBuf);
                writeBuf.clear();
                int readBytes = socketChannel.read(readBuf);
                if(readBytes == -1) socketChannel.close();
                else if (readBytes > 0){
                    readBuf.flip();
                    System.out.println("客户端收到回显数据"+ new String(readBuf.array(),0,readBytes));
                    readBuf.clear();
                }
                //if(count == 20)break;
            }
        }catch (IOException e){
            socketChannel.close();
        }
        socketChannel.close();
    }

    /**
     * 服务端Demo，这里模拟这样一个条件：
     * 如果客户端发过来的字符串尾数是"7"，服务端需要回写。
     * @throws IOException
     */
    @Test
    public void server() throws IOException{
        //配置通道
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.bind(new InetSocketAddress(9796));

        Selector selector = Selector.open(); //获取选择器
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);//为监听通道注册accept事件

        while(true) {//持续性监听
            if (selector.select(TIMEOUT) == 0) {
                System.out.println("（一小段时间内未有就绪事件）...");
                continue;
            }
            Iterator<SelectionKey> it = selector.selectedKeys().iterator();
            while (it.hasNext()) {
                SelectionKey key = it.next();
                //判断就绪的事件
                if(!key.isValid()){it.remove();continue;}//如果已经被取消的key，可以去除
                if (key.isAcceptable()) {
                    //多了个通道，进行非阻塞配置，进行读就绪注册
                    SocketChannel sc = serverSocketChannel.accept();
                    sc.configureBlocking(false);
                    sc.register(selector, SelectionKey.OP_READ,ByteBuffer.allocate(BUFFSIZE));//也可在这里为channel直接分配缓冲区大小，之后key通过key.attachment()作为缓冲区，attachment是一个volatile变量
                } else if (key.isReadable()) {
                    //获得读就绪状态的通道
                    SocketChannel sc = (SocketChannel) key.channel();
                    //读取数据(通道的缓冲区往往作为key的附属物)
                    ByteBuffer buf = (ByteBuffer)key.attachment();
                    try {
                        int len = sc.read(buf);//将收到的值写到buffer
                        String tmps = "";
                        if(len == -1) sc.close();//另一端已关闭
                        else if(len > 0) {
                            buf.flip();
                            System.out.println(tmps = new String(buf.array(), 0, len));
                            buf.rewind();
                            if(tmps.endsWith("7")){
                                System.out.println("出现幸运数字！");
                                key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                            }
                        }
                    } catch (IOException e) {//若读过程中对方断开连接，则将抛异常
                        //取消key并关闭通道
                        key.cancel();
                        sc.close();
                    }
                }
                //满足可写要求
                if(key.isValid() && key.isWritable()){
                    //获得读就绪状态的通道
                    SocketChannel sc = (SocketChannel) key.channel();
                    //读取数据
                     ByteBuffer buf = (ByteBuffer) key.attachment();
                    //ByteBuffer buf = ByteBuffer.allocate(BUFFSIZE);
                    //buf.put(new String("收到了数字7").getBytes());
                    //System.out.println("可写的key，当前buf状态"+"\nposition"+buf.position()+"\nlimit"+buf.limit() + "\ncapacity"+buf.capacity());
                    // buf.flip();因为之前已经flip过，所以在此flip会将limit也归零
                    //System.out.println("flip之后的buf状态"+"\nposition"+buf.position()+"\nlimit"+buf.limit() + "\ncapacity"+buf.capacity());

                    sc.write(buf);//之前打印用到了flip(),并且rewind过，故此处直接回写即可，也可从position == 0（limit > 0）判断
                    if(!buf.hasRemaining()) key.interestOps(SelectionKey.OP_READ);
                    buf.compact();//Make room for more data to be read in
                }
                //完成后去记得取消选择键
                it.remove();
            }
        }
    }
}
