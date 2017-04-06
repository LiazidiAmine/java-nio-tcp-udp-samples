package tcp.tp10.ex1;

import java.io.IOException;

public class Main {

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		ServerNonBlocking server = new ServerNonBlocking(7777);
		server.launch();
	}

}
