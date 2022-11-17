package xcc

import com.marklogic.xcc.*
import groovy.transform.Canonical

import static com.marklogic.xcc.ContentPermission.*

// for #SSL connection
// from https://docs.marklogic.com/guide/xcc/concepts?id=53408
import javax.net.ssl.SSLContext
import com.marklogic.xcc.SecurityOptions
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate
import java.security.cert.CertificateException


@Canonical
class XccConn {

    static XqProperties xqp = new XqProperties()

    URI uri
    Map params
    ContentSource cs
    Session session
    RequestOptions reqOptions
    ContentCreateOptions cntOptions
    long elapsed // after calling eachString or firstString, this will be set

    /**
        Construct from connection string,
        not using xq.properties.
        cannot use occluded password
    */
    XccConn(String u) { this(xqp.parseUri(u)) }

    /**
        Construct from label of property in xq.properties
        (actually named "uri.$label") which points to a
        connection string.

        Will use occluded password if it's given in
        xq.properties as "pw.$label"
    */
    static XccConn fromLabel(String label) {
        return new XccConn(xqp.uriMapFromLabel(label))
    }

    /**
        Construct from a map containing
        scheme, host, user, pass, and port.
    */
    XccConn(Map v) {
        params = v
        if (v.scheme == 'xccs') {
            cs = ContentSourceFactory.newContentSource(
                v.host, v.port as int, v.user, v.pass, null,
                newTrustOptions())
        }
        else {
            cs = ContentSourceFactory.newContentSource(
                v.host, v.port as int, v.user, v.pass, null
            )
        }

        session=cs.newSession()
        reqOptions = new RequestOptions()
        reqOptions.setCacheResult(false)

        cntOptions = new ContentCreateOptions()
        cntOptions.setFormatXml()
    }

    void setCollections(coll) { cntOptions.setCollections(coll) }

    //  in this format: ['hr-generalist,read,hr-manager,update']
    //  i.e. role, perm [,...]
    void setPermissions(permListStr) {
        List<ContentPermission> perms=[]
        def permList = permListStr.split(/,/).collect { it.trim() }
        for (int i = 0; i<permList.size()-1; i+=2) {
            ContentCapability cc
            switch(permList[i+1].toLowerCase()) {
                case 'read':         cc = READ; break;
                case 'update':       cc = UPDATE; break;
                case 'execute':      cc = EXECUTE; break;
                case 'insert':       cc = INSERT; break;
                case 'node_update:': cc = NODE_UPDATE; break;

                default: break;
            }
            ContentPermission cp = new ContentPermission(cc, permList[i])
            perms << cp
        }
        cntOptions.setPermissions(perms as ContentPermission[])
    }

    void setFormatJson() {
        cntOptions.setFormatJson();
    }
    void useJavascript() {
        reqOptions.setQueryLanguage('javascript')
    }
    void useXQuery() {
        reqOptions.setQueryLanguage('xquery')
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

    void eachString(String query, Closure action) {
        Request req = session.newAdhocQuery(query, reqOptions)
        def start = new Date().getTime()
        ResultSequence rs = session.submitRequest(req)
        elapsed = new Date().getTime() - start
        while (rs.hasNext()) {
            action.call(rs.next().getItem().asString())
        }
        rs.close()
    }
    String firstString(String query) {
        Request req = session.newAdhocQuery(query, reqOptions)
        def start = new Date().getTime()
        ResultSequence rs = session.submitRequest(req)
        elapsed = new Date().getTime() - start
        def result=''
        if  (rs.hasNext()) {
            result = rs.next().getItem().asString()
        }
        rs.close()
        result
    }

    List<String> allStrings(String query) {
        Request req = session.newAdhocQuery(query, reqOptions)
        def start = new Date().getTime()
        ResultSequence rs = session.submitRequest(req)
        elapsed = new Date().getTime() - start
        def result=new ArrayList<String>()
        while (rs.hasNext()) {
            result << rs.next().getItem().asString()
        }
        rs.close()
        result
    }

    def upload( content, docUri ) {
        try {
            session.insertContent(
                    ContentFactory.newContent(
                            docUri as String,
                            content as String, cntOptions))
        }
        catch (Exception ex) { ex.printStackTrace() }
    }


    def invoke(String modUri) { invoke(modUri,[:]) }

    //  invoke named module, using reqOptions
    //  returns a List of Strings
    //
    def invoke(String modUri, Map vars) {
        Request req = session.newModuleInvoke(null)
        req.setModuleUri(modUri)
        req.setOptions(reqOptions)
        if (vars) vars.keySet().each { k->
            //println "$k -> ${vars[k]}"
            req.setNewStringVariable(k,vars[k])
        }

        ResultSequence rs = session.submitRequest(req)

        def rval=rs.asStrings()
        rs.close()

        rval
    }
}
