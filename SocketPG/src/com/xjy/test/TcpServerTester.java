package com.xjy.test;
import java.io.CharArrayReader;
/**
 * 传统阻塞式socket编程，服务端
 */
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TcpServerTester {
	ServerSocket ss = null; //服务端套接字
	boolean started = true; 
	List<Client> clients = new ArrayList<Client>();//存储所有客户端

	// 主方法
	public static void main(String[] args) {
		new TcpServerTester().start();//开启主监听线程
	}

	public void start() {
		try {
			try {
				ss = new ServerSocket(8886);
			} catch (BindException e) {
				System.out.println("端口已经被占用！请先关闭其他服务器！");
				System.exit(0);
			}
			//不断循环接收
			while (started) {
				Client client = new Client(ss.accept());//accept方法返回客户端的Socket，然后初始化客户端类
				clients.add(client);
				new Thread(client).start();
			}
		} catch (IOException e) {
			started = false;
			e.printStackTrace();
		} finally {
			if (ss != null) {
				try {
					ss.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	//一个客户端对应一个客户端处理线程
	class Client implements Runnable {
		Socket sc = null;//用于接受客户端套接字
		DataInputStream dis = null;
		//InputStreamReader isr = null;
		DataOutputStream dos = null;
		boolean bConnect = false;

		Client(Socket s) {
			this.sc = s;//初始化服务端持有的客户端套接字
			try {
				dis = new DataInputStream(sc.getInputStream());//初始化输入流
				//isr = new InputStreamReader(sc.getInputStream());
				dos = new DataOutputStream(sc.getOutputStream());//初始化输出流
			} catch (IOException e) {
				e.printStackTrace();
			}
			System.out.println("A client connected!");
		}
		
		//发送方法
		public void send(String str) {
			try {
				//dos.writeUTF(str);//输出流，由服务端的客户端处理线程发送给对应的客户端
				dos.write(str.getBytes("ASCII"));//发送字符串按ASCII解码成的字节流
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			bConnect = true;

			while (bConnect) {
				try {
					String str = null;
					byte[] bs = new byte[1024];
					int len ;
					if((len=dis.read(bs))>0) {
						str = new String(bs,0,len,"ASCII");
					}
					System.out.println(str);
					//把读到的消息发送给所有的客户端
					for (int i = 0; i < clients.size(); i++) {
						clients.get(i).send(str);
					}

				} catch (Exception e) {
					System.out.println("lost a connect with Client!");
					clients.remove(this);
					bConnect = false;
				}
			}
			try {
				if (dis != null)
					dis.close();
				if (dos != null)
					dos.close();
				if (sc != null)
					sc.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

	}

}
