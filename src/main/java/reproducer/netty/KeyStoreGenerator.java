package reproducer.netty;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;

public class KeyStoreGenerator {

    public static final String KEY_ALIAS = "aliaskeys";

    public KeyStoreGenerator() {
        Security.addProvider(new BouncyCastleProvider());
    }

    public File generateKeyStore(String name, String keystoreType, String keyStorePassword, String privateKeyPassword) throws Exception {

        final KeyStore ks = KeyStore.getInstance(keystoreType);
        ks.load(null);

        final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");
        keyPairGenerator.initialize(2048, new SecureRandom());

        final KeyPair keyPair = keyPairGenerator.generateKeyPair();
        X509v3CertificateBuilder builder = new X509v3CertificateBuilder(

                //CN = Common Name, OU = Organisational Unit, O = Organisation, C = Country, ST = State
                //Needed for ClientDetail Integration Tests
                new X500Name("CN=" + name + ", OU=" + name + ", O=" + name + ", C=" + name + ", ST=" + name),
                BigInteger.valueOf(new SecureRandom().nextLong()),
                new Date(System.currentTimeMillis() - 10000),
                new Date(System.currentTimeMillis() + 24L * 3600 * 1000),
                new X500Name("CN=" + name + ", OU=" + name + ", O=" + name + ", C=" + name + ", ST=" + name),
                SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded()));


        AlgorithmIdentifier signatureAlgorithmId = new DefaultSignatureAlgorithmIdentifierFinder().find("SHA256withRSA");
        AlgorithmIdentifier digestAlgorithmId = new DefaultDigestAlgorithmIdentifierFinder().find(signatureAlgorithmId);

        byte[] encoded = keyPair.getPrivate().getEncoded();
        AsymmetricKeyParameter privateKey = PrivateKeyFactory.createKey(encoded);

        X509CertificateHolder holder = builder.build(new BcRSAContentSignerBuilder(signatureAlgorithmId, digestAlgorithmId).build(privateKey));
        org.bouncycastle.asn1.x509.Certificate structure = holder.toASN1Structure();

        InputStream is = new ByteArrayInputStream(structure.getEncoded());

        X509Certificate certificate = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(is);
        is.close();


        final X509Certificate[] certificateChain = {certificate};

        ks.setKeyEntry(KEY_ALIAS, keyPair.getPrivate(), privateKeyPassword.toCharArray(), certificateChain);

        File keyStoreFile = File.createTempFile(name, null);
        keyStoreFile.deleteOnExit();

        final FileOutputStream fos = new FileOutputStream(
                keyStoreFile);
        ks.store(fos, keyStorePassword.toCharArray());
        fos.close();
        return keyStoreFile;
    }
}