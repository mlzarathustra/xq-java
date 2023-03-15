package xq;

import com.marklogic.xcc.*;

import java.io.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;
import static java.util.stream.Collectors.*;
import java.nio.charset.Charset;
import java.util.regex.*;

import static java.lang.System.out;
import static java.lang.System.err;

// for #SSL connection
// from https://docs.marklogic.com/guide/xcc/concepts#id_53408
import javax.net.ssl.SSLContext;
import com.marklogic.xcc.SecurityOptions;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
//
// for KeyStore
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.KeyManager;


public class Main {

    public static final String usageFile = "usage.txt";
    public static final String NL = System.lineSeparator();

    String uriStr, fileName;
    boolean useJavaScript;
    boolean cacheResult = true; // ML default 

    // from xq.properties
    ResourceBundle bundle = null;
    String label=""; // given uri.x, "x" is the label
    boolean verbose = true;

    String charSet = "utf-8";

    static String envVar(String lbl) { return System.getenv(lbl); }    
    // keyStore
    static String ksFilePath = envVar("marklogic_keyStore_filePath");
    static String ksPassword = envVar("marklogic_keyStore_password");   
    
    public void usage() {
        try {
            BufferedReader in = new BufferedReader(
                new InputStreamReader(getClass()
                .getClassLoader()
                .getResourceAsStream(usageFile)));

            for (;;) {
                String line=in.readLine();
                if (line == null) break;
                out.println(line);
            }
        }
        catch (Exception ex) { ex.printStackTrace(); }
        System.exit(0);
    }

    public static String getText(File f) {
        try {
            return getText(new BufferedReader(new FileReader(f)));
        }
        catch (Exception ex) { return ""; }
    }

    public static String getText(InputStream inStream) {
        try {
            return getText(new BufferedReader(new InputStreamReader(inStream)));
        }
        catch (Exception ex) { return ""; }
    }

    public static String getText(BufferedReader in) throws IOException {
            StringBuilder sb = new StringBuilder();
            for (;;) {
                String line=in.readLine();
                if (line == null) return sb.toString();
                sb.append(line+NL);
            }
    }

    /* For #SSL connection

        KEYSTORE 

        While PKCS12 is the supposedly preferred keystore format,
        converting from JKS using keytool may not work.
         
        It doesn't seem to matter which you use in the call to 
        KeyStore.getInstance(), as apparently it auto-detects.

    */
    protected SecurityOptions newTrustOptions() throws Exception {
        TrustManager[] trust = new TrustManager[] { new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] x509Certificates, String s)
                throws CertificateException {
                // nothing to do
            }

            public void checkServerTrusted(X509Certificate[] x509Certificates, String s)
                throws CertificateException {
                // nothing to do
            }

            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        } };
        SSLContext sslContext = SSLContext.getInstance("TLSv1.2"); // was "SSLv3"

        KeyManager[] kms = null;
        if (ksFilePath != null && !"".equals(ksFilePath)) {
            // See note above about KEYSTORE
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(new FileInputStream(ksFilePath), null);

            KeyManagerFactory kmf = KeyManagerFactory.getInstance("PKIX");
            kmf.init(ks, ksPassword.toCharArray()); 
            kms = kmf.getKeyManagers();
        }
        sslContext.init(kms, trust, null);
        return new SecurityOptions(sslContext);
    }
    //

    void processBundle() {
        try {
            bundle=ResourceBundle.getBundle("xq");
        }
        catch (MissingResourceException ex) {
            try {
                bundle = new PropertyResourceBundle(
                    new FileReader("xq.properties")
                );
            }
            catch (Exception exx) {
                //err.println("Could not read file xq.properties.\n"+exx);
            }
            // if we can't find the file, they'll need to give the
            // connection string on the command line.
        }
        //  the verbose flag (if any) goes in the xq.properties file
        verbose = false;
        try {
            // it doesn't fail until here if the bundle is not found
            verbose = bundle.containsKey("verbose")? 
                "true".equals(bundle.getString("verbose")) : false;
        }
        catch (Exception ignore) { }
        if (verbose) err.println("verbose was set true in the properties file.");
    }

    public static String find(String[] ary, Predicate<String> p) {
        String rs = null;
        List<String> found =  Stream.of(ary).filter(p)
            .collect(toList());

        if (found.size() > 0) rs=found.get(0);    
        return rs;
    }    

    void processArgs(String[] args) throws Exception {

        List<String> nonOptArgs =
            Arrays.stream(args).filter( s-> !s.startsWith("-") )
            .collect(toList());

        List<String> optArgs =
            Arrays.stream(args).filter( s-> s.startsWith("-") )
            .collect(toList());


        //  Assign: useJavaScript, uriStr, fileName

        if (optArgs.contains("-js")) useJavaScript = true;
        if (optArgs.contains("-stream")) cacheResult = false;


        String ksFpArg = find(args, arg-> arg.matches( "-ksFilePath=.+" ));

        if (ksFpArg != null && !ksFpArg.equals("")) {
            String ksPwArg = find(args, arg-> arg.matches( "-ksPassword=.+" ));
            ksFilePath = ksFpArg.split("=")[1];
            ksPassword = ksPwArg == null ? "" : ksPwArg.split("=")[1];
        }

        // out.println("ksFilePath: "+ksFilePath+"; ksPassword: "+ksPassword);

        if (optArgs.contains("-")) fileName="-";
        else {
            if (nonOptArgs.size() == 0) {
                usage();
            }
            fileName=nonOptArgs.get(0);
            nonOptArgs.remove(0);
        }

        // loop explicitly to avoid concurrent mod error
        for (int i=0; i<nonOptArgs.size(); ++i) { 
            String arg=nonOptArgs.get(i);
            if (arg.startsWith("xcc://") || arg.startsWith("xccs://")) {
                uriStr = arg;
                nonOptArgs.remove(i);
                break;
            }
        }

        if (uriStr == null) {
            label = nonOptArgs.size() == 0 ? "" : nonOptArgs.get(0);
            String noXCC = "Connection string starting with xcc:// or xccs:// "+
                "not found in argument list, \n";

            //  get resource file xq.properties
            if (bundle == null) throw new Exception(
                noXCC+"and I cannot find xq.properties!");

            try {
                uriStr = bundle.getString(label.length()==0 ?
                    "uri": "uri."+label);
            }
            catch (Exception ex) {
                throw new Exception(
                    noXCC+"and I cannot find property uri."+label+
                    " in xq.properties!");
            }
        }

        if (verbose) err.println( "uriStr is " + uriStr );
    }

    String fetchQuery() {
        String query="";

        String[] xqExtensions = {"", ".xq", ".xqy"},
            jsExtensions = {"", ".js" };

        String[] extensions = useJavaScript ? jsExtensions : xqExtensions;

        if (fileName.equals("-")) {
          query=getText(System.in);
        }
        else {
            File f = null;
            for (String ext : extensions) {
                f = new File(fileName+ext);
                if (f.exists()) break;
            }
            if (f==null || !f.exists()) {
                err.println("Cannot find a file named "+f);
                usage();
            }

            query = getText(f);
        }
        if (verbose) {
            out.println("query:\n"+query);
            out.println("result:\n");
        }
        return query;
    }

    class URISpec {
        String scheme, user, pass, host; 
        int port;
    }


    URISpec parseUri(String cs) {
        Pattern p = Pattern.compile("(xccs?)://([^:]+):(.+)@([^:@]+):(\\d+)");
        Matcher m = p.matcher(cs);

        if (m.matches()) {
            URISpec rs = new URISpec();
            rs.scheme = m.group(1);
            rs.user = m.group(2);
            rs.pass = m.group(3);
            rs.host = m.group(4);
            rs.port = Integer.parseInt(m.group(5));
            return rs;
        }

        p = Pattern.compile("(xccs?)://([^:]+):?@([^@:]+):(\\d+)");
        m = p.matcher(cs);

        if (m.matches()) {
            URISpec rs = new URISpec();
            rs.scheme = m.group(1);
            rs.user = m.group(2);
            //  password not given
            rs.host = m.group(3);
            rs.port = Integer.parseInt(m.group(4));
            return rs;
        }

        return null;
    }


        //
        // // //  the main action // // //

    void runQuery(String query) throws Exception {
        URISpec v = parseUri( uriStr.trim() );

        if (v.pass == null) {
            String passProp = bundle.getString(
                label.length()==0 ? "pw": "pw."+label);
            if (passProp != null) v.pass = Occlude_J.reveal(passProp);
        }

        ContentSource cs;
        //  #SSL
        if (v.scheme.equals("xccs")) {
            cs = ContentSourceFactory.newContentSource(
                v.host, v.port, v.user, v.pass, null, 
                newTrustOptions()
            );
        }
        else {
            cs = ContentSourceFactory.newContentSource(
                v.host, v.port, v.user, v.pass, null
            );
        }
        Session session = cs.newSession();

        Request req = session.newAdhocQuery(query);

        RequestOptions options = new RequestOptions();
        if (useJavaScript) options.setQueryLanguage("javascript");
        options.setCacheResult(cacheResult);
        req.setOptions(options);

        ResultSequence rs = session.submitRequest( req );

        Writer outWriter=new OutputStreamWriter(System.out,Charset.forName(charSet));
        if (cacheResult) { //  the whole thing as one string 
            outWriter.write(rs.asString());
            outWriter.flush();
        }
        else {   //  stream 
            while (rs.hasNext()) {
                outWriter.write(rs.next().asString());
                outWriter.write("\n");
                outWriter.flush();
            }
        }

        session.close();
        out.println();
    }

    public void run(String[] args) throws Exception {
        processBundle();
        processArgs(args);
        String query = fetchQuery();
        runQuery(query);
    }

    public static String stripPassword(String uri) {
        if (uri == null) return "";
        return uri.replaceFirst("(xccs?://[^:]+):(.+)@([^:@]+):(\\d+)",
                "$1:*****@$3");
    }


    public static void main(String[] args) {
        Main m=new Main();
        try {
            m.run(args);
            System.exit(0);
        }
        catch (Exception ex) {
            out.println("\n---------------------\n" +
                "CAUGHT EXCEPTION running query against "+
                stripPassword(m.uriStr));

            out.println(ex);
            System.exit(-1);
        }
    }
}