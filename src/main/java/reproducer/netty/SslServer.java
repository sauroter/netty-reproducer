package reproducer.netty;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;


import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.GlobalEventExecutor;

/**
 * @author Georg Held
 */
public class SslServer {

    private int port;
    private final File store;


    private NioEventLoopGroup acceptGroup;
    private NioEventLoopGroup ioGroup;
    private ServerBootstrap bootstrap;
    private SSLContext sslContext;
    private ExecutorService service;
    private ChannelGroup channels;

    public SslServer(int port, final File store) {
        this.port = port;
        this.store = store;
    }

    public void start() {
        try {
            channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
            service = Executors.newFixedThreadPool(10);
            sslContext = SslContextBuilder.createJdkSSLContext(store);
            acceptGroup = new NioEventLoopGroup(1);
            ioGroup = new NioEventLoopGroup(1);
            bootstrap = new ServerBootstrap();
            bootstrap.group(acceptGroup, ioGroup)
                    .localAddress(port)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(getChannelInitializer());
            bootstrap.bind().sync();
        } catch (Exception e) {
            throw new RuntimeException("Failed to bootstrap server", e);
        }
    }

    private ChannelInitializer<SocketChannel> getChannelInitializer() {
        return new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new SslHandler(SslContextBuilder.createSslEngine(sslContext, "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA")));
                ch.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG));
                ch.pipeline().addLast(new StringDecoder());
                ch.pipeline().addLast(new StringEncoder());
                ch.pipeline().addLast(new EchoHandler());
            }
        };
    }

    private class EchoHandler extends SimpleChannelInboundHandler<String> {


        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            channels.add(ctx.channel());
            super.channelActive(ctx);
        }

        @Override
        protected void channelRead0(final ChannelHandlerContext ctx, final String msg) throws Exception {
            service.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        TimeUnit.SECONDS.sleep(1); // simulate business logic delay
                    } catch (InterruptedException e) {
                    }
                    ctx.writeAndFlush(msg);
                }
            });
        }
    }
}