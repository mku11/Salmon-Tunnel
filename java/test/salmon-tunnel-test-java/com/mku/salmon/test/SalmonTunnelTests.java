package com.mku.salmon.test;

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

import com.mku.salmon.streams.AesStream;
import com.mku.salmon.streams.ProviderType;
import com.mku.salmon.tunnel.net.TunnelManager;
import com.mku.salmon.tunnel.net.TunnelOptions;
import com.mku.salmon.tunnel.transform.TunnelTransformer;
import com.mku.salmon.tunnel.crypt.SalmonTunnelTransformer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class SalmonTunnelTests {

    @BeforeAll
    static void beforeAll() {
        SalmonTunnelTestHelper.initialize();
        ProviderType providerType = ProviderType.Default;
        AesStream.setAesProviderType(providerType);
    }

    @AfterAll
    static void afterAll() {
        SalmonTunnelTestHelper.close();
    }

    @Test
    public void shouldTunnelReverseText() throws Exception {
        TunnelOptions localOptions = SalmonTunnelTestHelper.createLocalManagerOptions();
        localOptions.setTransformer(new SalmonTunnelTestHelper.ReverseTransformer(false));
        TunnelManager localManager = new TunnelManager(localOptions);
        localManager.start();

        TunnelOptions remoteOptions = SalmonTunnelTestHelper.createRemoteManagerOptions();
        remoteOptions.setTransformer(new SalmonTunnelTestHelper.ReverseTransformer(true));
        TunnelManager remoteManager = new TunnelManager(remoteOptions);
        remoteManager.start();

        Thread.sleep(2000);

        String[] msgs = new String[]{
                "this is a test that will be tested"
                 , "and another test that will also be tested"
        };
        byte[][] dataMsgs = SalmonTunnelTestHelper.getBytes(msgs);
        byte[][] receivedDataMsgs = SalmonTunnelTestHelper.sendAndReceiveMsgs("localhost", localOptions.getSourcePort(), dataMsgs,
                "localhost", remoteOptions.getTargetPort());
        String[] receivedMsgs = SalmonTunnelTestHelper.getStrings(receivedDataMsgs);
        assertArrayEquals(msgs, receivedMsgs);

        localManager.close();
        remoteManager.close();
    }


    @Test
    public void shouldTunnelEncryptedText() throws Exception {
        TunnelOptions localOptions = SalmonTunnelTestHelper.createLocalManagerOptions();
        localOptions.setTransformer(new SalmonTunnelTransformer(SalmonTunnelTestHelper.TEST_PASSWORD, false));
        TunnelManager localManager = new TunnelManager(localOptions);
        localManager.start();

        TunnelOptions remoteOptions = SalmonTunnelTestHelper.createRemoteManagerOptions();
        remoteOptions.setTransformer(new SalmonTunnelTransformer(SalmonTunnelTestHelper.TEST_PASSWORD, true));
        TunnelManager remoteManager = new TunnelManager(remoteOptions);
        remoteManager.start();

        Thread.sleep(2000);

        String[] msgs = new String[]{
                "simple test"
                ,"this is a test that will be tested"
                ,"and another test that will also be tested"
        };
        byte[][] dataMsgs = SalmonTunnelTestHelper.getBytes(msgs);
        byte[][] receivedDataMsgs = SalmonTunnelTestHelper.sendAndReceiveMsgs("localhost", localOptions.getSourcePort(), dataMsgs,
                "localhost", remoteOptions.getTargetPort());
        String[] receivedMsgs = SalmonTunnelTestHelper.getStrings(receivedDataMsgs);
        assertArrayEquals(msgs, receivedMsgs);

        // close local, reconnect, and resend
        localManager.close();
        localOptions = SalmonTunnelTestHelper.createLocalManagerOptions();
        localOptions.setTransformer(new SalmonTunnelTransformer(SalmonTunnelTestHelper.TEST_PASSWORD, false));
        localManager = new TunnelManager(localOptions);
        localManager.start();
        Thread.sleep(2000);
        receivedDataMsgs = SalmonTunnelTestHelper.sendAndReceiveMsgs("localhost", localOptions.getSourcePort(), dataMsgs,
                "localhost", remoteOptions.getTargetPort());
        receivedMsgs = SalmonTunnelTestHelper.getStrings(receivedDataMsgs);
        assertArrayEquals(msgs, receivedMsgs);

        localManager.close();
        remoteManager.close();
    }


    @Test
    public void shouldTunnelSalmonWS() throws Exception {
        TunnelTransformer localTransformer;
        localTransformer = new SalmonTunnelTransformer(SalmonTunnelTestHelper.TEST_PASSWORD, false);
//        localTransformer = new SalmonTunnelTestHelper.ReverseTransformer(false);
        TunnelOptions localOptions = new TunnelOptions();
        localOptions.setName("localManager");
        localOptions.setSourcePort(8081);
        localOptions.setHost("localhost");
        localOptions.setTargetPort(8082);
        localOptions.setVerbose(true);
        localOptions.setDebug(true);
        localOptions.setTransformer(localTransformer);
        TunnelManager localManager = new TunnelManager(localOptions);
        localManager.start();

        TunnelTransformer remoteTransformer;
        remoteTransformer = new SalmonTunnelTransformer(SalmonTunnelTestHelper.TEST_PASSWORD, true);
//        remoteTransformer = new SalmonTunnelTestHelper.ReverseTransformer(true);
        TunnelOptions remoteOptions = new TunnelOptions();
        remoteOptions.setName("remoteManager");
        remoteOptions.setSourcePort(8082);
        remoteOptions.setTargetPort(8080);
        remoteOptions.setVerbose(true);
        remoteOptions.setDebug(true);
        remoteOptions.setTransformer(remoteTransformer);
        TunnelManager remoteManager = new TunnelManager(remoteOptions);
        remoteManager.start();

        Thread.sleep(2000000);
    }

    @Test
    public void shouldTunnelVNC() throws Exception {
        TunnelTransformer localTransformer;
        localTransformer = new SalmonTunnelTransformer(SalmonTunnelTestHelper.TEST_PASSWORD, false);
//        localTransformer = new SalmonTunnelTestHelper.ReverseTransformer(false);
        TunnelOptions localOptions = new TunnelOptions();
        localOptions.setName("localManager");
        localOptions.setSourcePort(5901);
        localOptions.setHost("localhost");
        localOptions.setTargetPort(5902);
        localOptions.setBufferSize(1024);
        localOptions.setTransformer(localTransformer);
        TunnelManager localManager = new TunnelManager(localOptions);
        localManager.start();

        TunnelTransformer remoteTransformer;
        remoteTransformer = new SalmonTunnelTransformer(SalmonTunnelTestHelper.TEST_PASSWORD, true);
//        remoteTransformer = new SalmonTunnelTestHelper.ReverseTransformer(true);
        TunnelOptions remoteOptions = new TunnelOptions();
        remoteOptions.setName("remoteManager");
        remoteOptions.setSourcePort(5902);
        remoteOptions.setHost("192.168.1.50"); // this generally should not be set, but only for testing
        remoteOptions.setRemote(true); // we force remote for testing only
        remoteOptions.setTargetPort(5900);
        remoteOptions.setBufferSize(1024);
        remoteOptions.setTransformer(remoteTransformer);
        TunnelManager remoteManager = new TunnelManager(remoteOptions);
        remoteManager.start();

        Thread.sleep(2000000);
    }

}
