# OMIM annotation pipeline: generate annotations for RDO terms having OMIM ids
#
. /etc/profile
SERVER=`hostname -s | tr '[a-z]' '[A-Z]'`
APPHOME=/home/rgddata/pipelines/OmimPipeline

EMAIL_LIST=mtutaj@mcw.edu
if [ "$SERVER" == "REED" ]; then
  EMAIL_LIST=mtutaj@mcw.edu,jrsmith@mcw.edu,slaulederkind@mcw.edu
fi

$APPHOME/_run.sh -annotations -qc_thread_count=6

mailx -s "[$SERVER] Omim Annotation pipeline OK" $EMAIL_LIST < $APPHOME/logs/status_annot.log