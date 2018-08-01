package server;

import java.io.Serializable;

public class User implements Serializable{


	private static final long serialVersionUID = 1L;
	private String username;
	private int miniServerPort;
	private String miniServerInetAddress;
	
	public User(String username, int port, String inetAddress) {
		this.username = username;
		miniServerPort = port;
		miniServerInetAddress = inetAddress;
	}
	
	public String getUsername() {
		return username;
	}
	
	public int getPort() {
		return miniServerPort;
	}
	
	public String getInetAddress() {
		return miniServerInetAddress;
	}

}
