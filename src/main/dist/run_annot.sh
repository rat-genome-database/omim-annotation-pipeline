# OMIM annotation pipeline: generate annotations for RDO terms with MIM ids
#
. /etc/profile
SERVER=`hostname -s | tr '[a-z]' '[A-Z]'`
APPHOME=/home/rgddata/pipelines/omim-pipeline

EMAIL_LIST=mtutaj@mcw.edu
if [ "$SERVER" == "REED" ]; then
  EMAIL_LIST=mtutaj@mcw.edu,jrsmith@mcw.edu,slaulederkind@mcw.edu
fi

$APPHOME/_run.sh -annotations

mailx -s "[$SERVER] Omim Annotation pipeline OK" $EMAIL_LIST < $APPHOME/logs/summary_annot.log
