package com.xmpp.oci;

import android.util.Log;

import com.oracle.bmc.http.DefaultConfigurator;

import org.glassfish.jersey.SslConfigurator;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.ws.rs.client.ClientBuilder;

public class TlsClientConfigurator extends DefaultConfigurator {

    @Override
    public void customizeBuilder(ClientBuilder builder) {
//        super.customizeBuilder(builder);

        SSLContext sslContext =
                SslConfigurator.newInstance(true)
                        .securityProtocol("TLSv1.2")
                        .createSSLContext();
        builder.sslContext(sslContext);
        setConnectorProvider(builder);
    }
}
