package tp6.ex1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Set;

public class ServerEchoNIOMultiPort {
	static class Info {
		ByteBuffer bb = ByteBuffer.allocateDirect(1024);
		SocketAddress sa;

		public Info() {
		}

		public ByteBuffer getBb() {
			return bb;
		}

		public SocketAddress getSa() {
			return sa;
		}

		public void setBb(ByteBuffer bb) {
			this.bb = bb;
		}

		public void setSa(SocketAddress sa) {
			this.sa = sa;
		}

	}

	private DatagramChannel dc;
	private final Selector selector;

	public ServerEchoNIOMultiPort(int port, int port2) throws IOException {
		selector = Selector.open();
		while (port <= port2) {
			dc = DatagramChannel.open();
			dc.configureBlocking(false);
			dc.bind(new InetSocketAddress(port));
			dc.register(selector, SelectionKey.OP_READ);
			port++;
		}
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
		System.out.println("read more");
		Info i = new Info();
		DatagramChannel d = (DatagramChannel) key.channel();
		SocketAddress a = d.receive(i.getBb());
		if (a == null) {
			System.out.println("no value");
			return;
		}
		i.setSa(a);
		key.attach(i);
		key.interestOps(SelectionKey.OP_WRITE);
	}

	private void doWrite(SelectionKey key) throws IOException {
		Info tmp = (Info) key.attachment();
		DatagramChannel d = (DatagramChannel) key.channel();
		System.out.println("write mode");
		d.send((ByteBuffer) tmp.getBb().flip(), tmp.getSa());
		if (tmp.getBb().remaining() != 0) {
			System.out.println("fail");
			tmp.getBb().compact();
		} else {
			key.interestOps(SelectionKey.OP_READ);
			System.out.println("succes");
		}

	}

	public static void usage() {
		System.out.println("Usage : ServerEchoNIO port");
	}

	public static void main(String[] args) throws IOException {
		if (args.length != 2) {
			usage();
			return;
		}
		ServerEchoNIOMultiPort server = new ServerEchoNIOMultiPort(Integer.valueOf(args[0]), Integer.valueOf(args[1]));
		server.serve();
	}

}
