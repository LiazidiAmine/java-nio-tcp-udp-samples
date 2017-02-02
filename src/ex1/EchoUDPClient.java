package ex1;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
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
		DatagramChannel dc = DatagramChannel.open();
		dc.bind(null);
		SocketAddress dest = new InetSocketAddress(dest_str, port);
		
		System.out.println("socket locale attachée à l'adresse : " + dc.getLocalAddress());
		System.out.println(bbOut.remaining() + " octets émis vers " + dest);
		System.out.println("capacité de la zone de stockage : "+ bbOut.capacity());
		
		dc.send(bbOut, dest);
		
		//RECEIVE
		ByteBuffer bbIn = ByteBuffer.allocate(BUFFER_SIZE);
		System.out.println(bbIn.remaining() + " octets reçus");
		
		SocketAddress exp = dc.receive(bbIn);
		bbIn.flip();
		final CharBuffer msg_receive = charset.decode(bbIn);
		
		System.out.println("contenant : "+ msg_receive);
		System.out.println("provenant : "+ exp);
		
		
	}
	
	

}
