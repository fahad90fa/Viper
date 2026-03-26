package com.shadowproxy.core.cert;

import com.shadowproxy.config.AppConfig;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultCertificateService implements CertificateService {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultCertificateService.class);
    private static final String BC_PROVIDER = "BC";
    private final AppConfig appConfig;
    private final Map<String, SslContext> sslContextCache = new ConcurrentHashMap<>();
    private KeyPair caKeyPair;
    private X509Certificate caCertificate;

    public DefaultCertificateService(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    @Override
    public void initialize() {
        try {
            ensureProviderRegistered();
            Files.createDirectories(appConfig.certificateDirectory());
            loadOrCreateCertificateAuthority();
            LOG.info("Certificate authority ready: {}", rootCertificatePath());
        } catch (IOException e) {
            throw new IllegalStateException("Unable to initialize certificate directory", e);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to initialize certificate authority", e);
        }
    }

    @Override
    public SslContext createServerSslContext(String hostname) {
        String normalized = hostname.toLowerCase();
        return sslContextCache.computeIfAbsent(normalized, this::buildServerSslContext);
    }

    @Override
    public Path rootCertificatePath() {
        return appConfig.certificateDirectory().resolve("shadowproxy-ca-cert.pem");
    }

    private SslContext buildServerSslContext(String hostname) {
        try {
            GeneratedCertificate generatedCertificate = generateServerCertificate(hostname);
            return SslContextBuilder.forServer(
                    generatedCertificate.privateKey(),
                    generatedCertificate.certificate(),
                    caCertificate
            ).build();
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException("Unable to generate server certificate for " + hostname, e);
        }
    }

    private void loadOrCreateCertificateAuthority() throws IOException, GeneralSecurityException {
        Path certPath = rootCertificatePath();
        Path keyPath = appConfig.certificateDirectory().resolve("shadowproxy-ca-key.pem");
        if (Files.exists(certPath) && Files.exists(keyPath)) {
            this.caCertificate = readCertificate(certPath);
            PrivateKey privateKey = readPrivateKey(keyPath);
            this.caKeyPair = new KeyPair(caCertificate.getPublicKey(), privateKey);
            return;
        }
        this.caKeyPair = generateRsaKeyPair();
        this.caCertificate = generateCertificate(
                "CN=ShadowProxy Root CA,O=ShadowProxy,C=US",
                caKeyPair.getPublic(),
                "CN=ShadowProxy Root CA,O=ShadowProxy,C=US",
                caKeyPair.getPrivate(),
                true,
                null
        );
        writePem(certPath, caCertificate);
        writePem(keyPath, caKeyPair.getPrivate());
    }

    private GeneratedCertificate generateServerCertificate(String hostname) throws GeneralSecurityException, IOException {
        KeyPair keyPair = generateRsaKeyPair();
        X509Certificate serverCertificate = generateCertificate(
                "CN=" + hostname + ",O=ShadowProxy,C=US",
                keyPair.getPublic(),
                caCertificate.getSubjectX500Principal().getName(),
                caKeyPair.getPrivate(),
                false,
                hostname
        );
        return new GeneratedCertificate(serverCertificate, keyPair.getPrivate());
    }

    private X509Certificate generateCertificate(String subjectDn,
                                                PublicKey subjectPublicKey,
                                                String issuerDn,
                                                PrivateKey signingPrivateKey,
                                                boolean isCa,
                                                String dnsName) throws GeneralSecurityException, IOException {
        Instant now = Instant.now();
        Date notBefore = Date.from(now.minus(1, ChronoUnit.DAYS));
        Date notAfter = Date.from(now.plus(isCa ? 3650 : 90, ChronoUnit.DAYS));
        BigInteger serial = new BigInteger(128, new SecureRandom()).abs();
        X500Name subject = new X500Name(subjectDn);
        X500Name issuer = new X500Name(issuerDn);
        SubjectPublicKeyInfo subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(subjectPublicKey.getEncoded());
        X509v3CertificateBuilder builder = new X509v3CertificateBuilder(
                issuer,
                serial,
                notBefore,
                notAfter,
                subject,
                subjectPublicKeyInfo
        );
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(isCa));
        if (isCa) {
            builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
        } else {
            builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
            builder.addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth));
            builder.addExtension(Extension.subjectAlternativeName, false, new GeneralNames(new GeneralName(GeneralName.dNSName, dnsName)));
        }
        JcaX509ExtensionUtils extensionUtils = new JcaX509ExtensionUtils();
        ASN1Encodable subjectKeyIdentifier = extensionUtils.createSubjectKeyIdentifier(subjectPublicKey);
        builder.addExtension(Extension.subjectKeyIdentifier, false, subjectKeyIdentifier);
        final ContentSigner signer;
        try {
            signer = new JcaContentSignerBuilder("SHA256withRSA")
                    .setProvider(BC_PROVIDER)
                    .build(signingPrivateKey);
        } catch (org.bouncycastle.operator.OperatorCreationException e) {
            throw new GeneralSecurityException("Unable to create certificate signer", e);
        }
        X509CertificateHolder holder = builder.build(signer);
        return new JcaX509CertificateConverter().setProvider(BC_PROVIDER).getCertificate(holder);
    }

    private KeyPair generateRsaKeyPair() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private X509Certificate readCertificate(Path path) throws IOException, GeneralSecurityException {
        try (Reader reader = Files.newBufferedReader(path);
             PEMParser parser = new PEMParser(reader)) {
            Object obj = parser.readObject();
            if (!(obj instanceof X509CertificateHolder holder)) {
                throw new IllegalStateException("Invalid certificate PEM: " + path);
            }
            return new JcaX509CertificateConverter().setProvider(BC_PROVIDER).getCertificate(holder);
        }
    }

    private PrivateKey readPrivateKey(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path);
             PEMParser parser = new PEMParser(reader)) {
            Object obj = parser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(BC_PROVIDER);
            if (obj instanceof PEMKeyPair pemKeyPair) {
                return converter.getKeyPair(pemKeyPair).getPrivate();
            }
            if (obj instanceof PrivateKeyInfo privateKeyInfo) {
                return converter.getPrivateKey(privateKeyInfo);
            }
            throw new IllegalStateException("Invalid private key PEM: " + path);
        }
    }

    private void writePem(Path path, Object object) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
             JcaPEMWriter pemWriter = new JcaPEMWriter(writer)) {
            pemWriter.writeObject(object);
        }
    }

    private void ensureProviderRegistered() {
        if (Security.getProvider(BC_PROVIDER) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private record GeneratedCertificate(X509Certificate certificate, PrivateKey privateKey) {
    }
}
