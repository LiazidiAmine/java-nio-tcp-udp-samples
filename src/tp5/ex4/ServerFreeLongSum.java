package tp5.ex4;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.BitSet;
import java.util.HashMap;

public class ServerFreeLongSum {

	public static final int BUFFER_SIZE = 34;
	public static HashMap<SocketAddress,HashMap<Long,LongSum>> map = new HashMap<>();
	private final DatagramChannel dc;
	private final ByteBuffer packet_buff = ByteBuffer.allocateDirect(BUFFER_SIZE);
	private final ByteBuffer ACK = ByteBuffer.allocateDirect(Byte.BYTES + Long.BYTES + Long.BYTES);
	private final ByteBuffer RES = ByteBuffer.allocateDirect(Byte.BYTES + Long.BYTES + Long.BYTES);
	private final ByteBuffer ACKCLEAN = ByteBuffer.allocateDirect(Byte.BYTES + Long.BYTES);
	
	private static final byte OP_CODE = 1;
	private static final byte ACK_CODE = 2;
	private static final byte RES_CODE = 3;
	private static final byte CLEAN_CODE = 4;
	private static final byte ACKCLEAN_CODE = 5;

	public ServerFreeLongSum(int port) throws IOException {
		dc = DatagramChannel.open();
		dc.bind(new InetSocketAddress(port));
		System.out.println("ServerLonSum started on port " + port);
	}

	public void serve() throws IOException {
		while (!Thread.interrupted()) {

			map.forEach((key,map)-> {
				map.forEach((key2, myobject)->{
					myobject.destroy();
				});
			});
			
			packet_buff.clear();
			ACK.clear();
			RES.clear();
			ACKCLEAN.clear();
			SocketAddress dst = dc.receive(packet_buff);

			packet_buff.flip();
			
			if(packet_buff.remaining() < Byte.BYTES){
				continue;
			}
			byte code = packet_buff.get();
			
			if (code == OP_CODE) {
				
				if(packet_buff.remaining() < Long.BYTES){
					continue; 
				}
				long sessionId = packet_buff.getLong();
				if(packet_buff.remaining() < Long.BYTES){
					continue; 
				}
				long idPosOper = packet_buff.getLong();
				if(packet_buff.remaining() < Long.BYTES){
					continue; 
				}
				long totalOper = packet_buff.getLong();
				if(packet_buff.remaining() < Long.BYTES){
					continue; 
				}
				long opValue = packet_buff.getLong();
				
				if (map.containsKey(dst)){
					if(map.get(dst).containsKey(sessionId)){
						
						System.out.println("Session ID : " + sessionId);
						System.out.println("Remaining : " + totalOper);
						System.out.println("# : " + idPosOper);
						
						map.get(dst).get(sessionId).handle(idPosOper, opValue);
						
						if(map.get(dst).get(sessionId).done()){
							RES.put(RES_CODE);
							RES.putLong(sessionId);
							RES.putLong(map.get(dst).get(sessionId).getRES());
							RES.flip();
							dc.send(RES, dst);
						}
					}
					
					else {
						System.out.println("New Session ID : " + sessionId + ". Created !");
						newLongSum(dst,sessionId,idPosOper,totalOper,opValue);
					}
				}
				else{
					System.out.println("New connection");
					newLongSum(dst,sessionId,idPosOper,totalOper,opValue);
				}
				
				ACK.put(ACK_CODE);
				ACK.putLong(sessionId);
				ACK.putLong(idPosOper);
				ACK.flip();
				dc.send(ACK, dst);
			}
			
			else if(code == CLEAN_CODE){
				if(packet_buff.remaining() < Long.BYTES){
					continue; 
				}
				long ID = packet_buff.getLong();
				if(map.containsKey(ID)){
					map.get(dst).remove(ID);
				}
				
				ACKCLEAN.put(ACKCLEAN_CODE);
				ACKCLEAN.putLong(ID);
				ACKCLEAN.flip();
				dc.send(ACKCLEAN, dst);
			}
			
			
			
		}

	}
	
	public void newLongSum(SocketAddress dst, long sessionId, long idPosOper, long totalOper, long opValue){
		
		LongSum LS = new LongSum(idPosOper,totalOper,opValue, sessionId);
		if(!map.containsKey(dst)){
			map.put(dst,new HashMap<>());
		}
		map.get(dst).put(sessionId, LS);
		
	}

	public static void usage() {
		System.out.println("Usage : ServerLongSum port");
	}
	
	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			usage();
			return;
		}
		ServerFreeLongSum server;
		int port = Integer.valueOf(args[0]);
		if (!(port >= 1024) & port <= 65535) {
			System.out.println("The port number must be between 1024 and 65535");
			return;
		}
		try {
			server = new ServerFreeLongSum(port);
		} catch (BindException e) {
			System.out
					.println("Server could not bind on " + port + "\nAnother server is probably running on this port.");
			return;
		}
		server.serve();
	}

	class LongSum {
		private long RES;
		private BitSet OpRcv;
		private long totalOper;
		private long sessionID;
		private long start;
		
		LongSum(long posOper, long totalOper, long opValue, long sessionID){
			this.RES = opValue;
			this.totalOper = totalOper;
			this.OpRcv = new BitSet((int)totalOper);
			this.OpRcv.set(0,(int)totalOper-1, false);
			this.sessionID = sessionID;
			this.start = System.currentTimeMillis();
			OpRcv.set((int)posOper);
		}
		long getRES(){
			return this.RES;
		}
		
		void handle(long idPosOper, long opValue){
			
			System.out.println("Handling...");
			if(!OpRcv.get((int) idPosOper)){
				OpRcv.set((int)idPosOper,true);
				this.RES += opValue;
			}
			else{
				System.out.println("Packet already received");
			}	
			System.out.println("Done : " + OpRcv.cardinality());
		}
		
		boolean done(){
			return OpRcv.cardinality() == (int)totalOper;
		}

		public void destroy() {
			long currentTime = System.currentTimeMillis();
			
			if(currentTime - start > 1000){
				if(map.containsKey(sessionID)){
					map.get(sessionID).remove(sessionID);
					System.out.println("SessionID : "+sessionID+" destroyed.");
				}
			}
			
		}
		
		

	}
	
}


