package sdist1g21;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class PeerChannel extends Thread {
    AsynchronousServerSocketChannel mainChannel;
    AsynchronousSocketChannel clientChannel, serverChannel;
    String serverAddress;
    int peerID, serverPort;
    boolean isMainPeer, closeChannel;

    ScheduledThreadPoolExecutor peerChannelExecutors = new ScheduledThreadPoolExecutor(Utils.MAX_THREADS);

    public PeerChannel(int peerID, Boolean isMain, String serverAddress, int serverPort) {
        this.serverAddress=serverAddress;
        this.serverPort=serverPort;
        this.peerID = peerID;
        isMainPeer = isMain;
        closeChannel = false;
    }

    public void serverChannel() {
        try {
            mainChannel = AsynchronousServerSocketChannel.open();
            if (mainChannel.isOpen()) {
                mainChannel.setOption(StandardSocketOptions.SO_RCVBUF, Utils.MAX_BYTE_MSG * 4);
                mainChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
                mainChannel.bind(new InetSocketAddress(serverAddress, serverPort));
                System.out.println("PeerChannel: Server channel open and awaiting for peer connections...");

                while (true) {
                    Future<AsynchronousSocketChannel> asynchFuture = mainChannel.accept();

                    try {
                        clientChannel = asynchFuture.get();
                        Callable<String> worker = () -> {

                            String host = clientChannel.getRemoteAddress().toString();
                            System.out.println("PeerChannel: Incoming connection from a peer at: " + host);

                            while(!closeChannel) {
                                ByteBuffer buffer = ByteBuffer.allocate(Utils.MAX_BYTE_MSG);
                                Future<Integer> result = clientChannel.read(buffer);

                                result.get();

                                String message = new String(buffer.array()).trim();
                                buffer.flip();
                                peerChannelExecutors.execute(() -> MainPeer.messageHandler(message, clientChannel, buffer));
                                buffer.clear();
                            }

                            clientChannel.close();
                            System.out.println("PeerChannel: Closing connection to one client.");
                            return host;
                        };
                        peerChannelExecutors.submit(worker);
                    } catch (InterruptedException | ExecutionException ex) {
                        System.err.println(ex);
                        System.err.println("PeerChannel: Server channel is shutting down ...");
                        peerChannelExecutors.shutdown();
                        break;
                    }
                }
            } else {
                System.out.println("PeerChannel: Failed to open the server channel!");
            }
        } catch (IOException ex) {
            System.out.println("PeerChannel: Failed to bind address to server channel!");
        }
    }

    private void clientChannel() {
        try {
            serverChannel = AsynchronousSocketChannel.open();

            InetSocketAddress hostAddress = new InetSocketAddress(serverAddress, serverPort);

            Future<Void> future = serverChannel.connect(hostAddress);
            future.get();

            System.out.println("PeerChannel: Successfully established the client connection at: " + hostAddress);
        } catch (IOException | InterruptedException | ExecutionException e) {
            System.err.println("PeerChannel exception: Failed to establish the client connection");
            e.printStackTrace();
        }
    }

    public String sendMessageToMain(String message) {
        return sendMessage(message, serverChannel);
    }

    public String sendMessageToPeer(String message) {
        return sendMessage(message, clientChannel);
    }

    private String sendMessage(String message, AsynchronousSocketChannel channel) {
        if(!channel.isOpen()) {
            System.err.println("PeerChannel exception: Failed to send message because channel connection is inactive");
            return null;
        }
        try {
            // SEND MESSAGE
            byte[] msgbytes = message.getBytes();
            ByteBuffer buffer = ByteBuffer.wrap(msgbytes);
            Future<Integer> writeResult = channel.write(buffer);
            System.out.println("Sent " + writeResult.get() + " bytes");

            // RECEIVE RESPONSE
            buffer = ByteBuffer.allocate(Utils.MAX_BYTE_MSG);
            Future<Integer> readResult = channel.read(buffer);

            // do some computation

            System.out.println("Received " + readResult.get() + " bytes");
            String response = new String(buffer.array()).trim();
            buffer.clear();
            return response;
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("PeerChannel exception: Failed to send message to server");
            return null;
        }
    }

    @Override
    public void run() {
        if(isMainPeer) peerChannelExecutors.execute(this::serverChannel);
        else peerChannelExecutors.execute(this::clientChannel);
    }
}
