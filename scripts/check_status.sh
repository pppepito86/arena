#!/bin/sh

wget -q -t 5 -O /dev/null arena.olimpiici.com:443

if [ $? -ne 0 ]
then
        date
        echo "The site is down. Restarting."
	killall java || true
	~/arena/arena.war > ~/arena/stdout 2> ~/arena/stderr &
fi


