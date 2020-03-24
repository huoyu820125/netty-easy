package com.easy.netty.frame.service;

import com.easy.netty.frame.heart.HandlerOutTimeMonitor;
import com.easy.netty.frame.connection.HandlerConnectionlayer;
import com.easy.netty.sdk.NetWorker;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @Author SunQian
 * @CreateTime 2020/3/11 15:08
 * @Description: TODO
 */
@Slf4j
@Component
public class NettyServer {
    private boolean isInitialized = false;
    private boolean isRunning = false;
    private ServerBootstrap bootstrap;
    private EventLoopGroup bossGroup = new NioEventLoopGroup();
    private EventLoopGroup workerGroup = new NioEventLoopGroup();
    private Channel acceptChannel;

    private ServerRuntimeOptions runtimeOptions = new ServerRuntimeOptions(this);
    private String svrName;
    private int port = 0;

    @Autowired
    private HandlerConnectionlayer handlerConnectionlayer;

    @Autowired
    private NetWorker netWorker;

    public synchronized NettyServer initialize(String svrName, int port) {
        if (isInitialized) {
            throw new RuntimeException("repeat initialization server");
        }

        isInitialized = true;
        this.svrName = svrName;

        if (port < 1 || port > 65535) {
            throw new RuntimeException("port should is a number from 1 to 65535");
        }

        this.port = port;
        return this;
    }

    public String svrName() {
        return this.svrName;
    }

    /**
     * author: SunQian
     * date: 2020/3/19 17:22
     * title: TODO
     * descritpion: Used to set runtime options
     * return: TODO
     */
    public ServerRuntimeOptions runtimeOptions() {
        return runtimeOptions;
    }

    /**
     * author: SunQian
     * date: 2020/3/20 9:54
     * title: TODO
     * descritpion: To start a chained call from an net worker object
     * return: TODO
     */
    public NetWorker netWorker() {
        return netWorker;
    }

    /**
     * 启动服务
     * @return
     * @throws Exception
     */
    public synchronized NettyServer run() {
        if (!isInitialized) {
            return this;
        }
        isRunning = true;

        if (StringUtils.isEmpty(svrName)) {
            throw new RuntimeException("not set server's name, call the svrname() before run()");
        }

        if (0 == port) {
            throw new RuntimeException("not set listen port, call the listenPort() before run()");
        }

        initNetty();
        bind(port);
        log.info("Netty server is running and listening on port {} and ready for connections...", port);

        return this;
    }

    private void initNetty() {
        try {
            HandlerOutTimeMonitor handlerOutTimeMonitor = new HandlerOutTimeMonitor(netWorker.assembler().handlerIdleConnection, TimeUnit.SECONDS, runtimeOptions.heartbeatSecond, 0);
            bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline().addLast(handlerOutTimeMonitor);
                        socketChannel.pipeline().addLast(handlerConnectionlayer);
                    }
                });

        }
        catch (Exception e) {
            throw new RuntimeException("netty server initialization is not successed");
        }
        finally {
        }
    }

    private void bind(int port) {
        try {
            ChannelFuture channelFuture = bootstrap.bind(port).sync();
            acceptChannel = channelFuture.channel();
            if (null == channelFuture || !channelFuture.isSuccess()) {
                throw new RuntimeException("server start up error");
            }
        }
        catch (Exception e) {
            throw new RuntimeException("server initialization is not successed");
        }
        finally {
        }
    }

    public synchronized void stop() {
        if (!isInitialized) {
            return;
        }
        if (!isRunning) {
            return;
        }
        isRunning = false;

        if(acceptChannel != null) {
            acceptChannel.close();
        }
        workerGroup.shutdownGracefully();
        bossGroup.shutdownGracefully();
    }

}
