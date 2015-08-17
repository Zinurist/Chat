package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

public class Server implements Runnable {

	public static void main(String[] args) {
		try {
			Server s=new Server(10000);
			new Thread(s).start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		JFrame f=new JFrame("Server Exit");
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setBounds(0,0,300,100);
		f.setVisible(true);
	}
	
	
	public static String readLine(BufferedReader in,String id) throws IOException{
		String s=null;
		do{
			try {
				StringBuffer buffer = new StringBuffer();
	            while (true) {
	                int ch = in.read();
	                if ((ch < 0) || (ch == '\n')) {
	                    break;
	                }
	                buffer.append((char) ch);
	            }
	            s = buffer.toString();
			}catch (IOException e) {
				throw new IOException("[CLIENT] "+id+": "+e.getMessage());
			}
		}while(s==null);
		return s;
	}
	
	public static void sendLine(PrintWriter out, String msg){
		out.print(msg+"\n");
		out.flush();
	}
	
	
	private volatile List<ChatRoom> rooms;
	private volatile List<ServerThread> threads;
	private boolean running;
	private int roomPorts;
	private ServerSocket server;

	
	public void stop(){
		try {
			server.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		for(ChatRoom c:rooms){
			c.closeConnections();
		}
		closeConnections();
	}
	
	public void closeConnections(){
		running=false;
		for(ServerThread t:threads){
			t.stop();
		}
		threads=new ArrayList<ServerThread>();
	}
	
	public boolean isRunning(){
		return running;
	}
	
	public List<ChatRoom> getRooms(){
		return rooms;
	}
	
	public synchronized void leave(ServerThread t){
		threads.remove(t);
	}
	
	public Server(int serverPort) throws IOException{
		roomPorts=serverPort+1;
		rooms=new ArrayList<ChatRoom>();
		threads=new ArrayList<ServerThread>();
		try {
			server=new ServerSocket(serverPort);
			server.setReuseAddress(true);
		} catch (IOException e) {
			throw e;
		}
	}

	public synchronized void closeRoom(ChatRoom room){
		if(!room.getName().equals("Main")){
			room.closeConnections();
			rooms.remove(room);
		}
	}
	
	public synchronized boolean addRoom(String name){
		boolean alreadyInUse=false;
		for(int i=0; i<rooms.size();i++){
			if(rooms.get(i).getName().equals(name)){
				alreadyInUse=true;
				break;
			}
		}
		if(!alreadyInUse){
			ChatRoom c=new ChatRoom(name,roomPorts++);
			rooms.add(c);
			new Thread(c).start();
		}
		return !alreadyInUse;//TODO send to all connected clients new list
	}
	
	public synchronized int getRoomPort(int id){
		return rooms.get(id).getPort();
	}
	
	public boolean isClosed(){
		return server.isClosed();
	}
	
	public void run() {
		try {
			running=true;
			addRoom("Main");
			Socket client;
			ServerThread servert;
			while(running){
				client=server.accept();
				servert=new ServerThread(this,client);
				threads.add(servert);
				new Thread(servert).start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
}
