package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;

public class ServerThread implements Runnable {

	private boolean running;
	private Socket client;
	private Server server;
	private BufferedReader in;
	private PrintWriter out;
	
	public void stop(){
		running=false;
	}
	
	public boolean isRunning(){
		return running;
	}
	
	public ServerThread(Server server, Socket client){
		running=true;
		this.client=client;
		this.server=server;
	}
	
	
	@Override
	public void run() {
		try {	
			in=new BufferedReader(new InputStreamReader(client.getInputStream()));
			out=new PrintWriter(client.getOutputStream());
			String cmd;
			do{
				cmd=Server.readLine(in,"(Server)Cmdread");
				if(cmd.equals("g")){//getlist
					List<ChatRoom> rooms=server.getRooms();
					for(int i=0; i<rooms.size();i++){
						Server.sendLine(out, rooms.get(i).getName());
					}
					//reached end of list
					Server.sendLine(out, "e");
				}else if(cmd.equals("c")){//create room
					String name=Server.readLine(in,"(Server)Nameread");
					
					if(server.addRoom(name)){
						//succeeded
						Server.sendLine(out, "s");
					}else{
						//failed
						Server.sendLine(out, "f");
					}
				}else if(cmd.equals("j")){//join roomName/roomID username	
					int roomID=Integer.parseInt(Server.readLine(in,"(Server)RoomIDread"));//TODO maybe with name
					int roomPort;
					try{
						roomPort=server.getRoomPort(roomID);
						Server.sendLine(out, roomPort+"");
					}catch(Exception e){
						Server.sendLine(out, "f");
					}
				}
			}while(running && !cmd.equals("e"));//done
			
			
			in.close();
			out.close();
			client.close();
			
			server.leave(this);
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
}
