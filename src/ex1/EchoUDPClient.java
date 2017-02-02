package ex1;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.util.Objects;


public class EchoUDPClient {

	public static final int BUFFER_SIZE = 1024;
	public static void main(String[] args) throws IOException {

		if(args != null && args.length != 3){
			throw new IllegalArgumentException("Invalid arguments");
		}
		
		final String dest_str = Objects.requireNonNull(args[0]);
		final int port = Integer.parseInt(Objects.requireNonNull(args[1]));
		final String msg = Objects.requireNonNull(args[2]);
		final Charset charset = Charset.defaultCharset();
		
		//SEND
		ByteBuffer bbOut = charset.encode(msg);
		DatagramChannel dc_out = DatagramChannel.open();
		dc_out.bind(null);
		SocketAddress dest = new InetSocketAddress(dest_str, port);
		dc_out.send(bbOut, dest);
		
		//RECEIVE
		DatagramChannel dc_in = DatagramChannel.open();
		dc_in.bind(null);
		ByteBuffer bbIn = ByteBuffer.allocate(BUFFER_SIZE);
		SocketAddress exp = dc_in.receive(bbIn);
		bbIn.flip();
		String msg_receive = charset.decode(bbIn).toString();
		System.out.println("receive " + msg_receive + "from "+ exp);
		
	}
	
	

}
