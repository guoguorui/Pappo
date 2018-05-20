package org.gary.pappo.server;

import org.gary.netframe.nio.NioServer;
import org.gary.pappo.common.ReflectionUtil;
import org.gary.pappo.zookeeper.ServiceRegister;

import java.net.InetAddress;
import java.util.List;

class NetFrameServer {

    static void processRequest(String implPackage, String zkAddress, int netFramePort) {
        try {
            MyServerEventHandler eventHandler = new MyServerEventHandler(implPackage);
            new NioServer(eventHandler).startup(netFramePort);
            eventHandler.connectAvailable();
            List<String> serviceNames = ReflectionUtil.getInterfaceNames(implPackage);
            if (serviceNames != null) {
                ServiceRegister serviceRegister = new ServiceRegister(zkAddress);
                for (String serviceName : serviceNames)
                    serviceRegister.register(serviceName, InetAddress.getLocalHost().getHostAddress() + ":" + String.valueOf(netFramePort));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
