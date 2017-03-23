package examUDP.ex1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class ClientConcatenationDrop {
	public static Charset ISO = Charset.forName("ISO-8859-15");
	public static Charset UTF8 = Charset.forName("UTF-8");
	public static int NB_OCTET = 4;
	public static int STRING_SIZE = 1024;
	public static List<ByteBuffer> buffers = new ArrayList<>();
	public static BlockingQueue<String> queue = new ArrayBlockingQueue<String>(STRING_SIZE);
	
	public static void main(String[] args) throws IOException, InterruptedException{
		if(args.length != 2){
			throw new IllegalArgumentException("invalid arguments");
		}
		String dest = Objects.requireNonNull(args[0]);
		int port = Integer.valueOf(args[1]);
		
		DatagramChannel dc = DatagramChannel.open();
		dc.bind(null);
		SocketAddress socketAd = new InetSocketAddress(dest, port);
		
		System.out.println("****** INPUTS ******");
		prepareRequest();
		System.out.println("********************");
		
		listen(dc);
		
		String response = null;
		while((response = queue.poll(300, TimeUnit.MILLISECONDS)) == null){
			System.out.println("[Client] Sending..");
			send(dc, socketAd);
		}
		System.out.println("[Server] Response : "+response);
	}
	
	public static ByteBuffer encode(String input){
		int size = input.length();
		
		ByteBuffer stringBuffer = ISO.encode(input);
		ByteBuffer resultBuff = ByteBuffer.allocate(stringBuffer.remaining() + Integer.BYTES);
		resultBuff.putInt(size).put(stringBuffer);
		
		return resultBuff;
	}

	public static void prepareRequest() {
		// SENDING
		@SuppressWarnings("resource")
		Scanner sc = new Scanner(System.in);
		String input;
		while(((input = sc.nextLine()) != null) && (!input.equals("")) && (!input.equals("\n"))){
			buffers.add(encode(input));
		}
	}
	
	public static void send(DatagramChannel dc, SocketAddress socketAd) throws IOException{
		ByteBuffer total = ByteBuffer.allocate((STRING_SIZE + NB_OCTET) * buffers.size());
		buffers.stream().forEach(x->{
			x.flip();
			total.put(x);
		});
		total.flip();
		dc.send(total, socketAd);
		System.out.println("[Client] All packets sended");
	}
	
	public static void listen(DatagramChannel dc){
		//RECEIVING
		Runnable listenerR = ()->{
			ByteBuffer responseBuff = ByteBuffer.allocate(STRING_SIZE);
			try {
				while(!Thread.currentThread().isInterrupted()){
					responseBuff.clear();
					dc.receive(responseBuff);
					responseBuff.flip();
					Optional<String> response = Optional.of(UTF8.decode(responseBuff).toString());
					if(response.isPresent()){
						queue.put(response.get());
					}
				}
				dc.close();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		};
		Thread listener = new Thread(listenerR);
		listener.start();
	}

}
