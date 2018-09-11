package io.left.rightmesh.libdtn.data.bundleV7.cbor;

import static io.left.rightmesh.libdtn.data.bundleV7.cbor.Constants.CborAdditionalInfo.IndefiniteLength;
import static io.left.rightmesh.libdtn.data.bundleV7.cbor.Constants.CborAdditionalInfo.Value16Bit;
import static io.left.rightmesh.libdtn.data.bundleV7.cbor.Constants.CborAdditionalInfo.Value32Bit;
import static io.left.rightmesh.libdtn.data.bundleV7.cbor.Constants.CborAdditionalInfo.Value64Bit;
import static io.left.rightmesh.libdtn.data.bundleV7.cbor.Constants.CborAdditionalInfo.Value8Bit;
import static io.left.rightmesh.libdtn.data.bundleV7.cbor.Constants.CborInternals.BreakByte;
import static io.left.rightmesh.libdtn.data.bundleV7.cbor.Constants.CborInternals.MajorTypeShift;
import static io.left.rightmesh.libdtn.data.bundleV7.cbor.Constants.CborMajorTypes.NegativeIntegerType;
import static io.left.rightmesh.libdtn.data.bundleV7.cbor.Constants.CborMajorTypes.UnsignedIntegerType;
import static io.left.rightmesh.libdtn.data.bundleV7.cbor.Constants.CborSimpleValues.Break;
import static io.left.rightmesh.libdtn.data.bundleV7.cbor.Constants.CborSimpleValues.FalseValue;
import static io.left.rightmesh.libdtn.data.bundleV7.cbor.Constants.CborSimpleValues.HalfPrecisionFloat;
import static io.left.rightmesh.libdtn.data.bundleV7.cbor.Constants.CborSimpleValues.NullValue;
import static io.left.rightmesh.libdtn.data.bundleV7.cbor.Constants.CborSimpleValues.TrueValue;
import static io.left.rightmesh.libdtn.data.bundleV7.cbor.Constants.CborType.CborArrayType;
import static io.left.rightmesh.libdtn.data.bundleV7.cbor.Constants.CborType.CborByteStringType;
import static io.left.rightmesh.libdtn.data.bundleV7.cbor.Constants.CborType.CborDoubleType;
import static io.left.rightmesh.libdtn.data.bundleV7.cbor.Constants.CborType.CborFloatType;
import static io.left.rightmesh.libdtn.data.bundleV7.cbor.Constants.CborType.CborHalfFloatType;
import static io.left.rightmesh.libdtn.data.bundleV7.cbor.Constants.CborType.CborMapType;
import static io.left.rightmesh.libdtn.data.bundleV7.cbor.Constants.CborType.CborSimpleType;
import static io.left.rightmesh.libdtn.data.bundleV7.cbor.Constants.CborType.CborTagType;
import static io.left.rightmesh.libdtn.data.bundleV7.cbor.Constants.CborType.CborTextStringType;

import java.lang.reflect.Array;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;

public class Encoder {

    private ByteBuffer out;

    Encoder(ByteBuffer buffer) {
        this.out = buffer;
    }

    /**
     * cbor_encode_object will try to encode the object given as a parameter. The Object must be an
     * instance of one of the following class:
     * <lu>
     *     <li>Double</li>
     *     <li>Float</li>
     *     <li>Long</li>
     *     <li>Integer</li>
     *     <li>Short</li>
     *     <li>Byte</li>
     *     <li>Boolean</li>
     *     <li>String</li>
     *     <li>Map</li>
     *     <li>Collection</li>
     *     <li>Object[]</li>
     * </lu>
     *
     * <p>For Map, Collection and array, the encapsulated data must also be one of the listed type.
     *
     * @param o object to be encoded
     * @return this encoder
     * @throws BufferOverflowException if the buffer is full
     * @throws CBOR.CborEncodingUnknown if object is not accepted type
     */
    public Encoder cbor_encode_object(Object o) throws BufferOverflowException, CBOR.CborEncodingUnknown {
        if(o instanceof Double) {
            cbor_encode_double(((Double) o));
        } else if(o instanceof Float) {
            cbor_encode_float(((Float) o));
        } else if(o instanceof Number) {
            cbor_encode_int(((Number) o).longValue());
        } else if(o instanceof String) {
            cbor_encode_text_string((String)o);
        } else if(o instanceof Boolean) {
            encode_number((byte)(CborSimpleType), (byte)(((Boolean) o) ? TrueValue : FalseValue));
        } else if(o instanceof Map) {
            cbor_encode_map((Map)o);
        } else if(o instanceof Collection) {
            cbor_encode_collection((Collection)o);
        } else if(o != null) {
            Class<?> type = o.getClass();
            if (type.isArray()) {
                int len = Array.getLength(o);
                cbor_start_array(len);
                for (int i = 0; i < len; i++) {
                    cbor_encode_object(Array.get(o, i));
                }
            } else {
                throw new CBOR.CborEncodingUnknown();
            }
        } else {
            encode_number((byte)(CborSimpleType), NullValue);
        }
        return this;
    }

    /**
     * cbor_encode_collection will try to encode the collection given as a paremeter. The item
     * embedded in this collection must be encodable with cbor_encode_object.
     *
     * @param c collection to encode
     * @return this encoder
     * @throws BufferOverflowException if the buffer is full
     * @throws CBOR.CborEncodingUnknown if object is not accepted type
     */
    public Encoder cbor_encode_collection(Collection c) throws BufferOverflowException, CBOR.CborEncodingUnknown {
        cbor_start_array(c.size());
        for(Object o : c) {
            cbor_encode_object(o);
        }
        return this;
    }

    /**
     * cbor_encode_map will try to encode the map given as a paremeter. The keys and items
     * embedded in this map must be encodable with cbor_encode_object.
     *
     * @param m Map to encode
     * @return this encoder
     * @throws BufferOverflowException if the buffer is full
     * @throws CBOR.CborEncodingUnknown if object is not accepted type
     */
    public Encoder cbor_encode_map(Map m) throws BufferOverflowException, CBOR.CborEncodingUnknown {
        cbor_start_map(m.size());
        for(Object o : m.keySet()) {
            cbor_encode_object(o);
            cbor_encode_object(m.get(o));
        }
        return this;
    }


    /**
     * Starts an array of length given. if length is negative, the array is assumed to be of size
     * indefinite. This encoder makes no check if a break ever appear later so a later call to
     * cbor_stop_array must be done to ensure cbor-validity.
     *
     * @param length of the array
     * @return this encoder
     * @throws BufferOverflowException if the buffer is full
     */
    public Encoder cbor_start_array (long length) throws BufferOverflowException {
        if(length < 0) {
            encode_number((byte) CborArrayType, IndefiniteLength);
        } else {
            encode_number((byte) CborArrayType, length);
        }
        return this;
    }

    /**
     * Close an opened array. This encoder makes no check wether a container was opened earlier.
     *
     * @return this encoder
     * @throws BufferOverflowException if the buffer is full
     */
    public Encoder cbor_stop_array () throws BufferOverflowException {
        out.put((byte)BreakByte);
        return this;
    }

    /**
     * Starts a map of length given. if length is negative, the map is assumed to be of size
     * indefinite. This encoder makes no check if a break ever appear later so it must be added
     * manually to ensure cbor-validity.
     *
     * @param length of the map
     * @return this encoder
     * @throws BufferOverflowException if the buffer is full
     */
    public Encoder cbor_start_map (long length) throws BufferOverflowException {
        if(length < 0) {
            encode_number((byte) CborMapType, IndefiniteLength);
        } else {
            encode_number((byte) CborMapType, length);
        }
        return this;
    }

    /**
     * Close an opened map. This encoder makes no check wether a container was opened earlier.
     *
     * @return this encoder
     * @throws BufferOverflowException if the buffer is full
     */
    public Encoder cbor_stop_map () throws BufferOverflowException {
        out.put((byte)BreakByte);
        return this;
    }

    /**
     * Starts a byte string of length given. if length is negative, the string is assumed to be of
     * size indefinite. While this byte string is open, chunks must be added with
     * {@see cbor_put_byte_string_chunk}. This encoder makes no check if a break ever appear later
     * so it must be added manually to ensure cbor-validity.
     *
     * @param length of the string
     * @return this encoder
     * @throws BufferOverflowException if the buffer is full
     */
    public Encoder cbor_start_byte_string (long length) throws BufferOverflowException {
        if(length < 0) {
            encode_number((byte) CborByteStringType, IndefiniteLength);
        } else {
            encode_number((byte) CborByteStringType, length);
        }
        return this;
    }

    /**
     * Add a fixed length byte string.
     *
     * @param chunk to add
     * @return this encoder
     * @throws BufferOverflowException if the buffer is full
     */
    public Encoder cbor_put_byte_string_chunk (byte[] chunk) throws BufferOverflowException {
        cbor_encode_byte_string(chunk);
        return this;
    }

    /**
     * Close an opened byte string. This encoder makes no check wether a container was opened
     * earlier.
     *
     * @return this encoder
     * @throws BufferOverflowException if the buffer is full
     */
    public Encoder cbor_stop_byte_string () throws BufferOverflowException {
        out.put((byte)BreakByte);
        return this;
    }

    /**
     * Starts a text string of length given. if length is negative, the string is assumed to be of
     * size indefinite. While this text string is open, chunks must be added with
     * {@see cbor_put_text_string_chunk}. This encoder makes no check if a break ever appear later
     * so it must be added manually to ensure cbor-validity.
     *
     * @param length of the string
     * @return this encoder
     * @throws BufferOverflowException if the buffer is full
     */
    public Encoder cbor_start_text_string (long length) throws BufferOverflowException {
        if(length < 0) {
            encode_number((byte) CborTextStringType, IndefiniteLength);
        } else {
            encode_number((byte) CborTextStringType, length);
        }
        return this;
    }

    /**
     * Add a fixed length text string.
     *
     * @param chunk to add
     * @return this encoder
     * @throws BufferOverflowException if the buffer is full
     */
    public Encoder cbor_put_text_string_chunk (String chunk) throws BufferOverflowException {
        cbor_encode_text_string(chunk);
        return this;
    }

    /**
     * Close an opened text string. This encoder makes no check wether a container was opened
     * earlier.
     *
     * @return this encoder
     * @throws BufferOverflowException if the buffer is full
     */
    public Encoder cbor_stop_text_string () throws BufferOverflowException {
        out.put((byte)BreakByte);
        return this;
    }

    /**
     * Add a fixed length byte string.
     *
     * @param array to add
     * @return this encoder
     * @throws BufferOverflowException if the buffer is full
     */
    public Encoder cbor_encode_byte_string(byte[] array) throws BufferOverflowException {
        encode_string((byte)CborByteStringType, array);
        return this;
    }

    /**
     * Add a fixed length text string. This encoder makes no check that the str supplied is
     * a UTF-8 text string.
     *
     * @param str to add
     * @return this encoder
     * @throws BufferOverflowException if the buffer is full
     */
    public Encoder cbor_encode_text_string(String str) throws BufferOverflowException {
        encode_string((byte)CborTextStringType, str.getBytes());
        return this;
    }

    /**
     * add a tag to the CBOR stream.
     *
     * @param tag to add
     * @return this encoder
     * @throws BufferOverflowException if the buffer is full
     */
    public Encoder cbor_encode_tag(long tag) throws BufferOverflowException {
        encode_number((byte)CborTagType, tag);
        return this;
    }

    /**
     * encode a double floating point number.
     *
     * @param value to add
     * @return this encoder
     * @throws BufferOverflowException if the buffer is full
     */
    public Encoder cbor_encode_double(double value) throws BufferOverflowException {
        encode_number((byte)CborDoubleType, Double.doubleToRawLongBits(value));
        return this;
    }

    /**
     * encode a single floating point number.
     *
     * @param value to add
     * @return this encoder
     * @throws BufferOverflowException if the buffer is full
     */
    public Encoder cbor_encode_float(float value) throws BufferOverflowException {
        encode_number((byte)CborFloatType, Float.floatToRawIntBits(value));
        return this;
    }

    /**
     * encode a half floating point number.
     *
     * @param value to add
     * @return this encoder
     * @throws BufferOverflowException if the buffer is full
     */
    public Encoder cbor_encode_half_float(float value) throws BufferOverflowException {
        encode_number((byte)CborHalfFloatType, halfPrecisionToRawIntBits(value));
        return this;
    }

    /**
     * add a simple value {@see Constants.CborSimpleValues}.
     *
     * @param value to add
     * @return this encoder
     * @throws BufferOverflowException if the buffer is full
     */
    public Encoder cbor_encode_simple_value(byte value) throws BufferOverflowException, CBOR.CborErrorIllegalSimpleType {
        if (value >= HalfPrecisionFloat && value <= Break)
            throw new CBOR.CborErrorIllegalSimpleType();
        encode_number((byte)(CborSimpleType), value);
        return this;
    }

    /**
     * encode a positive or negative byte/short/integer/long number.
     *
     * @param value to add
     * @return this encoder
     * @throws BufferOverflowException if the buffer is full
     */
    public Encoder cbor_encode_int(long value) throws BufferOverflowException {
        long ui = value >> 63;
        byte majorType = (byte)(ui & 0x20);
        ui ^= value;
        encode_number(majorType, ui);
        return this;
    }

    /**
     * encode an unsigned positive byte/short/integer/long number.
     *
     * @param ui to add
     * @return this encoder
     * @throws BufferOverflowException if the buffer is full
     */
    public Encoder cbor_encode_uint(long ui) throws BufferOverflowException {
        encode_number( (byte)(UnsignedIntegerType << MajorTypeShift), ui);
        return this;
    }

    /**
     * encode a negative byte/short/integer/long number.
     *
     * @param absolute_value to add, value must be absolute
     * @return this encoder
     * @throws BufferOverflowException if the buffer is full
     */
    public Encoder cbor_encode_negative_uint(long absolute_value) throws BufferOverflowException {
        encode_number((byte)(NegativeIntegerType << MajorTypeShift), absolute_value - 1);
        return this;
    }

    private void encode_number(byte shifted_mt, long ui) throws BufferOverflowException {
        if (ui < Value8Bit) {
            out.put((byte) (shifted_mt | ui & 0xff));
        } else if (ui < 0x100L) {
            putUInt8(shifted_mt, (byte) ui);
        } else if (ui < 0x10000L) {
            putUInt16(shifted_mt, (short) ui);
        } else if (ui < 0x100000000L) {
            putUInt32(shifted_mt, (int) ui);
        } else {
            putUInt64(shifted_mt, ui);
        }
    }

    private void putUInt8(byte shifted_mt, byte ui) throws BufferOverflowException {
        out.put((byte)(shifted_mt | Value8Bit));
        out.put(ui);
    }

    private void putUInt16(byte shifted_mt, short ui) throws BufferOverflowException {
        out.put((byte)(shifted_mt | Value16Bit));
        out.putShort(ui);
    }

    private void putUInt32(byte shifted_mt, int ui) throws BufferOverflowException {
        out.put((byte)(shifted_mt | Value32Bit));
        out.putInt(ui);
    }

    private void putUInt64(byte shifted_mt, long ui) throws BufferOverflowException {
        out.put((byte)(shifted_mt | Value64Bit));
        out.putLong(ui);
    }

    private void encode_string(byte shifted_mt, byte[] array) throws BufferOverflowException {
        int len = (array == null) ? 0 : array.length;
        encode_number(shifted_mt, len);
        out.put(array);
    }

    private long halfPrecisionToRawIntBits(float value) throws BufferOverflowException {
        int fbits = Float.floatToIntBits(value);
        int sign = (fbits >>> 16) & 0x8000;
        int val = (fbits & 0x7fffffff) + 0x1000;

        // might be or become NaN/Inf
        if (val >= 0x47800000) {
            if ((fbits & 0x7fffffff) >= 0x47800000) { // is or must become NaN/Inf
                if (val < 0x7f800000) {
                    // was value but too large, make it +/-Inf
                    return sign | 0x7c00;
                }
                return sign | 0x7c00 | (fbits & 0x007fffff) >>> 13; // keep NaN (and Inf) bits
            }
            return sign | 0x7bff; // unrounded not quite Inf
        }
        if (val >= 0x38800000) {
            // remains normalized value
            return sign | val - 0x38000000 >>> 13; // exp - 127 + 15
        }
        if (val < 0x33000000) {
            // too small for subnormal
            return sign; // becomes +/-0
        }

        val = (fbits & 0x7fffffff) >>> 23;
        // add subnormal bit, round depending on cut off and div by 2^(1-(exp-127+15)) and >> 13 | exp=0
        return sign | ((fbits & 0x7fffff | 0x800000) + (0x800000 >>> val - 102) >>> 126 - val);
    }

}