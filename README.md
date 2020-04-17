
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

  xq input [connect] [-js]

    WHERE:
      input = either a file name or dash (-) for standard input
      connect (optional) = either
         * a literal connection string starting with xcc:// or
           xccs://
         * the suffix of a property name in xq.properties

    FUNCTION:

      Execute input according to uri found either as "connect" on the
      command line, or in xq.properties.

      If "input" doesn't exist, .xq or .xqy suffix will be tried
      for XQuery, or .js for JavaScript.

      if xq.properties in the execution directory looks like this:

        uri = xcc://user:pass@host:port
        uri.qa = xccs://qa-user:qa-pass@qa-host:qa-port

      then
        xq count
      will execute count (or count.xqy) using the first URI, whereas
        xq count qa
      will use the second.

      A connection string given on the command line takes precedence.

        echo 'count(/)' xq -

      will run from standard input

      -js anywhere in the argument list will cause execution
      to be in JavaScript rather than XQuery, for example:

        echo 'fn.count(fn.doc())' | xq - -js

```
