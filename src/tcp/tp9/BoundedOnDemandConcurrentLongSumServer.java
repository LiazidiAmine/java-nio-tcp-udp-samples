package tcp.tp9;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Semaphore;

public class BoundedOnDemandConcurrentLongSumServer {
  
  private final ServerSocketChannel serverSocketChannel;
  private final Semaphore semaphore;
  private static final int maxClient = 5;
  
  public BoundedOnDemandConcurrentLongSumServer(int port) throws IOException {
    serverSocketChannel = ServerSocketChannel.open();
    serverSocketChannel.bind(new InetSocketAddress(port));
    System.out.println(this.getClass().getName() 
        + " bound on " + serverSocketChannel.getLocalAddress());
    semaphore = new Semaphore(maxClient);
  }
  
  public void launch() throws IOException {
	  while(!Thread.interrupted()) {
		  try {
			  semaphore.acquire();
			  SocketChannel client = serverSocketChannel.accept();
			  System.out.println("Connection accepted from " + client.getRemoteAddress());
			  Thread thread = new Thread(()->{
				  try {
					  serve(client);
				  } catch (IOException ioe) {
					  System.out.println("I/O Error while communicating with client... ");
					  ioe.printStackTrace();
				  } catch (InterruptedException ie) {
					  System.out.println("Server interrupted... ");
					  ie.printStackTrace();
					  return;
				  } finally {
					  silentlyClose(client);
					  semaphore.release();
				  }  
			  });
			  thread.start(); 
		  } catch (InterruptedException e) {
			  // TODO Auto-generated catch block
			  e.printStackTrace();
		  } finally{
			  semaphore.release();
		  }

		  
	  }
  }
  
  private void serve(SocketChannel sc) throws IOException, InterruptedException {
	  ByteBuffer bb = ByteBuffer.allocate(1024);
		while (true) {
			bb.clear();
			bb.limit(Integer.BYTES);
			if (!readFully(bb, sc)) {
				return;
			}
			bb.flip();
			int nbOp = bb.getInt();

			bb.clear();
			bb.limit(Long.BYTES * nbOp);
			if (!readFully(bb, sc)) {
				return;
			}
			bb.flip();
			long sum = 0;
			for (int i = 0; i < nbOp; i++) {
				sum += bb.getLong();
			}
			
			ByteBuffer ret = ByteBuffer.allocate(Long.BYTES);
			ret.putLong(sum);
			ret.flip();
			
			sc.write(ret);
		}
	}
  
  private boolean readFully(ByteBuffer bb, SocketChannel sc) throws IOException {
  	while(sc.read(bb) != -1){
		if(!bb.hasRemaining()){
			return true;
		}
	}
	return false;
}

private void silentlyClose(SocketChannel sc) {
    if (sc != null) {
      try {
        sc.close();
      } catch (IOException e) {
        // Do nothing
      }
    }
  }
  
  public static void main(String[] args) throws NumberFormatException, IOException {
    IterativeLongSumServer server = new IterativeLongSumServer(Integer.parseInt(args[0]));
    server.launch();
  }
}
