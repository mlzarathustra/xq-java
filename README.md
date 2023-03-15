
# XQ-Java 

## Execute scripts on a remote MarkLogic Server via XCC

A utility written in Java for use with the MarkLogic Database/Server. If you don&rsquo;t know what that is, please see https://www.marklogic.com/

Scripts can be in either XQuery or JavaScript. Default is XQuery. Adding `-js` anywhere in the command line will switch to JavaScript. The script can be either in a file or via `stdin`. 

A file named `xq.properties` as described below can be used to remember connection strings.

The easiest way to build is via Gradle: 

```
$ gradle uberJar
```
This generates `build/libs/xq-java-all.jar` which contains the XCC driver, so you can invoke it simply with `java -jar`

And if you run it without arguments (say from a bash prompt using the xq script) it gives help:

```

$ xq


  xq input [connect] [-js] [-stream]

    WHERE:
      input = either a file name or dash (-) for standard input
      connect (optional) = either
         * a literal connection string starting with xcc:// or
           xccs://
         * the suffix of a property name in xq.properties

    FUNCTION:

      Execute an XQuery or JavaScript input on the server found at
      the uri given either as "connect" on the command line, or in
      xq.properties.

      If "input" doesn't exist, .xq or .xqy suffix will be tried
      for XQuery, or .js for JavaScript.

      -js anywhere in the argument list will cause execution
      to be in JavaScript rather than XQuery, for example:

        echo 'fn.count(fn.doc())' | xq - -js

      -stream turns off memory cacheing, for large data sets.
      Try this if you get out of memory errors from Java.

    CONNECTION PROPERTIES

      The xq.properties Java Properties file is where connections
      can be kept, to be referred to by the specified alias.

      If xq.properties in the execution directory looks like this:

        uri = xcc://user:pass@host:port
        uri.qa = xccs://qa-user:qa-pass@qa-host:qa-port

      then
        xq count
      will execute count (or count.xqy) using the first URI, whereas
        xq count qa
      will use the second.

      A connection string given on the command line takes precedence.

        echo 'count(/)' | xq -

      will run from standard input

    --
    PASSWORD OCCLUSION

      You can use the 'occlude.groovy' script to occlude the
      password, in which case you would put it in a property
      prefixed with pw, e.g.

        uri.dev = xccs://user@host:1234
        pw.dev = 52460c5f3b3a55313043547a3d1d704e2072441668

      Note that the uri.dev value contains no password.
      This is enough to keep it from prying eyes, but the
      password can be revealed with 'reveal.groovy'

    --
    CERTIFICATE OPTIONS

      If you're using a keystore for a certificate, the path
      and password can be passed on the command line:

        -ksFilePath=<path to keystore>
        -ksPassword=<keystore password>

      or via the environment variables:

        export marklogic_keyStore_filePath=<path to keystore>
        export marklogic_keyStore_password=<keystore password>

      If both are defined, the command line options will
      override.

```
