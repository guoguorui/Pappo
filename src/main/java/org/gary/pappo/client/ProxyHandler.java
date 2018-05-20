package org.gary.pappo.client;

import org.gary.pappo.carrier.RpcResponse;
import org.gary.pappo.common.CASUtil;
import org.gary.pappo.zookeeper.ServiceDiscover;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

public class ProxyHandler implements InvocationHandler {

    private static AtomicInteger atomicId = new AtomicInteger(0);
    private NetFrameClient netFrameClient;
    private ServiceDiscover sd;
    private String serviceName;
    private String serverAddress;
    private Class serviceClass;
    private volatile int connectFlag;
    private AtomicInteger waitNum = new AtomicInteger(0);


    ProxyHandler(String zkAddress) {
        sd = new ServiceDiscover(zkAddress);
    }

    @SuppressWarnings("unchecked")
    <T> T getImplObj(Class<T> serviceClass) {
        this.serviceClass = serviceClass;
        serviceName = serviceClass.getSimpleName();
        connect();
        return (T) Proxy.newProxyInstance(serviceClass.getClassLoader(), new Class[]{serviceClass}, this);
    }

    public Object invoke(Object proxy, Method method, Object[] args) {
        int id = atomicId.addAndGet(1);
        RpcResponse rpcResponse = netFrameClient.call(method, args, id);
        System.out.println(Thread.currentThread() + "收到回应了  ：" + rpcResponse.getId());
        if (rpcResponse.getStatus() == -1) {
            try {
                atomicId.decrementAndGet();
                waitNum.incrementAndGet();
                synchronized (this) {
                    if (CASUtil.cas(this, 0, 1)) {
                        //在zk处阻塞，但不会在这里交出锁
                        connect();
                        waitNum.decrementAndGet();
                    } else {
                        if (waitNum.decrementAndGet() == 0)
                            CASUtil.cas(this, 1, 0);
                    }
                }
                return method.invoke(proxy, args);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return rpcResponse.getResult();
    }

    private void connect() {
        serverAddress = sd.discover(serviceName, serverAddress);
        netFrameClient = new NetFrameClient(serviceClass, serverAddress);
        netFrameClient.connect();
    }
}