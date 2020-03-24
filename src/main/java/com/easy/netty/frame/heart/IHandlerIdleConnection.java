package com.easy.netty.frame.heart;

import io.netty.channel.ChannelHandlerContext;

/**
 * @Author SunQian
 * @CreateTime 2020/3/20 17:59
 * @Description: TODO
 */
public interface IHandlerIdleConnection {
    void onReadOuttime(ChannelHandlerContext ctx);
    void onWriteOuttime(ChannelHandlerContext ctx);
}
