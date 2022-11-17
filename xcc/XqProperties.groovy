package xcc

class XqProperties {
    static def xqBundle=[:]
    static {
        // doesn't work from the command line.  (?!?)
        //   ResourceBundle.getBundle('xq')
        try {
            FileInputStream fis=new FileInputStream('xq.properties')
            xqBundle = new PropertyResourceBundle(fis)
        }
        catch (FileNotFoundException ex) {
            // ignore - user will need to specify conn str
        }
    }

    static Map uriMapFromLabel(String label) {
        label = (label?".$label":'')
        def uriStr = xqBundle.getString("uri$label").trim()
        if (!uriStr) {
            println 'uri undefined in properties file.'
            System.exit(-1)
        }
        def rs = parseUri(uriStr)
        if (!rs.pass) {
            rs.pass = Occlude_G.reveal(
                xqBundle.getString("pw$label").trim())
        }
        return rs
    }

    static Map parseUri(String cs) {
        def m=cs =~ /(xccs?|https?):\/\/([^:]+):(.+)@([^@:]+):(\d+)/

        if (m) {
            def m0=m[0]
            return [ scheme: m0[1], user: m0[2], pass: m0[3],
                host: m0[4], port: m0[5] ]
        }

        //  no password given
        m=cs =~ /(xccs?|https?):\/\/([^:]+):?@([^@:]+):(\d+)/
        if (m) {
            def m0=m[0]
            return [ scheme: m0[1], user: m0[2], pass: null,
                host: m0[3], port: m0[4] ]
        }
    }

}