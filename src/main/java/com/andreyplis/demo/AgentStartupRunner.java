package com.andreyplis.demo;

import java.io.*;
import java.util.*;

import com.tibbo.aggregate.common.*;
import com.tibbo.aggregate.common.agent.*;
import com.tibbo.aggregate.common.context.*;
import com.tibbo.aggregate.common.data.*;
import com.tibbo.aggregate.common.datatable.*;
import com.tibbo.aggregate.common.datatable.field.*;
import com.tibbo.aggregate.common.device.*;
import com.tibbo.aggregate.common.event.*;
import com.tibbo.aggregate.common.protocol.*;
import com.tibbo.aggregate.common.util.*;
import org.apache.log4j.*;

public class AgentStartupRunner {
    private static final String V_SETTING = "setting";
    private static final String V_PERIOD = "period";
    private static final String F_OPERATION = "operation";
    private static final String E_EVENT = "event";

    private static final String VF_SETTING_STRING = "string";
    private static final String VF_SETTING_INTEGER = "integer";
    private static final String VF_PERIOD_PERIOD = "period";

    private static final String FIF_OPERATION_LIMIT = "limit";

    private static final String FOF_OPERATION_RESULT = "result";

    private static final String EF_EVENT_DATA = "data";

    // Creating agent's first setting format (1..100 records table)
    public static final TableFormat VFT_SETTING = new TableFormat(1, 100);

    static {
        // Adding string field
        VFT_SETTING.addField("<" + VF_SETTING_STRING + "><S><D=String Field>");

        // Adding integer field
        VFT_SETTING.addField("<" + VF_SETTING_INTEGER + "><I><D=Integer Field>");
    }

    // Creating second setting format (one record, one long integer field)
    private static final TableFormat VFT_PERIOD = new TableFormat(1, 1, "<" + VF_PERIOD_PERIOD + "><L><A=5000><D=Event Generation Period><V=<L=100 100000000>><E=" + LongFieldFormat.EDITOR_PERIOD + ">");

    // Creating agent's function input format (one record, one integer field, default value is 100)
    public static final TableFormat FIFT_OPERATION = new TableFormat(1, 1, "<" + FIF_OPERATION_LIMIT + "><I><A=100><D=Limit>");

    // Creating agent's function output format (one record, one integer field)
    private static final TableFormat FOFT_OPERATION = new TableFormat(1, 1, "<" + FOF_OPERATION_RESULT + "><I><D=Result>");

    // Creating agent's event format (one record, one floating point field)
    private static final TableFormat EFT_EVENT = new TableFormat(1, 1, "<" + EF_EVENT_DATA + "><F><D=Data>");

    // Current setting value
    private static DataTable setting = new SimpleDataTable(VFT_SETTING, true);

    // Current event generation period
    private static long period = 5000;

    public static void run() {
        // Initializing AggreGate libraries logging
        Log.start();

        Log.PROTOCOL.setLevel(Level.DEBUG);

        beginDemo(period);
    }

    private static void beginDemo(final Long eventPeriod) {
        Thread eventGenerator = null;

        while (!Thread.currentThread().isInterrupted()) {
            try {

                final Agent agent = setUpDemoAgent(eventPeriod);

                // Connecting to the server and logging in
                agent.connect();

                // Creating and starting event generation thread
                eventGenerator = new Thread() {
                    @Override
                    public void run() {
                        while (!isInterrupted()) {
                            setNewVariable(agent, eventPeriod);
                        }
                    }
                };
                runAgent(eventGenerator, agent);
            } catch (DisconnectionException ex) {
                ex.printStackTrace();

                // Loop for reconnection
            } catch (Exception ex) {
                ex.printStackTrace();

                // Loop for reconnection
            }
            pause();
            interruptThread(eventGenerator);
        }
    }

    public static Agent setUpDemoAgent(final Long eventPeriod) {
        // Setting up server connection properties
        RemoteServer rls = new RemoteServer(RemoteServer.DEFAULT_ADDRESS, Agent.DEFAULT_PORT, RemoteServer.DEFAULT_USERNAME, RemoteServer.DEFAULT_PASSWORD);

        // Creating agent context
        final AgentContext agentContext = new DemoAgentContext(rls, "java", true);

        // Creating agent controller
        final Agent agent = new WebsocketAgent(agentContext, false, false, 0);

        // Agent context initialization
        initializeAgentContext(agent.getContext(), eventPeriod);

        return agent;
    }

    public static void setNewVariable(final Agent agent, Long period) {
        try {
            // Event generation period will vary depending to the period setting
            Thread.currentThread().sleep(period);
        } catch (InterruptedException ex) {
            return;
        }

        // Event generation is allowed only after the end of first synchronization
        if (agent.getContext().isSynchronized()) {
            // Constructing event data table
            DataTable eventData = new SimpleDataTable(agent.getContext().getEventDefinition(E_EVENT).getFormat(), new Float(Math.random() * 1000000));

            // Firing context event that will be forwarded to server
            agent.getContext().fireEvent(E_EVENT, EventLevel.INFO, eventData);

            if (Math.random() > 0.1) {
                // Additionally, illustrating how to report variable change to the server asynchronously
                try {
                    agent.getContext().setVariable(V_SETTING, agent.getContext().getVariable(V_SETTING).rec().getString(VF_SETTING_STRING), Math.random() * 1000);
                } catch (ContextException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private static void runAgent(Thread eventGenerator, Agent agent) throws SyntaxErrorException, DisconnectionException, IOException {
        eventGenerator.setDaemon(true);
        eventGenerator.start();

        // The main thread will call Agent.run() until the application is terminated or disconnection is detected and DisconnectionException is thrown
        while (!Thread.currentThread().isInterrupted()) {
            // Processing a single server command
            agent.run();
        }

        // Disconnecting from the server
        agent.disconnect();
    }

    private static void pause() {
        try {
            Thread.currentThread().sleep(1000); // Pausing before reconnection to prevent repeating errors
        } catch (InterruptedException ex) {
            System.exit(0);
        }
    }

    private static void interruptThread(Thread eventGenerator) {
        if (eventGenerator != null) {
            eventGenerator.interrupt();
        }
    }

    private static void initializeAgentContext(AgentContext context, final Long eventPeriod) {

        // If we add some assets here, we'll need to make sure group of all entities is remote|assetId|subassetID [|...]
        // Entity group can then be constructed via ContextUtils.createGroup(ContextUtils.GROUP_REMOTE, "Asset A", "Subasset B")

        // DeviceAssetDefinition asset = new DeviceAssetDefinition("Test Asset", "Test Asset");
        // context.addAsset(asset);
        //
        // asset.addSubgroup(new DeviceAssetDefinition("Test Subasset A", "Test Subasset A"));
        // asset.addSubgroup(new DeviceAssetDefinition("Test Subasset B", "Test Subasset B"));
        //
        // String entityGroupA = ContextUtils.createGroup(ContextUtils.GROUP_REMOTE, "Test Asset", "Test Subasset A");
        // String entityGroupB = ContextUtils.createGroup(ContextUtils.GROUP_REMOTE, "Test Asset", "Test Subasset B");

        // Creating Agent setting definition
        VariableDefinition vd = new VariableDefinition(V_SETTING, VFT_SETTING, true, true, "Tabular Setting", ContextUtils.GROUP_REMOTE);

        // Adding setting getter
        vd.setGetter(new VariableGetter() {
            @Override
            public DataTable get(Context con, VariableDefinition def, CallerController caller, RequestController request) throws ContextException {
                return setting;
            }
        });

        // Adding setting setter
        vd.setSetter(new VariableSetter() {
            @Override
            public boolean set(Context con, VariableDefinition def, CallerController caller, RequestController request, DataTable value) throws ContextException {
                setting = value;
                return true;
            }
        });

        // Adding setting definition to the context
        context.addVariableDefinition(vd);

        // Creating and adding another setting
        vd = new VariableDefinition(V_PERIOD, VFT_PERIOD, true, true, "Event Generation Period", ContextUtils.GROUP_REMOTE);
        vd.setGetter(new VariableGetter() {
            @Override
            public DataTable get(Context con, VariableDefinition def, CallerController caller, RequestController request) throws ContextException {
                return new DataRecord(VFT_PERIOD).addLong(eventPeriod).wrap();
            }
        });
        vd.setSetter(new VariableSetter() {
            @Override
            public boolean set(Context con, VariableDefinition def, CallerController caller, RequestController request, DataTable value) throws ContextException {
                period = value.rec().getLong(VF_PERIOD_PERIOD);
                Log.DEVICE_AGENT.info("Server has changed event generation period to: " + period);
                return true;
            }
        });
        context.addVariableDefinition(vd);

        // Creating Agent operation definition
        FunctionDefinition fd = new FunctionDefinition(F_OPERATION, FIFT_OPERATION, FOFT_OPERATION, "Agent Operation", ContextUtils.GROUP_REMOTE);

        // Setting operation implementation
        fd.setImplementation(new FunctionImplementation() {
            @Override
            public DataTable execute(Context con, FunctionDefinition def, CallerController caller, RequestController request, DataTable parameters) throws ContextException {
                int limit = parameters.rec().getInt(FIF_OPERATION_LIMIT);
                int result = (int) (Math.random() * limit);
                Log.DEVICE_AGENT.info("Server has executed random number generation operation with limit: " + limit + ", result: " + result);
                return new DataRecord(def.getOutputFormat()).addInt(result).wrap();
            }
        });

        // Adding operation definition to context
        context.addFunctionDefinition(fd);

        // Creating Agent event definition
        EventDefinition ed = new EventDefinition(E_EVENT, EFT_EVENT, "Agent Event", ContextUtils.GROUP_REMOTE);

        // Adding event definition to context
        context.addEventDefinition(ed);

        // Listening for event confirmation
        context.addEventListener(AgentContext.E_EVENT_CONFIRMED, new DefaultContextEventListener() {
            @Override
            public void handle(Event event) throws EventHandlingException {
                Log.DEVICE_AGENT.info("Server has confirmed event with ID: " + event.getData().rec().getLong(AgentContext.EF_EVENT_CONFIRMED_ID));
            }
        });
    }

    private static class DemoAgentContext extends AgentContext {
        // Flag indicating that fake "buffered history" was already sent to the server. Real-life agents should delete historical values from their buffer instead.
        private boolean sentHistory = false;

        public DemoAgentContext(RemoteServer server, String name, boolean eventConfirmation) {
            super(server, name, eventConfirmation);
        }

        /**
         * This method will be called in the beginning of every synchronization. It should build up and return a list of historical values buffered by the Agent during the time when server connection was
         * unavailable. If this method returns more than zero list elements, it will be called again. This allows to return values by batches limited in size.
         */
        @Override
        protected List<HistoricalValue> getHistory() {
            if (sentHistory) {
                return Collections.emptyList();
            }

            sentHistory = true;

            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date());
            cal.add(Calendar.SECOND, -1);

            DataTable historicalValue = new SimpleDataTable(VFT_SETTING, "Historical Value", 456);

            HistoricalValue hv = new HistoricalValue(V_SETTING, cal.getTime(), historicalValue);

            return Collections.singletonList(hv);
        }

    }
}
