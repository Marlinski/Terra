package io.disruptedsystems.libdtn.core.extension;

import io.disruptedsystems.libdtn.common.data.BaseBlockFactory;
import io.disruptedsystems.libdtn.common.data.BlockFactory;
import io.disruptedsystems.libdtn.common.data.CanonicalBlock;
import io.disruptedsystems.libdtn.common.data.blob.BlobFactory;
import io.disruptedsystems.libdtn.common.data.bundlev7.parser.BaseBlockDataParserFactory;
import io.disruptedsystems.libdtn.common.data.bundlev7.parser.BlockDataParserFactory;
import io.disruptedsystems.libdtn.common.data.bundlev7.processor.BaseBlockProcessorFactory;
import io.disruptedsystems.libdtn.common.data.bundlev7.processor.BlockProcessor;
import io.disruptedsystems.libdtn.common.data.bundlev7.processor.BlockProcessorFactory;
import io.disruptedsystems.libdtn.common.data.bundlev7.serializer.BaseBlockDataSerializerFactory;
import io.disruptedsystems.libdtn.common.data.bundlev7.serializer.BlockDataSerializerFactory;
import io.disruptedsystems.libdtn.common.data.eid.BaseClaEid;
import io.disruptedsystems.libdtn.common.data.eid.BaseClaEidFactory;
import io.disruptedsystems.libdtn.common.data.eid.BaseDtnEidFactory;
import io.disruptedsystems.libdtn.common.data.eid.BaseEidFactory;
import io.disruptedsystems.libdtn.common.data.eid.ClaEid;
import io.disruptedsystems.libdtn.common.data.eid.ClaEidParser;
import io.disruptedsystems.libdtn.common.data.eid.BaseDtnEid;
import io.disruptedsystems.libdtn.common.data.eid.Eid;
import io.disruptedsystems.libdtn.common.data.eid.EidFactory;
import io.disruptedsystems.libdtn.common.data.eid.EidFormatException;
import io.disruptedsystems.libdtn.common.data.eid.EidSspParser;
import io.disruptedsystems.libdtn.common.utils.Log;
import io.disruptedsystems.libdtn.core.api.ExtensionManagerApi;
import io.marlinski.libcbor.CborEncoder;
import io.marlinski.libcbor.CborParser;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ExtensionManager implements the ExtensionManagerAPI and provides entry point to add new
 * extension blocks and eids into the core.
 *
 * @author Lucien Loiseau on 22/11/18.
 */
public class ExtensionManager implements ExtensionManagerApi {

    public static final String TAG = "ExtensionManager";

    /* extension block */
    private Map<Integer, Supplier<CanonicalBlock>> extensionBlockFactory = new HashMap<>();
    private Map<Integer, Supplier<CborParser>> extensionBlockParserFactory = new HashMap<>();
    private Map<Integer, Supplier<CborEncoder>> extensionBlockSerializerFactory = new HashMap<>();
    private Map<Integer, Supplier<BlockProcessor>> extensionBlockProcessorFactory = new HashMap<>();

    /* extension Eid */
    private Map<Integer, String> extensionEidSchemeIana = new HashMap<>();
    private Map<String, EidSspParser> extensionEidParser = new HashMap<>();
    private Map<String, ClaEidParser> extensionClaEidParser = new HashMap<>();

    private Log logger;

    public ExtensionManager(Log logger) {
        this.logger = logger;
    }

    /*** BLOCK EXTENSION LOGIC ***/

    private BlockFactory coreBlockFactory = new BlockFactory() {
        BlockFactory baseBlockFactory = new BaseBlockFactory();

        @Override
        public CanonicalBlock create(int type) throws UnknownBlockTypeException {
            try {
                return baseBlockFactory.create(type);
            } catch (UnknownBlockTypeException ubte) {
                if (extensionBlockFactory.containsKey(type)) {
                    return extensionBlockFactory.get(type).get();
                }
            }
            throw new UnknownBlockTypeException();
        }
    };

    private BlockDataParserFactory coreBlockParserFactory = new BlockDataParserFactory() {
        BaseBlockDataParserFactory baseBlockParserFactory = new BaseBlockDataParserFactory();

        @Override
        public CborParser create(int type,
                                 CanonicalBlock block,
                                 BlobFactory blobFactory,
                                 EidFactory eidFactory,
                                 Log logger) throws UnknownBlockTypeException {
            try {
                return baseBlockParserFactory.create(type, block, blobFactory, eidFactory, logger);
            } catch (UnknownBlockTypeException ubte) {
                if (extensionBlockFactory.containsKey(type)) {
                    return extensionBlockParserFactory.get(type).get();
                }
            }
            throw new UnknownBlockTypeException();
        }
    };

    private BlockDataSerializerFactory coreSerializerFactory = new BlockDataSerializerFactory() {
        BaseBlockDataSerializerFactory baseSerializerFactory = new BaseBlockDataSerializerFactory();

        @Override
        public CborEncoder create(CanonicalBlock block) throws UnknownBlockTypeException {
            try {
                return baseSerializerFactory.create(block);
            } catch (UnknownBlockTypeException ubte) {
                if (extensionBlockSerializerFactory.containsKey(block.type)) {
                    return extensionBlockSerializerFactory.get(block.type).get();
                }
            }
            throw new UnknownBlockTypeException();
        }
    };

    private BlockProcessorFactory coreProcessorFactory = new BlockProcessorFactory() {
        BaseBlockProcessorFactory baseBlockProcessorFactory = new BaseBlockProcessorFactory();

        @Override
        public BlockProcessor create(int type) throws ProcessorNotFoundException {
            try {
                return baseBlockProcessorFactory.create(type);
            } catch (ProcessorNotFoundException pnfe) {
                if (extensionBlockProcessorFactory.containsKey(type)) {
                    return extensionBlockProcessorFactory.get(type).get();
                }
            }
            throw new ProcessorNotFoundException();
        }
    };

    /*** CLA EID EXTENSION LOGIC ***/

    private ClaEidParser claFactory = new BaseClaEidFactory(true) {
        @Override
        public ClaEid createClaEid(String claName, String claSpecific, String demux) throws EidFormatException {
            try {
                return super.createClaEid(claName, claSpecific,demux);
            } catch (EidFactory.UnknownEidScheme ues) {
                if (extensionClaEidParser.containsKey(claName)) {
                    return extensionClaEidParser.get(claName).createClaEid(claName, claSpecific,demux);
                }
            }
            return new BaseClaEid(claName, claSpecific, demux);
        }
    };

    private EidSspParser dtnParser = new BaseDtnEidFactory(claFactory);

    private EidFactory eidFactory = new BaseEidFactory(dtnParser) {
        @Override
        public String getIanaScheme(int ianaScheme) throws UnknownIanaNumber {
            try {
                return super.getIanaScheme(ianaScheme);
            } catch (UnknownIanaNumber uin) {
                if (extensionEidSchemeIana.containsKey(ianaScheme)) {
                    return extensionEidSchemeIana.get(ianaScheme);
                }
            }
            throw new UnknownIanaNumber(ianaScheme);
        }

        @Override
        public Eid create(String scheme, String ssp) throws EidFormatException {
            try {
                return super.create(scheme, ssp);
            } catch (UnknownEidScheme ues) {
                if (extensionEidParser.containsKey(scheme)) {
                    return extensionEidParser.get(scheme).create(ssp);
                }
            }
            throw new UnknownEidScheme(scheme);
        }
    };

    /*** EXTENSION MANAGEMENT ***/

    @Override
    public BlockFactory getBlockFactory() {
        return coreBlockFactory;
    }

    @Override
    public BlockDataParserFactory getBlockDataParserFactory() {
        return coreBlockParserFactory;
    }

    @Override
    public BlockDataSerializerFactory getBlockDataSerializerFactory() {
        return coreSerializerFactory;
    }

    @Override
    public EidFactory getEidFactory() {
        return eidFactory;
    }

    @Override
    public BlockProcessorFactory getBlockProcessorFactory() {
        return coreProcessorFactory;
    }

    @Override
    public void addExtensionBlock(int type,
                                  Supplier<CanonicalBlock> block,
                                  Supplier<CborParser> parser,
                                  Supplier<CborEncoder> serializer,
                                  Supplier<BlockProcessor> processor)
            throws BlockTypeAlreadyManaged {
        if (extensionBlockFactory.containsKey(type)) {
            throw new BlockTypeAlreadyManaged();
        }
        extensionBlockFactory.put(type, block);
        extensionBlockParserFactory.put(type, parser);
        extensionBlockSerializerFactory.put(type, serializer);
        extensionBlockProcessorFactory.put(type, processor);
    }

    @Override
    public void addExtensionEid(int ianaNumber, String scheme, EidSspParser parser)
            throws EidSchemeAlreadyManaged {
        if (extensionEidParser.containsKey(scheme)) {
            throw new EidSchemeAlreadyManaged();
        }
        extensionEidSchemeIana.put(ianaNumber, scheme);
        extensionEidParser.put(scheme, parser);
        logger.v(TAG, "new Eid added: " + scheme + " (iana = " + ianaNumber + ")");
    }

    @Override
    public void addExtensionClaEid(String clName, ClaEidParser parser) throws ClaNameAlreadyManaged {
        if (extensionClaEidParser.containsKey(clName)) {
            throw new ClaNameAlreadyManaged();
        }
        extensionClaEidParser.put(clName, parser);
        logger.v(TAG, "new CLA added: cla:" + clName);
    }
}
