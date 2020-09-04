package io.disruptedsystems.libdtn.core.events;

import io.disruptedsystems.libdtn.core.spi.ClaChannelSpi;

/**
 * ChannelClosed event is thrown whenever a {@link ClaChannelSpi} has closed.
 *
 * @author Lucien Loiseau on 10/10/18.
 */
public class ChannelClosed implements DtnEvent {
    public ClaChannelSpi channel;

    public ChannelClosed(ClaChannelSpi channel) {
        this.channel = channel;
    }

    @Override
    public String toString() {
        return "Channel closed: local=" + channel.localEid().toString()
                + " peer=" + channel.channelEid().toString();
    }
}
