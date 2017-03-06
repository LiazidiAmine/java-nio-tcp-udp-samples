package tp4.ex1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.sql.Time;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


/**
 * Created by carayol on 04/02/15.
 */
public abstract class Requester {

	public static Charset ASCII_CHARSET = Charset.forName("US-ASCII");
    private DatagramChannel dc;
    private InetSocketAddress serverAddress;

    public Requester(InetSocketAddress serverAddress){
        this.serverAddress=serverAddress;
    }

	/**
     * Create and return the String represented by the ByteBuffer bb 
     * in the following representation:
     * - the size (as an Big Indian int) of the charsetName encoding in ASCII<br/>
     * - the bytes encoding this charsetName in ASCII<br/>
     * - the bytes encoding the message in this charsetName.<br/>
     * The accepted ByteBuffer buffer must be in <strong>write mode</strong> 
     * (need to be flipped before to be used).
	 * 
	 * @param bb a ByteBuffer containing the representation of a message
	 * @return the String represented by bb
	 */
    public static Optional<String> decodeString(ByteBuffer bb) {
    	
    	if(bb.remaining() < Integer.BYTES){
    		System.out.println("[DECODE] bb.remaining() < Integer.BYTES");
    		return Optional.empty();
    	}

    	int oldLimit = bb.limit();
    	int csNameSize = bb.getInt();
    	System.out.println("[DECODE] csNameSize = bb.getInt() = "+csNameSize +"   ");
    	if(csNameSize <= 0 ){
    		System.out.println("[DECODE] csNameSize <= 0");
    		return Optional.empty();
    	}
    	if(csNameSize + Integer.BYTES >= oldLimit){
    		System.out.println("[DECODE] oldLimit = " + oldLimit);
    		System.out.println("[DECODE] csNameSize + Integer.BYTES = " + csNameSize + Integer.BYTES);
    		System.out.println("[DECODE] (csNameSize + Integer.BYTES >= bb.limit())");
    		return Optional.empty();
    	}

    	bb.limit(Integer.BYTES + csNameSize + Long.BYTES);
    	System.out.println("[DECODE] ByteBuffer.limit() : "+bb.limit());
    	System.out.println("[DECODE] Remaining : "+bb.remaining());
    	String csName = ASCII_CHARSET.decode(bb).toString();
    	System.out.println("[DECODE] CharsetName decoded : "+csName);
    	try{
    		Charset cs = Charset.forName(csName);
    		bb.limit(oldLimit);
    		return Optional.of(cs.decode(bb).toString());
    	} catch(IllegalArgumentException e){
    		System.out.println("[DECODE] "+e.getMessage());
    		return Optional.empty();
    	}
    }

    /**
     * Create and return a new ByteBuffer containing the representation of msg
     * string using the charset charsetName in the following format.
     * - the identifier of the request as a Big Endian long  
     * - the size (as an Big Indian int) of the charsetName encoding in ASCII<br/>
     * - the bytes encoding this charsetName in ASCII<br/>
     * - the bytes encoding msg in charsetName.<br/>
     * The returned ByteBuffer is in <strong>write mode</strong> (need to be flipped
     * before to be used).
     * @param requestNumber the identifier
     * @param msg the String to send 
     * @param charsetName name of the Charset to encode the String msg
     * @return a newly allocated ByteBuffer containing the representation of msg
     */
    protected ByteBuffer createPacket(long requestNumber,String msg,Charset cs){
    	String csName = cs.displayName();
    	ByteBuffer bbCSName = ASCII_CHARSET.encode(csName);
    	String msgUpperCase = msg.toUpperCase();
    	ByteBuffer bbMsg = cs.encode(msgUpperCase);
    	ByteBuffer bbReturned = ByteBuffer.allocate(Long.BYTES + Integer.BYTES + bbCSName.remaining() + bbMsg.remaining());
    	int bbCSNameRemaining = bbCSName.remaining();
    	int bbMsgRemaining = bbMsg.remaining();
    	System.out.println("[Requester] Buffer allocated : "+
    			(Long.BYTES + Integer.BYTES + bbCSName.remaining() + bbMsg.remaining()) + " For value : "+msg);
    	System.out.println("[Requester] Long.BYTES : "+Long.BYTES);
    	System.out.println("[Requester] Integer.BYTES : "+Integer.BYTES);
    	System.out.println("[Requester] bbCSName.remaining() : "+bbCSNameRemaining);
    	System.out.println("[Requester] bbMsg.remaining() : "+bbMsgRemaining);
    	bbReturned.putLong(requestNumber);
    	bbReturned.putInt(bbCSNameRemaining);
    	bbReturned.put(bbCSName);
    	bbReturned.put(bbMsg);
    	System.out.println("[Requester] Packet created for Value : "+msg+ " with Charset : "+cs.displayName());
    	System.out.println("[Requester] Limit "+bbReturned.limit());
    	return bbReturned;
    }

    protected void send(ByteBuffer buff) throws IOException {
        dc.send(buff,serverAddress);
        System.out.println("[Requester] buffer sended");
    }

    protected void receive(ByteBuffer buff) throws IOException {
    	System.out.println("[Requester][RECEIVER] trying to receive");
        dc.receive(buff);
        System.out.println("[Requester][RECEIVER] buffer received");
    }

    public void open() throws IOException {
        if (dc!=null) 
        	throw new IllegalStateException("Requester already opened.");
        dc=DatagramChannel.open();
        dc.bind(null);
    }

    public void close() throws IOException {
        if (dc==null) 
        	throw new IllegalStateException("Requester was never opened.");
        dc.close();
    }

    public abstract List<String> toUpperCase(List<String> list,Charset cs) throws IOException,InterruptedException;

}
