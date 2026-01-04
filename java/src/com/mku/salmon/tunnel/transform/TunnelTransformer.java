package com.mku.salmon.tunnel.transform;
/*
MIT License

Copyright (c) 2025 Max Kas

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

import java.io.IOException;

public abstract class TunnelTransformer {
    private final boolean remote;

    public TunnelTransformer(boolean remote) {
        this.remote = remote;
    }

    public abstract void setConfiguration(byte[] header) throws IOException;
    public abstract byte[] getConfiguration();

    /**
     * Transform a data packet
     * @param data The data to transform
     * @param fromServer True if this is coming from
     * @return
     */
    public abstract byte[] transform(byte[] data, boolean fromServer);

    /**
     * Initialize the transformer
     * @throws IOException
     */
    public abstract void initialize() throws IOException;

    /**
     * Packet header length, usually to send additional data with each packet
     * @return The header length in bytes
     */
    public abstract int getHeaderLength();

    public boolean isRemote() {
        return remote;
    }

}
