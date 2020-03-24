package com.easy.netty.frame.memory;

import com.easy.netty.frame.connection.NetConnection;
import com.easy.netty.frame.protocol.IProtocol;
import com.easy.netty.sdk.NetConnectContext;
import io.netty.channel.ChannelHandlerContext;

/**
 * @Author SunQian
 * @CreateTime 2020/3/18 15:19
 * @Description: TODO
 */
public interface IMemoryPool {
    /**
     * author: SunQian
     * date: 2020/3/18 15:20
     * title: TODO
     * descritpion: create a NetChannel instance
     * @param ctx
     * return: TODO
     */
    NetConnectContext newConnectContext(ChannelHandlerContext ctx, IProtocol protocol);

    NetConnection newConnect();
}
