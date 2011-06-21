#!/bin/bash


echo "curl -i -H \"Content-Type: application/json\" -X POST http://prmohan-desktop.ath.cx:5984/ostream/_temp_view -d @couchdbmap.json"
echo "couchdbmap.json: "
cat couchdbmap.json
#echo "curl -i -H \"Content-Type: application/json\" -X POST http://prmohan-desktop.ath.cx:5984/ostream/_temp_view -d @dumbquery.json"
#echo "dumbquery.json: "
#cat dumbquery.json
curl -i -H "Content-Type: application/json" -X POST http://prmohan-desktop.ath.cx:5984/ostream/_temp_view -d @couchdbmap.json #dumbquery.json
