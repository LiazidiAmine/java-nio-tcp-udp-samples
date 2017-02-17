package tp3.ex1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class ClientUpperCaseUDPFaultTolerant {
	
	public static final int BUFFER_SIZE = 1024;
	public static final BlockingQueue<String> queue = new ArrayBlockingQueue<>(BUFFER_SIZE);
	public static void main(String args[]) throws IOException{
		if(args != null && args.length != 3){
			throw new IllegalArgumentException("Invalid arguments");
		}
		
		final String dest_str = Objects.requireNonNull(args[0]);
		final int port = Integer.parseInt(Objects.requireNonNull(args[1]));
		final String charset_str = Objects.requireNonNull(args[2]);
		final Charset charset = Charset.forName(charset_str);
		
		@SuppressWarnings("resource")
		Scanner sc = new Scanner(System.in);
		
		DatagramChannel dc = DatagramChannel.open();
		dc.bind(null);
		SocketAddress dest = new InetSocketAddress(dest_str, port);
		
		Runnable responseListener = ()->{
			//RECEIVE
			ByteBuffer bbIn = ByteBuffer.allocate(BUFFER_SIZE);
			try {
				while(true){
					dc.receive(bbIn);
					bbIn.flip();
					CharBuffer msg_receive = charset.decode(bbIn);
					System.out.println(msg_receive.toString() + " added to the queue");
					queue.put(msg_receive.toString());
					bbIn.clear();
				}
			} catch (IOException | InterruptedException e) {
				e.printStackTrace();
			}
			
		};
		
		Thread listener = new Thread(responseListener);
		listener.start();
		
		ByteBuffer bbOut;
		while(sc.hasNextLine()){
			//SEND
			String input = sc.nextLine();
			bbOut = charset.encode(input);
			dc.send(bbOut, dest);
			System.out.println(input+ " sended to " + dest);
			String msg = null;
			while(msg == null || (msg != null && !msg.equals(""))){	
				try {
					msg = queue.poll(1, TimeUnit.SECONDS);
					if(msg != null){
						break;
					}else {
						System.out.println("retry to send packet");
						bbOut.clear();
						bbOut = charset.encode(input);
						dc.send(bbOut, dest);
						System.out.println(input+ " sended to " + dest);
					}
					
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			System.out.println("Packet received : "+msg);
		}

		
		dc.close();
		sc.close();
		
		
	}
}


