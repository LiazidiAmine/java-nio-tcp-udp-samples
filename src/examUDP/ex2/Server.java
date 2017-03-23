package examUDP.ex2;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;

public class Server {
	DatagramChannel dc;
	ByteBuffer bbIn = ByteBuffer.allocate(512);
	HashMap<SocketAddress, List<Long>> map = new HashMap();
	
	public Server(int port) throws IOException {
        dc = DatagramChannel.open();
        dc.bind(new InetSocketAddress(port));
        System.out.println("Server started on port " + port);
    }
	
    public void serve() throws IOException {
        while (!Thread.interrupted()) {      
        	bbIn.clear();
        	SocketAddress sockAd = dc.receive(bbIn);
        	bbIn.flip();
        	Long packetReceived;
        	if((packetReceived = bbIn.getLong()) != 0){
        		long time = System.currentTimeMillis() - packetReceived;
        		if(!map.containsKey(sockAd)){
        			List<Long> list = new ArrayList<>();
        			list.add(time);
        			map.put(sockAd, list);
        		}else{
        			map.get(sockAd).add(time);
        		}
        		ByteBuffer responseBuff = encode(time, sockAd);
        		responseBuff.flip();
        		dc.send(responseBuff, sockAd);
        	}
        }
    }
    
    private ByteBuffer encode(long time, SocketAddress socketAd){
    	ByteBuffer bbOut = ByteBuffer.allocate(Integer.BYTES * 3);
    	bbOut.putInt((int)time);
    	
    	int clientAverage = (int)map.get(socketAd).stream().mapToLong(x -> x).average().getAsDouble();
    	System.out.println("[Average client] : "+clientAverage);
    	
    	List<Long> values = new ArrayList<>();
    	for(Map.Entry<SocketAddress, List<Long>> entry : map.entrySet()){
    		long average = (long)entry.getValue().stream().mapToLong(x->x).average().getAsDouble();
    		values.add(average);
    	}
    	int allAverage = (int)values.stream().mapToLong(x->x).average().getAsDouble();
    	System.out.println("[All clients average] : "+allAverage);
    	
    	bbOut.putInt(clientAverage).putInt(allAverage);
    	
    	return bbOut;
    }
	
	public static void main(String[] args) throws IOException{
		Objects.requireNonNull(args);
		if(args.length != 1){
			throw new IllegalArgumentException("invalid arguments");
		}
		final int PORT = Integer.valueOf(args[0]);
		
		Server server = new Server(PORT);
		server.serve();
	}
	
	
}
