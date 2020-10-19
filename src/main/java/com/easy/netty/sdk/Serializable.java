package com.easy.netty.sdk;

/**
 * @Title 序列化接口
 * @Athor SunQian
 * @CreateTime 2020/10/13 15:24
 * @Description: todo
 */
public interface Serializable {
    void parse(byte[] stream);
    byte[] stream();
}
