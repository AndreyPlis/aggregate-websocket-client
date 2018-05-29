package com.andreyplis.demo;

import com.tibbo.aggregate.common.*;
import com.tibbo.aggregate.common.context.*;
import com.tibbo.aggregate.common.datatable.*;
import com.tibbo.aggregate.common.protocol.*;
import com.tibbo.aggregate.common.server.*;


public class ClientStartupRunner {


    public void run() throws Exception {

        System.out.println("Server version: ");
        try {

            // Enabling logging
            Log.start();

            // Provide correct server address/port and name/password of server user to log in as
            RemoteServer rls = new RemoteServer("localhost", 8080, "admin", "admin");

            // Creating server controller
            RemoteServerController rlc = new WebsocketClientController(rls, true);


            // Connecting to the server

            rlc.connect();

            // Authentication/authorization
            rlc.login();

            // Getting context manager
            ContextManager cm = rlc.getContextManager();

            // Getting root context
            Context rootContext = cm.getRoot();

            // Getting "version" variable from the root context
            DataTable versionData = rootContext.getVariable(RootContextConstants.V_VERSION);

            // Version string is contained in the first record and "version" field of the data table
            String serverVersion = versionData.rec().getString(RootContextConstants.VF_VERSION_VERSION);

            System.out.println("Server version: " + serverVersion);

            // Disconnecting from the server
            rlc.disconnect();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}