package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ChatRoom implements Runnable{

	private List<ChatConnection> connects;
	private String name;
	private int port,idCounter;
	private boolean running;
	
	public void setName(String name){
		this.name=name;
	}
	
	public String getName(){
		return name;
	}
	
	public int getPort(){
		return port;
	}
	
	public int getNumOfUsers(){
		return connects.size();
	}
	
	public ChatRoom(String name, int port){
		setName(name);
		this.port=port;
		idCounter=0;
		connects=new ArrayList<ChatConnection>();
	}
	
	public synchronized void message(String msg,String username,Date time){
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		String t=sdf.format(time);
		for(int i=0; i<connects.size();i++){	
			connects.get(i).print("["+t+"]"+username+": "+msg);
		}
	}
	
	public synchronized void leave(ChatConnection c){
		connects.remove(c);
	}
	
	public void closeConnections(){
		running=false;
		for(ChatConnection c:connects){
			c.stop();
		}
		connects=new ArrayList<ChatConnection>();
	}
	
	public void run() {
		running=true;
		try {
			ServerSocket server=new ServerSocket(port);
			Socket client;
			ChatConnection chatt;
			while(running){
				client=server.accept();
				chatt=new ChatConnection(this,client,idCounter++);
				connects.add(chatt);
				new Thread(chatt).start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
	@Override
	public boolean equals(Object o){
		try{
			ChatRoom c=(ChatRoom)o;
			return c.name.equals(name);
		}catch(Exception e){}
		return false;
	}
}
