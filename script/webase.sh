#!/bin/bash

DBUSER=${1}
PASSWD=${2}
IP=${3}
PORT=${4}

if [[ ! $DBUSER || ! $PASSWD || ! $IP || ! $PORT ]] ; then
    echo "Usage: bash webase.sh username password ip port"
    echo "eg: bash webase.sh root 123456 127.0.0.1 3306"
    exit 1
fi

echo -e "init start.... \n"

# connect to database then execute init
cat webase-sql.list | mysql --user=$DBUSER --password=$PASSWD --host=$IP --port=$PORT --default-character-set=utf8;

if [ "$?" == "0" ]; then
    echo -e "init success... \n"
else
    echo -e "init fail... \n"
fi

exit