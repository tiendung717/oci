package com.xmpp.oci;

import com.google.common.base.Supplier;

import java.io.InputStream;

public class MyKeySupplier implements Supplier<InputStream> {

    private InputStream is;
    public MyKeySupplier(InputStream inputStream) {
        this.is = inputStream;
    }
    @Override
    public InputStream get() {
        return is;
    }
}
