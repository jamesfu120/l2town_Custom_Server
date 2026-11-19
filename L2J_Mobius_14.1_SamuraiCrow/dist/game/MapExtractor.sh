#!/bin/sh
java -Xmx8g -cp ./../libs/*: org.l2jmobius.tools.MapExtractor "$@"
