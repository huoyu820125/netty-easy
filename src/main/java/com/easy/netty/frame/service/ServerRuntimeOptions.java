package com.easy.netty.frame.service;


/**
 * @Author SunQian
 * @CreateTime 2020/3/19 17:18
 * @Description: runtime options of server
 */
public class ServerRuntimeOptions {
    public int heartbeatSecond = 30;
    private NettyServer nettyServer;

    public ServerRuntimeOptions(NettyServer nettyServer) {
        this.nettyServer = nettyServer;
    }

    /**
     * author: SunQian
     * date: 2020/3/19 16:14
     * title: TODO
     * descritpion: To start a chained call from an netty server object
     * return: TODO
     */
    public NettyServer server() {
        return nettyServer;
    }

    public ServerRuntimeOptions heartbeatSecond(int heartbeatSecond) {
        if (0 >= heartbeatSecond) {
            //not check heartbeat
            heartbeatSecond = 0;
        }

        this.heartbeatSecond = heartbeatSecond;

        return this;
    }

}
