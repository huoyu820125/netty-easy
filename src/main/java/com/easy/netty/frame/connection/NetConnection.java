package com.easy.netty.frame.connection;

import com.easy.netty.frame.protocol.IProtocol;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;


/**
 * @Author SunQian
 * @CreateTime 2020/3/13 14:20
 * @Description: TODO
 */
public class NetConnection {
    private Channel channel;
    private ByteBuf msgBuffer;
    private int byteBufIdleCount;
    private IProtocol protocol;

    public ByteBuf getMsgBuffer() {
        return msgBuffer;
    }

    public void setMsgBuffer(ByteBuf msgBuffer) {
        this.msgBuffer = msgBuffer;
    }

    public int getByteBufIdleCount() {
        return byteBufIdleCount;
    }

    public void setByteBufIdleCount(int byteBufIdleCount) {
        this.byteBufIdleCount = byteBufIdleCount;
    }

    public void addByteBufIdleCount() {
        this.byteBufIdleCount++;
    }

    public Channel getChannel() {
        return channel;
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public IProtocol getProtocol() {
        return protocol;
    }

    public void setProtocol(IProtocol protocol) {
        this.protocol = protocol;
    }
}
