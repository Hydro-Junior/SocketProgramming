package com.xjy.bio.chatroom;
/**
 * 传统阻塞式Socket的客户端，每个客户端有一个textfield用以发送消息,一个textArea用以显示所有客户端向服务器发送的消息。
 */
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class ChatClient extends JFrame {
	private String name = new String();//模拟用户名称
	private static final long serialVersionUID = 1L;
	private boolean connectState = true;//一开始未连接上
	private boolean bConnected = true;//表示处于连接状态
	private DataOutputStream dos = null;//输出流写出消息
	private DataInputStream dis = null;//输入流读取消息
	private JTextField tf = new JTextField();//打字窗口
	private JTextArea ta = new JTextArea();//显示窗口
	private Socket sc;//客户端套接字

	public static void main(String[] args) {
		ChatClient cc = new ChatClient();
		cc.init();
	}
     //获取消息的Run方法
	class InfoGetter implements Runnable{
		@Override
		public void run() {
			while(bConnected){
				try {
					String info = dis.readUTF();
					System.out.println(info);
					ta.append(info+"\n");
				} catch (Exception e) {
					System.out.println("disconnected!");
					bConnected = false;
					disconnect();
				}
			}
			
		}
		
	}
	//初始化工作
	public void init() {
		Random rd = new Random();
		this.name = "代号" + String.format("%010d",Math.abs(rd.nextInt()));
		this.setTitle("Chating Room" + "      (您的代号：" + name + ")");
		this.setBounds(400, 300, 600, 400);
		this.add(ta, BorderLayout.CENTER);
		this.add(tf, BorderLayout.SOUTH);
		// this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				disconnect();
				System.exit(0);
			}

		});
		setVisible(true);
		registerListener();
		connect();
		//开启获取消息的线程
		new Thread(new InfoGetter()).start();
	}

	public void connect() {
		while (connectState) {
			try {
				sc = new Socket("127.0.0.1", 8886);
				dos = new DataOutputStream(sc.getOutputStream());//初始化输出流
				dis = new DataInputStream(sc.getInputStream());//初始化输入流
				System.out.println("success to connect the server！");
				connectState = false;
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (ConnectException e) {//捕获连接异常，并尝试重连
				System.out.println("trying to connect...");
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				continue;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	//断开连接处理
	public void disconnect() {
		try {
			if (dos != null)
				dos.close();
			if (sc != null)
				sc.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	//为textfield注册回车显示事件：清空field，放入输出流
	public void registerListener() {

		tf.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String ss = tf.getText().trim();
				tf.setText("");
				try {
					dos.writeUTF(name+": "+ss);
					dos.flush();
					// dos.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
	}
}
