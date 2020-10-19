package com.easy.netty.frame.assemble;

import com.easy.netty.frame.heart.IHandlerIdleConnection;
import com.easy.netty.frame.memory.DefaultMemoryPool;
import com.easy.netty.frame.memory.IMemoryPool;
import com.easy.netty.frame.protocol.IProtocol;
import com.easy.netty.frame.protocol.ProtocolPool;
import com.easy.netty.sdk.NetWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.stereotype.Component;

/**
 * @Author SunQian
 * @CreateTime 2020/3/19 11:40
 * @Description: used to assemble a netty process
 */
@Component
public class NettyAssembler {
    public IMemoryPool memoryPool;
    public IHandlerIdleConnection handlerIdleConnection;

    @Autowired
    private ProtocolPool protocolPool;

    @Autowired
    ApplicationObjectSupport applicationObjectSupport;

    @Autowired
    NetWorker netWorker;

    /**
     * author: SunQian
     * date: 2020/3/19 15:17
     * title: TODO
     * descritpion: To start a chained call from an net worker object
     * return: TODO
     */
    public NetWorker netWorker() {
        if (null == memoryPool) {
            ApplicationContext context = applicationObjectSupport.getApplicationContext();
            if(context==null){
                throw new RuntimeException("Application failed to get spring application context");
            }
            memoryPool = context.getBean(DefaultMemoryPool.class);
        }

        return netWorker;
    }

    /**
     * author: SunQian
     * date: 2020/3/19 16:51
     * title: TODO
     * descritpion: assemble a memory pool
     * @param memoryPool
     * return: TODO
     */
    public NettyAssembler setMemoryPool(IMemoryPool memoryPool) {
        this.memoryPool = memoryPool;
        return this;
    }

    /**
     * author: SunQian
     * date: 2020/3/19 14:29
     * title: TODO
     * descritpion: bind a protocol to server
     * @param svrName
     * @param protocol
     * return: TODO
     */
    public NettyAssembler setServerProtocol(String svrName, IProtocol protocol) {
        protocolPool.setServerProtocol(svrName, protocol);

        return this;
    }

    /**
     * author: SunQian
     * date: 2020/3/20 18:21
     * title: TODO
     * descritpion: set handler for idle connect
     * @param handlerIdleConnection
     * return: TODO
     */
    public NettyAssembler setHandlerIdleConnection(IHandlerIdleConnection handlerIdleConnection) {
        this.handlerIdleConnection = handlerIdleConnection;

        return this;
    }
}
