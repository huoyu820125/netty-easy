package com.easy.netty.frame.heart;

import com.easy.netty.frame.protocol.ProtocolPool;
import com.easy.netty.frame.protocol.IProtocol;
import io.netty.channel.ChannelHandlerContext;

/**
 * @Author SunQian
 * @CreateTime 2020/3/12 10:35
 * @Description: TODO
 */
public class DefaultHandlerIdleConnection implements IHandlerIdleConnection {
    private ProtocolPool protocolPool;

    public DefaultHandlerIdleConnection(ProtocolPool protocolPool) {
        this.protocolPool = protocolPool;
    }

    @Override
    public void onReadOuttime(ChannelHandlerContext ctx) {
        //close connection for has not heartbeat!
        ctx.channel().close();
    }

    @Override
    public void onWriteOuttime(ChannelHandlerContext ctx) {
        //send heartbeat!
        IProtocol protocol = protocolPool.getProtocol(ctx.channel());
        ctx.writeAndFlush(protocol.heartBeatMessage());
    }
}
