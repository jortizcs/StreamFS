#!/bin/bash
if [ $# -ne 3 ]; then
	echo "unsubscribe.sh <comma-delimited list of subscriber ids (no spaces)>";
else
	echo "curl -X POST http://$IS4_HOSTNAME:$IS4_PORT/is4/unsub -d '{\"name\":\"sub_removal\",\"SubIds\":[$1]}'\n"
	echo "{\"name\":\"sub_removal\",\"SubIds\":[$1]}" > removesubs.json
	curl -X POST http://$IS4_HOSTNAME:$IS4_PORT/is4/unsub -d @removesubs.json
	rm -f removesubs.json
fi
