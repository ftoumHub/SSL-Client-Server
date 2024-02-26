package server;

import protocol.KnockKnockProtocol;

import javax.net.ssl.*;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Objects;

import static java.lang.ClassLoader.getSystemClassLoader;
import static java.util.Objects.requireNonNull;

public class SSLServer {

    public static void main(String[] args) {
        try {
            // Get the keystore
            System.setProperty("javax.net.debug", "all"); // permet de voir les logs de javax.net.ssl
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            String password = "abcdefg";
            InputStream inputStream = getSystemClassLoader().getResourceAsStream("server/certificate-server.p12");
            keyStore.load(inputStream, password.toCharArray());

            System.out.println("KeyStore loaded with server certificate!");

            // TrustManagerFactory
            String password2 = "aabbcc";
            KeyStore trustStore = KeyStore.getInstance("PKCS12");
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("PKIX", "SunJSSE");
            InputStream inputStream1 = getSystemClassLoader().getResourceAsStream("client/certificate-client.p12");
            trustStore.load(inputStream1, password2.toCharArray());

            System.out.println("TrustStore loaded with client certificate!");
            trustManagerFactory.init(trustStore);
            System.out.println("Initializing TrustManagerFactory");

            X509TrustManager x509TrustManager = instanciateX509TrustManager(trustManagerFactory);
            X509KeyManager x509KeyManager = instanciateX509KeyManager(keyStore, password);

            // set up the SSL Context
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(new KeyManager[]{x509KeyManager}, new TrustManager[]{x509TrustManager}, null);

            SSLServerSocketFactory serverSocketFactory = sslContext.getServerSocketFactory();
            SSLServerSocket serverSocket = (SSLServerSocket) serverSocketFactory.createServerSocket(8333);
            serverSocket.setNeedClientAuth(true);
            serverSocket.setEnabledProtocols(new String[]{"TLSv1.2"});
            SSLSocket socket = (SSLSocket) serverSocket.accept();

            // InputStream and OutputStream Stuff
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String inputLine, outputLine;

            // Initiate conversation with client
            KnockKnockProtocol kkp = new KnockKnockProtocol();
            outputLine = kkp.processInput(null);
            out.println(outputLine);

            while ((inputLine = in.readLine()) != null) {
                outputLine = kkp.processInput(inputLine);
                out.println(outputLine);
                if (outputLine.equals("Bye."))
                    break;
            }
        } catch (IOException |
                CertificateException |
                NoSuchAlgorithmException |
                UnrecoverableKeyException |
                NoSuchProviderException |
                KeyStoreException |
                KeyManagementException e){
            e.printStackTrace();
        }
    }

    private static X509KeyManager instanciateX509KeyManager(KeyStore keyStore, String password) throws NoSuchAlgorithmException, NoSuchProviderException, KeyStoreException, UnrecoverableKeyException {
        System.out.println("===> instanciateX509KeyManager");
        // KeyManagerFactory ()
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509", "SunJSSE");
        keyManagerFactory.init(keyStore, password.toCharArray());
        X509KeyManager x509KeyManager = null;
        for (KeyManager keyManager : keyManagerFactory.getKeyManagers()) {
            if (keyManager instanceof X509KeyManager) {
                x509KeyManager = (X509KeyManager) keyManager;
                break;
            }
        }

        requireNonNull(x509KeyManager);
        return x509KeyManager;
    }

    private static X509TrustManager instanciateX509TrustManager(TrustManagerFactory trustManagerFactory) {
        System.out.println("===> instanciateX509TrustManager");
        X509TrustManager x509TrustManager = null;
        for (TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
            if (trustManager instanceof X509TrustManager) {
                x509TrustManager = (X509TrustManager) trustManager;
                break;
            }
        }

        requireNonNull(x509TrustManager);
        return x509TrustManager;
    }
}
