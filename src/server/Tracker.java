package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Tracker {
	private Map<Socket, User> users = new HashMap<Socket, User>();
	private Map<String, Set<String>> uploads = new HashMap<String, Set<String>>();
	private ExecutorService executor = Executors.newCachedThreadPool();
	private ServerSocket serverSocket;
	
	public Tracker(int port) throws IOException {
		serverSocket = new ServerSocket(port);
		
		new Thread() {
            public void run() {
            	Socket socket;
            	while(true){
        			System.out.println("Waiting for peer...");
        			
					try {
						//Should i close this socket and where
						socket = serverSocket.accept();
						executor.execute(new TrackerWorker(socket, users, uploads));
						Thread.sleep(10);
					} catch (Exception e) {
						System.out.println("Problem establishing connection");
						e.printStackTrace();
					}
        		}
            }
        }.start();
	}
	
	
	public Map<Socket, User> getUsers() {
		return users;
	}

	public Map<String, Set<String>> getUploads() {
		return uploads;
	}
	
	//TODO close method


	public static void main(String[] args) {
		try {
		Tracker myTracker = new Tracker(1111);//(Integer.parseInt(args[0]));
		} catch(IOException e) {
			System.out.println("Problem with running server on port " + args[0]);
		}
		return;
	}
}
