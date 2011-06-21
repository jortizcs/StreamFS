#!/bin/bash
if [ $# -ne 1 ]; then
	echo "unpublish.sh <comma-delimited list of publisher ids (no spaces)>";
else
	echo "curl -X POST http://$IS4_HOSTNAME:$IS4_PORT/is4/unpub -d '{\"name\":\"pub_removal\",\"PubIds\":[$1]}'"
	echo "{\"name\":\"pub_removal\",\"PubIds\":[$1]}" > removepubs.json
	curl -X POST http://$IS4_HOSTNAME:$IS4_PORT/is4/unpub -d @removepubs.json
	rm -f removepubs.json
fi
