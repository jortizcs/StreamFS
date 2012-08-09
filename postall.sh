for i in {1..1000}
do
    curl -i -X POST "http://energylens.sfsdev.is4server.com:8080/testpub?type=generic&pubid=3c7bb01d-1b14-4f2a-9c01-fdc9fd51a007" -d@datapt.json
done
