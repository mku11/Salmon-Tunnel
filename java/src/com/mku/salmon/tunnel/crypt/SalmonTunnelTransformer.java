package com.mku.salmon.tunnel.crypt;
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

import com.mku.convert.BitConverter;
import com.mku.salmon.Generator;
import com.mku.salmon.integrity.HMACSHA256Provider;
import com.mku.salmon.integrity.IHashProvider;
import com.mku.salmon.integrity.Integrity;
import com.mku.salmon.integrity.IntegrityException;
import com.mku.salmon.password.*;
import com.mku.salmon.streams.AesStream;
import com.mku.salmon.streams.EncryptionFormat;
import com.mku.salmon.streams.EncryptionMode;
import com.mku.salmon.transform.AesDefaultTransformer;
import com.mku.salmon.transform.ICTRTransformer;
import com.mku.salmon.tunnel.transform.TunnelTransformer;
import com.mku.streams.MemoryStream;

import java.io.IOException;

public class SalmonTunnelTransformer extends TunnelTransformer {
    private static final int POS_LENGTH = 8;
    private byte[] header;
    private byte[] encKey;
    private byte[] hashKey;

    private ICTRTransformer transformerEnc;
    private ICTRTransformer transformerDec;
    private final IHashProvider hmacProvider = new HMACSHA256Provider();
    private byte[] nonceEnc;
    private byte[] nonceDec;
    private byte[] magicBytes;
    private byte[] salt;
    private final String password;
    private int iterations = 64 * 1024;
    private long pos;
    public SalmonTunnelTransformer(String password, boolean remote) {
        super(remote);
        if (password == null || password.trim().length() == 0)
            throw new RuntimeException("Password cannot be empty");
        this.password = password;
    }

    public synchronized void initialize() throws IOException {
        if (isRemote())
            header = createHeader(password);
        else
            header = null;
        pos = 0;
    }

    @Override
    public int getHeaderLength() {
        return POS_LENGTH + Generator.HASH_RESULT_LENGTH;
    }

    private synchronized byte[] createHeader(String password) throws IOException {
        nonceEnc = Generator.getSecureRandomBytes(8);
        nonceDec = Generator.getSecureRandomBytes(8);

        magicBytes = Generator.getMagicBytes();
        byte version = Generator.getVersion();
        salt = Generator.getSecureRandomBytes(24);
        encKey = new byte[Generator.KEY_LENGTH];
        hashKey = new byte[Generator.HASH_KEY_LENGTH];
        byte[] combKey = Generator.getSecureRandomBytes(Generator.KEY_LENGTH + Generator.HASH_KEY_LENGTH);
        System.arraycopy(combKey, 0, encKey, 0, Generator.KEY_LENGTH);
        System.arraycopy(combKey, Generator.KEY_LENGTH, hashKey, 0, Generator.HASH_KEY_LENGTH);
        byte[] id = Generator.getSecureRandomBytes(16);

        byte[] masterKeyIv = Generator.getSecureRandomBytes(16);
        byte[] masterKey = Password.getMasterKey(password, salt, iterations, 32);
        MemoryStream ms = new MemoryStream();
        AesStream stream = new AesStream(masterKey, masterKeyIv, EncryptionMode.Encrypt, ms, EncryptionFormat.Generic);
        stream.write(encKey, 0, encKey.length);
        stream.write(hashKey, 0, hashKey.length);
        stream.write(id, 0, id.length);
        stream.flush();
        stream.close();
        ms.close();
        byte[] encData = ms.toArray();

        byte[] hashSignature = Integrity.calculateHash(hmacProvider, encData, 0, encData.length, hashKey, null);

        // construct the contents of the header
        MemoryStream ms2 = new MemoryStream();
        ms2.write(magicBytes, 0, magicBytes.length);
        ms2.write(new byte[]{version}, 0, 1);
        ms2.write(salt, 0, salt.length);
        ms2.write(BitConverter.toBytes(iterations, 4), 0, 4);
        ms2.write(masterKeyIv, 0, masterKeyIv.length);
        ms2.write(nonceEnc, 0, nonceEnc.length);
        ms2.write(nonceDec, 0, nonceDec.length);
        ms2.write(encData, 0, encData.length);
        ms2.write(hashSignature, 0, hashSignature.length);
        ms2.flush();
        ms2.setPosition(0);

        transformerEnc = new AesDefaultTransformer();
        transformerEnc.init(encKey, nonceEnc);
        transformerEnc.resetCounter();

        transformerDec = new AesDefaultTransformer();
        transformerDec.init(encKey, nonceDec);
        transformerDec.resetCounter();

        return ms2.toArray();
    }

    public synchronized void setConfiguration(byte[] header) throws IOException {
        this.header = header;
        magicBytes = new byte[Generator.MAGIC_LENGTH];
        salt = new byte[24];
        encKey = new byte[Generator.KEY_LENGTH];
        hashKey = new byte[Generator.HASH_KEY_LENGTH];

        MemoryStream ms = new MemoryStream(header);
        ms.read(magicBytes, 0, Generator.MAGIC_LENGTH);
        byte[] versionBytes = new byte[Generator.VERSION_LENGTH];
        ms.read(versionBytes, 0, Generator.VERSION_LENGTH);
        ms.read(salt, 0, 24);
        byte[] iterationsBytes = new byte[4];
        ms.read(iterationsBytes, 0, iterationsBytes.length);
        iterations = (int) BitConverter.toLong(iterationsBytes, 0, iterationsBytes.length);
        byte[] iv = new byte[16];
        ms.read(iv, 0, iv.length);

        // the nonces are reversed
        nonceDec = new byte[8];
        ms.read(nonceDec, 0, nonceDec.length);
        nonceEnc = new byte[8];
        ms.read(nonceEnc, 0, nonceEnc.length);

        byte[] encryptedData = new byte[Generator.KEY_LENGTH + Generator.HASH_KEY_LENGTH + 16];
        ms.read(encryptedData, 0, Generator.KEY_LENGTH + Generator.HASH_KEY_LENGTH + 16);
        byte[] hashSignature = new byte[Generator.HASH_RESULT_LENGTH];
        ms.read(hashSignature, 0, Generator.HASH_RESULT_LENGTH);
        ms.close();

        byte[] masterKey = Password.getMasterKey(password, salt, iterations, 32);
        MemoryStream ms2 = new MemoryStream(encryptedData);
        AesStream stream = new AesStream(masterKey, iv, EncryptionMode.Decrypt, ms2, EncryptionFormat.Generic);

        encKey = new byte[Generator.KEY_LENGTH];
        stream.read(encKey, 0, encKey.length);

        hashKey = new byte[Generator.HASH_KEY_LENGTH];
        stream.read(hashKey, 0, hashKey.length);

        byte[] id = new byte[16];
        stream.read(id, 0, id.length);

        byte[] hash = Integrity.calculateHash(hmacProvider, encryptedData, 0, encryptedData.length, hashKey, null);
        for (int i = 0; i < hashKey.length; i++)
            if (hashSignature[i] != hash[i])
                throw new IOException("Wrong tunnel password");

        stream.close();

        createSalmonTransformers();
    }

    private void createSalmonTransformers() {

        transformerEnc = new AesDefaultTransformer();
        transformerEnc.init(encKey, nonceEnc);
        transformerEnc.resetCounter();

        transformerDec = new AesDefaultTransformer();
        transformerDec.init(encKey, nonceDec);
        transformerDec.resetCounter();
    }

    public synchronized byte[] getConfiguration() {
        return header;
    }

    public synchronized byte[] transform(byte[] data, boolean fromServer) {
        if ((!isRemote() && fromServer) || (isRemote() && !fromServer))
            return encryptData(data);
        else
            return decryptData(data);
    }

    private byte[] encryptData(byte[] data) {
        transformerEnc.syncCounter(pos);
        // encrypt
        byte[] bytes = new byte[POS_LENGTH + Generator.HASH_RESULT_LENGTH + data.length ];
        transformerEnc.encryptData(data, 0, bytes, Generator.HASH_RESULT_LENGTH + POS_LENGTH, data.length);
        // apply integrity hash
        byte[] hash = hmacProvider.calc(hashKey, bytes, POS_LENGTH + Generator.HASH_RESULT_LENGTH, data.length);
        System.arraycopy(hash, 0, bytes, POS_LENGTH, Generator.HASH_RESULT_LENGTH);
        byte[] posBytes = BitConverter.toBytes(pos, POS_LENGTH);
        System.arraycopy(posBytes, 0, bytes, 0, POS_LENGTH);
        int len = data.length / 16 * 16;
        if (len < data.length)
            len += 16;
        pos += len;
        transformerEnc.syncCounter(pos);
        return bytes;
    }

    private byte[] decryptData(byte[] data) {
        // verify integrity
        byte[] hash = hmacProvider.calc(hashKey, data, POS_LENGTH + Generator.HASH_RESULT_LENGTH,
                data.length - POS_LENGTH - Generator.HASH_RESULT_LENGTH);
        for (int k = 0; k < hash.length; k++) {
            if (hash[k] != data[POS_LENGTH + k]) {
                throw new IntegrityException("Data corrupt or tampered");
            }
        }
        long dataPos = BitConverter.toLong(data, 0, POS_LENGTH);
        transformerDec.syncCounter(dataPos);

        // decrypt
        byte[] bytes = new byte[data.length - POS_LENGTH - Generator.HASH_RESULT_LENGTH];
        transformerDec.decryptData(data, POS_LENGTH + Generator.HASH_RESULT_LENGTH,
                bytes, 0, data.length - POS_LENGTH - Generator.HASH_RESULT_LENGTH);
        return bytes;
    }
}
