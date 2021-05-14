package sdist1g21;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class PeerChannel extends Thread {
    AsynchronousServerSocketChannel ownChannel;
    AsynchronousSocketChannel clientChannel;

    public PeerChannel(String address, int port) {
        try {
            ownChannel = AsynchronousServerSocketChannel.open();
            InetSocketAddress hostAddress = new InetSocketAddress(address, port);
            ownChannel.bind(hostAddress);

            System.out.println("Created AsynchronousServerSocketChannel at: " + hostAddress);

            Future<AsynchronousSocketChannel> acceptResult = ownChannel.accept();
            clientChannel = acceptResult.get();

            while(true) {
                ByteBuffer buffer = ByteBuffer.allocate(32);
                Future<Integer> result = clientChannel.read(buffer);
                while (!result.isDone()) {}

                buffer.flip();
                String message = new String(buffer.array()).trim();
                System.out.println(message);
                if (message.equals("Bye.")) {
                    break;
                }
                buffer.clear();
            }
        } catch (IOException | ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public PeerChannel(String ownAddress, int ownPort, String friendAddress, int friendPort) {
        try {
            AsynchronousSocketChannel client = AsynchronousSocketChannel.open();

            InetSocketAddress hostAddress = new InetSocketAddress(friendAddress, friendPort);

            Future<Void> future = client.connect(hostAddress);
            future.get();

            System.out.println("Connected AsynchronousServerSocketChannel at: " + hostAddress);

            String [] messages = new String [] {"Time goes fast.", "What now?", "Bye."};

            for (String s : messages) {
                byte[] message = s.getBytes();
                ByteBuffer buffer = ByteBuffer.wrap(message);
                Future<Integer> result = client.write(buffer);
                System.out.println("Sent " + result.get() + " bytes");
            }
        } catch (IOException | ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while(true) {

        }
    }
}
