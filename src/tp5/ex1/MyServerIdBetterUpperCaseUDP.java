package tp5.ex1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.Optional;
import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;

public class MyServerIdBetterUpperCaseUDP {

    public static final int BUFFER_SIZE = 1024;
    public static Charset ASCII_CHARSET = Charset.forName("US-ASCII");
    private final DatagramChannel dc;
    private final ByteBuffer buff = ByteBuffer.allocateDirect(BUFFER_SIZE);

    public MyServerIdBetterUpperCaseUDP(int port) throws IOException {
        dc = DatagramChannel.open();
        dc.bind(new InetSocketAddress(port));
        System.out.println("MyServerIdBetterUpperCaseUDP started on port " + port);
    }

    public void serve() throws IOException {
        while (!Thread.interrupted()) {
          // TODO
          
        	buff.clear();
        	SocketAddress dest = dc.receive(buff);
        	if(decodeAndCreate(buff).isPresent()){
        		ByteBuffer toSend = decodeAndCreate(buff).get();
        		toSend.flip();
        		dc.send(toSend, dest);
        		System.out.println("[SENDER] Packet sended to : "+dest.toString());
        	}
        	
          
        }
    }

    public static void usage() {
        System.out.println("Usage : ServerIdBetterUpperCaseUDP port");
    }
    
    public Optional<ByteBuffer> decodeAndCreate(ByteBuffer bb){
    	bb.flip();
    	
    	if(bb.remaining() < Long.BYTES){
    		System.out.println("[DECODE] bb.remaining() <= Long.BYTES");
    		return Optional.empty();
    	}
    	
    	int oldLimit = bb.limit();
    	long pack_id = bb.getLong();
    	
    	if(bb.remaining() < Integer.BYTES){
    		System.out.println("[DECODE] bb.remaining() < Integer.BYTES");
    		return Optional.empty();
    	}
    	
    	int csNameSize = bb.getInt();
    	System.out.println("[DECODE] csNameSize = bb.getInt() = "+csNameSize +"   ");
    	
    	if(csNameSize <= 0 ){
    		System.out.println("[DECODE] csNameSize <= 0");
    		return Optional.empty();
    	}
    	if(csNameSize + Integer.BYTES + Long.BYTES >= oldLimit){
    		System.out.println("[DECODE] oldLimit = " + oldLimit);
    		System.out.println("[DECODE] csNameSize + Integer.BYTES = " + (csNameSize + Integer.BYTES + Long.BYTES));
    		System.out.println("[DECODE] (csNameSize + Integer.BYTES + Long.BYTES >= bb.limit())");
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
    		String msg = cs.decode(bb).toString();
    		System.out.println("[DECODE] Message decoded : "+msg);
    		return Optional.of(createPacket(msg, pack_id));
    	} catch(IllegalArgumentException e){
    		System.out.println("[DECODE] "+e.getMessage());
    		return Optional.empty();
    	}
    }
    
    private ByteBuffer createPacket(String msg, long requestNumber){
    	msg = requireValidMsg(msg);
    	Charset cs = Charset.forName("UTF-8");
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
    	System.out.println("[Requester] Packet created for Value : "+msgUpperCase+ " with Charset : "+cs.displayName());
    	System.out.println("[Requester] Limit "+bbReturned.limit());
    	return bbReturned;
    }
    
    private String requireValidMsg(String msg){
    	Objects.requireNonNull(msg);
    	if(msg.length() == 0){
    		throw new IllegalStateException("Invalid msg : "+msg);
    	}
    	return msg;
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            usage();
            return;
        }
        MyServerIdBetterUpperCaseUDP server;
        int port = Integer.valueOf(args[0]);
        if (!(port >= 1024) & port <= 65535) {
            System.out.println("The port number must be between 1024 and 65535");
            return;
        }
        try {
            server = new MyServerIdBetterUpperCaseUDP(port);
        } catch (BindException e) {
            System.out.println("Server could not bind on " + port + "\nAnother server is probably running on this port.");
            return;
        }
        server.serve();
    }
}
