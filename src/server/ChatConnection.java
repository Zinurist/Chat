package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Calendar;

public class ChatConnection implements Runnable {
	
	private final Socket client;
	private final ChatRoom room;
	private String username;
	private int id;
	private String msgBuffer;
	private BufferedReader in;
	private PrintWriter out;
	private boolean running;
	
	public Calendar cal;
	
	public ChatConnection(ChatRoom room,Socket client,int id){
		this.room=room;
		this.client=client;
		this.id=id;
		msgBuffer="Chatroom: "+room.getName()+", currently "+(room.getNumOfUsers()+1)+" user(s) connected";
	}
	
	public synchronized void print(String msg){
		msgBuffer+="\n"+msg;
	}
	
	public void stop(){
		running=false;
	}
	
	@Override
	public void run() {
		try {
			in=new BufferedReader(new InputStreamReader(client.getInputStream()));
			out=new PrintWriter(client.getOutputStream());
			username=in.readLine();
			String msg;
			running=true;
			
			cal = Calendar.getInstance();
			room.message(username+"_"+id+" has joined.","SERVER",cal.getTime());
			do{
				msg=Server.readLine(in,"(Server)Msgread");
				if(!msg.isEmpty()){
					cal = Calendar.getInstance();
					if(msg.equals("e")){
						room.message(username+"_"+id+" has left.","SERVER",cal.getTime());
					}else{
						room.message(msg,username+"_"+id,cal.getTime());
					}
				}
				if(!msgBuffer.isEmpty()){
					Server.sendLine(out, msgBuffer);
					msgBuffer="";
				}
				Server.sendLine(out, "");
			}while(running && !msg.equals("e"));//TODO something else than "e"
			
			in.close();
			out.close();
			client.close();
			
			room.leave(this);
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
	@Override
	public boolean equals(Object o){
		try{
			ChatConnection c=(ChatConnection)o;
			return c.username.equals(username);
		}catch(Exception e){}
		return false;
	}
}
