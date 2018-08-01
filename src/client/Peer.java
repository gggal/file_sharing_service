package client;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import server.TrackerWorker;


public class Peer {

	static final private File file = new File("metaInfo.txt");
	private MiniServer miniServer;
	private Socket peerSocket;
	DataInputStream in;
	DataOutputStream out; //пише
	private final static Logger logger = Logger.getLogger(TrackerWorker.class.getName());
	
	public Peer(String username, int miniServerPort, int ClientPort) throws Exception {
		try {
			miniServer = new MiniServer(miniServerPort);
			peerSocket = new Socket("localhost", 1111, InetAddress.getLocalHost(), ClientPort);
			out = new DataOutputStream(peerSocket.getOutputStream());
			in = new DataInputStream(peerSocket.getInputStream());
			out.writeInt(1);
			out.writeInt(miniServerPort);
			out.writeUTF("localhost");
			out.writeUTF(username);
			out.flush();

			int a = in.readInt();
			if(a != 0) {
				System.out.println(in.readUTF());
				peerSocket.close();
				throw new Exception(in.readUTF());
			}
			MetaFileUpdater updater = new MetaFileUpdater(in, out);
			updater.start();
			miniServer.start();
			System.out.println("Socket created on port " + peerSocket.getLocalPort());
		} catch(Exception e) {
			if(in != null) in.close();
			if(out != null) out.close();
			miniServer.close();
			throw e;
		}
	}
	
	private String findPeer(String username) {
		synchronized(file) { //защо го синхоронизирам????
			try(DataInputStream fileIn = new DataInputStream(new FileInputStream(file.toString()))) {
				String user;
				int usersCount = fileIn.readInt();
				for(int i = 0; i < usersCount; ++i) {
					if(fileIn.readUTF().equals(username)) {
						return fileIn.readUTF() + ":" + fileIn.readInt();
					}
					fileIn.readUTF();
					fileIn.readInt();
				}
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	private int download(String command) {
		String[] words = command.split("[ \t]");
		words = Arrays.copyOfRange(words, 1, words.length);
		String info = findPeer(words[0]);
		if(info == null) {
			System.out.println("No such peer registered in tracker info.");
			return 1;
		}
		String[] socketinfo = info.split(":");
		logger.log(Level.INFO, "Socket info " + info);
		Socket socket;
		try{
			socket = new Socket(socketinfo[0], Integer.parseInt(socketinfo[1]));
		} catch (Exception e) {
			System.out.println("Couldn't connect with this peer.");
			e.printStackTrace();
			return 1;
		}

		File f = new File(words[2]);
		if(f.exists() || f.isDirectory()) { 
			System.out.println(words[2] + " already exists.");
			try {
				socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return 1;
		}
	
		int count;
		try(DataOutputStream peerOut = new DataOutputStream(socket.getOutputStream());
			DataInputStream peerIn = new DataInputStream(socket.getInputStream())) {
			peerOut.writeUTF(words[1]);
			peerOut.flush();
			logger.log(Level.INFO, "Just sent " + words[1]);
			if(peerIn.readInt() == 0) {
			try(BufferedOutputStream fileOut = new BufferedOutputStream(new FileOutputStream(words[2], false))) {
				byte[] buffer = new byte[8192]; // or 4096, or more
				while ((count = peerIn.read(buffer)) > 0)
				{
					System.out.println("received count " + count);
					fileOut.write(buffer, 0, count);
					fileOut.flush();
				}
				return 0;
			} catch(Exception e) {
				//TODO
				Files.deleteIfExists(Paths.get(words[2]));
				return 1;
			}
			} else {
				logger.log(Level.INFO, "here 2 ");
				//TODO: maybe others to send 1 for unexistant file and 2 otherwise
				System.out.println
				(peerIn.readUTF());
				return 1;
			}
			
		} catch (IOException e) {
			System.out.println("Couldn't connect with user.");
			return 1;
		} catch (Exception e) {
			System.out.println("File doesn't exist, is directory or can't be opened.");
			return 1;
		}finally {
			try {
				socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
			
	}
	
	private int register(String command) throws IOException {
		//System.out.println("Client is trying to register " + command);
		String[] words = command.split("[ \n\t]");
		words = Arrays.copyOfRange(words, 1, words.length);
		
		for(String s: words) {
			File f = new File(s);
			if(!f.exists()) { 
				System.out.println(f + " doesn't exist.");
				return 1;
			}
			else if(f.isDirectory()) {
				System.out.println(f + " is directory and cannot be shared.");
				return 1;
			}
		}
		
		out.writeInt(2);
		out.writeInt(words.length);
		for(String s : words) {
			out.writeUTF(s); 
		}
		out.flush();
		int res = in.readInt();
		if(res != 0) {
			System.out.println(in.readUTF());
		}
		//System.out.println("REGISTERED");
		return res;
	}
	
	private int unregister(String command) throws IOException {
		String[] words = command.split("[ \n\t]");
		words = Arrays.copyOfRange(words, 1, words.length);

		for(String s : words) {
			File f = new File(s);
			if(!f.exists() || f.isDirectory()) { 
				System.out.println(f + " doesn't exist.");
				return 1;
			}
		}
		out.writeInt(3);
		out.writeInt(words.length);
		for(String s : words) {
			out.writeUTF(s);
		}
		out.flush();
		int res = in.readInt();
		if(res != 0) {
			System.out.println(in.readUTF());
		}
		return res;
	}
	
	private void list_files() throws IOException {
		logger.log(Level.INFO, "listing files...");
		out.writeInt(4);
		out.flush();
		//System.out.println("Listing files...");
		int n = in.readInt();
		//System.out.println("n is " + n);
		if(n != 0) {
			System.out.println(in.readUTF());
			return;
		}
		else {
			
			int cntUsers = in.readInt();
			int cntUploads;
			String userInfo;
			logger.log(Level.INFO, "CntUsers is " + cntUsers);
			for(int i = 0; i < cntUsers; ++i) {
				cntUploads = in.readInt();
				logger.log(Level.INFO, "CntUploads is " + cntUploads);
				//System.out.println("CntUploads is " + cntUploads);
				userInfo = in.readUTF() + ":";
				//System.out.println("userInfo is " + userInfo);
				for(int j = 0; j < cntUploads; ++j) {
					logger.log(Level.INFO, "userInfo is " + userInfo);
					userInfo += " " + in.readUTF(); 
				}
				System.out.println(userInfo);
			}
		}
		logger.log(Level.INFO, "listed files...");
	}
	
	public int executeCommand(String command) throws IOException {

		synchronized(in) {
			synchronized(out) {
		//String[] words = command.split("[ \t]");
				switch(command.split("[ \n\t]")[0]) {
				case "download": 
					return download(command);
				case "register":
					return register(command);
				case "unregister":
					return unregister(command);
				case "list-files":
					list_files();
					return 0;
				default:
					System.out.println("Unknown command " + command + ".");
					return 1;
				}
			}
		}
	}
	
	public static void main(String[] args) {
		Peer me;
		try {
			String port = args[1];
			String port2 = args[2];
			me = new Peer(args[0], Integer.parseInt(port), Integer.parseInt(port2));
			//me = new Peer(4324, 1234);
		} catch(Exception e) {
			e.printStackTrace();
			System.out.println("Couldn't create client on this port.");
			return;
		}
		System.out.println("Successfully initialized the client");
		try {
			//me.executeCommand("list-files");
			//me.executeCommand("register f1.txt");
			 Scanner scanner = new Scanner(System.in);
			 String command;
			 while(true) {
				 command = scanner.nextLine();
				 me.executeCommand(command);
			 }
			 
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
