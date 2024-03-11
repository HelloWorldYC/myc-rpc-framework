package github.myc.remoting.transport.netty.client;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 保存和获取 Channel 对象
 */
@Slf4j
public class ChannelProvider {

    private final Map<String, Channel> channelMap;

    public ChannelProvider() {
        channelMap = new ConcurrentHashMap<>();
    }

    /**
     * 获取 channel
     * @param inetSocketAddress channel 的另一端的地址
     * @return channel 对象
     */
    public Channel get(InetSocketAddress inetSocketAddress) {
        String key = inetSocketAddress.toString();
        // 确定是否有对应地址的连接，有连接才能获取
        if(channelMap.containsKey(key)) {
            Channel channel = channelMap.get(key);
            // 进一步检查 channel 是否可用，如果可用才返回该 channel
            if(channel != null && channel.isActive()) {
                return channel;
            } else {
                channelMap.remove(key);
            }
        }
        return null;
    }

    public void set(InetSocketAddress inetSocketAddress, Channel channel) {
        String key = inetSocketAddress.toString();
        channelMap.put(key, channel);
    }

    public void remove(InetSocketAddress inetSocketAddress) {
        String key = inetSocketAddress.toString();
        channelMap.remove(key);
        log.info("Channel map size : [{}]", channelMap.size());
    }
}
