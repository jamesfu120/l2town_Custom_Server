#!/bin/sh

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$SCRIPT_DIR" || exit 1

if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
	JAVA="$JAVA_HOME/bin/java"
else
	JAVA=java
fi

exec "$JAVA" --enable-native-access=ALL-UNNAMED -Dfile.encoding=UTF-8 -jar libs/Dashboard.jar
