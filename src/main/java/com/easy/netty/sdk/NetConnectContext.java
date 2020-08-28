package com.easy.netty.sdk;

import com.easy.netty.frame.id.IIdGenerator;
import com.easy.netty.frame.protocol.AbstractMessage;
import com.easy.netty.frame.protocol.DefaultMessage;
import com.easy.netty.frame.protocol.IProtocol;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.net.SocketAddress;

/**
 * @Author SunQian
 * @CreateTime 2020/3/18 15:11
 * @Description: context of a connection
 */
public class NetConnectContext {
    private ChannelHandlerContext channelHandlerContext;
    private boolean isClient = true;
    private IProtocol protocol;
    private int dataType;
    private Object data;
    private IIdGenerator idGenerator;

    public NetConnectContext(ChannelHandlerContext ctx, IProtocol protocol) {
        this.channelHandlerContext = ctx;
        this.protocol = protocol;
    }

    public boolean isClient() {
        return isClient;
    }

    public void setIdentity(boolean isClient) {
        this.isClient = isClient;
    }

    public void bindData(int dataType, Object businessData) {
        this.dataType = dataType;
        this.data = businessData;
    }

    public int dataType() {
        return this.dataType;
    }

    public Object data() {
        return this.data;
    }

    /**
     * author: SunQian
     * date: 2020/3/23 15:23
     * title: TODO
     * descritpion: send message use default protocol
     * @param messageType
     * @param data
     * return: TODO
     */
    public void sendMessage(String messageType, Object data) {
        DefaultMessage message = new DefaultMessage();
        message.setMessageType(messageType);
        message.setData(data);
        sendMessage(message);
    }

    /**
     * author: SunQian
     * date: 2020/3/23 15:24
     * title: TODO
     * descritpion: send message use user defined protocol
     * @param message the class of derived from AbstractMessage
     * return: TODO
     */
    public void sendMessage(AbstractMessage message) {
        ByteBuf stream = protocol.packageMessage(message);
        channelHandlerContext.writeAndFlush(stream);
    }

    public String longId() {
        return channelHandlerContext.channel().id().asLongText();
    }

    public String shortId() {
        return channelHandlerContext.channel().id().asShortText();
    }

    public SocketAddress remoteAddress() {
        return channelHandlerContext.channel().remoteAddress();
    }

    public void close() {
        channelHandlerContext.close();
    }

    public ChannelHandlerContext channelHandlerContext() {
        return channelHandlerContext;
    }

}
