package examUDP.ex1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;

public class ClientConcatenation {
	public static Charset ISO = Charset.forName("ISO-8859-15");
	public static Charset UTF8 = Charset.forName("UTF-8");
	public static int NB_OCTET = 4;
	public static int STRING_SIZE = 1024;
	public static List<ByteBuffer> buffers = new ArrayList<>();
	
	public static void main(String[] args) throws IOException{
		if(args.length != 2){
			throw new IllegalArgumentException("invalid arguments");
		}
		String dest = Objects.requireNonNull(args[0]);
		int port = Integer.valueOf(args[1]);
		@SuppressWarnings("resource")
		Scanner sc = new Scanner(System.in);
		
		DatagramChannel dc = DatagramChannel.open();
		dc.bind(null);
		SocketAddress socketAd = new InetSocketAddress(dest, port);
		
		//RECEIVING
		ByteBuffer responseBuff = ByteBuffer.allocate((STRING_SIZE + NB_OCTET) * buffers.size());
		Runnable listenerR = ()->{
			
			try {
				while(!Thread.currentThread().isInterrupted()){
					responseBuff.clear();
					dc.receive(responseBuff);

					Optional<String> received = decode(responseBuff);
					if(received.isPresent()){
						String response = received.get();
						System.out.println("RESPONSE "+response);
					}
				}
				dc.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		};
		Thread listener = new Thread(listenerR);
		listener.start();
		
		
		
		// SENDING
		String input;
		while(((input = sc.nextLine()) != null) && (!input.equals("")) && (!input.equals("\n"))){
			buffers.add(encode(input));
		}
		ByteBuffer total = ByteBuffer.allocate((STRING_SIZE + NB_OCTET) * buffers.size());
		buffers.stream().forEach(x->{
			x.flip();
			total.put(x);
		});
		total.flip();
		dc.send(total, socketAd);
		System.out.println("All packets sended");
	}
	
	public static ByteBuffer encode(String input){
		int size = input.length();
		
		ByteBuffer stringBuffer = ISO.encode(input);
		ByteBuffer resultBuff = ByteBuffer.allocate(stringBuffer.remaining() + Integer.BYTES);
		resultBuff.putInt(size).put(stringBuffer);
		
		return resultBuff;
	}
	
	public static Optional<String> decode(ByteBuffer buffer){
		buffer.flip();
		System.out.println(buffer.remaining());
		return Optional.of(UTF8.decode(buffer).toString());
	}
}
