package org.gary.pappo.server;

public class RpcServer {

    private String zkAddress;

    public RpcServer(String zkAddress) {
        this.zkAddress = zkAddress;
    }

    public void provideService(String implPackage, int nettyPort) {
        NetFrameServer.processRequest(implPackage, zkAddress, nettyPort);
    }

}
