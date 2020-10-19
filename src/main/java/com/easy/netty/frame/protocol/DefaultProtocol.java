package com.easy.netty.frame.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * @Author SunQian
 * @CreateTime 2020/3/17 17:33
 * @Description: transfer object in protocol
 *  Message stream consists of message size is integer type,
 *  message type is string, message content is byte array
 */
@Component
public class DefaultProtocol implements IProtocol {
    @Override
    public AbstractMessage analyseMessage(ByteBuf netStream) throws ExceptionMessageFormat {
        /**
         * Analyse protocol
         *  msg type
         *  size of serialized object
         *  byte stream of serialized object
         */
        if (0 == netStream.writerIndex()) {
            throw new RuntimeException("protocl stream is empty");
        }

        int messageSize = netStream.readInt();
        if (messageSize > netStream.readableBytes()) {
            //data not all arrived, wait data arrived
            return null;
        }

        String messageType = null;
        Object data = null;
        try{
            byte[] objectStream = ProtocolUtils.readObject(Short.class, netStream);
            messageType = (String)ProtocolUtils.deserialize(objectStream);
            objectStream = ProtocolUtils.readObject(Integer.class, netStream);
            data = ProtocolUtils.deserialize(objectStream);
        }
        catch (Exception e) {
            throw new ExceptionMessageFormat(e.getMessage());
        }

        /**
         * build message
         */
        DefaultMessage message = new DefaultMessage();
        message.setMessageType(messageType);
        message.setData(data);

        return message;
    }

    @Override
    public ByteBuf packageMessage(AbstractMessage message) {
        byte[] messageTypeStream = ProtocolUtils.serialize(message.getMessageType());
        byte[] objectStream = ProtocolUtils.serialize(message.getData());

        /**
         * package into protocol
         */
        if (objectStream.length <= 0) {
            throw new RuntimeException("serialized stream's size is zero");
        }
        int messageSize = 2 + messageTypeStream.length
                        + 4 + objectStream.length;
        ByteBuf byteBuf = Unpooled.buffer(messageSize);
        byteBuf.writeInt(messageSize);
        ProtocolUtils.writeObject(Short.class, byteBuf, messageTypeStream);
        ProtocolUtils.writeObject(Integer.class, byteBuf, objectStream);

        return byteBuf;
    }

    @Override
    public ByteBuf heartBeatMessage() {
        DefaultMessage message = new DefaultMessage();
        message.setMessageType("");
        message.setData(0);
        ByteBuf stream = packageMessage(message);

        return stream;
    }

    @Override
    public Boolean isHeartBeatMessage(AbstractMessage message) {
        if (StringUtils.isEmpty(message.getMessageType())) {
            return true;
        }

        return false;
    }
}
