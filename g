#!/bin/bash

sep=:
if [ "$WINDIR" ]; then
  sep=';'
fi  

if [ -e lib ]; then
  P=$( find lib -name '*.jar' | tr '\n' "$sep" )
  #echo $P
fi

cmd=$1; shift
if [ -e $cmd.groovy ]; then
  cmd=$cmd.groovy
fi  

#  windoze version - with semicolon
#
export CLASSPATH="$CLASSPATH$sep$P"

groovy $cmd "${@}"







