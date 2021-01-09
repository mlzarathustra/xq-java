#!/bin/bash

D=$( dirname $0 )
J=$D/build/libs/xq-java-all.jar

(which cygpath>/dev/null) && J=$(cygpath -w $J)

java -jar $J "$@"

