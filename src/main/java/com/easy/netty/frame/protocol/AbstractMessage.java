package com.easy.netty.frame.protocol;

/**
 * @Author SunQian
 * @CreateTime 2020/3/18 14:36
 * @Description:
 */
public abstract class AbstractMessage {
    public abstract String getMessageType();

    public abstract void setMessageType(String messageType);

    public abstract Object getData();

    public abstract void setData(Object data);
}
