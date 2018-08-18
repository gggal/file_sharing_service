package client;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Scanner;

public class Peer {

	static final private File file = new File("metaInfo.txt");
	private MiniServer miniServer;
	private Socket peerSocket;
	private DataInputStream in;
	private DataOutputStream out;
	
	public Peer(String username, int miniServerPort, int clientPort, int serverPort) throws Exception {
		this(username, miniServerPort, clientPort, serverPort, "localhost");
	}
	
	public Peer(String username, int miniServerPort, int ClientPort, int serverPort, String serverAddress) throws Exception {
		try {
			miniServer = new MiniServer(miniServerPort);
			peerSocket = new Socket(serverAddress, serverPort, InetAddress.getLocalHost(), ClientPort);
			out = new DataOutputStream(peerSocket.getOutputStream());
			in = new DataInputStream(peerSocket.getInputStream());
			out.writeInt(1);
			out.writeInt(miniServerPort);
			out.writeUTF("localhost");
			out.writeUTF(username);
			out.flush();

			int a = in.readInt();
			if (a != 0) {
				System.out.println(in.readUTF());
				peerSocket.close();
				throw new Exception(in.readUTF());
			}
			MetaFileUpdater updater = new MetaFileUpdater(in, out);
			updater.start();
			miniServer.start();
			System.out.println("Socket created on port " + peerSocket.getLocalPort());
		} catch (Exception e) {
			if (in != null)
				in.close();
			if (out != null)
				out.close();
			miniServer.close();
			throw e;
		}
	}

	private String findPeer(String username) {
		try (DataInputStream fileIn = new DataInputStream(new FileInputStream(file.toString()))) {
			int usersCount = fileIn.readInt();
			for (int i = 0; i < usersCount; ++i) {
				if (fileIn.readUTF().equals(username)) {
					return fileIn.readUTF() + ":" + fileIn.readInt();
				}
				fileIn.readUTF();
				fileIn.readInt();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private int download(String command) {
		String[] words = command.split("[ \t]");
		words = Arrays.copyOfRange(words, 1, words.length);
		String info = findPeer(words[0]);
		if (info == null) {
			System.out.println("No such peer registered in tracker info.");
			return 1;
		}
		String[] socketinfo = info.split(":");
		
		File f = new File(words[2]);
		if (f.exists() || f.isDirectory()) {
			System.out.println(words[2] + " already exists.");
			return 1;
		}
		
		Socket socket;
		try {
			socket = new Socket(socketinfo[0], Integer.parseInt(socketinfo[1]));
		} catch (Exception e) {
			System.out.println("Couldn't connect with this peer.");
			e.printStackTrace();
			return 1;
		}

		int count;
		try (DataOutputStream peerOut = new DataOutputStream(socket.getOutputStream());
				DataInputStream peerIn = new DataInputStream(socket.getInputStream())) {
			peerOut.writeUTF(words[1]);
			peerOut.flush();
			if (peerIn.readInt() == 0) {
				try (BufferedOutputStream fileOut = new BufferedOutputStream(new FileOutputStream(words[2], false))) {
					byte[] buffer = new byte[8192];
					while ((count = peerIn.read(buffer)) > 0) {
						fileOut.write(buffer, 0, count);
						fileOut.flush();
					}
					return register("register " + words[2]);
					//return 0;
				} catch (Exception e) {
					Files.deleteIfExists(Paths.get(words[2]));
					return 1;
				}
			} else {
				System.out.println(peerIn.readUTF());
				return 1;
			}

		} catch (IOException e) {
			System.out.println("Couldn't connect with user.");
			return 1;
		} catch (Exception e) {
			System.out.println("File doesn't exist, is directory or can't be opened.");
			return 1;
		} finally {
			try {
				socket.close();
			} catch (IOException e) {
				System.out.println("Couldn't close socket " + socket.toString() + ". ");
				e.printStackTrace();
			}
		}

	}

	private int register(String command) throws IOException {
		String[] words = command.split("[ \n\t]");
		words = Arrays.copyOfRange(words, 1, words.length);

		for (String s : words) {
			File f = new File(s);
			if (!f.exists()) {
				System.out.println(f + " doesn't exist.");
				return 1;
			} else if (f.isDirectory()) {
				System.out.println(f + " is directory and cannot be shared.");
				return 1;
			}
		}

		out.writeInt(2);
		out.writeInt(words.length);
		for (String s : words) {
			out.writeUTF(s);
		}
		out.flush();
		int res = in.readInt();
		if (res != 0) {
			System.out.println(in.readUTF());
		}
		return res;
	}

	private int unregister(String command) throws IOException {
		String[] words = command.split("[ \n\t]");
		words = Arrays.copyOfRange(words, 1, words.length);

		for (String s : words) {
			File f = new File(s);
			if (!f.exists() || f.isDirectory()) {
				System.out.println(f + " doesn't exist.");
				return 1;
			}
		}
		out.writeInt(3);
		out.writeInt(words.length);
		for (String s : words) {
			out.writeUTF(s);
		}
		out.flush();
		int res = in.readInt();
		if (res != 0) {
			System.out.println(in.readUTF());
		}
		return res;
	}

	private void list_files() throws IOException {
		out.writeInt(4);
		out.flush();
		int n = in.readInt();
		if (n != 0) {
			System.out.println(in.readUTF());
			return;
		} else {

			int cntUsers = in.readInt();
			int cntUploads;
			String userInfo;
			for (int i = 0; i < cntUsers; ++i) {
				cntUploads = in.readInt();
				userInfo = in.readUTF() + ":";
				for (int j = 0; j < cntUploads; ++j) {
					userInfo += " " + in.readUTF();
				}
				System.out.println(userInfo);
			}
		}
	}

	public int executeCommand(String command) throws IOException {

		synchronized (in) {
			synchronized (out) {
				switch (command.split("[ \n\t]")[0]) {
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
	
	public void close() {
		miniServer.close();
		try {
			peerSocket.close();
		} catch (IOException e) {
			System.out.println("Couldn't close socket " + peerSocket.toString() + ". ");
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		Peer peer;
		try {
			if(args.length < 4 || args.length > 5) {
				System.out.println("Invalid argument count. Given arguemnts should be"
						+ "mini server port, client port, server port and/or server address");
				return;
			}
			else {
				peer = new Peer(args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]));
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Couldn't create client on this port.");
			return;
		}
		System.out.println("Successfully initialized the client");
		try {
			// peer.executeCommand("list-files");
			// peer.executeCommand("register f1.txt");
			@SuppressWarnings("resource")
			Scanner scanner = new Scanner(System.in);
			String command = scanner.nextLine();;
			while (!command.equals("close")) {
				if(peer.executeCommand(command) == 0)
					System.out.println("The command was successfully executed.");
				command = scanner.nextLine();
			}
			peer.close();

		} catch (IOException e) {
			System.out.println("Problem with reading/writing to the server occured"
					+ "and connection is broken.");
			e.printStackTrace();
			peer.close();
		}
		return;
	}
}
