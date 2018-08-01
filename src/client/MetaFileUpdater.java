package client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
//import java.net.Socket;

public class MetaFileUpdater extends Thread {
	
	static final private File file = new File("metaInfo.txt");
	private DataInputStream in;
	private DataOutputStream out;
	
	public MetaFileUpdater(DataInputStream in, DataOutputStream out) {
		this.in = in;
		this.out = out;
	}
	
    public void run() {
    	System.out.println("Trying to update..");
    	try(DataOutputStream fileOut = new DataOutputStream(new FileOutputStream(file.toString(), false))) {
    		while(true){
    			out.writeInt(5);
    			synchronized(in) {
    				synchronized(out) {
						//fileOut = );
						int state = in.readInt();
						if(state != 0) {
							System.out.println(in.readUTF());
							return;
						}
						int cntUsers = in.readInt();
						fileOut.writeInt(cntUsers);
						for(int i = 0; i < cntUsers; ++i) {
							fileOut.writeUTF(in.readUTF());
							fileOut.writeUTF(in.readUTF());
							fileOut.writeInt(in.readInt());
						}
    				}
				}
				Thread.sleep(30_000);
    		}
        } catch (Exception e) {
            e.printStackTrace();
            try {
				in.close();
				 out.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        }
    }

}
