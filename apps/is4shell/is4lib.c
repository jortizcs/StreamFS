#ifndef IS4_LIB
#define IS4_LIB true
#include "is4lib.h"
#endif

int getRsrcType(char * typeStr){
	if(typeStr != NULL && strlen(typeStr)>0){
		if(strcmp(typeStr, "default")==0){
			return default_;
		} else if (strcmp(typeStr, "publisher")==0){
			return publisher;
		} else if (strcmp(typeStr, "general")==0){
			return general;
		}
	}
	return -1;
};

cJSON* create_is4SimpleSmapRequestObj(char* device_name, char* smap_url, cJSON* buffer){
	if(buffer != NULL && device_name != NULL){
		cJSON_AddStringToObject(buffer, "deviceName", device_name);
		cJSON_AddStringToObject(buffer, "smap_url", smap_url);
	}
	return buffer;
}

cJSON* create_is4MultiSmapRequestObj(char* device_name, const char* smap_urls[], int array_size, cJSON* buffer){
	cJSON* json_array;
	if(buffer != NULL && device_name != NULL && array_size>0){
		json_array = cJSON_CreateStringArray(smap_urls, array_size);
		cJSON_AddStringToObject(buffer, "deviceName", device_name);
		cJSON_AddItemToObject(buffer, "smap_urls", json_array);
	}
	return buffer;
}

cJSON* create_is4SimpleAliasSmapRequestObj(char* device_name, char* smap_url, char* alias, cJSON* buffer){
	if(buffer != NULL && device_name != NULL && alias != NULL){
		cJSON_AddStringToObject(buffer, "deviceName", device_name);
		cJSON_AddStringToObject(buffer, "smap_url", smap_url);
		cJSON_AddStringToObject(buffer, "alias", alias);
	}
	return buffer;
}

cJSON* create_is4ComplexSmapRequestObj(char* device_name, const char* smap_urls[], const char* aliases[], int smap_array_size, int alias_array_size, cJSON* buffer){
	cJSON* json_smap_array;
	cJSON* json_alias_array;
	if(buffer != NULL && device_name != NULL && smap_array_size>0 && alias_array_size>0){
		json_smap_array = cJSON_CreateStringArray(smap_urls, smap_array_size);
		json_alias_array = cJSON_CreateStringArray(aliases, alias_array_size);
		cJSON_AddStringToObject(buffer, "deviceName", device_name);
		cJSON_AddItemToObject(buffer, "smap_urls", json_smap_array);
		cJSON_AddItemToObject(buffer, "aliases", json_alias_array);
	}
	return buffer;
}

/*
 * Create a JSON Object to create an generic publisher resource request:
 * {
 *	"operations":"create_resource",
 *	"resourceName":	...,
 *	"resourceType": "default"
 * }
 *
 */
cJSON* create_is4CreateDefaultRsrcReqObj(char* rsrcName, cJSON* buffer){
	if(buffer != NULL && rsrcName != NULL){
		printf("Creating default resource\n");
		cJSON_AddStringToObject(buffer, "operation", "create_resource");
		cJSON_AddStringToObject(buffer, "resourceName", rsrcName);
		cJSON_AddStringToObject(buffer, "resourceType", "default");
	}
	return buffer;
}

/*
 * Create a JSON Object to create an devices resource request:
 * {
 *	"operations":"create_resource",
 *	"resourceName":	"devices",
 *	"resourceType": "devices"
 * }
 *
 */
cJSON* create_is4CreateDevicesRsrcReqObj(cJSON* buffer){
	if(buffer != NULL ){
		printf("Creating devices resource\n");
		cJSON_AddStringToObject(buffer, "operation", "create_resource");
		cJSON_AddStringToObject(buffer, "resourceName", "devices");
		cJSON_AddStringToObject(buffer, "resourceType", "devices");
	}
	return buffer;
}

/*
 * Create a JSON Object to create an device resource request:
 * {
 *	"operations":"create_resource",
 *	"resourceName":	...,
 *	"resourceType": "device"
 * }
 *
 */
cJSON* create_is4CreateDeviceRsrcReqObj(char * resourceName, cJSON* buffer){
	if(buffer != NULL && resourceName != NULL){
		printf("Creating default resource\n");
		cJSON_AddStringToObject(buffer, "operation", "create_resource");
		cJSON_AddStringToObject(buffer, "resourceName", resourceName);
		cJSON_AddStringToObject(buffer, "deviceName", resourceName);
		cJSON_AddStringToObject(buffer, "resourceType", "device");
	}
	return buffer;
}

/*
 * Create a JSON Object to create an generic publisher resource request:
 * {
 *	"operations":"create_generic_resource",
 *	"resourceName":	...
 * }
 *
 *
 */
cJSON* create_is4CreateGenPubResourceRequestObj(char* rsrcName, cJSON* buffer){
	if(buffer != NULL && rsrcName != NULL){
		printf("Creating general publisher resource\n");
		cJSON_AddStringToObject(buffer, "operation", "create_generic_resource");
		cJSON_AddStringToObject(buffer, "resourceName", rsrcName);
	}
	return buffer;
}

/*
 * Create a JSON Object to create an sMAP Publisher resource request:
 * {
 *	"operations":"create_smap_publisher",
 *	"smap_url":	...,
 *	"alias":	...
 * }
 *
 * Alias is optional.  buffer must be allocated before calling this function.
 *
 */
cJSON* create_is4CreateSmapPubResourceRequestObj(char* smap_url, char* alias, cJSON* buffer){
	if(buffer != NULL && smap_url != NULL && strlen(smap_url)>0){
		printf("Creating publisher resource\n");
		cJSON_AddStringToObject(buffer, "operation", "create_smap_publisher");
		cJSON_AddStringToObject(buffer, "smap_url", smap_url);
		if(alias != NULL && strlen(alias)>0)
			cJSON_AddStringToObject(buffer, "alias", alias);
	}
	return buffer;
}

/*
 * Create a JSON Object to create an sMAP Publisher resource request:
 * {
 *	"operations":"create_smap_resource",
 *	"smap_urls":[...],
 *	"aliases":[...]]
 * }
 *
 * Aliases are optional, at least one smap url must be provided.  The num_smapurls and num_aliases parameters
 * must be greater than 0 and must reflect the number of elements in their associated arrays.  The buffer, smapurls_json,
 * and aliases_json must be allocated before making a to this function.
 *
 */
cJSON* create_is4CreateSmapPubResourcesRequestObj(const char* smap_urls[], const char* aliases[], size_t num_smapurls, size_t num_aliases, cJSON* buffer, 
							cJSON* smapurls_json, cJSON* aliases_json){
	if(buffer != NULL &&  smap_urls != NULL && num_smapurls>0){
		printf("Creating publisher resource\n");
		cJSON_AddStringToObject(buffer, "operation", "create_smap_resource");
		smapurls_json = cJSON_CreateStringArray(smap_urls, num_smapurls);
		cJSON_AddItemToObject(buffer, "smap_urls", smapurls_json);
		if(aliases != NULL && num_aliases>0){
			aliases_json = cJSON_CreateStringArray(aliases, num_aliases);
			cJSON_AddItemToObject(buffer, "aliases", aliases_json);
		}
	}
	return buffer;
}

/*
 * Create a JSON Object to create an generic publisher resource request:
 * {
 *	"operations":"create_resource",
 *	"resourceName":	...,
 * }
 *
 */
cJSON* create_is4CreateGenPubRsrcReqObj(char* rsrcName, cJSON* buffer){
	if(buffer != NULL && rsrcName != NULL){
		printf("Creating default resource\n");
		cJSON_AddStringToObject(buffer, "operation", "create_generic_publisher");
		cJSON_AddStringToObject(buffer, "resourceName", rsrcName);
	}
	return buffer;
}

/*
 * Create a JSON Object to creation a subscription;
 * {
 *	"s_uri":...,
 * 	"target":...
 * }
 *
 */
cJSON* create_is4SubReqObj(char* rsrcName, const char* url, cJSON* buffer){
	if(buffer != NULL && rsrcName != NULL && url != NULL){
		printf("WHAT Installing subscription; %s -> %s\n", rsrcName, url);
		cJSON_AddStringToObject(buffer, "s_uri", rsrcName);
		cJSON_AddStringToObject(buffer, "target", url);
	}
	return buffer;
}

/*
 * Create a JSON Object to creation a subscription;
 * {
 *	"operation":"create_symlink",
 * 	"name":...,
 	"[url|uri]":...
 * }
 *
 */
cJSON* create_is4CreateSymlinkReqObj(char* rsrcName, const char* uril, int local, cJSON* buffer){
	if(buffer != NULL && rsrcName != NULL && uril != NULL){
		printf("Creating symlink; %s -> %s\n", rsrcName, uril);
		cJSON_AddStringToObject(buffer, "operation", "create_symlink");
		cJSON_AddStringToObject(buffer, "name", rsrcName);
		if(local==1){
			cJSON_AddStringToObject(buffer, "uri", uril);
		} else{
			cJSON_AddStringToObject(buffer, "url", uril);
		}
	}
	return buffer;
}
