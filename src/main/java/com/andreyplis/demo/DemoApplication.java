package com.andreyplis.demo;

public class DemoApplication {

    public static void main(String[] args) throws Exception {


        //ClientStartupRunner runner = new ClientStartupRunner();

        AgentStartupRunner runner = new AgentStartupRunner();

        runner.run();
    }
}
