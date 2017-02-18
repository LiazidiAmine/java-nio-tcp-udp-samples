package tp4.ex1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;


/**
 * Created by carayol on 04/02/15.
 */
public class OneByOneRequester extends Requester {


	private static final int BUFFER_SIZE = 4096;
	private static final int TIMEOUT = 500;

	private final BlockingQueue<ByteBuffer> queue = new ArrayBlockingQueue<ByteBuffer>(1);
	
	private final Thread listener = new Thread(() -> {
		ByteBuffer bbIn = ByteBuffer.allocate(BUFFER_SIZE);
		System.out.println("[OneByOne][LISTENER] ByteBuffer allocated - START LISTENER");
		try {
			while(!Thread.interrupted()){
				bbIn.clear();
				System.out.println("[OneByOne][LISTENER] Preparing to receive...");
				receive(bbIn);
				bbIn.flip();
				System.out.println("[OneByOne][LISTENER] Check remaining received packet...");
				if(bbIn.remaining() > 0){
					System.out.println("[OneByOne][LISTENER] Trying to add packet..");
					queue.add(bbIn);
					System.out.println("[OneByOne][LISTENER] ByteBuffer added : Remaining packet : "+bbIn.remaining());
				}else{
					System.out.println("[OneByOne][LISTENER] Packet with no remaining received");
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	});

	public OneByOneRequester(InetSocketAddress serverAddress) {
		super(serverAddress);
	}

	@Override
	public void open() throws IOException {
		super.open();
		listener.start();
	}

	@Override
	public List<String> toUpperCase(List<String> list,Charset cs) throws IOException, InterruptedException {
		List<String> linesUpperCase = new ArrayList<>(list.size());
		for(int i=0;i<list.size();i++){
			System.out.println("[OneByOne][INIT] Line "+i+" toUpperCase \\ Value : "+list.get(i));
			String packetUpperCase = toUpperCase(i,list.get(i),cs);
			if(packetUpperCase != null){
				System.out.println("[OneByOne] Adding upperCase to List");
				linesUpperCase.add(packetUpperCase);
			}
		}
		return linesUpperCase;
	}

	private String toUpperCase(long i, String string, Charset cs) throws IOException, InterruptedException {
		ByteBuffer buffsend = createPacket(i, string, cs);
		buffsend.flip();
		System.out.println("[OneByOne] Packed remaining : "+buffsend.remaining());
		send(buffsend);
		System.out.println("[OneByOne] String sended : "+ string +" |=> elapsedTimeOut < TIMEOUT |=> queue.poll() ");
		buffsend.flip();
		long lastSend = System.currentTimeMillis();
		while(!Thread.interrupted()){
			long currentTime = System.currentTimeMillis();
			long elapsedTime = currentTime - lastSend;
			if(elapsedTime < TIMEOUT){
				System.out.println("[OneByOne] Elapsed timeOut");
				System.out.println("[OneByOne] Trying to poll..");
				ByteBuffer answer = queue.poll(elapsedTime,TimeUnit.MILLISECONDS);
				if(answer == null){
					System.out.println("[OneByOne] [ERROR] answer == null");
					continue;
				}
				answer.flip();
				System.out.println("[OneByOne] Packet received remaining : "+answer.remaining());
				if((answer.remaining() < Long.BYTES) || (answer.getLong() != i)){
					if(answer.remaining() < Long.BYTES){
						System.out.println("[OneByOne] [ERROR] answer.getLong != i");
					}
					continue;
				}
				System.out.println("[OneByOne] answer's ID is ok. Answer : "+answer.getLong() + " \\ ID : "+i);
				answer.compact();
				Optional<String> opt = decodeString(answer);
				if(!opt.isPresent()){
					System.out.println("[OneByOne] [ERROR] Optional.isPresent() == False");
					continue;
				}
				System.out.println(opt.get());
				return opt.get();
			} else{
				send(buffsend);
				System.out.println("[OneByOne] String sended : "+ string +" |=> elapsedTimeOut < TIMEOUT |=> queue.poll() ");
				buffsend.flip();
				lastSend = System.currentTimeMillis();
			}
		}
		return null; 
	}

	@Override
	public void close() throws IOException {
		super.close();
		listener.interrupt();
	}

}
