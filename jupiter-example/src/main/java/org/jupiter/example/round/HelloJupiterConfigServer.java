package org.jupiter.example.round;

import org.jupiter.registry.ConfigServer;

/**
 * jupiter
 * org.jupiter.example.round
 *
 * @author jiachun.fjc
 */
public class HelloJupiterConfigServer {

    public static void main(String[] args) {
        ConfigServer configServer = new ConfigServer(20001, 1);
        try {
            configServer.start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
