package org.gary.pappo.test;

import org.gary.nettyrpc.service.UserService;
import org.gary.pappo.client.RpcClient;

public class ClientTest {
    public static void main(String[] args) {
        RpcClient rpcClient = new RpcClient("127.0.0.1:2181");
        UserService userService = rpcClient.getImpl(UserService.class);
        for (int i = 0; i < 10; i++) {
            //User user = userService.getUser();
            //System.out.println("泪流满面: " + user.getName());
            //System.out.println("泪流满面: " + user.getPassword());
            System.out.println("泪流满面：" + userService.getPasswordByName("hello"));
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}
