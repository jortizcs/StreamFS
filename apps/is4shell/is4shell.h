#include <stdio.h>
#include <string.h>
#include <curl/curl.h>
#include <stdlib.h>

#ifndef CJSON_LIB
#define CJSON_LIB true
#include "cJSON/cJSON.h"
#endif

#ifndef IS4_LIB
#define IS4_LIB true
#include "is4lib.h"
#endif

#ifndef LOCAL_LIB
#define LOCAL_LIB true
#include "locallib.h"
#endif

enum OPS{
	get,
	put,
	post,
	cr,		//current resource
	ch,		//current host
	chprt,		//change port
	pwr,		//print working resource
	pwh,		//print working host
	pwp, 		//print working port
	mkrsrc,
	enter,
	exit_,
	delete_,
	pipe,
	help
};

#define MAX_BUF_LENGTH 255
#define MAX_TOKENS 10
#define EXTRA_SPACE 10

char current_host[MAX_BUF_LENGTH];
int current_port=80;
char current_rsrc[MAX_BUF_LENGTH];

//token buffer
char* input_token_buffers[MAX_TOKENS];

void print_help(char*);
int parse_args(int , char**);
int getop(char *);
int handle_get(char *);
int handle_post(char*,cJSON*);
int handle_delete(char*);
void make_resource(char*);
void eval_get_params(char *);
void eval_put_params(char *);
void eval_post_params(char *);
void eval_del_params(char *);
void eval_cr_params(char *);
void eval_ch_params(char *);
void eval_mkrsrc_params(char *);
void eval_chprt_params(char *);
void clearTokBuf(char**);
void handle_piping(char*);
int pop_tokenArray(char* string, char*, char**);
char * resolveResource(char *, char *, size_t, int);
void delete_resource(char*);

inline int parse_args(int argc, char* argv[]){
	int i=1;
	if(argc >=3 && argc%2==1){
		while(i<argc){
			if(strcmp(argv[i], "--host")==0){
				strcpy(current_host, argv[i+1]);
				printf("host set: %s\n", current_host);
			} else if (strcmp(argv[i], "--port")==0) {
				current_port = atoi(argv[i+1]);
				printf("port set: %d\n", current_port);
			}
			i += 2;
		}
		return 0;
	} else {
		return -1;
	}
}

inline int getop(char * line){
	char* thisop;
	char line_cpy[MAX_BUF_LENGTH];
	strcpy(line_cpy, line);
	if(line != NULL){
		thisop = strtok(line, " ");
		if(thisop != NULL){
			if(strcmp(thisop, "get")==0){
				return get;
			} else if(strcmp(thisop, "put")==0){
				return put;
			} else if(strcmp(thisop, "post")==0){
				return post;
			} else if(strcmp(thisop, "cr") == 0 ){
				return cr;
			} else if(strcmp(thisop, "ch") == 0 ) {
				return ch;
			} else if(strcmp(thisop, "pwr") == 0) {
				return pwr;
			} else if(strcmp(thisop, "pwh") == 0 ){
				return pwh;
			} else if (strcmp(thisop, "pwp") ==0){
				return pwp;
			} else if(strcmp(thisop, "mkrsrc") == 0){
				return mkrsrc;
			} else if(strcmp(thisop, "chprt") ==0){
				return chprt;
			} else if(strcmp(thisop, "exit") == 0){
				return exit_;
			} else if(strcmp(thisop, "help") ==0){
				return help;
			} else if(strcmp(thisop, "mkrsrc") ==0){
				return mkrsrc;
			} else if(strcmp(thisop, "delete") ==0){
				return delete_;
			} else if(strpbrk(line_cpy, "|") != NULL){
				return pipe;
			}
		} else {
			return enter;
		}
	}
	return -1;
}

inline void clearTokBuf(char* buf[]){
	int i;
	for(i=0; i<MAX_TOKENS; i++)
		memset(buf[i], 0, MAX_BUF_LENGTH);
}

inline int pop_tokenArray(char* input_string, char* delims, char* buf[]){
	int i=0;
	char * thisTokenSlot; 
	char * token;
	char * string_cpy;
	size_t blocksize = (size_t)sizeof(char)*strlen(input_string)+EXTRA_SPACE;

	//printf("Input string: %s\n", input_string);

	string_cpy = (char*) malloc(blocksize);
	strcpy(string_cpy, input_string);
	token = strtok(string_cpy, delims);
	while(token != NULL && i<MAX_TOKENS){
		//printf("pop_tokenArray():token: %s\n", token);
		//printf("string_cpy=%s\n", string_cpy);
		thisTokenSlot = buf[i];
		memset(thisTokenSlot, 0, MAX_BUF_LENGTH);
		strcpy(thisTokenSlot, token);
		token = strtok(NULL, delims);
		i+=1;
	}
	free(string_cpy);
	return i--;
}

static size_t read_callback(void *ptr, size_t size, size_t nmemb, void *stream)
{
  size_t retcode;
 
  /* in real-world cases, this would probably get this data differently
     as this fread() stuff is exactly what the library already would do
     by default internally */ 
  retcode = fread(ptr, size, nmemb, stream);
 
  fprintf(stderr, "*** We read %d bytes from file\n", (int)retcode);
 
  return retcode;
}
