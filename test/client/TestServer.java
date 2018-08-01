package client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import server.User;

public class TestServer {
	private Map<Socket, User> users = new HashMap<Socket, User>();
	private Map<String, Set<String>> uploads = new HashMap<String, Set<String>>();
	private ServerSocket serverSocket;
	
	public TestServer() throws IOException {
		
		serverSocket = new ServerSocket(1111);
		
		new Thread() {
            public void run() {
            	while(true){
        			//System.out.println("Waiting for peer...");
        			
						new Thread() {
							
							public void run() {
								try(Socket socket = serverSocket.accept()) {
								executeCommand(socket);
								} catch(Exception e) {
									System.out.println("Socket problem occured.");
									e.printStackTrace();
								}
							}
						}.start();
        		}
            }
        }.start();
	}
	
	void executeCommand(Socket socket) {
		//System.out.println("executing command from " + socket);
		try(DataOutputStream out = new DataOutputStream(socket.getOutputStream());
			DataInputStream in = new DataInputStream(socket.getInputStream())) {
			 	while(true) {
					//if(!info.containsKey(socket)) {
					//	ret
					//} TODO::::

					int uploadsCount = 0;
					User user;
					int option = in.readInt();
					switch(option) {
					case 1:
						System.out.println("add on " + socket);
						int port = in.readInt();
						String adrs = in.readUTF();
						String name = in.readUTF();
						//User user = (User)in.readObject();
						user = new User(name, port, adrs);
						//logger.log(Level.FINE, "Received " + user.toString() + "from " + socket.toString());
						users.put(socket, user);
						uploads.put(name, new HashSet<String>());
						out.writeInt(0);
						out.flush();
						break;
					case 2: 
						//System.out.println("register on " + socket);
						uploadsCount = in.readInt();
						String username = users.get(socket).getUsername();
						for(int i = 0; i < uploadsCount; ++i) {
							String uploadpath = in.readUTF();
							uploads.get(username).add(uploadpath);
						}
						//System.out.println("registered on " + socket);
						out.writeInt(0);
						out.flush();
						
						break;
					case 3:
						System.out.println("unregister on " + socket);
						uploadsCount = in.readInt();
						String username1 = users.get(socket).getUsername();
						for(int i = 0; i < uploadsCount; ++i) {
							String uploadpath = in.readUTF();
							uploads.get(username1).remove(uploadpath);
						}
						out.writeInt(0);
						out.flush();
						break;
					case 4:
						//System.out.println("list files on " + socket);
						out.writeInt(0);
						//logger.log(Level.INFO, "writitng Uploads.size()" + uploads.size());
						out.writeInt(uploads.size());
						for(String u: uploads.keySet()) {
							out.writeInt(uploads.get(u).size());
							//logger.log(Level.INFO, "writing uploads.get(u).size() " + uploads.get(u).size());
							out.writeUTF(u);
							//logger.log(Level.INFO, " writing U" + u);
							for(String upload: uploads.get(u))
								out.writeUTF(upload);
						}
						out.flush();
						break;	
					case 5:
						//System.out.println("update on " + socket);
						out.writeInt(0);
						out.writeInt(users.size());
						for(User u: users.values()) {
							out.writeUTF(u.getUsername());
							out.writeUTF(u.getInetAddress());
							out.writeInt(u.getPort());
						}	
						out.flush();
						break;
					}
				}
			} catch(IOException ioe) {

			}
	}
}
