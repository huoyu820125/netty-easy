package com.easy.netty.frame.protocol;


/**
 * @Author SunQian
 * @CreateTime 2020/3/23 15:13
 * @Description: TODO
 */
public class DefaultMessage extends AbstractMessage {
    private String messageType;
    private Object data;

    @Override
    public String getMessageType() {
        return messageType;
    }

    @Override
    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
