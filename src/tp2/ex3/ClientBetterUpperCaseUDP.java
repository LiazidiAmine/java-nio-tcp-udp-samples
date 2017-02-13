package tp2.ex3;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
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
    public static CharBuffer decodeMessage(ByteBuffer buffer) {
    	buffer.flip();
    	if(buffer.remaining() < 4){
    		throw new IllegalArgumentException("Invalid packet - packet remaining < 4");
    	}
    	int charsetSize = buffer.getInt();
    	if(charsetSize > buffer.remaining()){
    		throw new IllegalArgumentException("Invalid packet - charsetSize > buffers remaining");
    	}
    	
    	int oldLimit = buffer.limit();
    	buffer.limit(buffer.position() + charsetSize);
    	byte[] arr = new byte[buffer.remaining()];
    	buffer.get(arr);
    	ByteBuffer charsetName = ByteBuffer.wrap(arr);
    	CharBuffer charsetName_str = ASCII_CHARSET.decode(charsetName);
    	
    	buffer.limit(oldLimit);
    	
    	return Charset.forName(charsetName_str.toString()).decode(buffer);
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
      ByteBuffer bbCharsetName = ASCII_CHARSET.encode(charsetName).order(ByteOrder.BIG_ENDIAN);
      ByteBuffer bbMsg = Charset.forName(charsetName).encode(msg);
      
      return ByteBuffer.allocate(BUFFER_SIZE).putInt(charsetName.length()).put(bbCharsetName).put(bbMsg);
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
            System.out.println(decodeMessage(buff));
        }
        scan.close();
        dc.close();
    }

}
