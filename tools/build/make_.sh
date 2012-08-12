cd $IS4HOME 
rm -f is4app.jar
jar cmfv manifest/is4main_manifest is4app.jar is4/IS4DataStore.class
jar ufv is4app.jar is4/exceptions/InvalidProtocolException.class
jar ufv is4app.jar is4/exceptions/InvalidResourceException.class
jar ufv is4app.jar is4/exceptions/InvalidTypeException.class
jar ufv is4app.jar is4/exceptions/NameRegisteredException.class
jar ufv is4app.jar is4/exceptions/NoMoreRegistrantsException.class
jar ufv is4app.jar is4/JoinInfoTuple.class
jar ufv is4app.jar is4/Registrar.class
jar ufv is4app.jar is4/SubMngr.class
jar ufv is4app.jar is4/stats/SFSStatistics.class

cd lib

#local library files
jar ufv ../is4app.jar local/rest/proxy/Proxy.class
jar ufv ../is4app.jar local/rest/proxy/Proxy\$GetDataHandler.class
jar ufv ../is4app.jar local/rest/proxy/Proxy\$KillHandler.class
jar ufv ../is4app.jar local/rest/proxy/ProxyInternal.class
jar ufv ../is4app.jar local/html/tags/HTMLSimpleTags.class
jar ufv ../is4app.jar local/db/Is4Database.class
jar ufv ../is4app.jar local/db/DBAbstractionLayer.class
jar ufv ../is4app.jar local/db/MySqlDriver.class
jar ufv ../is4app.jar local/db/MongoDBDriver.class
jar ufv ../is4app.jar local/json/validator/JSONSchemaValidator.class
jar ufv ../is4app.jar local/json/javascript/Js2JSONUtils.class
jar ufv ../is4app.jar local/rest/handlers/JoinHandler.class
jar ufv ../is4app.jar local/rest/handlers/RootHandler.class
jar ufv ../is4app.jar local/rest/handlers/StreamHandler.class
jar ufv ../is4app.jar local/rest/handlers/PubHandler.class
jar ufv ../is4app.jar local/rest/handlers/SubHandler.class
jar ufv ../is4app.jar local/rest/handlers/SubControlHandler.class
jar ufv ../is4app.jar local/rest/handlers/SmapSourceHandler.class
jar ufv ../is4app.jar local/rest/RESTServer.class
jar ufv ../is4app.jar local/rest/RESTServer\$StreamInfoHandler.class
jar ufv ../is4app.jar local/rest/RESTServer\$SubInfoHandler.class
jar ufv ../is4app.jar local/rest/RESTServer\$AsyncTask.class
jar ufv ../is4app.jar local/rest/RESTServer\$ShutdownProc.class
jar ufv ../is4app.jar local/metadata/MetadataMngr.class
jar ufv ../is4app.jar local/metadata/BindStateMngr.class
jar ufv ../is4app.jar local/metadata/Binding.class
jar ufv ../is4app.jar local/metadata/context/ContextGraphEdge.class
jar ufv ../is4app.jar local/metadata/context/ContextGraphNode.class
jar ufv ../is4app.jar local/metadata/context/exceptions/InvalidContextEdgeFormatException.class
jar ufv ../is4app.jar local/metadata/context/exceptions/InvalidContextNodeFormatException.class
jar ufv ../is4app.jar local/metadata/context/MetadataGraph.class
jar ufv ../is4app.jar local/metadata/context/MetadataGraph\$PathFinder.class
jar ufv ../is4app.jar local/rest/smap/SmapConnector.class
jar ufv ../is4app.jar local/rest/interfaces/Is4Resource.class
jar ufv ../is4app.jar local/rest/resources/Resource.class
jar ufv ../is4app.jar local/rest/resources/util/ResourceUtils.class
jar ufv ../is4app.jar local/rest/resources/PublisherResource.class
jar ufv ../is4app.jar local/rest/resources/GenericPublisherResource.class
jar ufv ../is4app.jar local/rest/resources/DemuxResource.class
jar ufv ../is4app.jar local/rest/resources/DemuxResource\$PostBulkDataTask.class
jar ufv ../is4app.jar local/rest/resources/DemuxResource2.class
jar ufv ../is4app.jar local/rest/resources/DemuxResource2\$PostBulkDataTask.class
jar ufv ../is4app.jar local/rest/resources/SubscriptionResource.class
jar ufv ../is4app.jar local/rest/resources/SymlinkResource.class
jar ufv ../is4app.jar local/rest/resources/TimeResource.class
jar ufv ../is4app.jar local/rest/resources/AdminDataReposIndexesResource.class
jar ufv ../is4app.jar local/rest/resources/AdminPropsReposIndexesResource.class
jar ufv ../is4app.jar local/rest/resources/AllNodesResource.class
jar ufv ../is4app.jar local/rest/resources/ModelResource.class
jar ufv ../is4app.jar local/rest/resources/ModelGenericPublisherResource.class
jar ufv ../is4app.jar local/rest/resources/ModelManagerResource.class
jar ufv ../is4app.jar local/rest/resources/ResyncSmapStreams.class
jar ufv ../is4app.jar local/rest/resources/InfoBusResource.class
jar ufv ../is4app.jar local/analytics/Node\$1.class
jar ufv ../is4app.jar local/analytics/Node\$AggTask.class
jar ufv ../is4app.jar local/analytics/Node.class
jar ufv ../is4app.jar local/analytics/ProcType.class
jar ufv ../is4app.jar local/analytics/Router.class
jar ufv ../is4app.jar local/analytics/Router\$1.class
jar ufv ../is4app.jar local/analytics/Router\$PathFinder.class
jar ufv ../is4app.jar local/analytics/RouterCommand\$CommandType.class
jar ufv ../is4app.jar local/analytics/RouterCommand.class
jar ufv ../is4app.jar local/analytics/lib/data/TSDataset.class
jar ufv ../is4app.jar local/analytics/lib/db/OlapMongoDBDriver.class
jar ufv ../is4app.jar local/analytics/lib/maths/Interpolator.class
jar ufv ../is4app.jar local/analytics/lib/maths/Interpolator\$1.class
jar ufv ../is4app.jar local/analytics/lib/maths/InterpType.class
jar ufv ../is4app.jar local/analytics/util/BiHashtable.class
jar ufv ../is4app.jar local/rest/resources/proc/ProcessManagerResource.class
jar ufv ../is4app.jar local/rest/resources/proc/ProcessManagerResource\$ConnectionCheckerTask.class
jar ufv ../is4app.jar local/rest/resources/proc/ProcessPublisherResource.class
jar ufv ../is4app.jar local/rest/resources/proc/ProcessResource.class
jar ufv ../is4app.jar local/rest/RESTServer\$ShutdownProc.class
jar ufv ../is4app.jar local/db/MongoDBDriver\$ShutdownHook.class

cd ../
jar tf is4app.jar


###############################
# Setup logging
###############################
cd config

rm -f logger.config

if [ "$1" = "verbose" ]; then
	echo "Setting up VERBOSE logger";
	ln -s logger.config.verbose logger.config
elif [ "$1" = "file" ]; then
	echo "Setting up VERBOSE FILE logger";
	ln -s logger.config.verbosefile logger.config
else 
	echo "Setting up QUIET logger";
	ln -s logger.config.quiet logger.config
fi

cd ..
################################


if [ "$2" = "perf" ]; then
	echo "Making executable w/perf"
	echo "$JAVAHOME -Xrunhprof:cpu=samples,depth=30,file=is4.hprof -classpath $CLASSPATH -Djava.util.logging.config.file=config/logger.config -jar is4app.jar $IS4_HOSTNAME $IS4_PORT" > runis4
else 
	echo "Making executable"
	echo "$JAVAHOME -classpath $CLASSPATH -Djava.util.logging.config.file=config/logger.config -jar is4app.jar $IS4_HOSTNAME $IS4_PORT" > runis4
fi
chmod 777 runis4
