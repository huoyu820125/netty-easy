package com.easy.netty.frame.protocol;

import io.netty.buffer.ByteBuf;

/**
 * @Author SunQian
 * @CreateTime 2020/3/17 17:06
 * @Description: TODO
 */
public interface IProtocol {
    /**
     * author: SunQian
     * date: 2020/3/18 15:06
     * title: TODO
     * descritpion: analyse the net stream produce a message
     * @param netStream
     * return: if The data int the buffer is not enough, must be return null
     */
    AbstractMessage analyseMessage(ByteBuf netStream) throws ExceptionMessageFormat;

    /**
     * author: SunQian
     * date: 2020/3/18 15:08
     * title: TODO
     * descritpion: package the message into net stream
     * @param object
     * return: TODO
     */
    ByteBuf packageMessage(AbstractMessage object);

    /**
     * author: SunQian
     * date: 2020/3/18 15:06
     * title: TODO
     * descritpion: create a message for heartbeat
     * return: TODO
     */
    ByteBuf heartBeatMessage();

    /**
     * author: SunQian
     * date: 2020/3/23 14:43
     * title: TODO
     * descritpion: TODO
     * @param object result of call analyseMessage()
     * return: TODO
     */
    Boolean isHeartBeatMessage(AbstractMessage object);
}
