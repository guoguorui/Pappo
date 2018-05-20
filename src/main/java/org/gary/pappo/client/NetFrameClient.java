package org.gary.pappo.client;

import org.gary.netframe.nio.NioClient;
import org.gary.pappo.carrier.RpcRequest;
import org.gary.pappo.carrier.RpcResponse;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

class NetFrameClient {


    private MyClientEventHandler eventHandler;
    private String serverAddress;
    private Class<?> serviceClass;
    private NioClient nioClient;

    NetFrameClient(Class<?> serviceClass, String serverAddress) {
        this.serverAddress = serverAddress;
        this.serviceClass = serviceClass;
    }

    void connect() {
        eventHandler = new MyClientEventHandler();
        String[] addresses = serverAddress.split(":");
        String host = addresses[0];
        int port = Integer.parseInt(addresses[1]);
        new NioClient(eventHandler).startup(host, port);
    }

    RpcResponse call(Method method, Object[] args, int id) {
        try {
            eventHandler.connectAvailable();
        } catch (Exception e) {
            return getErrorResponse();
        }
        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setId(id);
        rpcRequest.setInterfaceClass(serviceClass);
        rpcRequest.setMethodName(method.getName());
        rpcRequest.setArgs(args);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        eventHandler.writeToServer(rpcRequest, countDownLatch);
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        ConcurrentHashMap<Integer, RpcResponse> map = eventHandler.idToResult;
        RpcResponse rpcResponse = map.get(id);
        map.remove(id);
        return rpcResponse;
    }

    private RpcResponse getErrorResponse() {
        RpcResponse rpcResponse = new RpcResponse();
        rpcResponse.setStatus(-1);
        rpcResponse.setId(-1);
        return rpcResponse;
    }

}
