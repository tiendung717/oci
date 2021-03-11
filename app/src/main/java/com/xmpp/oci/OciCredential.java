package com.xmpp.oci;

import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.requests.GetNamespaceRequest;
import com.oracle.bmc.objectstorage.responses.GetNamespaceResponse;

public class OciCredential {
    private ObjectStorage service;
    private AuthenticationDetailsProvider provider;

    public OciCredential(ObjectStorage service, AuthenticationDetailsProvider provider) {
        this.service = service;
        this.provider = provider;
    }

    public ObjectStorage getService() {
        return service;
    }

    public AuthenticationDetailsProvider getProvider() {
        return provider;
    }

    public String getNamespace() {
        GetNamespaceResponse namespaceResponse = service.getNamespace(GetNamespaceRequest.builder().build());
        return namespaceResponse.getValue();
    }

    public String getTenantId() {
        return provider.getTenantId();
    }
}
