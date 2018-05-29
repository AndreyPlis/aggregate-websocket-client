package com.andreyplis.demo;

import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.TimeUnit;

import com.tibbo.aggregate.common.util.*;
import org.eclipse.jetty.websocket.api.*;
import org.eclipse.jetty.websocket.api.annotations.*;


@WebSocket(maxBinaryMessageSize = 64 * 1024)
public class WebsocketClientChannel implements BlockingChannel {
    private final CountDownLatch closeLatch;
    @SuppressWarnings("unused")
    private Session session;

    private final ByteArrayOutputStream inputBuffer;

    public WebsocketClientChannel() {
        this.closeLatch = new CountDownLatch(1);
        this.inputBuffer = new ByteArrayOutputStream();
    }

    public boolean awaitClose(int duration, TimeUnit unit) throws InterruptedException {
        return this.closeLatch.await(duration, unit);
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        System.out.printf("Connection closed: %d - %s%n", statusCode, reason);
        this.session = null;
        this.closeLatch.countDown(); // trigger latch
    }

    @OnWebSocketConnect
    public void onConnect(Session session) throws IOException {
        this.session = session;

    }

    @OnWebSocketMessage
    public void onMessage(byte buf[], int offset, int length) {
        inputBuffer.write(buf, offset, length);
    }

    @Override
    public boolean isUsesCompression() {
        return false;
    }

    @Override
    public void setUsesCompression(boolean b) {

    }

    @Override
    public void flush() throws IOException {

    }

    @Override
    public String getChannelAddress() {
        return session.getLocalAddress().getHostName();
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        if (inputBuffer.size() > 0) {
            int dstSize = dst.remaining();
            byte[] bufToWrite;

            if (inputBuffer.size() > dstSize) {
                byte[] buf = inputBuffer.toByteArray();
                inputBuffer.reset();

                bufToWrite = Arrays.copyOf(buf, dstSize);
                byte[] bufRemaining = Arrays.copyOfRange(buf, dstSize, buf.length);

                inputBuffer.write(bufRemaining);
                dst.put(bufToWrite);
            } else {
                bufToWrite = inputBuffer.toByteArray();
                inputBuffer.reset();
                dst.put(bufToWrite);
            }
            return bufToWrite.length;
        }
        return 0;
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        int size = src.remaining();
        session.getRemote().sendBytes(src);
        return size;
    }

    @Override
    public boolean isOpen() {
        return session.isOpen();
    }

    @Override
    public void close() throws IOException {
        if (session != null)
            session.close();
    }
}
