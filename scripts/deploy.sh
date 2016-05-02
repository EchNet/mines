#
# Build the app and deploy it.  Depends on setup.sh
#

mkdir -p $APPROOT
mkdir -p $CLASSES

# Copy stuff.
cp -r $ROOT/www/* $APPROOT

# Compile applet.
jc $SRC/MinesApplet.java

