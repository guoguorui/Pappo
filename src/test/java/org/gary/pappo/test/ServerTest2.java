package org.gary.pappo.test;

import org.gary.pappo.server.RpcServer;

public class ServerTest2 {

    public static void main(String[] args) {
        RpcServer rpcServer = new RpcServer("127.0.0.1:2181");
        rpcServer.provideService("org.gary.pappo.serviceimpl", 9999);
    }
}
