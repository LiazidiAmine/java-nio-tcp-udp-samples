package tp7.ex1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class ClientEOS {

    public static final Charset UTF8_CHARSET = Charset.forName("UTF-8");
    public static final int BUFFER_SIZE = 1024;

    public static String getFixedSizeResponse(String request, SocketAddress server, int bufferSize) throws IOException {
    	SocketChannel sc = SocketChannel.open();
    	sc.connect(server);
    	ByteBuffer requestBuffer = UTF8_CHARSET.encode(request);
    	sc.write(requestBuffer);
    	sc.shutdownOutput();
    	
    	ByteBuffer responseBuff = ByteBuffer.allocate(bufferSize);
    	while(sc.read(responseBuff) != -1){

			if(!responseBuff.hasRemaining()){
				sc.shutdownInput();
				break;
			}
    	}
    	responseBuff.flip();
    	return UTF8_CHARSET.decode(responseBuff).toString();
    }

    public static String getUnboundedResponse(String request, SocketAddress server) throws IOException {
    	List<ByteBuffer> buffers = new ArrayList();
    	SocketChannel sc = SocketChannel.open();
    	sc.connect(server);
    	ByteBuffer requestBuffer = UTF8_CHARSET.encode(request);
    	sc.write(requestBuffer);
    	sc.shutdownOutput();
    	
    	ByteBuffer responseBuff = ByteBuffer.allocate(BUFFER_SIZE);
    	while(readFully(sc, responseBuff)){
    		buffers.add(responseBuff);
    		responseBuff = ByteBuffer.allocate(BUFFER_SIZE);
    	}
    	
    	ByteBuffer total = ByteBuffer.allocate((buffers.size()+1) * BUFFER_SIZE);
    	buffers.stream().forEach(x->{
    		x.flip();
    		total.put(x);
    	});
    	responseBuff.flip();
    	total.put(responseBuff);
    	total.flip();
    	return UTF8_CHARSET.decode(total).toString();
    }
    
    public static boolean readFully(SocketChannel sc, ByteBuffer bb) throws IOException{
    	while(sc.read(bb) != -1){
    		if(!bb.hasRemaining()){
    			return true;
    		}
    	}
    	return false;
    }

    public static void main(String[] args) throws IOException {
        System.out.println(getFixedSizeResponse("GET / HTTP/1.1\r\nHost: www.google.fr\r\n\r\n", 
            new InetSocketAddress("www.google.fr", 80), 1024));
         
         System.out.println(getUnboundedResponse("GET / HTTP/1.1\r\nHost: www.google.fr\r\n\r\n", 
              new InetSocketAddress("www.google.fr", 80)));
        
    }
}
