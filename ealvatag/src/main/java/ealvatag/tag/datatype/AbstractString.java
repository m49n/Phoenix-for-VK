/*
 * @author : Paul Taylor
 * @author : Eric Farng
 * <p>
 * Version @version:$Id$
 * <p>
 * MusicTag Copyright (C)2003,2004
 * <p>
 * This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public  License as
 * published by the Free Software Foundation; either version 2.1 of the License, or (at your option) any later version.
 * <p>
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 * <p>
 * You should have received a copy of the GNU Lesser General Public License along with this library; if not, you can get a copy from
 * http://www.opensource.org/licenses/lgpl-license.php or write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA 02110-1301 USA
 * <p>
 * Description:
 */
package ealvatag.tag.datatype;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.util.NoSuchElementException;

import ealvatag.tag.exceptions.IllegalCharsetException;
import ealvatag.tag.id3.AbstractTagFrameBody;
import ealvatag.tag.id3.valuepair.TextEncoding;
import ealvatag.utils.StandardCharsets;
import okio.Buffer;

/**
 * A partial implementation for String based ID3 fields
 */
public abstract class AbstractString extends AbstractDataType {
    protected AbstractString(String identifier, AbstractTagFrameBody frameBody) {
        super(identifier, frameBody);
    }

    public AbstractString(String identifier, AbstractTagFrameBody frameBody, String value) {
        super(identifier, frameBody, value);
    }

    protected AbstractString(AbstractString object) {
        super(object);
    }

    public int getSize() {
        return size;
    }

    /**
     * Sets the size in bytes of this data type. This is set after writing the data to allow us to recalculate the size for
     * frame header.
     * <p>
     * During read (parsing) this size is checked against the overall frame size. Some data, such as null terminated strings, vary in
     * size (this includes double byte character sets and 2 byte null terminators).
     *
     * @param size the size of this data type
     */
    protected void setSize(int size) {
        this.size = size;
    }

    /**
     * Return String representation of data type
     *
     * @return a string representation of the value
     */
    @NotNull
    public String toString() {
        return (String) value;
    }

    /**
     * Check the value can be encoded with the specified encoding
     * <p>
     * Try and write to buffer using the CharSet defined by the textEncoding field (note if using UTF16 we dont
     * need to worry about LE,BE at this point it makes no difference)
     *
     * @return true if {@link CharsetEncoder#canEncode(CharSequence)} reports it can encode the value
     */
    public boolean canBeEncoded() {
        final byte textEncoding = this.getBody().getTextEncoding();
        final TextEncoding encoding = TextEncoding.getInstanceOf();
        final Charset charset = encoding.getCharsetForId(textEncoding);
        CharsetEncoder encoder = charset.newEncoder();

        return encoder.canEncode((String) value);
    }

    /**
     * If they have specified UTF-16 then decoder works out by looking at BOM
     * but if missing we have to make an educated guess otherwise just use
     * specified decoder
     */
    CharsetDecoder getCorrectDecoder(ByteBuffer inBuffer) {
        CharsetDecoder decoder;
        if (inBuffer.remaining() <= 2) {
            decoder = getTextEncodingCharSet().newDecoder();
            decoder.reset();
            return decoder;
        }

        if (getTextEncodingCharSet() == StandardCharsets.UTF_16) {
            if (inBuffer.getChar(0) == 0xfffe || inBuffer.getChar(0) == 0xfeff) {
                //Get the Specified Decoder
                decoder = getTextEncodingCharSet().newDecoder();
            } else {
                if (inBuffer.get(0) == 0) {
                    decoder = StandardCharsets.UTF_16BE.newDecoder();
                } else {
                    decoder = StandardCharsets.UTF_16LE.newDecoder();
                }
            }
        } else {
            decoder = getTextEncodingCharSet().newDecoder();
        }
        decoder.reset();
        return decoder;
    }

    /**
     * Peek into the buffer and try to determine the correct text encoding to use, starting with {@link #getTextEncodingCharSet()}
     * <p>
     * If {@link StandardCharsets#UTF_16} look at the first code point and make a guess, otherwise just return
     * {@link #getTextEncodingCharSet()}
     *
     * @param buffer the {@link Buffer} being parsed
     * @return a {@link Charset} to use for decoding text
     * @throws IllegalCharsetException if {@link #getTextEncodingCharSet()} throws this
     */
    Charset peekCorrectDecoder(Buffer buffer) {
        final Charset encodingCharSet = getTextEncodingCharSet();
        if (buffer.size() <= 2) {
            return encodingCharSet;
        }

        if (encodingCharSet == StandardCharsets.UTF_16) {
            final int firstCodePoint = getShort(buffer);  // doesn't move position
            if (firstCodePoint == 0xfffe || firstCodePoint == 0xfeff) {
                return StandardCharsets.UTF_16;
            } else {
                if (buffer.getByte(0) == 0) {
                    return StandardCharsets.UTF_16BE;
                } else {
                    return StandardCharsets.UTF_16LE;
                }
            }
        } else {
            return encodingCharSet;
        }
    }

    private int getShort(Buffer buffer) {
        return (buffer.getByte(0) & 0xff) << 8 | (buffer.getByte(1) & 0xff);
    }

    /**
     * Get the text encoding being used.
     * <p>
     * The text encoding is defined by the frame body that the text field belongs to.
     *
     * @return the text encoding charset
     * @throws IllegalCharsetException if the character encoding id is not found
     */
    protected Charset getTextEncodingCharSet() throws IllegalCharsetException {
        try {
            return TextEncoding.getInstanceOf().getCharsetForId(getBody().getTextEncoding());
        } catch (NoSuchElementException e) {
            throw new IllegalCharsetException("Bad Charset Id ", e);
        }
    }
}
