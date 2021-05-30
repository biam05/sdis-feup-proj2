package sdist1g21;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
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
    AsynchronousSocketChannel serverChannel;
    PeerContainer peerContainer;
    String serverAddress;
    int peerID, serverPort;
    boolean isMainPeer, isServerPeer, closeChannel;

    ScheduledThreadPoolExecutor peerChannelExecutors = new ScheduledThreadPoolExecutor(Utils.MAX_THREADS);

    public PeerChannel(int peerID, Boolean isMain, Boolean isServerPeer, String serverAddress, int serverPort, PeerContainer peerContainer) {
        this.serverAddress=serverAddress;
        this.serverPort=serverPort;
        this.peerID = peerID;
        this.isMainPeer = isMain;
        this.isServerPeer = isServerPeer;
        this.peerContainer = peerContainer;
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
                    AsynchronousSocketChannel clientChannel;

                    try {
                        clientChannel = asynchFuture.get();
                        Callable<String> worker = () -> {

                            String host = clientChannel.getRemoteAddress().toString();
                            System.out.println("PeerChannel: Incoming connection from a peer at: " + host);

                            while(!closeChannel) {
                                ByteBuffer buffer = ByteBuffer.allocate(Utils.MAX_BYTE_MSG);
                                Future<Integer> result = clientChannel.read(buffer);

                                result.get();

                                byte[] msgBytes = buffer.array();
                                buffer.flip();
                                peerChannelExecutors.execute(() -> {
                                    Future<Integer> writeResult;
                                    if(isMainPeer) writeResult = clientChannel.write(ByteBuffer.wrap(MainPeer.messageFromPeerHandler(msgBytes).getBytes()));
                                    else writeResult = clientChannel.write(ByteBuffer.wrap(Peer.messageFromPeerHandler(msgBytes).getBytes()));
                                    try {
                                        System.out.println("Sent " + writeResult.get() + " bytes.");
                                    } catch (InterruptedException | ExecutionException e) {
                                        e.printStackTrace();
                                    }
                                    buffer.clear();
                                });
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

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(peerContainer);
            out.flush();
            byte[] peerContainerBytes = bos.toByteArray();

            bos.close();
            sendMessageToMain(peerID + ":WELCOME:", peerContainerBytes);
        } catch (IOException e) {
            System.err.println("PeerChannel exception: Failed to encode peerContainer!");
            e.printStackTrace();
        }
    }

    public String sendMessageToMain(String message, byte[] bytesMsg) {
        return sendMessage(message, bytesMsg, serverChannel);
    }

    public String sendMessageToPeer(String op, String address, String port, String file_name, long fileSize, byte[] fileBytes) {
        String message = op + ":" + file_name + ":" + fileSize + ":";
        AsynchronousSocketChannel channel;
        try {
            channel = AsynchronousSocketChannel.open();
            InetSocketAddress hostAddress = new InetSocketAddress(address, Integer.parseInt(port));

            Future<Void> future = channel.connect(hostAddress);
            future.get();
        } catch (IOException | ExecutionException | InterruptedException e) {
            System.err.println("PeerChannel exception: Failed to send message to peer!");
            return "error";
        }
        return sendMessage(message, fileBytes, channel);
    }

    private String sendMessage(String message, byte[] bytesMsg, AsynchronousSocketChannel channel) {
        if(!channel.isOpen()) {
            System.err.println("PeerChannel exception: Failed to send message because channel connection is inactive");
            return null;
        }

        try {
            // SEND MESSAGE
            byte[] msgbytes = message.getBytes();

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try {
                outputStream.write(msgbytes);
                if(bytesMsg != null) outputStream.write(bytesMsg);
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("PeerChannel exception: Failed to mount message bytes");
                return null;
            }

            byte[] msg = outputStream.toByteArray();
            ByteBuffer buffer = ByteBuffer.wrap(msg);
            Future<Integer> writeResult = channel.write(buffer);
            System.out.println("Sent " + writeResult.get() + " bytes");
            buffer = ByteBuffer.allocate(Utils.MAX_BYTE_MSG);

            // RECEIVE RESPONSE
            Future<Integer> readResult = channel.read(buffer);

            System.out.println("Received " + readResult.get() + " bytes");
            String response = new String(buffer.array()).trim();
            System.out.println("Message received: " + response);

            buffer.clear();
            return response;
        } catch (InterruptedException | ExecutionException e ) {
            System.err.println("PeerChannel exception: Failed to send message to server");
            return null;
        }
    }

    @Override
    public void run() {
        if(isMainPeer || isServerPeer) peerChannelExecutors.execute(this::serverChannel);
        else peerChannelExecutors.execute(this::clientChannel);
    }
}
