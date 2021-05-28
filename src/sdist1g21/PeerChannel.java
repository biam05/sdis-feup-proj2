package sdist1g21;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Locale;
import java.util.concurrent.*;

public class PeerChannel extends Thread {
    AsynchronousServerSocketChannel mainChannel;
    AsynchronousSocketChannel serverChannel;
    PeerContainer peerContainer;
    String serverAddress;
    int peerID, serverPort;
    boolean isMainPeer, closeChannel;

    ScheduledThreadPoolExecutor peerChannelExecutors = new ScheduledThreadPoolExecutor(Utils.MAX_THREADS);

    public PeerChannel(int peerID, Boolean isMain, String serverAddress, int serverPort, PeerContainer peerContainer) {
        this.serverAddress=serverAddress;
        this.serverPort=serverPort;
        this.peerID = peerID;
        isMainPeer = isMain;
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
                                    Future<Integer> writeResult = clientChannel.write(ByteBuffer.wrap(MainPeer.messageFromPeerHandler(msgBytes, clientChannel).getBytes()));
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

    public String sendMessageToPeer(String message, byte[] bytesMsg) {
        //return sendMessage(message, bytesMsg, clientChannel);
        return "";
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

            //this.messageFromPeerHandler();

            buffer.clear();
            return response;
        } catch (InterruptedException | ExecutionException e ) {
            System.err.println("PeerChannel exception: Failed to send message to server");
            return null;
        }
    }


    public void messageFromPeerHandler(String message) {
        System.out.println("This peer got the message: " + message);
        String[] msg = message.split(":");
        String filename;
        long max_disk_space;
        byte[] file_content;
        switch (msg[0].toUpperCase(Locale.ROOT)) {
            case "BACKUP" -> {
                if (msg.length < 3) {
                    System.err.println("> Peer " + this.peerID + " exception: invalid message received");
                    return;
                }
                filename = msg[1];
                file_content = msg[2].getBytes();
                Backup backup = new Backup(filename,file_content);
                backup.performBackup();
            }
            default -> System.err.println("> Peer " + this.peerID + " got the following basic message: " + message);
        }
    }

    @Override
    public void run() {
        if(isMainPeer) peerChannelExecutors.execute(this::serverChannel);
        else peerChannelExecutors.execute(this::clientChannel);
    }
}
