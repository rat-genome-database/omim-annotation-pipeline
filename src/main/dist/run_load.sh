# download OMIM ids from OMIM and update them in RGD
#
. /etc/profile
SERVER=`hostname -s | tr '[a-z]' '[A-Z]'`
APPHOME=/home/rgddata/pipelines/OmimPipeline

EMAIL_LIST=mtutaj@mcw.edu
if [ "$SERVER" == "REED" ]; then
  EMAIL_LIST=rgd.developers@mcw.edu,jrsmith@mcw.edu
fi

$APPHOME/_run.sh -qc_thread_count=6

mailx -s "[$SERVER] OMIM pipeline OK!" $EMAIL_LIST < $APPHOME/logs/summary.log
