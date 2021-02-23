package xq;

import com.marklogic.xcc.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.System.out;
import static java.lang.System.err;

// for #SSL connection
// from https://docs.marklogic.com/guide/xcc/concepts#id_53408
import javax.net.ssl.SSLContext;
import com.marklogic.xcc.SecurityOptions;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
//import java.security.KeyManagementException;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
//


public class Main {

    public static final String usageFile = "usage.txt";
    public static final String NL = System.lineSeparator();

    String uriStr, fileName;
    boolean useJavaScript;
    ResourceBundle bundle = null;

    boolean verbose = true;
    boolean raw=false; // could add an option to skip utf-8

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

    //  For #SSL connection
    //
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
                return null;
            }
        } };
        SSLContext sslContext = SSLContext.getInstance("SSLv3");
        sslContext.init(null, trust, null);
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
            verbose = bundle.containsKey("verbose")? "true".equals(bundle.getString("verbose")) : false;
        }
        catch (Exception ignore) { }
        if (verbose) err.println("verbose was set true in the properties file.");
    }

    void processArgs(String[] args) throws Exception {

        List<String> nonOptArgs =
            Arrays.stream(args).filter( s-> !s.startsWith("-") )
            .collect(Collectors.toList());

        List<String> optArgs =
            Arrays.stream(args).filter( s-> s.startsWith("-") )
            .collect(Collectors.toList());


        //  Assign: useJavaScript, uriStr, fileName

        if (optArgs.contains("-js")) useJavaScript=true;

        if (optArgs.contains("-")) fileName="-";
        else {
            if (nonOptArgs.size() == 0) {
                usage();
            }
            fileName=nonOptArgs.get(0);
            nonOptArgs.remove(0);
        }

        for (int i=0; i<nonOptArgs.size(); ++i) { // avoid concurrent mod error
            String arg=nonOptArgs.get(i);
            if (arg.startsWith("xcc://") || arg.startsWith("xccs://")) {
                uriStr = arg;
                nonOptArgs.remove(i);
                break;
            }
        }

        if (uriStr == null) {
            String propNameSuffix = nonOptArgs.size() == 0 ? "" : nonOptArgs.get(0);
            String noXCC = "Connection string starting with xcc:// or xccs:// "+
                "not found in argument list, \n";

            //  get resource file xq.properties
            if (bundle == null) throw new Exception(noXCC+"and I cannot find xq.properties.");

            try {
                uriStr = bundle.getString(propNameSuffix.length()==0 ?
                    "uri": "uri."+propNameSuffix);
            }
            catch (Exception ex) {
                throw new Exception(
                    noXCC+"and I cannot find property uri."+propNameSuffix+" in xq.properties!");
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
            if (!f.exists()) {
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

        //
        // // //  the main action // // //

    void runQuery(String query) throws Exception {
        URI uri=new URI( uriStr.trim() );
        ContentSource cs;
        //  #SSL
        if (uriStr.startsWith("xccs://")) {
            cs = ContentSourceFactory.newContentSource(uri, newTrustOptions());
        }
        else {
            cs = ContentSourceFactory.newContentSource(uri);
        }
        Session session = cs.newSession();

        Request req = session.newAdhocQuery(query);

        if (useJavaScript) {
            RequestOptions options = new RequestOptions();
            options.setQueryLanguage("javascript");
            req.setOptions(options);
        }

        ResultSequence rs = session.submitRequest( req );

        if (raw) out.println(rs.asString());
        else {
            Writer outUtf8=new OutputStreamWriter(System.out,"utf-8");

            outUtf8.write(rs.asString());
            outUtf8.flush();
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
        return uri.replaceFirst(":[^/]*@",":*****@");
    }


    public static void main(String[] args) {
        Main m=new Main();
        try {
            m.run(args);
            System.exit(0);
        }
        catch (Exception ex) {
            out.println("\n---------------------\n" +
                "CAUGHT EXCEPTION running query against "+stripPassword(m.uriStr));

            out.println(ex);
            System.exit(-1);
        }
    }
}



