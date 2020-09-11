package io.disruptedsystems.libdtn.core.spi;

import java.net.URI;

import io.disruptedsystems.libdtn.common.ExtensionToolbox;
import io.disruptedsystems.libdtn.common.data.Bundle;
import io.disruptedsystems.libdtn.common.data.blob.BlobFactory;
import io.disruptedsystems.libdtn.common.data.bundlev7.serializer.BlockDataSerializerFactory;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;

/**
 * A ClaChannelSpi is an abstraction of the underlying transport protocol used by a CLA
 * and should be able to receive and send Bundles.
 *
 * @author Lucien Loiseau on 04/09/18.
 */
public interface ClaChannelSpi {

    enum ChannelMode {
        OutUnidirectional,
        InUnidirectional,
        BiDirectional
    }

    /**
     * return this channel mode of operation.
     *
     * @return ChannelMode
     */
    ChannelMode getMode();

    /**
     * check wether this channel is open. A channel is open if it can send or receive bundle.
     * when a channel is close, it can no longer be used for sending or receiving bundle.
     *
     * @return true if the channel is open
     */
    boolean isOpen();

    /**
     * return the Eid specific for this Channel. It must be unique accross all channels.
     * It is used to identify this interface.
     *
     * @return Eid of this channel
     */
    URI channelEid();

    /**
     * return the Eid that represents the local host for this specific Channel.
     *
     * @return Eid of this channel
     */
    URI localEid();

    /**
     * Receive a deserialized stream of Bundle from this Convergence Layer.
     *
     * @param toolbox to create new block, parse block data and extended eid
     * @param blobFactory to store blob
     * @return Flowable of Bundle
     */
    Observable<Bundle> recvBundle(ExtensionToolbox toolbox,
                                  BlobFactory blobFactory);

    /**
     * Send a Bundle.
     * todo add priority
     *
     * @param bundle to send
     * @param serializerFactory to serialize all the extension
     * @return an Observable to track the number of bytes sent.
     */
    Observable<Integer> sendBundle(Bundle bundle,
                                   BlockDataSerializerFactory serializerFactory);

    /**
     * Send a stream of Bundles.
     *
     * @param upstream the stream of bundle to be sent
     * @param serializerFactory to serialize all the extension
     * @return an Observable to track the number of bundle sent.
     */
    Observable<Integer> sendBundles(Flowable<Bundle> upstream,
                                    BlockDataSerializerFactory serializerFactory);

    /**
     * Close that channel. Once a channel is closed, it cannot receive nor send Bundles.
     */
    void close();

}