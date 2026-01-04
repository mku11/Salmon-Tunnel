#!/bin/bash

CURRDIR=$(pwd)
export SALMON_CLASS_PATH="$CURRDIR/*:$CURRDIR/libs/*:$CURRDIR/salmon/*"
export MAIN_CLASS=com.mku.salmon.tunnel.main.Main
java -Djava.library.path="." -cp $SALMON_CLASS_PATH $MAIN_CLASS $@

