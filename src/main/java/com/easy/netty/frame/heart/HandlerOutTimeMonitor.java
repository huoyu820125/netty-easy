package com.easy.netty.frame.heart;

import io.netty.channel.*;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @Author SunQian
 * @CreateTime 2020/3/17 10:34
 * @Description: TODO
 */
@Slf4j
@ChannelHandler.Sharable
public class HandlerOutTimeMonitor extends ChannelOutboundHandlerAdapter implements ChannelInboundHandler {
    private IHandlerIdleConnection handlerIdleConnect;
    private ConcurrentHashMap<String, IOMonitor> channelMonitorMap = new ConcurrentHashMap<>();
    private long writerIdleTimeNanos = 0L;
    private long readIdleTimeNanos = 0L;

    public HandlerOutTimeMonitor(IHandlerIdleConnection handlerIdleConnect, TimeUnit unit, int readIdleTime, int writeIdleTime) {
        this.handlerIdleConnect = handlerIdleConnect;
        if (readIdleTime > 0) {
            readIdleTimeNanos = Math.max(unit.toNanos(readIdleTime), TimeUnit.MILLISECONDS.toNanos(1));
            return;
        }

        if (writeIdleTime > 0) {
            writerIdleTimeNanos = Math.max(unit.toNanos(writeIdleTime), TimeUnit.MILLISECONDS.toNanos(1));
            return;
        }

    }

    /**
     * author: SunQian
     * date: 2020/3/17 11:39
     * title: TODO
     * descritpion: start monitor on this connection when the connection establishment
     * @param ctx
     * return: TODO
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        try {
            //create monitor associated with this channel
            IOMonitor moniter = new IOMonitor();
            channelMonitorMap.put(ctx.channel().id().asLongText(), moniter);

            //start monitor
            moniter.start(ctx, readIdleTimeNanos, writerIdleTimeNanos);
        }
        catch (Exception e) {
            throw new RuntimeException("start the task of write monitor issue exception:");
        }
        finally {
            //call next handler's the function of channelActive=
            ctx.fireChannelActive();
        }
    }

    /**
     * author: SunQian
     * date: 2020/3/17 12:18
     * title: TODO
     * descritpion: monitor write event
     * @param ctx
     * return: TODO
     */
    @Override
    public void flush(ChannelHandlerContext ctx) throws Exception {
        try {
            if (writerIdleTimeNanos <= 0) {
                //not monitor write event
                return;
            }

            IOMonitor moniter = channelMonitorMap.get(ctx.channel().id().asLongText());
            if (null != moniter) {
                moniter.writeTask.refreshTime();
            }
        }
        catch (Exception e){
            throw new RuntimeException("refresh last write time issue exception:");
        }
        finally {
            ctx.flush();
        }
    }

    /**
     * author: SunQian
     * date: 2020/3/20 14:18
     * title: TODO
     * descritpion: monitor read event
     * @param ctx
     * @param o
     * return: TODO
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object o) throws Exception {
        try {
            if (readIdleTimeNanos <= 0) {
                //not monitor read event
                return;
            }

            IOMonitor moniter = channelMonitorMap.get(ctx.channel().id().asLongText());
            if (null != moniter) {
                moniter.readTask.refreshTime();
            }
        }
        catch (Exception e){
            throw new RuntimeException("refresh last read time issue exception:");
        }
        finally {
            ctx.fireChannelRead(o);
        }
    }

    /**
     * author: SunQian
     * date: 2020/3/17 12:18
     * title: TODO
     * descritpion: TODO
     * @param ctx
     * return: TODO
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        try {
            IOMonitor moniterData = channelMonitorMap.remove(ctx.channel().id().asLongText());
            moniterData.stop();
        }
        catch (Exception e) {
            throw new RuntimeException("stop the task of monitor issue exception:");
        }
        finally {
            ctx.fireChannelInactive();
        }
    }

    /**
     * author: SunQian
     * date: 2020/3/17 11:39
     * title: TODO
     * descritpion: TODO
     * @param ctx
     * return: TODO
     */
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        channelMonitorMap.entrySet().forEach(entry -> {
            entry.getValue().stop();
        });
    }

    /**
     * monitor event constant value
     */
    static final int READ_EVENT = 1;
    static final int WRITE_EVENT = 2;

    /**
     * event outtime monitor task
     */
    private final class EventTimeoutTask implements Runnable {
        private ChannelHandlerContext ctx;
        private int monitorEvent = HandlerOutTimeMonitor.READ_EVENT;
        private long outTime = 0L;
        private long lastEventTime = 0L;
        private ScheduledFuture<?> scheduledFuture = null;

        EventTimeoutTask(int monitorEvent, ChannelHandlerContext ctx, long outTime) {
            this.ctx = ctx;
            this.monitorEvent = monitorEvent;
            this.outTime = outTime;
        }

        @Override
        public void run() {
            checkOuttime(ctx);
        }

        protected void checkOuttime(ChannelHandlerContext ctx) {
            if (!ctx.channel().isOpen()) {
                //The connection is closed and IO events are no longer monitored
                return;
            }

            IOMonitor moniter = channelMonitorMap.get(ctx.channel().id().asLongText());
            if (null == moniter) {
                //The monitor may be removed when the connection is closed
                return;
            }

            long nextDelay = 0;
            try {
                nextDelay = outTime - (System.nanoTime() - lastEventTime);
                if (nextDelay <= 0) {
                    nextDelay = outTime;
                    if (HandlerOutTimeMonitor.READ_EVENT == monitorEvent) {
                        log.info("{} read outtime!", ctx.channel().remoteAddress());
                        handlerIdleConnect.onReadOuttime(ctx);
                        moniter.readTask.refreshTime();
                    }
                    else if (HandlerOutTimeMonitor.WRITE_EVENT == monitorEvent) {
                        log.info("{} write outtime!", ctx.channel().remoteAddress());
                        handlerIdleConnect.onWriteOuttime(ctx);
                        moniter.writeTask.refreshTime();
                    }
                }
            }
            catch (Throwable t) {
                ctx.fireExceptionCaught(t);
            }
            finally {
                //continue wait event
                if (HandlerOutTimeMonitor.READ_EVENT == monitorEvent) {
                    moniter.readTask.wait(ctx, nextDelay);
                }
                if (HandlerOutTimeMonitor.WRITE_EVENT == monitorEvent) {
                    moniter.writeTask.wait(ctx, nextDelay);
                }
            }
        }

        /**
         * author: SunQian
         * date: 2020/3/20 14:21
         * title: TODO
         * descritpion: start wait event
         * @param ctx       the object of occur to event
         * @param outTime   max wait time
         * return: TODO
         */
        public void wait(ChannelHandlerContext ctx, long outTime) {
            scheduledFuture = ctx.executor().schedule(this, outTime, TimeUnit.NANOSECONDS);

            return;
        }

        public void refreshTime() {
            lastEventTime = System.nanoTime();
        }

        public void stop() {
            scheduledFuture.cancel(false);
        }
    }

    /**
     * io monitor of channel
     */
    private class IOMonitor {
        //read monitor task
        EventTimeoutTask readTask = null;

        //write monitor task
        EventTimeoutTask writeTask = null;

        public void start(ChannelHandlerContext ctx, long readOutTime, long writeOutTime) {
            if (readOutTime > 0) {
                readTask = new EventTimeoutTask(HandlerOutTimeMonitor.READ_EVENT, ctx, readOutTime);
                readTask.refreshTime();
                readTask.wait(ctx, readOutTime);
            }

            if (writeOutTime > 0) {
                writeTask= new EventTimeoutTask(HandlerOutTimeMonitor.WRITE_EVENT, ctx, writeOutTime);
                writeTask.refreshTime();
                writeTask.wait(ctx, writeOutTime);
            }

            return;
        }

        public void stop() {
            if (null != writeTask) {
                writeTask.stop();
                writeTask = null;
            }
            if (null != readTask) {
                readTask.stop();
                readTask = null;
            }
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        ctx.write(msg, promise);
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelRegistered();
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelUnregistered();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelReadComplete();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object o) throws Exception {
        ctx.fireUserEventTriggered(o);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelWritabilityChanged();
    }

}
