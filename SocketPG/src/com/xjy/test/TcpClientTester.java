package com.xjy.test;

/**
 * 自定义的客户端测试类
 */
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.SoftBevelBorder;
import javax.swing.border.TitledBorder;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

public class TcpClientTester extends JFrame {
	// 默认IP和端口
	private String serverIP = "127.0.0.1";
	private int port = 8886;
	private String serialNum;// 每次启动随机生成10位的序列号
	private String sendSerialNum;// 添加了方框的序列号

	// 固定数据
	private static final String HEAD = "HEADHEAD";// 报文头
	private static final String TAIL = "TAILTAIL";// 报文尾

	private Socket sc;// 客户端套接字
	private boolean connectState = true;// 一开始未连接上
	private boolean bConnected = true;// 表示处于连接状态
	private DataOutputStream dos = null;// 输出流写出消息
	private DataInputStream dis = null;// 输入流读取消息
	private Thread receiveThread;
	
	private JPanel contentPane;// 总容器
	private JTextField tf_IP;// 输入服务端IP
	private JTextField tf_Port;// 输入服务端端口
	private JLabel lb_CSN;// 客户端序列号标签

	private JButton btn_Connect;// 连接
	private JButton btn_DisConnect;// 断开连接
	private JButton btn_MakeHead;// 添加报文头
	private JButton btn_MakeTail;// 添加报文尾
	private JButton btn_FullFragment;// 添加完整报文
	private JButton bt_Data;// 添加数据
	private JButton btn_Send;// 发送
	private JTextArea textArea_Send;// 显示要发送的报文
	private JTextArea textArea_Receive;// 显示接收的报文
	private JLabel lb_SerialNum;// 显示客户端序列号
	private JLabel lb_connect;// 右下角连接显示
	private JLabel lb_State;// 连接状态
	private JButton btn_ClearSend;// 清空发送窗口
	private JButton btn_clearReceive;// 清空接收窗口
	private JScrollPane scrollPane_rec;
	private JTextField tf_sequence;
	

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					TcpClientTester frame = new TcpClientTester();
					frame.init();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * initialize the parameters
	 */
	public void init() {
		// 生成随机客户端序列号
		serialNum = getFixlenthString(6);
		sendSerialNum = "[" + serialNum + "]";
		this.lb_SerialNum.setText(serialNum);// 设置序列号显示标签
		this.setTitle("客户端序列号：" + sendSerialNum);// 为标题添加客户端序列号

		registerListener();
	}

	/**
	 * register event listeners for some buttons
	 */
	public void registerListener() {
		// 为连接按钮注册事件
		btn_Connect.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				connect();
				receiveThread = new Thread(new InfoGetter());//接收线程
				receiveThread.start();
			}
		});
		// 为断开按钮注册事件
		btn_DisConnect.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				disconnect();
			}
		});
		// 为发送按钮注册事件
		btn_Send.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String message = textArea_Send.getText().trim();
				send(message);
				textArea_Send.setText("");
			}
		});
		// 为send方的清空按钮注册事件
		btn_ClearSend.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				textArea_Send.setText("");
			}
		});
		// 为send方的清空按钮注册事件
		btn_clearReceive.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				textArea_Receive.setText("");
			}
		});
		// 生成报文头
		btn_MakeHead.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				textArea_Send.append(HEAD + sendSerialNum);
			}
		});
		// 生成报文尾
		btn_MakeTail.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				textArea_Send.append(TAIL);
			}
		});
		//生成随机长整数
		bt_Data.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Random rd = new Random();
				long strL = Math.abs(rd.nextLong());
				String sequence = tf_sequence.getText();
				String randomStr = String.valueOf(strL);
				textArea_Send.append(sequence + randomStr);
			}
		});
		//生成完整帧
		btn_FullFragment.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Random rd = new Random();
				long strL = Math.abs(rd.nextLong());
				String sequence = tf_sequence.getText();
				String randomStr = String.valueOf(strL);
				textArea_Send.append(HEAD + sendSerialNum +sequence + randomStr + TAIL);
				
			}
		});

	}

	/**
	 * Create the frame.
	 */
	public TcpClientTester() {
		setResizable(false);

		setFont(new Font("微软雅黑", Font.BOLD, 16));
		setTitle("\u5BA2\u6237\u7AEF\u5E8F\u5217\u53F7");
		setBackground(Color.GRAY);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 573, 474);
		contentPane = new JPanel();
		contentPane.setBackground(new Color(128, 128, 128));
		contentPane.setBorder(new SoftBevelBorder(BevelBorder.LOWERED, null, null, null, null));
		contentPane.setForeground(Color.PINK);
		setContentPane(contentPane);
		contentPane.setLayout(null);

		btn_Connect = new JButton("\u8FDE   \u63A5");
		btn_Connect.setFont(new Font("新宋体", Font.PLAIN, 12));
		btn_Connect.setBackground(SystemColor.controlHighlight);
		btn_Connect.setBounds(409, 27, 93, 23);
		contentPane.add(btn_Connect);

		btn_DisConnect = new JButton("\u65AD   \u5F00");
		btn_DisConnect.setFont(new Font("新宋体", Font.PLAIN, 12));
		btn_DisConnect.setBackground(SystemColor.controlHighlight);
		btn_DisConnect.setBounds(409, 73, 93, 23);
		contentPane.add(btn_DisConnect);

		btn_MakeHead = new JButton("\u751F \u6210 \u5E27 \u5934");
		btn_MakeHead.setFont(new Font("新宋体", Font.PLAIN, 12));
		btn_MakeHead.setBackground(SystemColor.controlHighlight);
		btn_MakeHead.setBounds(409, 179, 109, 23);
		contentPane.add(btn_MakeHead);

		btn_MakeTail = new JButton("\u751F \u6210 \u5E27 \u5C3E");
		btn_MakeTail.setFont(new Font("新宋体", Font.PLAIN, 12));
		btn_MakeTail.setBackground(SystemColor.controlHighlight);
		btn_MakeTail.setBounds(409, 212, 109, 23);
		contentPane.add(btn_MakeTail);

		btn_FullFragment = new JButton("\u751F \u6210 \u5B8C \u6574 \u5E27");
		btn_FullFragment.setFont(new Font("新宋体", Font.PLAIN, 12));
		btn_FullFragment.setBackground(SystemColor.controlHighlight);
		btn_FullFragment.setBounds(409, 295, 124, 34);
		contentPane.add(btn_FullFragment);

		JPanel panel_Info = new JPanel();
		panel_Info.setBackground(new Color(105, 105, 105));
		panel_Info.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "\u4FE1\u606F\u680F",
				TitledBorder.LEADING, TitledBorder.TOP, null, Color.LIGHT_GRAY));
		panel_Info.setBounds(10, 5, 290, 140);
		contentPane.add(panel_Info);
		panel_Info.setLayout(null);

		JLabel lb_IP = new JLabel("IP\uFF1A");
		lb_IP.setForeground(Color.WHITE);
		lb_IP.setBounds(10, 20, 31, 22);
		panel_Info.add(lb_IP);
		lb_IP.setFont(new Font("微软雅黑", Font.PLAIN, 16));

		JLabel lblPort = new JLabel("Port\uFF1A");
		lblPort.setForeground(Color.WHITE);
		lblPort.setBounds(10, 52, 48, 22);
		panel_Info.add(lblPort);
		lblPort.setFont(new Font("微软雅黑", Font.PLAIN, 16));

		tf_IP = new JTextField();
		tf_IP.setBackground(Color.LIGHT_GRAY);
		tf_IP.setHorizontalAlignment(SwingConstants.RIGHT);
		tf_IP.setBounds(91, 17, 156, 28);
		panel_Info.add(tf_IP);
		tf_IP.setFont(new Font("微软雅黑", Font.PLAIN, 16));
		tf_IP.setText("127.0.0.1");
		tf_IP.setColumns(10);

		tf_Port = new JTextField();
		tf_Port.setBackground(Color.LIGHT_GRAY);
		tf_Port.setHorizontalAlignment(SwingConstants.RIGHT);
		tf_Port.setBounds(133, 49, 114, 28);
		panel_Info.add(tf_Port);
		tf_Port.setFont(new Font("微软雅黑", Font.PLAIN, 16));
		tf_Port.setText("8886");
		tf_Port.setColumns(10);

		lb_CSN = new JLabel("ClientSerialNumber\uFF1A");
		lb_CSN.setForeground(Color.WHITE);
		lb_CSN.setBounds(10, 81, 164, 22);
		panel_Info.add(lb_CSN);
		lb_CSN.setFont(new Font("微软雅黑", Font.PLAIN, 16));

		lb_SerialNum = new JLabel("");
		lb_SerialNum.setHorizontalAlignment(SwingConstants.LEFT);
		lb_SerialNum.setForeground(Color.WHITE);
		lb_SerialNum.setFont(new Font("微软雅黑", Font.ITALIC, 16));
		lb_SerialNum.setBounds(10, 108, 130, 22);
		panel_Info.add(lb_SerialNum);

		lb_State = new JLabel("\u672A \u8FDE \u63A5");
		lb_State.setHorizontalAlignment(SwingConstants.CENTER);
		lb_State.setForeground(Color.WHITE);
		lb_State.setFont(new Font("微软雅黑", Font.ITALIC, 16));
		lb_State.setBounds(133, 108, 130, 22);
		panel_Info.add(lb_State);

		JPanel panel_Send = new JPanel();
		panel_Send.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		panel_Send.setBounds(10, 185, 370, 89);
		contentPane.add(panel_Send);
		panel_Send.setLayout(null);

		textArea_Send = new JTextArea();
		textArea_Send.setFont(new Font("Monospaced", Font.PLAIN, 15));
		textArea_Send.setWrapStyleWord(true);
		textArea_Send.setBackground(new Color(245, 245, 245));
		textArea_Send.setBounds(0, 0, 370, 89);
		textArea_Send.setLineWrap(true);;
		panel_Send.add(textArea_Send);
		
		JScrollPane scrollPane = new JScrollPane(textArea_Send);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBounds(0, 0, 370, 89);
		panel_Send.add(scrollPane);

		JLabel lb_toSend = new JLabel("MessageToSend");
		lb_toSend.setForeground(new Color(220, 220, 220));
		lb_toSend.setHorizontalAlignment(SwingConstants.LEFT);
		lb_toSend.setFont(new Font("微软雅黑", Font.BOLD | Font.ITALIC, 15));
		lb_toSend.setBounds(10, 153, 251, 28);
		contentPane.add(lb_toSend);

		btn_Send = new JButton("\u53D1   \u9001");
		btn_Send.setFont(new Font("新宋体", Font.PLAIN, 12));
		btn_Send.setBackground(SystemColor.controlHighlight);
		btn_Send.setBounds(409, 367, 109, 23);
		contentPane.add(btn_Send);

		JLabel lb_Receive = new JLabel("MessageReceived");
		lb_Receive.setHorizontalAlignment(SwingConstants.LEFT);
		lb_Receive.setForeground(new Color(211, 211, 211));
		lb_Receive.setFont(new Font("微软雅黑", Font.BOLD | Font.ITALIC, 15));
		lb_Receive.setBounds(10, 284, 251, 28);
		contentPane.add(lb_Receive);

		textArea_Receive = new JTextArea();
		textArea_Receive.setFont(new Font("Monospaced", Font.PLAIN, 15));
		textArea_Receive.setForeground(SystemColor.windowText);
		textArea_Receive.setBackground(new Color(245, 245, 245));
		textArea_Receive.setBounds(10, 315, 370, 99);
		textArea_Receive.setLineWrap(true);;
		contentPane.add(textArea_Receive);

		bt_Data = new JButton("\u751F \u6210 \u6570 \u636E");
		bt_Data.setFont(new Font("新宋体", Font.PLAIN, 12));
		bt_Data.setBackground(SystemColor.controlHighlight);
		bt_Data.setBounds(409, 251, 109, 23);
		contentPane.add(bt_Data);

		lb_connect = new JLabel("\u5F53\u524D\u672A\u8FDE\u63A5\u670D\u52A1\u7AEF");
		lb_connect.setHorizontalAlignment(SwingConstants.RIGHT);
		lb_connect.setForeground(Color.LIGHT_GRAY);
		lb_connect.setBounds(341, 422, 206, 15);
		contentPane.add(lb_connect);

		btn_ClearSend = new JButton("\u6E05  \u7A7A");
		btn_ClearSend.setFont(new Font("新宋体", Font.PLAIN, 12));
		btn_ClearSend.setBackground(SystemColor.controlHighlight);
		btn_ClearSend.setForeground(SystemColor.windowText);
		btn_ClearSend.setBounds(263, 152, 93, 23);
		contentPane.add(btn_ClearSend);

		btn_clearReceive = new JButton("\u6E05  \u7A7A");
		btn_clearReceive.setFont(new Font("新宋体", Font.PLAIN, 12));
		btn_clearReceive.setBackground(SystemColor.controlHighlight);
		btn_clearReceive.setBounds(263, 284, 93, 23);
		contentPane.add(btn_clearReceive);

		JLabel lb_dynamic = new JLabel("");
		lb_dynamic.setHorizontalAlignment(SwingConstants.RIGHT);
		lb_dynamic.setForeground(Color.LIGHT_GRAY);
		lb_dynamic.setBounds(-100, 422, 206, 15);
		contentPane.add(lb_dynamic);
		
		scrollPane_rec = new JScrollPane(textArea_Receive);
		scrollPane_rec.setBounds(10, 315, 370, 99);
		contentPane.add(scrollPane_rec);
		
		JLabel lb_sequence = new JLabel("\u5E27\u5E8F\u53F7\uFF1A");
		lb_sequence.setForeground(Color.LIGHT_GRAY);
		lb_sequence.setFont(new Font("宋体", Font.PLAIN, 12));
		lb_sequence.setBounds(409, 137, 62, 22);
		contentPane.add(lb_sequence);
		
		tf_sequence = new JTextField();
		tf_sequence.setText("00");
		tf_sequence.setHorizontalAlignment(SwingConstants.CENTER);
		tf_sequence.setFont(new Font("微软雅黑", Font.PLAIN, 16));
		tf_sequence.setColumns(10);
		tf_sequence.setBackground(Color.LIGHT_GRAY);
		tf_sequence.setBounds(459, 135, 59, 24);
		contentPane.add(tf_sequence);
	}

	/**
	 * Help Methods
	 */
	// 返回固定长度的随机序列
	private static String getFixlenthString(int strLen) {
		Random rd = new Random();
		double pross = ((1 + rd.nextDouble()) * Math.pow(10, strLen));// 获取随机数
		String fixedLenthString = String.valueOf(pross);// 转化为字符串
		return fixedLenthString.substring(1, strLen + 1);
	}

	/**
	 * 与连接相关的方法
	 *
	 */
	public void connect() {
		int reconnectTimes = 0;
		while (connectState) {
			try {
				serverIP = this.tf_IP.getText();
				port = Integer.parseInt(this.tf_Port.getText());
				//lb_State.setText("正在连接...");
				sc = new Socket(this.serverIP, this.port);
				dos = new DataOutputStream(sc.getOutputStream());// 初始化输出流
				dis = new DataInputStream(sc.getInputStream());// 初始化输入流
				// System.out.println("success to connect the server！");
				lb_connect.setText("成功连接服务端[" + serverIP + ":" + port + "]");
				lb_State.setText("已 连 接");
				// 更新按钮状态
				btn_Connect.setEnabled(false);
				tf_IP.setEnabled(false);
				tf_Port.setEnabled(false);
				connectState = false;
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (ConnectException e) {// 捕获连接异常，并尝试重连
				System.out.println("trying to connect...");
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				reconnectTimes++;
				if (reconnectTimes > 3) {
					connectState = true;
					break;
				}
				continue;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// 消息监听内部类
	class InfoGetter implements Runnable {
		@Override
		public void run() {
			bConnected = true;
			while (bConnected) {
				try {
					String str = null;
					byte[] bs = new byte[1024];
					int len;
					if ((len = dis.read(bs)) > 0) {
						str = new String(bs, 0, len, "ASCII");
						textArea_Receive.append(str);
					}
					System.out.println(str);
				} catch (Exception e) {
					System.out.println("disconnected!");
					bConnected = false;
					disconnect();
				}
			}

		}

	}

	public void send(String s) {
		if(s == null) {return;}
		try {
			dos.write(s.getBytes("ASCII"));// 发送字符串按ASCII解码成的字节流
		} catch (IOException e) {
			System.out.println("由于不明原因发送失败！");
			e.printStackTrace();
		}
	}

	// 断开连接处理
	public void disconnect() {
		try {
			if (dos != null)
				dos.close();
			if (sc != null)
				sc.close();
			// 更新连接状态
			connectState = true;
			// 更新按钮状态
			btn_Connect.setEnabled(true);
			tf_IP.setEnabled(true);
			tf_Port.setEnabled(true);
			lb_connect.setText("当前未连接服务端");
			lb_State.setText("连接失败");
			receiveThread = null;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
