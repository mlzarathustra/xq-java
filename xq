#!/bin/bash

D=$( dirname $0 )

java -jar $D/build/libs/xq-java-all.jar $*

