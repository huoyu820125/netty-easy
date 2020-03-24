package com.easy.netty.frame.id;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @Author SunQian
 * @CreateTime 2020/3/23 13:28
 * @Description: TODO
 */
@Component
public class DefaultIdGenerator implements IIdGenerator {

    @Value("${nodeId:0}")
    Long nodeId;

    private AtomicLong id = new AtomicLong(0L);


    @Override
    public long sessionId() {
        nodeId = nodeId << 56;
        nodeId = nodeId & 0xff00000000000000L;
        nodeId += id.getAndAdd(1);
        return nodeId;
    }
}
