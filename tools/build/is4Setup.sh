############################
### IS4 Settings ###
############################

############################
# IS4 Core Libraries
############################
#IS4_LIB_PATH="/home/jortiz/is4/branches/is4_stable_v0.1/lib"
#IS4_LIB_PATH="/home/jortiz/is4/trunk/lib"
export IS4_LIB_PATH="${PWD}/lib"

export CLASSPATH="$CLASSPATH:$IS4_LIB_PATH"
export CLASSPATH="$CLASSPATH:$IS4_LIB_PATH/commons-beanutils.jar"
export CLASSPATH="$CLASSPATH:$IS4_LIB_PATH/commons-codec-1.3.jar"
export CLASSPATH="$CLASSPATH:$IS4_LIB_PATH/commons-collections.jar"
export CLASSPATH="$CLASSPATH:$IS4_LIB_PATH/commons-lang.jar"
export CLASSPATH="$CLASSPATH:$IS4_LIB_PATH/commons-logging-1.1.jar"
export CLASSPATH="$CLASSPATH:$IS4_LIB_PATH/commons-httpclient-3.0.jar"
export CLASSPATH="$CLASSPATH:$IS4_LIB_PATH/ezmorph-1.0.3.jar"
export CLASSPATH="$CLASSPATH:$IS4_LIB_PATH/httpclient-4.0-beta2.jar"
export CLASSPATH="$CLASSPATH:$IS4_LIB_PATH/httpcore-4.0-beta2.jar"
export CLASSPATH="$CLASSPATH:$IS4_LIB_PATH/json-lib-2.2.3-MODIFIED-jdk15.jar"
export CLASSPATH="$CLASSPATH:$IS4_LIB_PATH/mongo-2.5.3.jar"

############################
# Is4 Security Libraries
############################
#export CLASSPATH="$CLASSPATH:$IS4_LIB_PATH/is4"
#export CLASSPATH="$CLASSPATH:$IS4_LIB_PATH/shiro/all/target/shiro-all-1.0-incubating-SNAPSHOT.jar"

############################
# Logging libraries
############################
export CLASSPATH="$CLASSPATH:$IS4_LIB_PATH/slf4j-1.5.11/slf4j-api-1.5.11.jar"
export CLASSPATH="$CLASSPATH:$IS4_LIB_PATH/slf4j-1.5.11/slf4j-ext-1.5.11.jar"
export CLASSPATH="$CLASSPATH:$IS4_LIB_PATH/slf4j-1.5.11/slf4j-jdk14-1.5.11.jar"

#export CLASSPATH="$CLASSPATH:$IS4_LIB_PATH/slf4j-1.5.11/slf4j-jcl-1.5.11.jar"
#export CLASSPATH="$CLASSPATH:$IS4_LIB_PATH/slf4j-1.5.11/jcl-over-slf4j-1.5.11-sources.jar"
#export CLASSPATH="$CLASSPATH:$IS4_LIB_PATH/slf4j-1.5.11/jcl-over-slf4j-1.5.11.jar"
#export CLASSPATH="$CLASSPATH:$IS4_LIB_PATH/slf4j-1.5.11/jul-to-slf4j-1.5.11-sources.jar"
#export CLASSPATH="$CLASSPATH:$IS4_LIB_PATH/slf4j-1.5.11/jul-to-slf4j-1.5.11.jar"
#export CLASSPATH="$CLASSPATH:$IS4_LIB_PATH/slf4j-1.5.11/log4j-over-slf4j-1.5.11.jar"
#export CLASSPATH="$CLASSPATH:$IS4_LIB_PATH/slf4j-1.5.11/slf4j-log4j12-1.5.11.jar"
#export CLASSPATH="$CLASSPATH:$IS4_LIB_PATH/slf4j-1.5.11/slf4j-migrator-1.5.11.jar"
#export CLASSPATH="$CLASSPATH:$IS4_LIB_PATH/slf4j-1.5.11/slf4j-nop-1.5.11.jar"
#export CLASSPATH="$CLASSPATH:$IS4_LIB_PATH/slf4j-1.5.11/slf4j-simple-1.5.11.jar"

############################
# MySQL Connector Libraries
############################
export CLASSPATH="$CLASSPATH:$IS4_LIB_PATH/mysql-connector-java-5.0.8/mysql-connector-java-5.0.8-bin.jar"
export CLASSPATH="$CLASSPATH:$IS4_LIB_PATH/DBPool-5.0.jar"

############################
# Rhino (javascript)
############################
export CLASSPATH="$CLASSPATH:$IS4_LIB_PATH/rhino_js/js-14.jar"
export CLASSPATH="$CLASSPATH:$IS4_LIB_PATH/rhino_js/js.jar"

############################
# Java Data Structures Library
############################
export CLASSPATH="$CLASSPATH:$IS4_LIB_PATH/jdsl-2.1.1/lib/jdsl.jar"

###########################
# IS4 environment variables
###########################
#source $IS4_LIB_PATH/../setenv
export IS4HOME=${PWD}
export JAVAHOME=java
#export IS4_HOSTNAME="is4server.com"
#export IS4_PORT=8080
#export IS4_HOSTNAME="localhost"
export IS4_HOSTNAME="192.168.1.123"
export IS4_PORT=8081
export IS4_SUB_BUFFER="http://$IS4_HOSTNAME/buffer.php"

# Set up is4 tools
export IS4TOOLS_ROOT="$IS4HOME/tools/is4tools/is4v0.2"
alias allpubs="$IS4TOOLS_ROOT/allpubs.sh"
alias allsubs="$IS4TOOLS_ROOT/allsubs.sh"
alias subscribe="$IS4TOOLS_ROOT/subscribe.sh"
alias unpublish="$IS4TOOLS_ROOT/unpublish.sh"
alias unsubscribe="$IS4TOOLS_ROOT/unsubscribe.sh"
alias subctrl="$IS4TOOLS_ROOT/subctrl.sh"
alias subinfo="$IS4TOOLS_ROOT/subinfo.sh"
