#include <stdio.h>
#include <string.h>

#ifndef CJSON_LIB
#include "cJSON/cJSON.h"
#define CJSON_LIB true
#endif

enum resourceTypes{
	default_,
	publisher,
	general
};

cJSON* create_is4SimpleSmapRequestObj(char*, char*, cJSON*);
cJSON* create_is4MultiSmapRequestObj(char*, const char**, int, cJSON*);
cJSON* create_is4SimpleAliasSmapRequestObj(char*, char*, char*, cJSON*);
cJSON* create_is4ComplexSmapRequestObj(char*, const char**, const char**, int, int, cJSON*);

//create resource objects
cJSON* create_is4CreateDefaultRsrcReqObj(char*, cJSON*);
cJSON* create_is4CreateGenPubResourceRequestObj(char*, cJSON*);
cJSON* create_is4CreateSmapPubResourceRequestObj(char*, char*, cJSON*);
cJSON* create_is4CreateSmapPubResourcesRequestObj(const char**, const char**, size_t, size_t, cJSON*, cJSON*, cJSON*);
cJSON* create_is4CreateDevicesRsrcReqObj(cJSON*);
cJSON* create_is4CreateDeviceRsrcReqObj(char*, cJSON*);
cJSON* create_is4CreateGenPubRsrcReqObj(char*, cJSON*);
cJSON* create_is4SubReqObj(char*, const char*, cJSON*);
cJSON* create_is4CreateSymlinkReqObj(char*, const char*, int, cJSON*);
int getRsrcType(char*);
