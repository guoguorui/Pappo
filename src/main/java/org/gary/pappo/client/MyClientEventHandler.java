package org.gary.pappo.client;

import org.gary.netframe.eventhandler.ClientEventHandler;
import org.gary.netframe.eventhandler.Reply;
import org.gary.pappo.carrier.RpcRequest;
import org.gary.pappo.carrier.RpcResponse;
import org.gary.pappo.common.SerializeUtil;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class MyClientEventHandler extends ClientEventHandler {

    private ConcurrentHashMap<Integer, CountDownLatch> idToSignal = new ConcurrentHashMap<>();
    ConcurrentHashMap<Integer, RpcResponse> idToResult = new ConcurrentHashMap<>();

    @Override
    public Reply onRead(byte[] readBytes) {
        RpcResponse rpcResponse = SerializeUtil.deserialize(readBytes, RpcResponse.class);
        int id = rpcResponse.getId();
        idToResult.put(id, rpcResponse);
        CountDownLatch countDownLatch = idToSignal.get(id);
        if (countDownLatch != null) {
            idToSignal.remove(id);
            countDownLatch.countDown();
        }
        return new Reply(false, null);
    }

    void writeToServer(RpcRequest rpcRequest, CountDownLatch countDownLatch) {
        int id = rpcRequest.getId();
        idToSignal.put(id, countDownLatch);
        byte[] content = SerializeUtil.serialize(rpcRequest, RpcRequest.class);
        writeToServer(content);
    }
}
