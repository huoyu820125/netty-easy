package com.easy.netty.frame.id;


import java.util.concurrent.atomic.AtomicLong;

/**
 * @Author SunQian
 * @CreateTime 2020/3/23 13:28
 * @Description: TODO
 */
public class DefaultIdGenerator implements IIdGenerator {

    Long nodeId = 0L;

    private AtomicLong id = new AtomicLong(0L);


    @Override
    public long sessionId() {
        nodeId = nodeId << 56;
        nodeId = nodeId & 0xff00000000000000L;
        nodeId += id.getAndAdd(1);
        return nodeId;
    }
}
