package server;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;


class TrackerTest {
	static private Tracker tracker;
	static private TestClient client1;
	static private TestClient client2;
	static private TestClient client3;

	@BeforeAll
	static void setup() throws Exception {
		tracker = new Tracker(2222);
		client1 = new TestClient("user1", 4001);
		client2 = new TestClient("user2", 4002);
		client3 = new TestClient("user3", 4003);
		
	}
	
	@BeforeEach 
	void initialize() throws IOException {
		assertEquals(client1.register("f1.txt"), 0);
		assertEquals(client2.register("f2.txt"), 0);
		assertEquals(client3.register("f3.txt"), 0);
		
	}
	
	@Test
	void shouldSuccessfullyRegisterNewClient() {
		assertEquals(tracker.getUsers().size(), 3);
	}
	
	@Test
	void shouldNotRegisterClientWithTakenName() {
		Executable closure = () -> new TestClient("user1", 4004);
		assertThrows(Exception.class, closure, "");
		assertEquals(tracker.getUsers().size(), 3);	
	}
	
	@Test
	void shouldSuccessfullyUnregisterClient() throws Exception {
		TestClient client4 = new TestClient("user4", 8008);
		assertEquals(4, tracker.getUsers().size());
		client4.closeSocket();
		client2.register("f2.txt"); //omitting race condition
		assertEquals(3, tracker.getUsers().size());	
	}
	
	@Test
	void shouldRegisterFilesSuccessfully() {
		assertEquals(tracker.getUploads().size(), 3);
	}
	
	@Test
	void shouldIgnoreRegisteredFiles() throws IOException {
		Map<String, Set<String>> uploads = tracker.getUploads();
		client1.register("f1.txt");
		assertEquals(tracker.getUploads(), uploads);
	}

	@Test
	void shouldUnregisterFilesSuccessfully() throws IOException {
		client1.unregister("f1.txt");
		int uploadsCnt = tracker.getUploads().get("user1").size();
		assertEquals(uploadsCnt, 0);
	}
	
	@Test
	void shouldNotUnregisterNotRegisteredFile() throws IOException {
		client1.unregister("f2.txt");
		assertEquals(tracker.getUploads().get("user1").size(), 1);
	}
	
	@Test
	void shouldListFiles1() throws IOException {
		
		Set<String> set = new HashSet<>(Arrays.asList("user1: f1.txt", "user2: f2.txt", "user3: f3.txt"));
		String files = client1.list_files();
		String[] fileArray = files.split("\n");
		
		for(String file: fileArray) {
			assertTrue(set.contains(file));
		}
		assertEquals(fileArray.length, set.size());
		
	}
	
	@Test
	void shouldListFiles2() throws IOException {
		client1.unregister("f1.txt");
		Set<String> set = new HashSet<>(Arrays.asList("user1:", "user3: f3.txt", "user2: f2.txt"));
		String files = client1.list_files();
		String[] fileArray = files.split("\n");
		for(String file: fileArray)
			assertTrue(set.contains(file));
		assertEquals(fileArray.length, set.size());
		
	}
	
	@Test
	void shouldUpdate() throws IOException {
		Set<String> set = new HashSet<>(Arrays.asList("user1", "user2", "user3"));
		String users = client1.update();
		String[] userArray = users.split(" ");
		for(String user: userArray)
			assertTrue(set.contains(user));
		assertEquals(userArray.length, set.size());
	}
	
	@Test
	void shouldReturn1AfterUnknownCommand() throws IOException {
		assertEquals(client1.send_command(7), "Unknown command.");
	}
}