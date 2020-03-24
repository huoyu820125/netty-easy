package com.easy.netty.frame.connection;

import com.easy.netty.frame.protocol.AbstractMessage;
import com.easy.netty.sdk.IHandlerBusinessLayer;
import com.easy.netty.sdk.NetConnectContext;
import com.easy.netty.sdk.NetWorker;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @Author SunQian
 * @CreateTime 2020/3/11 16:00
 * @Description: handler of connection layer
 */
@Component
@ChannelHandler.Sharable
public class HandlerConnectionlayer extends SimpleChannelInboundHandler<ByteBuf> {
    private NetWorker netWorker;

    @Autowired
    IHandlerBusinessLayer handlerBusinessLayer;

    /**
     * author: SunQian
     * date: 2020/3/17 17:36
     * title: TODO
     * descritpion: TODO
     * @param netWorker
     * return: TODO
     */
    public void init(NetWorker netWorker) {
        this.netWorker = netWorker;
    }

    /**
     * author: SunQian
     * date: 2020/3/19 18:56
     * title: TODO
     * descritpion: handle connection finished event
     * @param ctx
     * return: TODO
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        NetConnection connection = netWorker.getConnect(ctx.channel().id().asLongText());
        if (null == connection) {
            //connection object uninitialized, so current view is server, client connected in
            netWorker.onNewClient(ctx.channel());
        }

        NetConnectContext connectContext = netWorker.createConnectContext(ctx);
        connectContext.setIdentity(null == connection);
        handlerBusinessLayer.onConnected(connectContext);
    }

    /**
     * author: SunQian
     * date: 2020/3/13 17:48
     * title: connection closed event
     * descritpion: TODO
     * @param ctx
     * return: TODO
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        NetConnectContext connectContext = netWorker.onDisconnected(ctx.channel());
        handlerBusinessLayer.onDisconnected(connectContext);
    }

    /**
     * handle message arrived event
     * @param ctx
     * @param byteBuf
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf byteBuf) throws Exception {
        NetConnection connection = netWorker.getConnect(ctx.channel().id().asLongText());
        ByteBuf allByteBuf = connection.getMsgBuffer();
        AbstractMessage message = null;
        if (null == allByteBuf || allByteBuf.readableBytes() <= 0) {
            if (null != allByteBuf) {
                connection.addByteBufIdleCount();
                if (connection.getByteBufIdleCount() > 100) {
                    connection.getMsgBuffer().clear();
                    connection.setMsgBuffer(null);
                    connection.setByteBufIdleCount(0);
                }
            }

            int oldIndex = byteBuf.readerIndex();
            message = connection.getProtocol().analyseMessage(byteBuf);
            if (null == message) {
                //When the next IO event reads data from the last position of the receive buffer
                byteBuf.readerIndex(oldIndex);
            }

            if (byteBuf.readableBytes() > 0) {
                allByteBuf = Unpooled.buffer();
                allByteBuf.writeBytes(byteBuf);
                connection.setMsgBuffer(allByteBuf);
            }
        }
        else {
            allByteBuf.writeBytes(byteBuf);
            int oldIndex = allByteBuf.readerIndex();
            message = connection.getProtocol().analyseMessage(allByteBuf);
            if (null == message) {
                //When the next IO event reads data from the last position of the receive buffer
                allByteBuf.readerIndex(oldIndex);
            }

            if (allByteBuf.readerIndex() > 10240) {
                //realloc memory when the data of read attain 10k
                allByteBuf = Unpooled.buffer().writeBytes(allByteBuf);
                connection.setMsgBuffer(allByteBuf);
            }
        }
        if (null == message) {
            return;
        }

        if (connection.getProtocol().isHeartBeatMessage(message)) {
            //Ignore heartbeat
            return;
        }

        NetConnectContext connectContext = netWorker.getConnectContext(ctx.channel().id().asLongText());
        handlerBusinessLayer.onMessage(connectContext, message.getMessageType(), message.getData());
    }

    /**
     * take place exception during proccessing
     * @param ctx
     * @param cause
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        NetConnectContext connectContext = netWorker.getConnectContext(ctx.channel().id().asLongText());
        handlerBusinessLayer.onException(connectContext, cause);
    }
}
