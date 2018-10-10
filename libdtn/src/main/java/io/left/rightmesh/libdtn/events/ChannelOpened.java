package io.left.rightmesh.libdtn.events;

import io.left.rightmesh.libdtn.network.cla.CLAChannel;

/**
 * @author Lucien Loiseau on 10/10/18.
 */
public class ChannelOpened implements DTNEvent {
    public CLAChannel channel;

    public ChannelOpened(CLAChannel channel) {
        this.channel = channel;
    }
}
