package tp2.ex3;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.Scanner;

/**
 * Created by carayol on 02/03/14.
 */
public class ClientBetterUpperCaseUDP {

	public static Charset ASCII_CHARSET = Charset.forName("US-ASCII");
	public static int BUFFER_SIZE = 1024;
	
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
    //Decode packet
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
    	return bbReturned;
  }

    public static void usage() {
        System.out.println("Usage : ClientBetterUpperCaseUDP host port charsetName");
    }

    public static void main(String[] args) throws IOException {
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
        ByteBuffer buff = ByteBuffer.allocateDirect(BUFFER_SIZE);
        
        while (scan.hasNextLine()) {
            String line = scan.nextLine();
            ByteBuffer packet = encodeMessage(line, charsetName);
            packet.flip();
            dc.send(packet, dest);
            buff.clear();
            dc.receive(buff);
            if(decodePacket(buff).isPresent()){
            	System.out.println(decodePacket(buff).get());
            }
            
        }
        scan.close();
        dc.close();
    }
    


}
