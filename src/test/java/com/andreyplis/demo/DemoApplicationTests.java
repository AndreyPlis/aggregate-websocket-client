package com.andreyplis.demo;

import java.net.*;

import com.tibbo.aggregate.common.agent.*;
import com.tibbo.aggregate.common.context.*;
import com.tibbo.aggregate.common.datatable.*;
import com.tibbo.aggregate.common.examples.*;
import examples.agent.*;

import static org.junit.Assert.assertNotEquals;

public class DemoApplicationTests extends AbstractTestExamples {
    private static final String E_EVENT = "event";
    private static final String V_SETTING = "setting";
    private static final String V_PERIOD = "period";
    private static final String F_SEND_BUFFERED_EVENTS = "sendBufferedEvents";
    private static final String F_GET_HISTORY = "getHistory";
    private static final String F_OPERATION = "operation";
    private static final String CONTEXT_PATH = "users.admin.devices.java";
    private static final Long EVENT_PERIOD = 300L;

    private Agent agent;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        agent = DemoAgent.setUpDemoAgent(EVENT_PERIOD);
    }

    public void testSetVariableCount() throws Exception {
        assertEquals(0, agent.getContext().getVariableData(V_SETTING).getSetCount());

        startThread(EVENT_PERIOD);

        assertNotEquals(0, agent.getContext().getVariableData(V_SETTING).getSetCount());
    }

    public void testFireEventFireCount() throws Exception {
        assertEquals(0, agent.getContext().getEventData(E_EVENT).getFireCount());

        startThread(EVENT_PERIOD);

        assertNotEquals(0, agent.getContext().getEventData(E_EVENT).getFireCount());
    }

    public void testSendBufferedEventsExecutionCount() throws Exception {
        assertEquals(0, agent.getContext().getFunctionData(F_SEND_BUFFERED_EVENTS).getExecutionCount());

        startThread(EVENT_PERIOD);

        assertNotEquals(0, agent.getContext().getFunctionData(F_SEND_BUFFERED_EVENTS).getExecutionCount());
    }

    public void testSendBufferedEvents() throws Exception {
        DataTable dt = agent.getContext().callFunction(F_SEND_BUFFERED_EVENTS);

        assertNotEquals(0, dt.getRecordCount());
    }

    public void testGetSetting() throws Exception {
        startThread(EVENT_PERIOD);

        assertNotEquals(0, getResultFromVariable(agent.getContext(), V_SETTING, 1));
    }

    public void testGetSettingOnServerSide() throws Exception {
        startThread(EVENT_PERIOD);
        Context context = getContext(CONTEXT_PATH);
        DataTable result = context.getVariable(V_SETTING);

        assertNotNull(result);
        assertNotEquals(0, result.getRecordCount());
    }

    public void testSetSettingOnServerSide() throws Exception {
        startThread(EVENT_PERIOD);
        Context context = getContext(CONTEXT_PATH);
        int oldData = (int) getResultFromVariable(context, V_SETTING, 1);
        int newData = 111222333;
        DataTable newSetting = new SimpleDataTable(DemoAgent.VFT_SETTING, true);
        newSetting.rec().setValue(1, newData);
        context.setVariable(V_SETTING, newSetting);

        assertNotEquals(oldData, getResultFromVariable(context, V_SETTING, 1));
        assertEquals(newData, getResultFromVariable(context, V_SETTING, 1));
    }

    public void testGetPeriod() throws Exception {
        startThread(EVENT_PERIOD);

        assertEquals(EVENT_PERIOD, getResultFromVariable(agent.getContext(), V_PERIOD, 0));
    }

    public void testGetPeriodOnServerSide() throws Exception {
        startThread(EVENT_PERIOD);

        assertEquals(EVENT_PERIOD, getResultFromVariable(getContext(CONTEXT_PATH), V_PERIOD, 0));
    }

    public void testGetHistory() throws Exception {
        DataTable dt = agent.getContext().callFunction(F_GET_HISTORY);

        assertNotEquals(0, dt.getRecordCount());

        String firstData = "Historical Value";
        int secondData = 456;

        DataTable result = (DataTable) dt.rec().getValue(2);

        assertEquals(firstData, result.rec().getString(0));
        assertEquals(secondData, result.rec().getInt(1).intValue());
    }

    public void testCallOperationFunction() throws Exception {
        startThread(EVENT_PERIOD);
        Context context = getContext(CONTEXT_PATH);
        FunctionDefinition fd = context.getFunctionDefinition(F_OPERATION);
        assertNotNull(fd);

        int limit = 0;
        DataTable newSetting = new SimpleDataTable(DemoAgent.FIFT_OPERATION, true);
        newSetting.rec().setValue(0, limit);
        fd.setImplementation(agent.getContext().getFunctionDefinition(F_OPERATION).getImplementation());
        DataTable dt = fd.getImplementation().execute(null, fd, null, null, newSetting);

        assertEquals(limit, dt.rec().getInt("result").intValue());
    }

    private void startThread(final Long eventPeriod) throws Exception {
        agent = DemoAgent.setUpDemoAgent(eventPeriod);
        agent.connect();

        final Thread eventAgent = new Thread() {
            @Override
            public void run() {
                while (!isInterrupted()) {
                    DemoAgent.setNewVariable(agent, eventPeriod);
                    if (agent.getContext().getVariableData(V_SETTING).getSetCount() > 0) {
                        interrupt();
                        agent.disconnect();
                    }
                }
            }
        };
        eventAgent.setDaemon(true);
        eventAgent.start();
        while (!eventAgent.isInterrupted()) {
            try {
                agent.run();
            } catch (SocketException ex) {
                return;
            }
        }
    }

    private Object getResultFromVariable(Context context, String variable, int index) throws Exception {
        DataTable dt = context.getVariable(variable);
        return dt.rec().getValue(index);
    }

}