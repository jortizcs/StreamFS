cd $IS4HOME 
rm -f is4app.jar
rm -f is4/IS4DataStore.class
rm -f is4/exceptions/InvalidProtocolException.class
rm -f is4/exceptions/InvalidResourceException.class
rm -f is4/exceptions/InvalidTypeException.class
rm -f is4/exceptions/NameRegisteredException.class
rm -f is4/exceptions/NoMoreRegistrantsException.class
rm -f is4/JoinInfoTuple.class
rm -f is4/Registrar.class
rm -f is4/SubMngr.class
rm -f is4/Subscriber.class
rm -f is4/SubscriberTable.class
rm -f is4/SubscriberTable.class
rm -f is4/stats/SFSStatistics.class

cd lib

#local library files
rm -f local/rest/proxy/Proxy.class
rm -f local/rest/proxy/Proxy\$GetDataHandler.class
rm -f local/rest/proxy/Proxy\$KillHandler.class
rm -f local/rest/proxy/Proxy\$PutDataHandler.class
rm -f local/rest/proxy/Proxy\$DumpBufferTimerTask.class
rm -f local/rest/proxy/ProxyInternal.class
rm -f local/html/tags/HTMLSimpleTags.class
rm -f local/db/Is4Database.class
rm -f local/db/DBAbstractionLayer.class
rm -f local/db/CouchDBDriver.class
rm -f local/db/MySqlDriver.class
rm -f local/db/MongoDBDriver.class
rm -f local/json/validator/JSONSchemaValidator.class
rm -f local/json/javascript/Js2JSONUtils.class
rm -f local/rest/handlers/JoinHandler.class
rm -f local/rest/handlers/RootHandler.class
rm -f local/rest/handlers/StreamHandler.class
rm -f local/rest/handlers/PubHandler.class
rm -f local/rest/handlers/SubHandler.class
rm -f local/rest/handlers/MetadataHandler.class
rm -f local/rest/handlers/SubControlHandler.class
rm -f local/rest/handlers/SmapSourceHandler.class
rm -f local/rest/RESTServer.class
rm -f local/rest/RESTServer\$StreamInfoHandler.class
rm -f local/rest/RESTServer\$SubInfoHandler.class
rm -f local/metadata/MetadataMngr.class
rm -f local/metadata/BindStateMngr.class
rm -f local/metadata/Binding.class
rm -f local/metadata/context/ContextGraphEdge.class
rm -f local/metadata/context/ContextGraphNode.class
rm -f local/metadata/context/ContextMngr.class
rm -f local/metadata/context/MetadataGraph.class
rm -f local/metadata/context/MetadataGraph\$PathFinder.class
rm -f local/metadata/context/exceptions/InvalidContextEdgeFormatException.class
rm -f local/metadata/context/exceptions/InvalidContextNodeFormatException.class
rm -f local/rest/resources/Resource.class
rm -f local/rest/resources/LoadTreeResource.class
rm -f local/rest/resources/ContextResource.class
rm -f local/rest/resources/DevicesResource.class
rm -f local/rest/resources/DeviceInstanceResource.class
rm -f local/rest/resources/PublisherResource.class
rm -f local/rest/resources/GenericPublisherResource.class
rm -f local/rest/resources/DemuxResource.class
rm -f local/rest/resources/SubscriptionResource.class
rm -f local/rest/resources/SymlinkResource.class
rm -f local/rest/resources/TimeResource.class
rm -f local/rest/resources/AdminDataReposIndexesResource.class
rm -f local/rest/resources/AdminPropsReposIndexesResource.class
rm -f local/rest/resources/AllNodesResource.class
rm -f local/rest/resources/ModelResource.class
rm -f local/rest/resources/ModelGenericPublisherResource.class
rm -f local/rest/resources/ModelManagerResource.class
rm -f local/rest/resources/ResyncSmapStreams.class
rm -f local/rest/resources/InfoBusResource.class
rm -f local/rest/util/ResourceUtils.class
rm -f local/rest/smap/SmapConnector.class
rm -f local/rest/interfaces/Is4Resource.class
rm -f local/rest/resources/util/ResourceUtils.class

cd ../

rm -f runis4
rm -f is4app.jar

echo "Delete previous state";
rm -f .state/*.obj

echo "Deleting logs";
rm -f logs/*