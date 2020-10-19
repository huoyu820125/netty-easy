package com.easy.netty.frame.protocol;

import io.netty.buffer.ByteBuf;

import java.io.*;

/**
 * @Author SunQian
 * @CreateTime 2020/3/18 14:18
 * @Description: TODO
 */
public class ProtocolUtils {
    /**
     * author: SunQian
     * date: 2020/3/18 14:28
     * title: TODO
     * descritpion: serialize object into byte stream
     * @param object
     * return: TODO
     */
    public static byte[] serialize(Object object) {
        ByteArrayOutputStream bos = null;
        ObjectOutputStream oos = null;
        try {
            bos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(bos);
            oos.writeObject(object);
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (null != oos) {
                try {
                    oos.close();
                }
                catch (IOException ex) {}
            }
            if (null != bos) {
                try {
                    bos.close();
                }
                catch (IOException ex) {}
            }
        }
    }

    /**
     * author: SunQian
     * date: 2020/3/18 14:27
     * title: TODO
     * descritpion: serialization of negative direction
     * @param objectStream
     * return: TODO
     */
    public static Object deserialize(byte[] objectStream) {
        ByteArrayInputStream bais = null;
        try {
            bais = new ByteArrayInputStream(objectStream);
            ObjectInputStream ois = new ObjectInputStream(bais);
            return ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            if (bais != null) {
                try {
                    bais.close();
                } catch (IOException ex) {
                }
            }
        }
    }

    /**
     * author: SunQian
     * date: 2020/3/24 9:56
     * title: TODO
     * descritpion:
     *      Read an object from the input stream.
     *      It consists of object size and object data.
     *      The type of object size may be short or integer.
     * @param objectSizeType
     * @param netStream
     * return: TODO
     */
    public static byte[] readObject(Class<?> objectSizeType, ByteBuf netStream) {
        int objectSize;
        if (Integer.class.equals(objectSizeType)) {
            objectSize = netStream.readInt();
        }
        else if (Short.class.equals(objectSizeType)) {
            objectSize = netStream.readShort();
        }
        else {
            throw new RuntimeException("object size only supports the type of Integer or Short");
        }

        byte[] objectStream = new byte[objectSize];
        netStream.readBytes(objectStream);
        return objectStream;
    }

    /**
     * author: SunQian
     * date: 2020/3/24 10:00
     * title: TODO
     * descritpion:
     *      Write an object into the output stream.
     *      It consists of object size and object data.
     *      The type of object size may be short or integer.
     * @param objectSizeType
     * @param netStream
     * @param objectStream
     * return: TODO
     */
    public static void writeObject(Class<?> objectSizeType, ByteBuf netStream, byte[] objectStream) {
        if (Integer.class.equals(objectSizeType)) {
            netStream.writeInt(objectStream.length);
        }
        else if (Short.class.equals(objectSizeType)) {
            netStream.writeShort(objectStream.length);
        }
        else {
            throw new RuntimeException("object size only supports the type of Integer or Short");
        }

        if (objectStream.length > 0) {
            netStream.writeBytes(objectStream);
        }
    }
}
