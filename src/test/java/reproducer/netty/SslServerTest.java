package reproducer.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslHandler;
import org.junit.Before;
import org.junit.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;

import java.io.File;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Georg Held
 */
public class SslServerTest {

    private SslHandler sslHandler;
    private File store;


    @Before
    public void setUp() throws Exception {
        store = new KeyStoreGenerator().generateKeyStore("teststore", "jks", SslContextBuilder.PASSWORD, SslContextBuilder.PASSWORD);
    }

    @Test(timeout = 5000, expected = SSLException.class)
    public void test_invalid_ssl_configuration() throws Exception {

        final SslServer sslServer = new SslServer(8000, store);
        sslServer.start();

        final Bootstrap tlsClient = createTlsClient();
        final ChannelFuture connect = tlsClient.connect("127.0.0.1", 8000);
        connect.sync();
        final Channel channel = connect.channel();
        final ChannelPromise channelPromise = channel.newPromise();
        channel.write("test-message", channelPromise);


        sslHandler.handshakeFuture().await();
        assertFalse(sslHandler.handshakeFuture().isSuccess());

        channelPromise.await();

        // this fails
        assertFalse(channelPromise.isSuccess());
        // additionally we expect a SSLException here
        channelPromise.sync();
    }


    private Bootstrap createTlsClient() {

        final EventLoopGroup group = new NioEventLoopGroup();
        final Bootstrap b = new Bootstrap();
        b.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        final SSLContext sslContext = SslContextBuilder.createJdkSSLContext(store);
                        sslHandler = new SslHandler(SslContextBuilder.createSslEngine(sslContext, "TLS_RSA_WITH_AES_128_CBC_SHA"));
                        sslHandler.engine().setUseClientMode(true);

                        ch.pipeline().addFirst(sslHandler);
                        ch.pipeline().addLast(new LoggingHandler(LogLevel.DEBUG));
                        ch.pipeline().addLast(new StringDecoder());
                        ch.pipeline().addLast(new StringEncoder());
                    }
                });

        return b;
    }

}