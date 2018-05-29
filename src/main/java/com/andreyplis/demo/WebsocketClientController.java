package com.andreyplis.demo;

import java.net.*;

import com.tibbo.aggregate.common.device.*;
import com.tibbo.aggregate.common.protocol.*;
import org.eclipse.jetty.websocket.client.*;

public class WebsocketClientController extends RemoteServerController {

    public WebsocketClientController(RemoteServer device, boolean async) {
        super(device, async);
        setDataChannel(new WebsocketClientChannel());
    }

    @Override
    protected void prepareDataChannel() throws RemoteDeviceErrorException {
        WebSocketClient client = new WebSocketClient();
        try {
            client.start();

            URI echoUri = new URI("ws://localhost:8080/websockets/client");
            ClientUpgradeRequest request = new ClientUpgradeRequest();
            client.connect(getDataChannel(), echoUri, request);
            //FIXME
            Thread.sleep(1000);
            System.out.printf("Connecting to : %s%n", echoUri);
            setCommandParser(new AggreGateCommandParser(getDataChannel()));
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}