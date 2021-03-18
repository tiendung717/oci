package com.xmpp.oci;

import android.util.Log;

import com.oracle.bmc.http.ClientConfigurator;
import com.oracle.bmc.http.CrossTenancyRequestConfigurator;
import com.oracle.bmc.http.DefaultConfigurator;
import com.xmpp.oci.base.Tls12SocketFactory;

import org.glassfish.jersey.SslConfigurator;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

public class TlsClientConfigurator extends DefaultConfigurator {

    @Override
    public void customizeBuilder(ClientBuilder builder) {
        super.customizeBuilder(builder);
//
        SSLContext sslContext =
                SslConfigurator.newInstance(true)
                        .securityProtocol("TLSv1.2")
                        .createSSLContext();
        builder.sslContext(sslContext);

//        SSLContext sc = null;
//        try {
//            sc = SSLContext.getInstance("TLSv1.2");
//            sc.init(null, null, null);
//
//            SSLEngine sslEngine = sc.createSSLEngine();
//            builder.sslContext(sc);
//        } catch (NoSuchAlgorithmException | KeyManagementException e) {
//            e.printStackTrace();
//        }
//
//
        setConnectorProvider(builder);
    }

    @Override
    public void customizeClient(Client client) {
        super.customizeClient(client);
        SSLContext sslContext = client.getSslContext();

        Log.d("nt.dung", "SSL: " + sslContext.getProtocol());
    }
}
