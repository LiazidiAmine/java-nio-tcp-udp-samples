package tp6.ex1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.IntStream;

public class ServerEchoNIOTemplate {

    private final Selector selector;
    private final int BUFFER_SIZE = 1024;

    public ServerEchoNIOTemplate(int port, int portLimit) throws IOException {
        selector = Selector.open();
        
        while(port <= portLimit){
            DatagramChannel dc = DatagramChannel.open();
            dc.configureBlocking(false);
            
            dc.bind(new InetSocketAddress(port));
            dc.register(selector, SelectionKey.OP_READ);
            port++;
        }
    }


    public void serve() throws IOException {
        Set<SelectionKey> selectedKeys = selector.selectedKeys(); 
        while (!Thread.interrupted() && selector.isOpen()) {
        	
            selector.select();

            Iterator<SelectionKey> iterator = selectedKeys.iterator();
            while (iterator.hasNext()) {
            	SelectionKey key = (SelectionKey) iterator.next();
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
        ByteBuffer buff = con.getBb();
        buff.flip();
        dchan.send(buff, con.getSa());
        if(buff.remaining()==0){
        	System.out.println("packet sended");
        	key.interestOps(SelectionKey.OP_READ);
        	buff.clear();
        	return;
        }
        buff.compact();
    }

    public static void usage() {
        System.out.println("Usage : ServerEchoNIO port");
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            usage();
            return;
        }
        ServerEchoNIOTemplate server= new ServerEchoNIOTemplate(Integer.valueOf(args[0]),Integer.valueOf(args[1]));
        server.serve();
    }

    class Connection {
    	ByteBuffer req = ByteBuffer.allocate(BUFFER_SIZE);;
        SocketAddress socketA;

        public Connection() {
            
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

