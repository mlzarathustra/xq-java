package xq;

import static java.lang.System.out;
import java.util.regex.*;

public class URISpecTest {
    class URISpec {
        String scheme, user, pass, host; 
        int port;

        public String toString() {
            return "URISpec { scheme: "+scheme+"; user: "+user+
                "; pass: "+pass+"; host: "+host+"; port: "+port + "}";
        }
    }


    URISpec parseUrl(String cs) {
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
        return null;
    }

    static String t ="xccs://user:pass!@host:123";

    public static void main(String[] args) {
        URISpecTest obj = new URISpecTest();

        out.println("parseUrl("+t+") = "+obj.parseUrl(t));
    }
    
}
