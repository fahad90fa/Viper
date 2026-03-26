package com.shadowproxy.core.cert;
import io.netty.handler.ssl.SslContext;

import java.nio.file.Path;

public interface CertificateService {
    void initialize();

    SslContext createServerSslContext(String hostname);

    Path rootCertificatePath();
}
