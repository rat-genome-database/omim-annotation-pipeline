# download MIM ids from OMIM and update them in RGD
#
. /etc/profile
SERVER=`hostname -s | tr '[a-z]' '[A-Z]'`
APPHOME=/home/rgddata/pipelines/omim-pipeline

EMAIL_LIST=mtutaj@mcw.edu
if [ "$SERVER" == "REED" ]; then
  EMAIL_LIST=rgd.devops@mcw.edu,jrsmith@mcw.edu
fi

$APPHOME/_run.sh

# sometimes summary.log contains a bunch of null characters (occasional bug in log4j2 lib)
# we remove them before sending the final email
tr -d '\000' < $APPHOME/logs/summary.log > $APPHOME/logs/summary_no_nulls.log
mailx -s "[$SERVER] OMIM pipeline OK!" $EMAIL_LIST < $APPHOME/logs/summary_no_nulls.log
