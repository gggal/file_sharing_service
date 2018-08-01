package client;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
import java.util.logging.Logger;

import server.TrackerWorker;


public class MiniServer {

	private ServerSocket serverSocket;
	boolean toClose = false;
	//private ExecutorService executor = Executors.newCachedThreadPool();
	private final static Logger logger = Logger.getLogger(TrackerWorker.class.getName());

	public MiniServer(int port) throws IOException {
		serverSocket = new ServerSocket(port);
	}
	
	public void start() {
		new Thread() {
            public void run() {
            	while(!toClose){
        			//System.out.println("MiniServer is waiting for peer...");
        			
					try(Socket socket = serverSocket.accept()){
						//socket = serverSocket.accept();
						//System.out.println("MiniServer is connected to client with port" + socket.getPort());
						//executor.execute(new MiniServerWorker(socket));
					
						try(DataOutputStream out = new DataOutputStream(socket.getOutputStream());
							DataInputStream in = new DataInputStream(socket.getInputStream())) {
								
							//грешка в четенето на утф
							String fromDir = in.readUTF();;
							logger.log(Level.INFO, "Start");
							//File file = new File(fromDir);
							//достатъчно ли е isFIle или трябва exists
							if(!new File(fromDir).exists()) {
								logger.log(Level.INFO, "File doesnt exist.");
								out.writeInt(1);
								out.writeUTF(fromDir + " doesn't exist or cannot open.");
								out.flush();
								return;
							}
							try(InputStream fileIn = new BufferedInputStream(new FileInputStream(fromDir))) {
								out.writeInt(0);
								int count;
								byte[] buffer = new byte[8192];
								while ((count = fileIn.read(buffer)) > 0)
								{
									System.out.println("count: " + count);
								  out.write(buffer, 0, count);
								  out.flush();
								}
								out.flush();
							} catch (Exception e) {
								out.writeInt(1);
								out.writeUTF("File doesn't exist or cannot open.");
								out.flush();
							}

						}catch (IOException ioe) {
							ioe.printStackTrace();
						}
					} catch (IOException e1) {
						e1.printStackTrace();
						System.out.println("A problem with mini server socket occured.");
					}
        		}
            }
        }.start();
	}
	
	public void close() {
		toClose = true;
	}
}

