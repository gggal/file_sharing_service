package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public class TestClient {
	private Socket peerSocket;
	private DataInputStream in;
	private DataOutputStream out;

	public TestClient(String username, int ClientPort) throws Exception {
		try {
			peerSocket = new Socket("localhost", 2222, InetAddress.getLocalHost(), ClientPort);
			out = new DataOutputStream(peerSocket.getOutputStream());
			in = new DataInputStream(peerSocket.getInputStream());

			out.writeInt(1);
			out.writeInt(0);
			out.writeUTF("localhost");
			out.writeUTF(username);
			out.flush();
			if (in.readInt() != 0) {
				System.out.println(in.readUTF());
				peerSocket.close();
				throw new Exception();
			}

		} catch (Exception e) {
			if (peerSocket != null)
				peerSocket.close();
			throw e;
		}
	}

	public int register(String command) throws IOException {
		String[] words = command.split("[ \n\t]");
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

	public int unregister(String command) throws IOException {
		String[] words = command.split("[ \n\t]");
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

	public String list_files() throws IOException {
		out.writeInt(4);
		out.flush();
		int n = in.readInt();
		String res = "";
		if (n == 0) {
			int cntUsers;
			cntUsers = in.readInt();
			for (int i = 0; i < cntUsers; ++i) {
				int cntUploads = in.readInt();
				res += in.readUTF() + ":";
				for (int j = 0; j < cntUploads; ++j) {
					res += " " + in.readUTF();
				}
				res += "\n";
			}
		} else {
			System.out.println(in.readUTF());
		}
		return res;
	}

	public String update() throws IOException {
		out.writeInt(5);
		out.flush();
		int n = in.readInt();
		String res = "";
		if (n == 0) {
			int cntUploads = in.readInt();
			System.out.println("number of files " + cntUploads);
			for (int i = 0; i < cntUploads; ++i) {
				res = res + in.readUTF() + " ";
				in.readUTF();
				in.readInt();
			}
		} else {
			System.out.println(in.readUTF());
		}
		return res;
	}

	public String send_command(int command) throws IOException {
		out.writeInt(command);
		out.flush();
		in.readInt();
		return in.readUTF();
	}

	public void closeSocket() throws IOException {
		peerSocket.close();
	}
}
