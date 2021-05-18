package sdist1g21;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;

public class PeerChannel extends Thread {
    AsynchronousServerSocketChannel ownChannel;
    AsynchronousSocketChannel clientChannel, friendChannel;
    String ownAddress;
    int ownPort;
    String friendAddress;
    int friendPort;

    ScheduledThreadPoolExecutor peerChannelServerExecutors = new ScheduledThreadPoolExecutor(Utils.MAX_THREADS);
    ScheduledThreadPoolExecutor peerChannelClientExecutors = new ScheduledThreadPoolExecutor(Utils.MAX_THREADS);

    public PeerChannel(String ownAddress, int ownPort, String friendAddress, int friendPort) {
        this.ownAddress=ownAddress;
        this.ownPort=ownPort;
        this.friendAddress=friendAddress;
        this.friendPort=friendPort;
    }

    public void serverChannel() {
        try {
            ownChannel = AsynchronousServerSocketChannel.open();
            InetSocketAddress hostAddress = new InetSocketAddress(ownAddress, ownPort);
            ownChannel.bind(hostAddress);

            System.out.println("PeerChannel: Successfully established the server connection at: " + hostAddress);

            Future<AsynchronousSocketChannel> acceptResult = ownChannel.accept();
            clientChannel = acceptResult.get();

            while(true) {
                ByteBuffer buffer = ByteBuffer.allocate(Utils.MAX_BYTE_MSG);
                Future<Integer> result = clientChannel.read(buffer);

                result.get();

                String message = new String(buffer.array()).trim();
                peerChannelServerExecutors.execute(() -> handlePeerMessage(message));
                buffer.clear();
            }
        } catch (ExecutionException e) {
            try {
                ownChannel.close();
            } catch (IOException ioException) {
                System.err.println("PeerChannel exception: Failed to close the server connection");
                e.printStackTrace();
            }
            peerChannelServerExecutors.execute(this::serverChannel);
        } catch (IOException | InterruptedException e) {
            System.err.println("PeerChannel exception: Failed to establish the server connection");
            e.printStackTrace();
        }
    }

    private void handlePeerMessage(String msg) {
        System.out.println("Got message: " + msg);
    }

    private void clientChannel() {
        try {
            friendChannel = AsynchronousSocketChannel.open();

            InetSocketAddress hostAddress = new InetSocketAddress(friendAddress, friendPort);

            Future<Void> future = friendChannel.connect(hostAddress);
            future.get();

            System.out.println("PeerChannel: Successfully established the client connection at: " + hostAddress);
        } catch (IOException | InterruptedException | ExecutionException e) {
            System.err.println("PeerChannel exception: Failed to establish the client connection");
            e.printStackTrace();
        }

        sendMessageToFriend("Let's test!");
    }

    private void sendMessageToFriend(String message) {
        if(!friendChannel.isOpen()) {
            System.err.println("PeerChannel exception: Failed to send message because friend channel connection is inactive");
            return;
        }
        try {
            byte[] msgbytes = message.getBytes();
            ByteBuffer buffer = ByteBuffer.wrap(msgbytes);
            Future<Integer> result = friendChannel.write(buffer);
            System.out.println("Sent " + result.get() + " bytes");
            buffer.clear();
            Scanner userInput = new Scanner(System.in);
            while(true) {
                System.out.println("Send next message: ");

                String input = userInput.nextLine();
                System.out.println("input is '" + input + "'");

                if (!input.isEmpty()) {
                    sendMessageToFriend(input);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("PeerChannel exception: Failed to send message to friend");
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        peerChannelServerExecutors.execute(this::serverChannel);
        if(!(ownAddress.equals(friendAddress) && ownPort == friendPort)) peerChannelClientExecutors.execute(this::clientChannel);
    }
}
