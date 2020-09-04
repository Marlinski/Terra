package io.disruptedsystems.libdtn.core.events;

import io.disruptedsystems.libdtn.core.spi.ClaChannelSpi;

/**
 * LinkLocalEntryDown event is thrown whenever a link-local entry was removed from link-local table.
 *
 * @author Lucien Loiseau on 17/10/18.
 */
public class LinkLocalEntryDown {
    public ClaChannelSpi channel;

    public LinkLocalEntryDown(ClaChannelSpi channel) {
        this.channel = channel;
    }

    @Override
    public String toString() {
        return "Delete link-local entry: local=" + channel.localEid().toString()
                + " peer=" + channel.channelEid().toString();
    }
}
