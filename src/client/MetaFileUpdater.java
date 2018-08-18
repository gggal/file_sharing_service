package client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

public class MetaFileUpdater extends Thread {

	static final private File file = new File("metaInfo.txt");
	private DataInputStream in;
	private DataOutputStream out;

	public MetaFileUpdater(DataInputStream in, DataOutputStream out) {
		this.in = in;
		this.out = out;
	}

	public void run() {
		try {
			while (true) {
				synchronized (in) {
					synchronized (out) {
						try (DataOutputStream fileOut = new DataOutputStream(new FileOutputStream(file.toString(), false))) {
						out.writeInt(5);
						int state = in.readInt();
						if (state != 0) {
							System.out.println(in.readUTF());
							return;
						}
						int cntUsers = in.readInt();
						PrintWriter pw = new PrintWriter(file.toString());
						pw.close();
						fileOut.writeInt(cntUsers);
						for (int i = 0; i < cntUsers; ++i) {
							fileOut.writeUTF(in.readUTF());
							fileOut.writeUTF(in.readUTF());
							fileOut.writeInt(in.readInt());
						}
						} catch (Exception e) {
							e.printStackTrace();
							try {
								in.close();
								out.close();
							} catch (IOException e1) {
								e1.printStackTrace();
							}
						}
					}
				}
				Thread.sleep(30_000);
				new PrintWriter(file.toString()).close();
			} 
		} catch (Exception e) {
			e.printStackTrace();
			try {
				in.close();
				out.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}

}
