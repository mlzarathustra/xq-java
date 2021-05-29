#!/bin/bash



if [ -e lib ]; then
  P=$( find lib -name '*.jar' | tr '\n' ';' )
  #echo $P
fi

#  windoze version - with semicolon
#
export CLASSPATH="$CLASSPATH;$P"

groovy $*

#  times out for some reason
#groovyclient $*





