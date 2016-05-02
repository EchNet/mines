#
# Set environment and aliases for building/testing.
# Depends on setting of ROOT and TOMCAT
#

[ ! "$ROOT" ] && (echo "\$ROOT not set"; exit 1)
[ ! "$TOMCAT" ] && (echo "\$TOMCAT not set"; exit 1)

export JAVA="c:/j2sdk1.4.2_08"
export PATH="$JAVA/bin;$JAVA/jre/bin;$PATH"
export SRC=$ROOT/java

export APPROOT=$TOMCAT/webapps/ROOT/mines
export CLASSES=$APPROOT

alias jc="$JAVA/bin/javac -d $CLASSES -classpath $CLASSES"

