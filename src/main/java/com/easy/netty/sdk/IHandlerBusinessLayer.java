package com.easy.netty.sdk;


/**
 * @Author SunQian
 * @CreateTime 2020/3/17 16:10
 * @Description: user implements the handler for business event
 */
public interface IHandlerBusinessLayer {
    void onConnected(NetConnectContext channel) throws Exception;
    void onMessage(NetConnectContext connectContext, String messageType, Object data) throws Exception;
    void onDisconnected(NetConnectContext channel) throws Exception;
    void onException(NetConnectContext channel, Throwable cause) throws Exception;

}
