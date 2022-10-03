package com.linkedin.d2.xds.util;

import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.linkedin.d2.xds.util.SslConfig.*;


/**
 * A utility class is used to create server {@link SslContext} and client {@link SslContext}.
 */
public final class SslContextUtil {

    private static final Logger _log = LoggerFactory.getLogger(SslContextUtil.class);

    /**
     * Builds a client {@link SslContext} object from given key store and trust store parameters
     * @param keyStoreFile
     * @param keyStorePassword
     * @param keyStoreType
     * @param trustStoreFile
     * @param trustStorePassword
     * @return sslContext {@link SslContext}
     * @throws Exception
     */
    public static SslContext buildClientSslContext(File keyStoreFile, String keyStorePassword, String keyStoreType,
        File trustStoreFile, String trustStorePassword) throws Exception {
        KeyManagerFactory keyManagerFactory = getKeyManagerFactory(keyStoreFile, keyStorePassword, keyStoreType);
        TrustManagerFactory trustManagerFactory =  getTrustManagerFactory(trustStoreFile, trustStorePassword);
        SslContext sslContext = GrpcSslContexts.forClient().keyManager(keyManagerFactory)
            .trustManager(trustManagerFactory)
            .protocols(DEFAULT_SSL_PROTOCOLS)
            .ciphers(DEFAULT_CIPHER_SUITES)
            .build();
        return sslContext;
    }

    /**
     * A helper method which is used to generate KeyManagerFactory based on the given KeyStoreFile and KeyStorePassword.
     * @param keyStoreFile
     * @param keyStorePassword
     * @param keyStoreType
     * @return keyFactoryManager
     * @throws Exception when generates KeyFactoryManager
     */
    private static KeyManagerFactory getKeyManagerFactory(File keyStoreFile, String keyStorePassword, String keyStoreType) throws Exception {
        // Load key store
        final KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        keyStore.load(toInputStream(keyStoreFile), keyStorePassword.toCharArray());

        // Set key manager from key store
        final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(DEFAULT_ALGORITHM);
        keyManagerFactory.init(keyStore, keyStorePassword.toCharArray());
        return keyManagerFactory;
    }

    /**
     * A helper method which is used to generate TrustManagerFactory based on the given TrustStoreFile and TrustStorePassword.
     * @param trustStoreFile
     * @param trustStorePassword
     * @return trustManagerFactory
     * @throws Exception when generates TrustManagerFactory
     */
    private static TrustManagerFactory getTrustManagerFactory(File trustStoreFile, String trustStorePassword) throws Exception {
        // Load trust store
        final KeyStore trustStore = KeyStore.getInstance(JKS_STORE_TYPE_NAME);
        trustStore.load(toInputStream(trustStoreFile), trustStorePassword.toCharArray());

        // Set trust manager from trust store
        final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(DEFAULT_ALGORITHM);
        trustManagerFactory.init(trustStore);
        return trustManagerFactory;
    }

    /**
     * A helper method that converts a File to InputStream.
     * @param storeFile
     * @return inputStream
     * @throws IOException
     */
    private static InputStream toInputStream(File storeFile) throws IOException {
        byte[] data = FileUtils.readFileToByteArray(storeFile);
        return new ByteArrayInputStream(data);
    }

    private SslContextUtil() {
    }
}
