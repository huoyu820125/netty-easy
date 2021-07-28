package com.easy.netty.frame.memory;

import com.easy.netty.frame.connection.NetConnection;
import com.easy.netty.frame.protocol.IProtocol;
import com.easy.netty.sdk.NetConnectContext;
import io.netty.channel.ChannelHandlerContext;

/**
 * @Author SunQian
 * @CreateTime 2020/3/18 15:18
 * @Description: TODO
 */
public class DefaultMemoryPool implements IMemoryPool {

    @Override
    public NetConnectContext newConnectContext(ChannelHandlerContext ctx, IProtocol protocol) {
        return new NetConnectContext(ctx, protocol);
    }

    @Override
    public NetConnection newConnect() {
        return new NetConnection();
    }
}
