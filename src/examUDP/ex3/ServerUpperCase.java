package examUDP.ex3;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.Set;

public class ServerUpperCase {

    private final DatagramChannel dc;
    private final Selector selector;
    private final int BUFFER_SIZE = 1024;
    private final ByteBuffer buff = ByteBuffer.allocateDirect(BUFFER_SIZE);
    private SocketAddress exp;
    private final Charset UTF8 = Charset.forName("UTF-8");

    public ServerUpperCase(int port) throws IOException {
        selector = Selector.open();
        dc = DatagramChannel.open();
        dc.configureBlocking(false);
        dc.bind(new InetSocketAddress(port));
        dc.register(selector, SelectionKey.OP_READ);
    }


    public void serve() throws IOException {
        Set<SelectionKey> selectedKeys = selector.selectedKeys();
        while (!Thread.interrupted()) {
            selector.select();
            for (SelectionKey key : selectedKeys) {
                if (key.isValid() && key.isWritable()) {
                    doWrite(key);
                }
                if (key.isValid() && key.isReadable()) {
                    doRead(key);
                }
            }
            selectedKeys.clear();
    }
    }

    private void doRead(SelectionKey key) throws IOException {
    	DatagramChannel dchann = (DatagramChannel)key.channel();
        Connection con = new Connection();
        SocketAddress socketA = dchann.receive(con.getBb());
        
        if(socketA == null){
        	System.out.println("receive null");
        	return;
        }
        con.setSa(socketA);
        key.attach(con);
        key.interestOps(SelectionKey.OP_WRITE);
    	
    }

    private void doWrite(SelectionKey key) throws IOException {
    	Connection con = (Connection)key.attachment();
        DatagramChannel dchan = (DatagramChannel)key.channel();
        Optional<ByteBuffer> buffOp = con.encode();
        if(buffOp.isPresent()){
        	ByteBuffer buff = buffOp.get();
        	
            dchan.send(buff, con.getSa());
            if(buff.remaining()==0){
            	System.out.println("packet sended");
            	key.interestOps(SelectionKey.OP_READ);
            	buff.clear();
            	return;
            }
            buff.compact();
        }else{
        	System.out.println("No data to send");
        }
        
    	
    }

    public static void usage() {
        System.out.println("Usage : ServerUpperCase port");
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            usage();
            return;
        }
        ServerUpperCase server= new ServerUpperCase(Integer.valueOf(args[0]));
        server.serve();
    }

    class Connection {
    	ByteBuffer req = ByteBuffer.allocate(BUFFER_SIZE);;
        SocketAddress socketA;

        public Connection() {
            
        }
        
        public Optional<ByteBuffer> encode(){
        	req.flip();
        	Optional<String> strOp = Optional.of(UTF8.decode(req).toString());
        	if(strOp.isPresent()){
        		String toSend = strOp.get().toUpperCase();
        		System.out.println("Uppercase : "+toSend);
        		return Optional.of(UTF8.encode(toSend));
        	}else{
        		return Optional.empty();
        	}
        }
        
        public ByteBuffer getBb(){
        	return req;
        }
        
		public SocketAddress getSa() {
			return socketA;
		}

		public void setBb(ByteBuffer bb) {
			this.req = bb;
		}

		public void setSa(SocketAddress sa) {
			this.socketA = sa;
		}

    }

}

