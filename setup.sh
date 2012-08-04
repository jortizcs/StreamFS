export SFSHOME=${PWD}
export CLASSPATH=".:lib/DBPool-5.0.jar:lib/commons-logging-1.1.jar:lib/json-simple-1.1.1.jar:lib/log4j.jar:lib/memcached/:lib/mongo-2.8.0.jar:lib/mysql-connector-java-5.1.20-bin.jar:lib/simple/"
javac -classpath $CLASSPATH lib/memcached/com/meetup/memcached/*.java
