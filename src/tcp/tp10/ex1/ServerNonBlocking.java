package tcp.tp10.ex1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.Set;

public class ServerNonBlocking {

    private final ServerSocketChannel serverSocketChannel;
    private final Selector selector;
    private final Set<SelectionKey> selectedKeys;


    public ServerNonBlocking(int port) throws IOException {
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(port));
        selector = Selector.open();
        selectedKeys = selector.selectedKeys();
    }

   
    public void launch() throws IOException {
        serverSocketChannel.configureBlocking(false);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        Set<SelectionKey> selectedKeys = selector.selectedKeys();
        while (!Thread.interrupted()) {
            printKeys();
            System.out.println("Starting select");
            selector.select();
            System.out.println("Select finished");
            printSelectedKey();
            processSelectedKeys();
            selectedKeys.clear();
        }
    }

    private void processSelectedKeys() throws IOException {
        for (SelectionKey key : selectedKeys) {
            if (key.isValid() && key.isAcceptable()) {
                doAccept(key);
            }
            if (key.isValid() && key.isWritable()) {
                doWrite(key);
            }
            if (key.isValid() && key.isReadable()) {
                doRead(key);
            }
        }
    }

    private void doAccept(SelectionKey key) throws IOException {
           SocketChannel socketChannel = serverSocketChannel.accept();
        if(socketChannel == null){
        	return;
        }
        socketChannel.configureBlocking(false);
        socketChannel.register(selector,
        SelectionKey.OP_READ, new Attachement());

    }

    private void doRead(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        Attachement att = (Attachement)key.attachment();
        int readi = channel.read(att.getBb());
        if(readi == -1){
        	channel.close();
        }
        if(att.getBb().hasRemaining()) {
        	return;
        }
        att.getBb().flip();
        att.int1 = att.getBb().getInt();
        att.int2 = att.getBb().getInt();
        att.getBb().clear();
        att.getBb().putInt(att.int1+att.int2);
        channel.register(selector, SelectionKey.OP_WRITE, att);

    }

    private void doWrite(SelectionKey key) throws IOException {
    	Attachement att = (Attachement)key.attachment();
    	SocketChannel channel = (SocketChannel) key.channel();
    	
    	att.getBb().flip();
        channel.write(att.getBb());
        
        if(att.getBb().hasRemaining()){
        	att.getBb().compact();
        	return;
        }
        att.getBb().clear();
        key.interestOps(SelectionKey.OP_READ);
        
        
    }



    public static void main(String[] args) throws NumberFormatException, IOException {
        new ServerNonBlocking(Integer.parseInt(args[0])).launch();

    }


    /***
     *  Theses methods are here to help understanding the behavior of the selector
     ***/

    private String interestOpsToString(SelectionKey key){
        if (!key.isValid()) {
            return "CANCELLED";
        }
        int interestOps = key.interestOps();
        ArrayList<String> list = new ArrayList<>();
        if ((interestOps&SelectionKey.OP_ACCEPT)!=0) list.add("OP_ACCEPT");
        if ((interestOps&SelectionKey.OP_READ)!=0) list.add("OP_READ");
        if ((interestOps&SelectionKey.OP_WRITE)!=0) list.add("OP_WRITE");
        return String.join("|",list);
    }

    public void printKeys() {
        Set<SelectionKey> selectionKeySet = selector.keys();
        if (selectionKeySet.isEmpty()) {
            System.out.println("The selector contains no key : this should not happen!");
            return;
        }
        System.out.println("The selector contains:");
        for (SelectionKey key : selectionKeySet){
            SelectableChannel channel = key.channel();
            if (channel instanceof ServerSocketChannel) {
                System.out.println("\tKey for ServerSocketChannel : "+ interestOpsToString(key));
            } else {
                SocketChannel sc = (SocketChannel) channel;
                System.out.println("\tKey for Client "+ remoteAddressToString(sc) +" : "+ interestOpsToString(key));
            }


        }
    }

    private String remoteAddressToString(SocketChannel sc) {
        try {
            return sc.getRemoteAddress().toString();
        } catch (IOException e){
            return "???";
        }
    }

    private void printSelectedKey() {
        if (selectedKeys.isEmpty()) {
            System.out.println("There were not selected keys.");
            return;
        }
        System.out.println("The selected keys are :");
        for (SelectionKey key : selectedKeys) {
            SelectableChannel channel = key.channel();
            if (channel instanceof ServerSocketChannel) {
                System.out.println("\tServerSocketChannel can perform : " + possibleActionsToString(key));
            } else {
                SocketChannel sc = (SocketChannel) channel;
                System.out.println("\tClient " + remoteAddressToString(sc) + " can perform : " + possibleActionsToString(key));
            }

        }
    }

    private String possibleActionsToString(SelectionKey key) {
        if (!key.isValid()) {
            return "CANCELLED";
        }
        ArrayList<String> list = new ArrayList<>();
        if (key.isAcceptable()) list.add("ACCEPT");
        if (key.isReadable()) list.add("READ");
        if (key.isWritable()) list.add("WRITE");
        return String.join(" and ",list);
    }
    
    class Attachement {
    	ByteBuffer req = ByteBuffer.allocate(8);
        int int1; int int2;

        public Attachement() {
            
        }
        
        public ByteBuffer getBb(){
        	return req;
        }

		public void setBb(ByteBuffer bb) {
			this.req = bb;
		}
		
    }
}
