package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
//import java.util.logging.Level;
//import java.util.logging.Logger;

public class TrackerWorker extends Thread {
	private Socket socket;
	private Map<Socket, User> info;
	private Map<String, Set<String>> uploads;

	// private final static Logger logger =
	// Logger.getLogger(TrackerWorker.class.getName());

	public TrackerWorker(Socket socket, Map<Socket, User> info, Map<String, Set<String>> uploads) {
		System.out.println("Client " + socket.toString() + " has connected.");
		this.socket = socket;
		this.info = info;
		this.uploads = uploads;
	}

	public void run() {

		try (DataOutputStream out = new DataOutputStream(socket.getOutputStream());
				DataInputStream in = new DataInputStream(socket.getInputStream())) {

			while (true) {
				int option = in.readInt();

				if (!info.containsKey(socket) && option != 1) {
					out.writeInt(1);
					out.writeUTF("Unregistered user.");
					out.flush();
					return;
				}

				switch (option) {
				case 1:
					add(in, out);
					break;
				case 2:
					register(in, out);
					break;
				case 3:
					unregister(in, out);
					break;
				case 4:
					list_files(in, out);
					break;
				case 5:
					update(in, out);
					break;
				default:
					out.writeInt(1);
					out.writeUTF("Unknown command.");
					out.flush();
				}
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} finally {
			System.out.println("Client " + socket.toString() + " has disconnected.");
			String toDelete = info.get(socket).getUsername();
			info.remove(socket);
			uploads.remove(toDelete);
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	void add(DataInputStream in, DataOutputStream out) throws IOException {
		int port = in.readInt();
		String adrs = in.readUTF();
		String name = in.readUTF();
		if (uploads.containsKey(name)) {
			out.writeInt(1);
			out.writeUTF("Taken username.");
			out.flush();
			return;
		}
		User user = new User(name, port, adrs);
		info.put(socket, user);
		uploads.put(name, new HashSet<String>());
		out.writeInt(0);
		out.flush();
	}

	void register(DataInputStream in, DataOutputStream out) throws IOException {
		int uploadsCount = in.readInt();
		String user = info.get(socket).getUsername();
		for (int i = 0; i < uploadsCount; ++i) {
			String uploadpath = in.readUTF();
			uploads.get(user).add(uploadpath);
		}
		out.writeInt(0);
		out.flush();
	}

	void unregister(DataInputStream in, DataOutputStream out) throws IOException {
		String user = info.get(socket).getUsername();
		int uploadsCount = in.readInt();

		for (int i = 0; i < uploadsCount; ++i) {
			String uploadpath = in.readUTF();
			uploads.get(user).remove(uploadpath);
		}
		out.writeInt(0);
		out.flush();
	}

	void list_files(DataInputStream in, DataOutputStream out) throws IOException {
		out.writeInt(0);
		out.writeInt(uploads.size());
		for (String user : uploads.keySet()) {
			out.writeInt(uploads.get(user).size());
			out.writeUTF(user);
			for (String upload : uploads.get(user))
				out.writeUTF(upload);
		}
		out.flush();
	}

	void update(DataInputStream in, DataOutputStream out) throws IOException {
		out.writeInt(0);
		out.writeInt(info.size());
		for (User user : info.values()) {
			out.writeUTF(user.getUsername());
			out.writeUTF(user.getInetAddress());
			out.writeInt(user.getPort());
		}
		out.flush();
	}
}
