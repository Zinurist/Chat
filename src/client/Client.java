package client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import server.Server;

import javax.swing.JLabel;

@SuppressWarnings("serial")
public class Client extends JFrame implements Runnable{

	public static void main(String[] args) {
		Client c= new Client();
		c.setVisible(true);
		new Thread(c).start();
	}
	
	private Server server;
	private String serverC;
	private int serverPort,roomPort;
	private volatile boolean connectedToServer,connectedToRoom,running;
	private Socket serverSocket,roomSocket;
	private BufferedReader inServer,inRoom;
	private PrintWriter outServer,outRoom;
	private JButton btnConnectServer,btnGetList,btnConnect,btnSend,btnCreateRoom;
	private JList<String> roomList;
	private JPanel contentPane,pServerStuff,pChatStuff,pChatControl,pServerButtons;
	private JTextArea tChat;
	private JLabel lRoomname;
	private JTextField tfChat,tfUsername;
	private JScrollPane spList,spChat;
	
	public Client(){
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		
		addWindowListener(new WindowAdapter(){
			@Override
			public void windowClosing(WindowEvent e){
				JFrame fr=(JFrame)e.getSource();
				disconnectFromServer();
				fr.dispose();
			}
		});
		
		connectedToServer=false;
		connectedToRoom=false;
		
		btnConnectServer=new JButton("Connect to server");
		btnConnectServer.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if(!connectedToServer){
					connectToServer();
				}else{
					disconnectFromServer();
				}	
			}
		});
		
		btnGetList=new JButton("(Re)Load room list");
		btnGetList.setEnabled(false);
		btnGetList.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent ae) {
				try {
					getList();
				} catch (Exception e) {
					msgChat(e.getMessage());
				}
			}
		});
		
		btnConnect=new JButton("Connect to room");
		btnConnect.setEnabled(false);
		btnConnect.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {			
				if(connectedToRoom){//dont check for server connection, bc of this btn being enabled/disabled
					disconnectFromRoom();
				}else{
					connectToRoom();
				}
			}
		});

		btnSend=new JButton("Send");
		btnSend.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				if(connectedToRoom){
					Server.sendLine(outRoom, tfChat.getText());
					tfChat.setText("");
				}
			}
		});
		
		btnCreateRoom=new JButton("Create Room");
		btnCreateRoom.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent arg0) {
				//disabled if not connected to server
				String name="";
				while(name.isEmpty()){
					name=JOptionPane.showInputDialog("Room name:");
					if(name==null){
						return;
					}
				}
				Server.sendLine(outServer, "c");
				Server.sendLine(outServer, name);
				try {
					String ans=Server.readLine(inServer, "Create room");
					if(ans.equals("f")){
						throw new Exception("[CLIENT] Creating room \""+name+"\" failed. Maybe this name is already taken?");
					}else{
						throw new Exception("[CLIENT] Creating room \""+name+"\" succeeded.");
					}
				} catch (Exception e) {
					msgChat(e.getMessage());
				}
			}
		});
		
		roomList=new JList<String>();
		roomList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		roomList.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		
		spList = new JScrollPane(roomList);
		
		tChat=new JTextArea("---Not connected---");
		tChat.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		tChat.setFont(new Font("Arial", Font.PLAIN,15));
		tChat.setEditable(false);
		tChat.setLineWrap(true);
		spChat=new JScrollPane(tChat);
		spChat.setPreferredSize(new Dimension(500,200));
		
		tfChat=new JTextField();
		tfChat.addKeyListener(new KeyListener(){
			@Override
			public void keyPressed(KeyEvent k) {
				if(k.getKeyCode()==KeyEvent.VK_ENTER){
					if(connectedToRoom){
						Server.sendLine(outRoom, tfChat.getText());
						tfChat.setText("");
					}
				}
			}
			@Override
			public void keyReleased(KeyEvent arg0) {}
			@Override
			public void keyTyped(KeyEvent arg0) {}
		});
		
		enableButtons(false);
		
		tfUsername=new JTextField("username");
		
		lRoomname=new JLabel("Room: ---");
		
		pChatControl=new JPanel(new BorderLayout());
		pChatControl.add(tfChat, BorderLayout.CENTER);
		pChatControl.add(btnSend, BorderLayout.EAST);
		
		
		pServerButtons=new JPanel(new GridLayout(0,1,0,5));
		pServerButtons.add(tfUsername);
		pServerButtons.add(btnConnectServer);
		pServerButtons.add(btnGetList);
		pServerButtons.add(btnConnect);
		
		pServerStuff=new JPanel(new BorderLayout());
		pServerStuff.add(pServerButtons,BorderLayout.NORTH);
		pServerStuff.add(spList,BorderLayout.CENTER);
		pServerStuff.add(btnCreateRoom,BorderLayout.SOUTH);
		pServerStuff.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		pServerStuff.setPreferredSize(new Dimension(200,200));
		
		pChatStuff=new JPanel(new BorderLayout());
		pChatStuff.add(lRoomname ,BorderLayout.NORTH);
		pChatStuff.add(spChat ,BorderLayout.CENTER);
		pChatStuff.add(pChatControl ,BorderLayout.SOUTH);	
		pChatStuff.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		
		contentPane=new JPanel();
		contentPane.setLayout(new BorderLayout());
		contentPane.add(pServerStuff, BorderLayout.WEST);
		contentPane.add(pChatStuff, BorderLayout.CENTER);
		setContentPane(contentPane);
		pack();
	}
	
	public void connectToServer(){
		//TODO get port?
		String ip="";
		while(ip.isEmpty()){
			ip=JOptionPane.showInputDialog("Server address:","localhost");
			if(ip==null){
				return;
			}
		}
		
		if(ip!=null){
			serverC=ip;
			serverPort=10000;
			roomList.removeAll();
			
			try {
				serverSocket = new Socket(serverC,serverPort);
				inServer=new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
				outServer=new PrintWriter(serverSocket.getOutputStream());
				
				getList();						
				connectedToServer=true;
				btnConnectServer.setText("Disconnect from server");
				enableButtons(true);
			} catch (Exception e) {
				msgChat(e.getMessage());
			}
		}
	}
	
	public void disconnectFromServer(){
		try {
			if(connectedToRoom){
				disconnectFromRoom();
			}
			
			Server.sendLine(outServer, "e");
			
			inServer.close();
			outServer.close();
			serverSocket.close();
			connectedToServer=false;
			btnConnectServer.setText("Connect to server");
			enableButtons(false);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void connectToRoom(){
		int chatroom=roomList.getSelectedIndex();
		String roomName=roomList.getSelectedValue();	
		try {
			Server.sendLine(outServer, "j");
			Server.sendLine(outServer, chatroom+"");
			
			String ans=Server.readLine(inServer,"Server/roomconnection");
			roomPort=Integer.parseInt(ans);//error if not the port but "f" for failed, noob
			
			roomSocket = new Socket(serverC,roomPort);
			inRoom=new BufferedReader(new InputStreamReader(roomSocket.getInputStream()));
			outRoom=new PrintWriter(roomSocket.getOutputStream());
			
			Server.sendLine(outRoom, tfUsername.getText());
			
			
			
			connectedToRoom=true;
			btnConnect.setText("Leave room");
			lRoomname.setText("Room: "+roomName);
		} catch (Exception e) {
			msgChat(e.getMessage());
			try {//same as the standard disconnect from server
				inServer.close();
				outServer.close();
				serverSocket.close();
				connectedToServer=false;
				btnConnectServer.setText("Connect to server");
				enableButtons(false);
			} catch (IOException e1) {
				e1.printStackTrace();
			}	
		}
	}
	
	public void disconnectFromRoom(){
		try {
			Server.sendLine(outRoom, "e");
			
			inRoom.close();
			outRoom.close();
			roomSocket.close();
			connectedToRoom=false;
			btnConnect.setText("Connect to room");
			lRoomname.setText("Room: ---");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void msgChat(String msg){
		tChat.setText(tChat.getText()+"\r\n"+msg);
		tChat.setCaretPosition(tChat.getDocument().getLength());
	}
	
	public void enableButtons(boolean enable){
		btnConnect.setEnabled(enable);
		btnCreateRoom.setEnabled(enable);
		btnGetList.setEnabled(enable);
	}

	public void getList() throws Exception{
		try{
			Server.sendLine(outServer, "g");
			String name;
			List<String> roomNames=new ArrayList<String>();
			do{
				name=Server.readLine(inServer,"Server/request roomlist");
				if(name.equals("e")){
					break;
				}
				roomNames.add(name);
			}while(true);
			roomList.setListData(roomNames.toArray(new String[1]));
			roomList.setSelectedIndex(0);
		}catch(Exception e){
			throw e;
		}
	}
	
	@Override
	public void run() {
		running=true;
		while(running){
			try{
				if(connectedToRoom){
					Server.sendLine(outRoom, "");
					String msg=Server.readLine(inRoom,"Chatroom");
					if(!msg.isEmpty()){
						msgChat(msg);
					}
				}
			}catch(Exception e){
				msgChat(e.getMessage());
				try {//same as the standard disconnect from room
					inRoom.close();
					outRoom.close();
					roomSocket.close();
					connectedToRoom=false;
					btnConnect.setText("Connect to room");
					lRoomname.setText("Room: ---");
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		}
	}
}
