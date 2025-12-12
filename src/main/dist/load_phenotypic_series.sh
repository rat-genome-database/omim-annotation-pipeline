# update OMIM_PHENOTYPIC_SERIES table from OMIM data
#
. /etc/profile
SERVER=`hostname -s | tr '[a-z]' '[A-Z]'`
APPHOME=/home/rgddata/pipelines/omim-pipeline

EMAIL_LIST=mtutaj@mcw.edu
if [ "$SERVER" == "REED" ]; then
  EMAIL_LIST="rgd.devops@mcw.edu jrsmith@mcw.edu"
fi

$APPHOME/_run.sh --phenotypic_series

mailx -s "[$SERVER] OMIM phenotypic series OK!" $EMAIL_LIST < $APPHOME/logs/omim_ps_summary.log
