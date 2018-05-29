package com.andreyplis.demo;

import java.io.*;
import java.net.*;

import com.tibbo.aggregate.common.agent.*;
import com.tibbo.aggregate.common.util.*;
import org.eclipse.jetty.websocket.client.*;

public class WebsocketAgent extends Agent {


    public WebsocketAgent(AgentContext context, boolean useSecureConnection, boolean useDataCompression, int bufferSize) {
        super(context, useSecureConnection, useDataCompression, bufferSize);
    }

    @Override
    protected BlockingChannel constructChannel() throws IOException {
        WebsocketClientChannel websocketClientChannel = new WebsocketClientChannel();

        WebSocketClient client = new WebSocketClient();
        try {
            client.start();

            URI echoUri = new URI("ws://localhost:8080/websockets/agent");
            ClientUpgradeRequest request = new ClientUpgradeRequest();
            client.connect(websocketClientChannel, echoUri, request);
            //FIXME
            Thread.sleep(1000);
            System.out.printf("Connecting to : %s%n", echoUri);
        } catch (Throwable t) {
            t.printStackTrace();
        }

        return websocketClientChannel;
    }
}
