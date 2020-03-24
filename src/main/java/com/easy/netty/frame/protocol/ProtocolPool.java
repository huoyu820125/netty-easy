package com.easy.netty.frame.protocol;

import io.netty.channel.Channel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author SunQian
 * @CreateTime 2020/3/19 10:49
 * @Description: TODO
 */
@Component
public class ProtocolPool {
    private Map<String, IProtocol> serverProtocolMap = new HashMap<>();
    private Map<String, String> serverNameMap = new HashMap<>();
    private IProtocol defaultProtocol = null;

    @Autowired
    ApplicationObjectSupport applicationObjectSupport;

    /**
     * author: SunQian
     * date: 2020/3/19 14:46
     * title: TODO
     * descritpion: get protocol of the client channel
     * @param clientChannel
     * return: TODO
     */
    public IProtocol getProtocol(Channel clientChannel) {
        InetSocketAddress serverSocket = (InetSocketAddress) clientChannel.remoteAddress();
        StringBuffer fullAddress = new StringBuffer();
        fullAddress.append(serverSocket.getAddress().getHostAddress())
            .append(":").append(serverSocket.getPort());
        String svrName = serverNameMap.get(fullAddress.toString());
        if (null == svrName) {
            throw new RuntimeException("current process is a server");
        }

        IProtocol protocol = serverProtocolMap.get(svrName);
        if (null == protocol) {
            protocol = defaultProtocol();
        }

        return protocol;
    }

    /**
     * author: SunQian
     * date: 2020/3/19 18:35
     * title: TODO
     * descritpion: TODO
     * @param svrName
     * return: TODO
     */
    public IProtocol getProtocol(String svrName) {
        IProtocol protocol = serverProtocolMap.get(svrName);
        if (null == protocol) {
            protocol = defaultProtocol();
        }

        return protocol;
    }

    /**
     * author: SunQian
     * date: 2020/3/19 12:01
     * title: TODO
     * descritpion: default protocol
     * return: TODO
     */
    public IProtocol defaultProtocol() {
        if (null == defaultProtocol) {
            ApplicationContext context = applicationObjectSupport.getApplicationContext();
            defaultProtocol = (DefaultProtocol)context.getBean("defaultProtocol");
        }

        return defaultProtocol;
    }

    /**
     * author: SunQian
     * date: 2020/3/19 11:28
     * title: TODO
     * descritpion: Associate a service name with the address of the server in the network
     * @param svrSocket
     * @param serverName
     * return: TODO
     */
    public void associateServerName(InetSocketAddress svrSocket, String serverName) {
        StringBuffer fullAddress = new StringBuffer();
        fullAddress.append(svrSocket.getAddress().getHostAddress())
            .append(":").append(svrSocket.getPort());
        if (serverNameMap.containsKey(fullAddress.toString())) {
            return;
        }

        serverNameMap.put(fullAddress.toString(), serverName);
    }

    /**
     * author: SunQian
     * date: 2020/3/19 11:24
     * title: TODO
     * descritpion: Set the protocol used by the external server
     * @param svrName
     * @param protocol
     * return: TODO
     */
    public void setServerProtocol(String svrName, IProtocol protocol) {
        if (null == protocol) {
            return;
        }

        serverProtocolMap.put(svrName, protocol);
        return;
    }

}
