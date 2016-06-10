#!/usr/bin/env bash

##############################################################################
##
##  make script for UN*X
##
##############################################################################

ASSEMBLED="assembled/"
JAVABIN="builder/bin/"
JAVASRC="builder/"

PrintInfo=true

info () {
    if $PrintInfo ; then
        echo "INFO: $*"
    fi
}

warn () {
    echo "WARNING:\n\t$*"
}

die () {
    echo "ERROR:\n\t$*"
    exit 1
}

for arg in $@ ; do
    if [ $arg = "q" -o $arg = "quiet" -o $arg = "-q" -o $arg = "-quiet" ] ; then
        PrintInfo=false
    elif [ $arg = "?" -o $arg = "help" -o $arg = "-?" -o $arg = "-help" ] ; then
        info "Assembled the txt files in *.hex and *.bmp format.\nUse 'q' parameter to start this script in quite mode."
        exit 0
    fi
done

if [ -n "$JAVA_HOME" ] ; then
    if [ -x "$JAVA_HOME/jre/sh/java" ] ; then
        JAVACMD="$JAVA_HOME/jre/sh/java"
        JAVAC="$JAVA_HOME/jre/sh/javac"
    else
        JAVACMD="$JAVA_HOME/bin/java"
        JAVAC="$JAVA_HOME/bin/javac"
    fi
    if [ ! -x "$JAVACMD" ] ; then
        die "JAVA_HOME is set to an invalid directory: $JAVA_HOME".
    fi
    if [ ! -x "$JAVAC" ] ; then
        die "No Javac found. Please install the JDK first!"
    fi
else
    JAVACMD="java"
    JAVAC="javac"
    which java >/dev/null 2>&1 || die "JAVA_HOME is not set and no 'java' command could be found in your PATH."
    which javac >/dev/null 2>&1 || die "No Javac found. Please install the JDK first!"
fi

if [ ! -d "$JAVABIN" ] ; then
    info "Making \"$JAVABIN\" directory"
    mkdir "$JAVABIN"
fi

if [ ! -d "$ASSEMBLED" ] ; then
    info "Making \"$ASSEMBLED\" directory"
    mkdir $ASSEMBLED
fi

if [ ! "$(ls -A "$JAVABIN")" ] ; then
    info "Compile java classes..."
    ${JAVAC} -d $JAVABIN $JAVASRC*.java || die "Java source compile error"
fi

if [ "$(ls -A "$ASSEMBLED")" ] ; then
    info "Remove old files..."
    rm -f $ASSEMBLED/*.*
fi

info "Assembling font hex..."
${JAVACMD} -cp $JAVABIN Main font.txt 8 hex "$ASSEMBLED/funscii-8.hex" || die "funscii-8 Assembling ERROR"
${JAVACMD} -cp $JAVABIN Main font.txt 8 hex "$ASSEMBLED/funscii-8-alt.hex" || die "funscii-8-alt Assembling ERROR"
${JAVACMD} -cp $JAVABIN Main font.txt 8 hex "$ASSEMBLED/funscii-8-fantasy.hex" || die "funscii-8-fantasy Assembling ERROR"
${JAVACMD} -cp $JAVABIN Main font.txt 8 hex "$ASSEMBLED/funscii-8-mcr.hex" || die "funscii-8-mcr Assembling ERROR"
${JAVACMD} -cp $JAVABIN Main font.txt 8 hex "$ASSEMBLED/funscii-8-thin.hex" || die "funscii-8-thin Assembling ERROR"
${JAVACMD} -cp $JAVABIN Main font.txt 16 hex "$ASSEMBLED/funscii-16.hex" || die "funscii-16 Assembling ERROR"
info "Assembling font bitmap..."
${JAVACMD} -cp $JAVABIN Main font.txt 8 bmp "$ASSEMBLED/funscii-8-coverage.bmp" 256 256 || die "funscii-8-coverage Assembling ERROR"
${JAVACMD} -cp $JAVABIN Main font.txt 16 bmp "$ASSEMBLED/funscii-16-coverage.bmp" 256 256 || die "funscii-16-coverage Assembling ERROR"
info "Done"
exit 0
