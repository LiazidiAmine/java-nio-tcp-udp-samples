package tp3.ex2;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
	private static List<String> sendingLines = new ArrayList();
	private static List<String> receivingLines = new ArrayList();
	private static final String file = "/home/amine/workspace/javanetworktp/src/text.txt";
	private static final int BUFFER_SIZE = 1024;
	public static void main(String[] args) throws IOException, InterruptedException{
		if(args != null && args.length != 3){
			throw new IllegalArgumentException("Invalid arguments");
		}
		
		final String dest_str = Objects.requireNonNull(args[0]);
		final int port = Integer.parseInt(Objects.requireNonNull(args[1]));
		final String charset_str = Objects.requireNonNull(args[2]);
		final Charset charset = Charset.forName(charset_str);
		
		DatagramChannel dc = DatagramChannel.open();
		dc.bind(null);
		SocketAddress dest = new InetSocketAddress(dest_str, port);
		
		Runnable responseListener = ()->{
			//RECEIVE
			ByteBuffer bbIn = ByteBuffer.allocate(BUFFER_SIZE);
			try {
				while(!(sendingLines.size() == receivingLines.size())){
					dc.receive(bbIn);
					bbIn.flip();
					CharBuffer msg_receive = charset.decode(bbIn);
					if(msg_receive != null){
						receivingLines.add(msg_receive.toString());
						System.out.println("msg added to receive list : "+msg_receive);
					}
					bbIn.clear();
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
			
		};
		
		try(Stream<String> lines = Files.lines(Paths.get(file))){
			sendingLines = lines.collect(Collectors.toList());
		}
		
		Thread listener = new Thread(responseListener);
		listener.start();
		
		ByteBuffer bbOut = ByteBuffer.allocate(BUFFER_SIZE);
		while(sendingLines.size() != receivingLines.size()){
			Iterator<String> iterator = sendingLines.iterator();
			while(iterator.hasNext()){
				String input = iterator.next();
				if(input != null && !receivingLines.contains(input)){
					bbOut = charset.encode(input);
					System.out.println(input+ " sended to " + dest);
					dc.send(bbOut, dest);
					Thread.sleep(1000);
				}else{
					System.out.println("packet already in receiving list");
				}
				bbOut.clear();
			}
		}
		System.out.println("*************** RESPONSES ***************");
		receivingLines.stream().forEach(System.out::println);
		

		
	}
}
