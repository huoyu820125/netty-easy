package com.easy.netty.frame.client;

import com.easy.netty.frame.heart.HandlerOutTimeMonitor;
import com.easy.netty.frame.protocol.ProtocolPool;
import com.easy.netty.frame.connection.HandlerConnectionlayer;
import com.easy.netty.sdk.NetWorker;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @Author SunQian
 * @CreateTime 2020/3/11 15:58
 * @Description: TODO
 */
@Slf4j
@Component
public class NettyClient {
    private boolean isInitialized = false;
    private boolean isRunning = false;
    private ClientRuntimeOptions runtimeOptions = new ClientRuntimeOptions(this);
    private Bootstrap bootstrap;
    private EventLoopGroup group = new NioEventLoopGroup();
    private List<InetSocketAddress> serverAddressList = new ArrayList<>();

    @Autowired
    NetWorker netWorker;

    @Autowired
    private ProtocolPool protocolPool;

    @Autowired
    HandlerConnectionlayer handlerConnectionlayer;

    public synchronized NettyClient initialize() {
        if (isInitialized) {
            throw new RuntimeException("repeat initialization client");
        }
        isInitialized = true;

        return this;
    }

    /**
     * author: SunQian
     * date: 2020/3/19 15:49
     * title: TODO
     * descritpion: Used to set runtime options
     * return: TODO
     */
    public ClientRuntimeOptions runtimeOptions() {
        return runtimeOptions;
    }

    /**
     * author: SunQian
     * date: 2020/3/12 18:34
     * title:  add server address
     * descritpion: TODO
     * @param serverName
     * @param host
     * @param port
     * return: this
     */
    public NettyClient addServer(String serverName, String host, Integer port) {
        InetSocketAddress socketAddress = new InetSocketAddress(host, port);
        serverAddressList.add(new InetSocketAddress(host, port));
        protocolPool.associateServerName(socketAddress, serverName);
        return this;
    }

    /**
     * author: SunQian
     * date: 2020/3/19 16:50
     * title: TODO
     * descritpion: To start a chained call from an net worker object
     * return: TODO
     */
    public NetWorker netWorker() {
        return netWorker;
    }

    /**
     * author: SunQian
     * date: 2020/3/13 10:26
     * title: start client
     * descritpion: connect to all server
     * return: TODO
     */
    public synchronized NettyClient run() {
        if (!isInitialized) {
            return this;
        }
        isRunning = true;

        if (serverAddressList.isEmpty()) {
            throw new RuntimeException("lack server connect info! please call the function addServer() before startClient");
        }

        initNetty();

        /**
         * connect to all server
         */
        for (InetSocketAddress socketAddress : serverAddressList) {
            connect(socketAddress, runtimeOptions.reconnectSecond);
        }

        return this;
    }

    private void initNetty() {
        try {
            HandlerOutTimeMonitor handlerWriteMoniter = new HandlerOutTimeMonitor(netWorker.assembler().handlerIdleConnection, TimeUnit.SECONDS, 0, runtimeOptions.heartbeatSecond);
            bootstrap = new Bootstrap()
                .group(group)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline().addLast(handlerWriteMoniter);
                        socketChannel.pipeline().addLast(handlerConnectionlayer);
                    }
                });
        }
        catch (Exception e) {
            throw new RuntimeException("client initialization is not successed");
        }
        finally {
        }
    }

    /**
     * author: SunQian
     * date: 2020/3/13 10:18
     * title: connect to the server
     * descritpion: TODO
     * @param serverAddress
     * return: TODO
     */
    private void connect(InetSocketAddress serverAddress, Integer reconnectSecond) {
        try {
            bootstrap.remoteAddress(serverAddress);
            ChannelFuture f = bootstrap.connect().addListener((ChannelFuture connectedfuture) -> {
                if (connectedfuture.isSuccess()) {
                    netWorker.onConnectionSuccessed(connectedfuture.channel());
                    return;
                }

                /**
                 * try to reconnect if the connection is failed
                 */
                log.error("connect to the server of {}:{} is failed",
                    serverAddress.getAddress().getHostAddress(), serverAddress.getPort());
                if (null == reconnectSecond || reconnectSecond < 0) {
                    log.info("not reconnect!");
                    return;
                }

                //in this case wait a minute reconnect to the server
                log.info("reconnect after {} second!", reconnectSecond);
                final EventLoop eventLoop = connectedfuture.channel().eventLoop();
                eventLoop.schedule(() -> connect(serverAddress, reconnectSecond), reconnectSecond, TimeUnit.SECONDS);
            });
        } catch (Exception e) {
            log.error("client connect exception:", e);
        }
    }

    /**
     * author: SunQian
     * date: 2020/3/13 10:32
     * title: connect to server
     * descritpion:
     *          connect to server and check that the address exists in the service list,
     *      if not exist then add to service list
     * @param svrName
     * @param hostname
     * @param port
     * @param reconnectSecond
     * return: TODO
     */
    public void connect(String svrName, String hostname, Integer port, Integer reconnectSecond) {
        addServer(svrName, hostname, port);
        InetSocketAddress serverAddress = new InetSocketAddress(hostname, port);
        connect(serverAddress, reconnectSecond);
    }

    public synchronized void stop() {
        if (!isInitialized) {
            return;
        }
        if (!isRunning) {
            return;
        }
        isRunning = false;

        //shutdown thread group
        group.shutdownGracefully();
    }
}
