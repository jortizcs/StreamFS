#!/bin/bash
if [[ $# -gt 3 || $# -lt 2 ]]; then
	echo "subscribe.sh <comma-separated pub id list> <sub_host> [<sub_port>]"
elif [ $# -eq 2 ]; then
	echo "curl -X POST http://$IS4_HOSTNAME:$IS4_PORT/is4/sub -d '{\"streams\":[$1],\"url\":\"http://$2\"}'"
	echo "{\"url\":\"http://$2\",\"streams\":[$1]}" > sub.json
	curl -X POST http://$IS4_HOSTNAME:$IS4_PORT/is4/sub -d @sub.json
	rm -f sub.json
else
	echo "curl -X POST http://$IS4_HOSTNAME:$IS4_POST/is4/sub -d '{\"streams\":[$1],\"url\":\"http://$2:$3\"}'"
	echo "{\"url\":\"http://$2:$3\",\"streams\":[$1]}" > sub.json
	curl -X POST http://$IS4_HOSTNAME:$IS4_PORT/is4/sub -d @sub.json
	rm -f sub.json
fi
