package helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

public class TLSWrapper {
    public static SSLSocket returnTLSSocket(String cerPath, String serverAddress, int serverPort) {
        try {
            File certificateFile = new File(cerPath);
            Certificate certificate = CertificateFactory.getInstance("X.509").generateCertificate(new FileInputStream(certificateFile)); 
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setCertificateEntry("RichChatServer", certificate);

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);

            SSLSocketFactory sslSocketFactory =  sslContext.getSocketFactory();
            InetAddress serverInetAddress = InetAddress.getByName(serverAddress);
            SSLSocket socket = (SSLSocket) sslSocketFactory.createSocket(serverInetAddress, serverPort);
            return socket;
        }
        catch (CertificateException | KeyManagementException | KeyStoreException | NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static SSLServerSocket returnTLSServerSocket(String keystorePath, char[] password, int serverPort) {
        try {
            KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(new FileInputStream(keystorePath), password);

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keystore);

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("NewSunX509");
            keyManagerFactory.init(keystore, password);

            SSLContext context = SSLContext.getInstance("TLS");
            context.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

            SSLServerSocketFactory factory = context.getServerSocketFactory();

            SSLServerSocket sslServerSocket = (SSLServerSocket) factory.createServerSocket(serverPort);
            return sslServerSocket;
        }
        catch (CertificateException | KeyManagementException | UnrecoverableKeyException| KeyStoreException | NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
