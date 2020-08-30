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
import java.util.concurrent.ConcurrentHashMap;
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
    private ConcurrentHashMap<String, ServerState> serverStateMap = new ConcurrentHashMap();

    private class ServerState {
        public final static int DISCONNECTION = 0;
        public final static int RUNNING = 1;
        public String svrName = "";
        public InetSocketAddress address;
        public int state = ServerState.DISCONNECTION;

        public String addressKey() {
            return NettyClient.addressKey(address);
        }
    }

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
        ServerState serverState = new ServerState();
        serverState.address = new InetSocketAddress(host, port);
        serverState.svrName = serverName;
        if (serverStateMap.contains(serverState.addressKey())) {
            return this;
        }
        serverStateMap.put(serverState.addressKey(), serverState);
        protocolPool.associateServerName(serverState.address, serverName);
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

        initNetty();

        /**
         * connect to all server
         */
        startConnectTread();

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


    public class ClientConnectTread implements Runnable {
        private void sleep(long millSecond) {
            try {
                Thread.sleep(millSecond);
            }
            catch (InterruptedException e) {

            }
        }

        public void run() {
            while (isRunning) {
                //不可用java8方式遍历(即：stream().forEach(entry -> {}))，因为其它线程的插入新元素的操作，会导致遍历直接终止。
                for (ConcurrentHashMap.Entry<String, ServerState> entry : serverStateMap.entrySet()) {
                    ServerState serverState = entry.getValue();
                    if (ServerState.DISCONNECTION != serverState.state) {
                        continue;
                    }

                    /**
                     * Running indicates that the operation has started,
                     * the connection may be in progress or the connection may be completed
                     */
                    serverState.state = ServerState.RUNNING;
                    connect(serverState.address, runtimeOptions.reconnectSecond);
                }
                sleep(runtimeOptions.reconnectSecond*1000);
            }
        }
    }

    public void startConnectTread() {
        Thread thread = new Thread(new ClientConnectTread());
        thread.start();
    }

    public void onServerDisconnected(Channel channel) {
        ServerState serverState = serverStateMap.get(addressKey(((InetSocketAddress)channel.remoteAddress())));
        //active reconnect task
        serverState.state = ServerState.DISCONNECTION;
    }

    public static String addressKey(InetSocketAddress address) {
        StringBuffer fullAddress = new StringBuffer();
        fullAddress.append(address.getAddress().getHostAddress())
                .append(":").append(address.getPort());

        return fullAddress.toString();
    }
}
