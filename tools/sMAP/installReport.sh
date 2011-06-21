#!/bin/bash

if [ $1 = "acme" ]; then
	curl -i "http://buzzing.cs.berkeley.edu:8080/reporting/create" -d @$2
elif [ $1 = "veris" ]; then
	curl -i "http://local.cs.berkeley.edu:8001/reporting/create" -d @$2
else
	echo "installReport.sh [acme|veris] <reportrequest.json>"
fi
