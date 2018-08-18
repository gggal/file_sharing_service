package client;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class ClientTest {

	static TestServer tracker;
	static Peer client1;
	static Peer client2;
	static Peer client3;

	private ByteArrayOutputStream out = new ByteArrayOutputStream();

	@BeforeAll
	static void initialize() throws Exception {
		tracker = new TestServer();
		client1 = new Peer("user1", 4101, 5001, 1111);
		client2 = new Peer("user2", 4102, 5002, 1111);
		client3 = new Peer("user3", 4103, 5003, 1111);
	}

	@BeforeEach
	void setUp() throws IOException {
		assertEquals(client1.executeCommand("register f1.txt"), 0);
		assertEquals(client2.executeCommand("register f2.txt"), 0);
		assertEquals(client3.executeCommand("register f3.txt"), 0);
	}

	public String getCommandOutput(String command) throws IOException {
		out = new ByteArrayOutputStream();
		System.setOut(new PrintStream(out)); // stream that needs to be closed
		client1.executeCommand(command);
		String output = out.toString();
		System.setOut(System.out);
		out.close();
		return output;
	}

	@Disabled
	void shouldRegisterFile() throws IOException {
		assertEquals(getCommandOutput("list-files"), "user1: f1.txt\nuser2: f2.txt\nuser3: f3.txt\n");
	}

	@Test
	void shouldNotRegisterNonexistingFile() throws IOException {
		assertEquals(getCommandOutput("register f4.txt"), "f4.txt doesn't exist.\n");
	}

	@Test
	void shouldNotRegisterDirectory() throws IOException {
		assertEquals(getCommandOutput("register dir"), "dir is directory and cannot be shared.\n");
	}

	@Test
	void shouldIgnoreRegisteredFile() throws IOException {
		String listedFilesBefore = getCommandOutput("list-files");

		String listedFilesAfter = getCommandOutput("list-files");
		assertEquals(listedFilesBefore, listedFilesAfter);
	}

	@Test
	void shouldUnregisterAFile() throws IOException {
		client1.executeCommand("unregister f1.txt");
		String listedFiles = getCommandOutput("list-files");
		String[] arr = listedFiles.split("[\n]");
		for (String str : arr) {
			if (str.startsWith("user1")) {
				assertFalse(str.contains("f1.txt"));
			}
		}
	}

	@Test
	void shouldNotUnregisterNotRegisteredFile() throws IOException {
		assertEquals(client1.executeCommand("unregister f4.txt"), 1);
		assertEquals(getCommandOutput("unregister f4.txt"), "f4.txt doesn't exist.\n");
	}

	@Test
	void shouldUpdate() throws FileNotFoundException, IOException {
		Set<String> usersSet = new HashSet<String>();
		try (DataInputStream fileIn = new DataInputStream(new FileInputStream("metaInfo.txt"))) {
			int cntUsers = fileIn.readInt();
			for (int i = 0; i < cntUsers; ++i) {
				usersSet.add(fileIn.readUTF() + " " + fileIn.readUTF() + " " + fileIn.readInt());
			}
		}
		Set<String> expected = new HashSet<String>(
		Arrays.asList("user1 localhost 4101", "user2 localhost 4102", "user3 localhost 4103"));
		assertEquals(expected, usersSet);
	}

	@Test
	void shouldDownloadFile() throws IOException {
		assertEquals(client1.executeCommand("download user2 f2.txt f2(1).txt"), 0);
		byte[] f2 = Files.readAllBytes(Paths.get("f2(1).txt"));
		assertTrue(Arrays.equals(f2, "file2 content.\n".getBytes()));
		Files.delete(Paths.get("f2(1).txt"));
	}

	@Test
	void shouldNotDownloadFromUnexistingUser() throws IOException {
		assertEquals(getCommandOutput("download user4 f2.txt f2(1).txt"), "No such peer registered in tracker info.\n");
	}

	@Test
	void shouldNotDownloadUnexistingFile() throws IOException {
		assertEquals(getCommandOutput("download user2 f4.txt f4(1).txt"), "f4.txt doesn't exist or cannot open.\n");
	}

	@Test
	void shouldNotDownloadIfFIleAlreadyExists() throws IOException {
		assertEquals(getCommandOutput("download user2 f3.txt f3.txt"), "f3.txt already exists.\n");
	}

	@Test
	void shouldNotCreateFileAfterUnsuccessfulDownload() throws IOException {
		client1.executeCommand("download user4 f2.txt f2(2).txt");
		File file = new File("f2(2).txt");
		assertFalse(file.exists());
	}

	@Test
	void shouldNotSendUnknownCommand() throws IOException {
		assertEquals(getCommandOutput("unknown_command"), "Unknown command unknown_command.\n");
	}
}
