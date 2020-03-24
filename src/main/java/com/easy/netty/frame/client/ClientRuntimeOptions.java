package com.easy.netty.frame.client;

/**
 * @Author SunQian
 * @CreateTime 2020/3/19 15:27
 * @Description: runtime options of client
 */
public class ClientRuntimeOptions {
    public int heartbeatSecond = 0;
    public int reconnectSecond = 5;
    private NettyClient nettyClient;

    public ClientRuntimeOptions(NettyClient nettyClient) {
        this.nettyClient = nettyClient;
    }

    /**
     * author: SunQian
     * date: 2020/3/19 16:14
     * title: TODO
     * descritpion: To start a chained call from an netty cient object
     * return: TODO
     */
    public NettyClient client() {
        return nettyClient;
    }

    public ClientRuntimeOptions heartbeatSecond(int heartbeatSecond) {
        if (0 >= heartbeatSecond) {
            //not send heartbeat
            heartbeatSecond = 0;
        }

        this.heartbeatSecond = heartbeatSecond;

        return this;
    }

    public ClientRuntimeOptions reconnectSecond(int reconnectSecond) {
        if (0 < reconnectSecond && reconnectSecond < 5) {
            reconnectSecond = 5;
        }

        this.reconnectSecond = reconnectSecond;

        return this;
    }

}
