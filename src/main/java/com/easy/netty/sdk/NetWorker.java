package com.easy.netty.sdk;

import com.easy.netty.frame.heart.DefaultHandlerIdleConnection;
import com.easy.netty.frame.heart.IHandlerIdleConnection;
import com.easy.netty.frame.protocol.ProtocolPool;
import com.easy.netty.frame.service.NettyServer;
import com.easy.netty.frame.protocol.IProtocol;
import com.easy.netty.frame.connection.HandlerConnectionlayer;
import com.easy.netty.frame.connection.NetConnection;
import com.easy.netty.frame.NettyAssembler;
import com.easy.netty.frame.client.NettyClient;
import io.netty.channel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

/**
 * @Author SunQian
 * @CreateTime 2020/3/13 15:37
 * @Description: a worker in the network, it may is server also client
 */
@Service
public class NetWorker {

    @Autowired
    private NettyAssembler assembler;

    @Autowired
    ProtocolPool protocolPool;

    @Autowired
    private NettyServer server;

    @Autowired
    private NettyClient client;

    @Autowired
    ApplicationObjectSupport applicationObjectSupport;

    protected ConcurrentHashMap<String, NetConnection> connectMap = new ConcurrentHashMap<>();
    private Semaphore stopSemaphore = new Semaphore(0,true);
    protected ConcurrentHashMap<String, NetConnectContext> contextMap = new ConcurrentHashMap<>();

    /**
     * author: SunQian
     * date: 2020/3/19 11:49
     * title: TODO
     * descritpion: assemble of protocol memory pool
     * return: TODO
     */
    public NettyAssembler assembler() {
        return assembler;
    }

    /**
     * author: SunQian
     * date: 2020/3/19 16:04
     * title: TODO
     * descritpion: used to operate client
     * return: TODO
     */
    public NettyClient client() {
        return client;
    }

    /**
     * author: SunQian
     * date: 2020/3/19 19:35
     * title: TODO
     * descritpion: used to operate server
     * return: TODO
     */
    public NettyServer server() {
        return server;
    }

    private void init() {
        if (null == assembler.handlerIdleConnection) {
            ApplicationContext context = applicationObjectSupport.getApplicationContext();
            IHandlerIdleConnection handlerIdleConnection = (DefaultHandlerIdleConnection) context.getBean("defaultHandlerIdleConnection");
            assembler.setHandlerIdleConnection(handlerIdleConnection);
        }

        ApplicationContext context = applicationObjectSupport.getApplicationContext();
        HandlerConnectionlayer handlerConnectionlayer = (HandlerConnectionlayer) context.getBean("handlerConnectionlayer");
        handlerConnectionlayer.init(this);
    }

    /**
     * author: SunQian
     * date: 2020/3/19 16:02
     * title: TODO
     * descritpion: run netty process
     * return: TODO
     */
    public NetWorker run() {
        init();
        client.run();
        server.run();

        NetWorker netWorker = this;
        Runtime.getRuntime().addShutdownHook(new Thread(){
            @Override
            public void run() {
                netWorker.stop();
            }
        });

        return this;
    }

    /**
     * author: SunQian
     * date: 2020/3/13 10:20
     * title: wait for all socket connection to close
     * descritpion: TODO
     * return: TODO
     */
    public void waitStop() {
        try {
            stopSemaphore.acquire();
            connectMap.entrySet().stream().forEach(entry -> {
                Channel channel = entry.getValue().getChannel();
                channel.closeFuture().syncUninterruptibly();
            });
        }
        catch (Exception e) {

        }
        finally {

        }
    }

    /**
     * author: SunQian
     * date: 2020/3/13 10:22
     * title: stop host
     * descritpion: close all connection, stop the user source, shutdown thread group
     * return: TODO
     */
    public void stop() {
        /**
         * close all connection
         */
        connectMap.entrySet().stream().forEach(entry -> {
            Channel channel = entry.getValue().getChannel();
            channel.close();
        });

        client.stop();
        server.stop();

        //relsase stop signal，activated the thread of call waitCancel function
        stopSemaphore.release();
    }

    /**
     * author: SunQian
     * date: 2020/3/19 19:06
     * title: TODO
     * descritpion: success connected to server, current view is client.
     *      trigger before business layer connect event
     * @param channel
     * return: TODO
     */
    public NetConnection onConnectionSuccessed(Channel channel) {
        //initialize connection and add to connect list
        IProtocol protocol = protocolPool.getProtocol(channel);
        NetConnection connection = assembler.memoryPool.newConnect();
        connection.setChannel(channel);
        connection.setProtocol(protocol);
        connectMap.put(connection.getChannel().id().asLongText(), connection);

        return connection;
    }

    /**
     * author: SunQian
     * date: 2020/3/19 19:05
     * title: TODO
     * descritpion: new client connected in server, current view is server.
     *      trigger before business layer connect event
     * @param channel
     * return: TODO
     */
    public NetConnection onNewClient(Channel channel) {
        //initialize connection and add to connect list
        IProtocol protocol = protocolPool.getProtocol(server.svrName());
        NetConnection connection = assembler.memoryPool.newConnect();
        connection.setChannel(channel);
        connection.setProtocol(protocol);
        connectMap.put(channel.id().asLongText(), connection);
        return connection;
    }

    /**
     * author: SunQian
     * date: 2020/3/19 19:07
     * title: TODO
     * descritpion: create connext and add list
     * @param ctx
     * return: TODO
     */
    public NetConnectContext createConnectContext(ChannelHandlerContext ctx) {
        //initialize context and add to context list
        NetConnection connection = connectMap.get(ctx.channel().id().asLongText());
        IProtocol protocol = connection.getProtocol();
        NetConnectContext connectContext = assembler.memoryPool.newConnectContext(ctx, protocol);
        contextMap.put(connectContext.longId(), connectContext);

        return connectContext;
    }

    /**
     * author: SunQian
     * date: 2020/3/19 19:15
     * title: TODO
     * descritpion: trigger before business layer disconnect event
     * @param channel
     * return: TODO
     */
    public NetConnectContext onDisconnected(Channel channel) {
        String id = channel.id().asLongText();
        NetConnection connection = connectMap.remove(id);
        connection.getChannel().close();
        NetConnectContext netConnectContext = contextMap.remove(id);
        if (netConnectContext.isClient()) {
            //server close
            client.onServerDisconnected(channel);
        }
        return netConnectContext;
    }

    /**
     * author: SunQian
     * date: 2020/3/13 15:08
     * title: TODO
     * descritpion: get the channel by id of channel
     * @param channelId
     * return: TODO
     */
    public NetConnection getConnect(String channelId) {
        return connectMap.get(channelId);
    }

    /**
     * author: SunQian
     * date: 2020/3/13 15:08
     * title: TODO
     * descritpion: get the channel by id of channel
     * @param channelId
     * return: TODO
     */
    public NetConnectContext getConnectContext(String channelId) {
        return contextMap.get(channelId);
    }
}
