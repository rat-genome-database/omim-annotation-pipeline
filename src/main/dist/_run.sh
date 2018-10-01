#!/usr/bin/env bash
# shell script to run Omim pipeline
. /etc/profile

APPNAME=OmimPipeline
APPDIR=/home/rgddata/pipelines/$APPNAME

cd $APPDIR
pwd
DB_OPTS="-Dspring.config=$APPDIR/../properties/default_db.xml"
LOG4J_OPTS="-Dlog4j.configurationFile=file://$APPDIR/properties/log4j2.xml"
export OMIM_PIPELINE_OPTS="$DB_OPTS $LOG4J_OPTS"

bin/$APPNAME "$@" 2>&1
