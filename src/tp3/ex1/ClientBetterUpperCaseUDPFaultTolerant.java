package tp3.ex1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created by carayol on 02/03/14.
 */
public class ClientBetterUpperCaseUDPFaultTolerant {

	public static Charset ASCII_CHARSET = Charset.forName("US-ASCII");
	public static int BUFFER_SIZE = 1024;
	public static final BlockingQueue<String> queue = new ArrayBlockingQueue<>(BUFFER_SIZE);
	
	/**
     * Create and return a String message represented by the ByteBuffer buffer 
     * in the following representation:
     * - the size (as an Big Indian int) of the charsetName encoding in ASCII<br/>
     * - the bytes encoding this charsetName in ASCII<br/>
     * - the bytes encoding the message in this charsetName.<br/>
     * The accepted ByteBuffer buffer must be in <strong>write mode</strong> 
     * (need to be flipped before to be used).
	 * 
	 * @param buffer a ByteBuffer containing the representation of a message
	 * @return the String represented by bb
	 */
    public static Optional<String> decodePacket(ByteBuffer bb){
    	bb.flip();//change mode to read it
    	if(bb.remaining() < Integer.BYTES){//i.e there is no charset
    		return Optional.empty();
    	}
    	int csNameSize = bb.getInt();
    	if(csNameSize <= 0 || (csNameSize + Integer.BYTES > bb.limit())){ // invalid packet format or invalid charset
    		return Optional.empty();
    	}
    	int oldLimit = bb.limit();
    	bb.limit(Integer.BYTES + csNameSize);
    	String csName = ASCII_CHARSET.decode(bb).toString();
    	try{
    		Charset cs = Charset.forName(csName);
    		bb.limit(oldLimit);
    		return Optional.of(cs.decode(bb).toString());
    	} catch(IllegalArgumentException e){
    		return Optional.empty();
    	}
    }

    /**
     * Create and return a new ByteBuffer containing the representation of msg
     * string using the charset charsetName in the following format.
     * - the size (as an Big Indian int) of the charsetName encoding in ASCII<br/>
     * - the bytes encoding this charsetName in ASCII<br/>
     * - the bytes encoding msg in charsetName.<br/>
     * The returned ByteBuffer is in <strong>write mode</strong> (need to be flipped
     * before to be used).
     * @param msg the String to send 
     * @param charsetName name of the Charset to encode the String msg
     * @return a newly allocated ByteBuffer containing the representation of msg
     */
    private static ByteBuffer encodeMessage(String msg, String charsetName) {
    	Charset cs = Charset.forName(charsetName);
    	ByteBuffer bbCSName = ASCII_CHARSET.encode(charsetName);
    	ByteBuffer bbMsg = cs.encode(msg);
    	ByteBuffer bbReturned = ByteBuffer.allocate(Integer.BYTES + bbCSName.remaining() + bbMsg.remaining());
    	bbReturned.putInt(bbCSName.remaining());
    	bbReturned.put(bbCSName);
    	bbReturned.put(bbMsg);
    	System.out.println("LIMIT "+bbReturned.limit());
    	return bbReturned;
  }

    public static void usage() {
        System.out.println("Usage : ClientBetterUpperCaseUDP host port charsetName");
    }

    public static void main(String[] args) throws IOException, InterruptedException {
    	// check and retrieve parameters
        if (args.length != 3) {
            usage();
            return;
        }
        String host = args[0];
        int port = Integer.valueOf(args[1]);
        String charsetName = args[2];
        
        // prepare DatagramChannel and destination
        DatagramChannel dc = DatagramChannel.open();
        SocketAddress dest = new InetSocketAddress(host, port);
        
        // prepare input and receive buffer
        Scanner scan = new Scanner(System.in);
        
		Runnable responseListener = ()->{
			//RECEIVE
			ByteBuffer buff = ByteBuffer.allocateDirect(BUFFER_SIZE);
			try {
				while(!Thread.currentThread().isInterrupted()){
					buff.clear();
					dc.receive(buff);
					Optional<String> msg_receive = Optional.of(decodePacket(buff).toString());
					if(msg_receive.isPresent()){
						queue.put(msg_receive.get());
					}
					
					buff.clear();
				}
			} catch (InterruptedException e) {
				System.err.print("Listener interrupted while wainting to put message in the queue");
			} catch(ClosedByInterruptException e1){
				System.err.println("Listener interrupted while receiving");
			} catch(AsynchronousCloseException e2){
				System.err.print("Listener interrupted because the channel was closed in another thread");
			} catch(IOException e3){
				System.out.println("Listener stopped by an IO error");
				e3.printStackTrace();
			}
			
		};
		
		Thread listener = new Thread(responseListener);
		listener.start();
        
        while (scan.hasNextLine()) {
            String line = scan.nextLine();
            ByteBuffer packet = encodeMessage(line, charsetName);
            while(true){
	            packet.flip();
	            dc.send(packet, dest);
	            System.out.println("Packet sended to server : "+line);
	            String msg = queue.poll(1, TimeUnit.SECONDS);
	            System.out.println("try to receive");
	            while(msg == null){
	            	System.out.println("Receive timeout.. retry..");
	            	
	            	packet.flip();
	            	dc.send(packet, dest);
	            	System.out.println("Packet sended to server : "+line);
		            msg = queue.poll(1, TimeUnit.SECONDS);
	            }
	            System.out.println(msg);
	            break;
            }
        }
        scan.close();
        listener.interrupt();
        listener.join();
        dc.close();
    }

}
