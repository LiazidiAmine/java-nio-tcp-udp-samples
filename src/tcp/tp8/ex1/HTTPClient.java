package tcp.tp8.ex1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Objects;

public class HTTPClient {

	static Charset charsetASCII = Charset.forName("ASCII");
	
    public static void sendRequest(String request, SocketAddress server) throws IOException{
    	SocketChannel sc = SocketChannel.open();
    	sc.connect(server);
    	sc.write(charsetASCII.encode(request));
    	ByteBuffer bb = ByteBuffer.allocate(50);
    	HTTPReader reader = new HTTPReader(sc,bb);
    	HTTPHeader header = reader.readHeader();
    	Map<String,String> headerFields = header.getFields();
    	
    	if(headerFields.containsKey("Content-Type")){
    		if(headerFields.get("Content-Type").contains("text/html") && headerFields.containsKey("Content-Length")){
    			int size = Integer.valueOf(headerFields.get("Content-Length"));
    			int i = 0;
    			while(i<size){
    				System.out.println(reader.readLineCRLF());
    				i++;
    			}
    		}else{
    			System.out.println("no html data");
    		}
    		
    	}
    	sc.close();
    	
    }
    
    
    public static void main(String[] args) throws IOException{
    	Objects.requireNonNull(args);
    	if(args.length != 2){
    		throw new IllegalArgumentException("invalid arguments");
    	}
    	int PORT = 80;
    	String RESOURCE = args[1];
    	String ADDRESS = args[0];
    	StringBuilder requestBuilder = new StringBuilder();
    	requestBuilder
    		.append("GET ")
    		.append(RESOURCE)
    		.append(" HTTP/1.1\r\n")
    		.append("Host: ")
    		.append(ADDRESS)
    		.append("\r\n"
    				+ "\r\n");
    	String request = requestBuilder.toString();

    	
    	SocketAddress server = new InetSocketAddress(ADDRESS, PORT);
    	
    	sendRequest(request, server);
    	
    }
}
