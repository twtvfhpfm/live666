package org.twtvfhpfm.live666.rtspserver;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;

import io.netty.handler.codec.serialization.ObjectEncoder;
import org.twtvfhpfm.live666.rtspcodec.RtspRequestDecoder;
import org.twtvfhpfm.live666.rtspcodec.RtspResponseEncoder;

public class RtspServer {
    public void run(final int port) throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) 
                        throws Exception {
                        //ch.pipeline().addLast("encoder", new ObjectEncoder());
                        ch.pipeline().addLast("rtsp-response-encoder", new RtspResponseEncoder());
                        ch.pipeline().addLast("rtsp-decoder", new RtspRequestDecoder());
                        ch.pipeline().addLast("rtsp-server-handler", new RtspServerHandler());
                    }
                });
            ChannelFuture future = b.bind("localhost", port).sync();
            System.out.println("RTSP Server Startup.");
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
