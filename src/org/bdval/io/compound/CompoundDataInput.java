/*
 * Copyright (C) 2008-2009 Institute for Computational Biomedicine,
 *                         Weill Medical College of Cornell University
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.bdval.io.compound;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.DataInput;

/**
 * A DataOutput object that also supports writeObject.
 * TODO: Work on reverting back to thread safe version!
 * @author Kevin Dorff
 */
public class CompoundDataInput implements DataInput {

    /**
     * Used to log debug and informational messages.
     */
    private static final Log LOG = LogFactory.getLog(CompoundDataInput.class);

    /** The delegate DataInput object. */
    private RandomAccessFile dataInput;

    /**
     * Create a CompoundDataInput. This is created by CompoundFileReader.
     * @param input the current reader stream
     */
    CompoundDataInput(final RandomAccessFile input) {
        this.dataInput = input;
    }

    /**
     * {@inheritDoc}
     */
    public void readFully(final byte[] b) throws IOException {
        dataInput.readFully(b);
    }

    /**
     * {@inheritDoc}
     */
    public void readFully(final byte[] b, final int off, final int len) throws IOException {
        dataInput.readFully(b, off, len);
    }

    /**
     * {@inheritDoc}
     */
    public int skipBytes(final int n) throws IOException {
        return dataInput.skipBytes(n);
    }

    /**
     * {@inheritDoc}
     */
    public boolean readBoolean() throws IOException {
        return dataInput.readBoolean();
    }

    /**
     * {@inheritDoc}
     */
    public byte readByte() throws IOException {
        return dataInput.readByte();
    }

    /**
     * {@inheritDoc}
     */
    public int readUnsignedByte() throws IOException {
        return dataInput.readUnsignedByte();
    }

    /**
     * {@inheritDoc}
     */
    public short readShort() throws IOException {
        return dataInput.readShort();
    }

    /**
     * {@inheritDoc}
     */
    public int readUnsignedShort() throws IOException {
        return dataInput.readUnsignedShort();
    }

    /**
     * {@inheritDoc}
     */
    public char readChar() throws IOException {
        return dataInput.readChar();
    }

    /**
     * {@inheritDoc}
     */
    public int readInt() throws IOException {
        return dataInput.readInt();
    }

    /**
     * {@inheritDoc}
     */
    public long readLong() throws IOException {
        return dataInput.readLong();
    }

    /**
     * {@inheritDoc}
     */
    public float readFloat() throws IOException {
        return dataInput.readFloat();
    }

    /**
     * {@inheritDoc}
     */
    public double readDouble() throws IOException {
        return dataInput.readDouble();
    }

    /**
     * {@inheritDoc}
     */
    public String readLine() throws IOException {
        return dataInput.readLine();
    }

    /**
     * {@inheritDoc}
     */
    public String readUTF() throws IOException {
        return dataInput.readUTF();
    }

    /**
     * Read an object from the current stream position.
     * @return the object
     * @throws IOException error reading the object
     * @throws ClassNotFoundException error de-serializing the object
     */
    public Object readObject() throws IOException, ClassNotFoundException {
        final int size = dataInput.readInt();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Reading an object that should be " + (size + 4) + " bytes long");
        }
        final byte[] buf = new byte[size];
        dataInput.readFully(buf);
        final ByteArrayInputStream bis = new ByteArrayInputStream(buf);
        final ObjectInputStream ois = new ObjectInputStream(bis);
        final Object deserializedObject = ois.readObject();
        ois.close();
        return deserializedObject;
    }
}
