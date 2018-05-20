package org.gary.pappo.server;

import org.gary.netframe.eventhandler.Reply;
import org.gary.netframe.eventhandler.ServerEventHandler;
import org.gary.pappo.carrier.RpcRequest;
import org.gary.pappo.carrier.RpcResponse;
import org.gary.pappo.common.ReflectionUtil;
import org.gary.pappo.common.SerializeUtil;

class MyServerEventHandler extends ServerEventHandler {

    private String implPackage;

    MyServerEventHandler(String implPackage) {
        this.implPackage = implPackage;
    }

    @Override
    public Reply onRead(byte[] readBytes) {
        try {
            RpcRequest rpcRequest = SerializeUtil.deserialize(readBytes, RpcRequest.class);
            RpcResponse rpcResponse = new RpcResponse();
            rpcResponse.setResult(ReflectionUtil.getResult(rpcRequest, implPackage));
            int id = rpcRequest.getId();
            rpcResponse.setId(id);
            System.out.println("处理完请求：" + id);
            return new Reply(true, SerializeUtil.serialize(rpcResponse, RpcResponse.class));
        } catch (Exception e) {
            e.printStackTrace();
            return new Reply(false, null);
        }
    }
}
