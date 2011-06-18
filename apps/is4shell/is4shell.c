#ifndef IS4_SHELL
#define IS4_SHELL true
#include "is4shell.h"
#endif

int main(int argc, char* argv[]){
	char* exit_str = "exit\0";
	char line[MAX_BUF_LENGTH];
	char line_cpy[MAX_BUF_LENGTH];
	char* p;
	int exit = 0;
	int op=-1;
	int in_tok_idx=0;

	printf("IS4 Shell version 2.0\n");

	//init input_token_buffers
	while(in_tok_idx < MAX_TOKENS){
		input_token_buffers[in_tok_idx] = (char*) malloc(sizeof(char)*MAX_BUF_LENGTH);
		in_tok_idx += 1;
	}

	//testis4lib();
	memset(current_rsrc, 0, MAX_BUF_LENGTH);
	//strcpy(current_rsrc, "/is4\0");
	strcpy(current_rsrc, "/\0");

	parse_args(argc, argv);

	while(exit==0) {
		printf("> ");
		p=fgets (line, MAX_BUF_LENGTH, stdin);
		if(p!=NULL){
			size_t last = strlen (line) - 1;
			if (line[last] == '\n'){
				line[last] = '\0';
			} else {
				//trailing characters longer than BUFFER_LENGTH -- get rid of them
				fscanf (stdin, "%*[^\n]");
				/**** And also discard the newline... */
				(void) fgetc (stdin);
			}

			strcpy(line_cpy, line);
			op = getop(line_cpy);
			memset(line_cpy, 0, MAX_BUF_LENGTH);
			strcpy(line_cpy, line);
			switch(op){
				case get:
					//handle_get(".");
					eval_get_params(line_cpy);
					break;
				case cr:
					eval_cr_params(line_cpy);
					break;
				case ch:
					eval_ch_params(line_cpy);
					break;
				case pwr:
					printf("%s\n", current_rsrc);
					break;
				case pwh:
					printf("%s\n", current_host);
					break;
				case pwp:
					printf("%d\n", current_port);
					break;
				case chprt:
					eval_chprt_params(line_cpy);
					break;
				case post:
					eval_post_params(line_cpy);
					break;
				case help:
					print_help(line_cpy);
					break;
				case enter:
					break;
				case mkrsrc:
					make_resource(line_cpy);
					break;
				case delete_:
					delete_resource(line_cpy);
					break;
				case pipe:
					handle_piping(line_cpy);
					break;
				case exit_:
					break;
				default:
					printf("Unknown command\n");
					break;
			}

		 	if(strcmp(line, exit_str) == 0) exit=1;
		} else {
			printf("\n");
		}
		memset(line_cpy, 0, MAX_BUF_LENGTH);
	}

	//free all init input_token_buffers
	in_tok_idx=0;
	while(in_tok_idx < MAX_TOKENS){
		free(input_token_buffers[in_tok_idx]);
		in_tok_idx += 1;
	}
	printf("bye\n");
	return 0;
}

void eval_get_params(char * line){
	int i=0;
	int count=0;
	char* tok;
	char resPath[MAX_BUF_LENGTH];

	//clear and then populate tokens buffer
	clearTokBuf(input_token_buffers);
	pop_tokenArray(line, " ", input_token_buffers);

	tok = input_token_buffers[i];
	while(tok != NULL && count<1){
		if(strcmp(tok, "get")!=0){
			count +=1;
			resolveResource(line, resPath, MAX_BUF_LENGTH, -1);
			if(resPath != NULL){
				//printf("resolved path: %s\n", resPath);
				handle_get(resPath);
			}
		}

		i+=1;
		tok = input_token_buffers[i];
	}
}

void eval_put_parms(char * line) {
}

void eval_post_params(char * line){
	int swLength = 3;
	char startsWith[swLength];

	int numtoks = 0, idx = 1, rawdata_idx=-1;
	int rd_mem_size=0;

	char rsrc_input[MAX_BUF_LENGTH];
	char res_rsrc[MAX_BUF_LENGTH];
	char* op_param=NULL;
	char* rawdata=NULL;
	char* line_cpy=NULL;
	char* op=NULL;
	char* devname=NULL;
	char* smapurl_=NULL;
	char* filename=NULL;

	cJSON* jsonObj = NULL;

	enum argops{
		op_flag,
		file_flag,
		rawdata_flag
	};
	int param_op=-1;
	
	line_cpy = (char*) malloc(sizeof(char) *strlen(line));
	strcpy(line_cpy, line);

	memset(rsrc_input, 0, MAX_BUF_LENGTH);
	rsrc_input[0]='.';

	clearTokBuf(input_token_buffers);
	numtoks = pop_tokenArray(line_cpy, " ", input_token_buffers);


	printf("eval_post_params: MAX_BUF_LENGTH=%d\n", (int)MAX_BUF_LENGTH);
	//post [rsrc] [options]
	//Options
	//--op [mkpub|mkgenpub| --devname arg --smapurl arg
	//--file arg
	//--rawdata arg
	//a missing resource param will be interpretted as a post on the current resource "."
	while (idx < numtoks && numtoks>=3){
		//printf("idx=%d, Token=%s\n", idx, input_token_buffers[idx]);
		memset(startsWith, 0, swLength-1);
		if(strlen(input_token_buffers[idx])>=2){
			strncpy(startsWith, input_token_buffers[idx], swLength-1);
			startsWith[swLength-1]='\0';
		}
		//printf("startsWith: %s\n", startsWith);

		if(idx ==1 && strcmp(startsWith, "--")!=0){
			//path entered as second argument, resolve and set it
			memset(res_rsrc, 0, MAX_BUF_LENGTH);
			resolveResource(line_cpy, res_rsrc, MAX_BUF_LENGTH, -1);
			//printf("res_rsrc=%s, strlen=%d\n", res_rsrc, (int)strlen(res_rsrc));
			if(res_rsrc != NULL && strlen(res_rsrc)>0){
				memset(rsrc_input, 0, MAX_BUF_LENGTH);
				strcpy(rsrc_input, res_rsrc);
				//printf("Set resource input: %s,streln=%d\n", rsrc_input, (int)strlen(rsrc_input));
			}
			idx+=1;
		} else if(idx>=1 && strcmp(startsWith, "--")==0){
			op = input_token_buffers[idx];
			if(strcmp(op, "--op")==0 && op_param == NULL){
				//printf("op eval\n");
				param_op = op_flag;
				if(idx+1<numtoks){
					op_param = (char*)malloc(sizeof(char)*strlen(input_token_buffers[idx+1]));
					strcpy(op_param, input_token_buffers[idx+1]);
					idx += 2;
				} else {
					idx=numtoks;
				}
			}else if (strcmp(op,"--file") ==0 && filename==NULL){
				param_op = file_flag;
				if(idx+1<numtoks){
					filename = (char*)malloc(sizeof(char)*strlen(input_token_buffers[idx+1]));
					strcpy(filename, input_token_buffers[idx+1]);
					//printf("filename set: %s\n", filename);
					idx+=2;
				} else{
					idx = numtoks;
				}
			} else if(strcmp(op, "--rawdata")==0 && rawdata==NULL){
				//printf("rawdata eval\n");
				param_op = rawdata_flag;
				if(idx+1<numtoks){
					rawdata_idx = idx+1;
					rd_mem_size = (numtoks-idx)*2;
					rawdata = (char*) malloc(sizeof(char)*rd_mem_size);
					memset(rawdata, 0, sizeof(char)*rd_mem_size);
					while(rawdata_idx<numtoks){
						strcpy(&rawdata[strlen(rawdata)], input_token_buffers[rawdata_idx]);
						if(rawdata_idx<numtoks-1)
							strcpy(&rawdata[strlen(rawdata)], " ");
						rawdata_idx +=1;
					}
					if(strlen(rawdata)>0)
						//printf("rawdata=%s\n", rawdata);

					idx=numtoks;
				}
			} else if(strcmp(op, "--devname")==0 && idx+1<numtoks && devname == NULL){
				devname = (char*) malloc(sizeof(char)*strlen(input_token_buffers[idx+1]));
				strcpy(devname, input_token_buffers[idx+1]);
				//printf("devname set: %s\n", devname);
				idx +=2;
			} else if(strcmp(op, "--smapurl")==0 && idx+1<numtoks && smapurl_ == NULL){
				smapurl_ = (char*) malloc(sizeof(char)*strlen(input_token_buffers[idx+1]));
				strcpy(smapurl_, input_token_buffers[idx+1]);
				//printf("smapurl set: %s\n", smapurl_);
				idx+=2;
			}else{
				idx = numtoks;
			}
		} else{
			idx = numtoks;
			//idx +=1;
		}
	}

	//interpret the flags and associated parameters
	//printf("Checking param_op\n");
	switch(param_op){
		case op_flag:
				//printf("op_flag set\n");
				if(op_param != NULL && strcmp(op_param, "mkgenpub")==0 && 
					devname != NULL){
				} else if(op_param!= NULL && strcmp(op_param, "mksmappub")==0 &&
					devname != NULL && smapurl_ != NULL && rsrc_input != NULL){
					jsonObj = cJSON_CreateObject();
					create_is4SimpleSmapRequestObj(devname, smapurl_, jsonObj);
					handle_post(&rsrc_input[0], jsonObj);
					free(jsonObj);
					jsonObj = NULL;
				}
				//printf("done\n");
				break;
		case file_flag:
				break;
		case rawdata_flag:
				break;
	}

	if(line_cpy != NULL)	free(line_cpy);
	if(rawdata != NULL) 	free(rawdata);
	if(devname != NULL)	free(devname);
	if(smapurl_ != NULL)	free(smapurl_);
	if(filename != NULL)	free(filename);
	if(op_param != NULL)	free(op_param);
	if(jsonObj != NULL)	free(jsonObj);
}

void print_help(char * line){
	printf("Commands:\n");
	printf("\tget\t[resource]\n");
	printf("\tput\t[resource]\n");
	printf("\tpost\t[resource]\n");
	printf("\tdelete\t[resource]\n");
	printf("\tcr\t[resource]\tChange resource\n");
	printf("\tch\t[host]\tChange host\n");
	printf("\tchprt\t[port]\t\tChange port\n");
	printf("\tpwr\t\tprint working resource\n");
	printf("\tpwh\t\tprint working host\n");
	printf("\tpwp\t\tprint working port\n");
	printf("\tmkrsrc\t[resource]\n");
	printf("\thelp\t\tprint help\n");
}

void eval_del_params(char * line){
}

void eval_mkrsrc_params(char * line){
}

void eval_chprt_params(char * line){
	int i=0;
	int count=0;
	char* tok;
	int new_port = current_port;

	//clear and then populate tokens buffer
	clearTokBuf(input_token_buffers);
	pop_tokenArray(line, " ", input_token_buffers);

	tok = input_token_buffers[i];
	while(tok != NULL && count<1){
		if(strcmp(tok, "chprt")!=0){
			count +=1;
			new_port = atoi(tok);
			current_port = new_port;
			printf("Changed port: %d\n", new_port);
		}

		i+=1;
		tok = input_token_buffers[i];
	}
}

void eval_ch_params(char * line){
	int i=0;
	int count=0;
	char* tok;

	//clear and then populate tokens buffer
	clearTokBuf(input_token_buffers);
	pop_tokenArray(line, " ", input_token_buffers);

	tok = input_token_buffers[i];
	while(tok != NULL && count<1){
		if(strcmp(tok, "ch")!=0){
			count +=1;
			memset(current_host, 0, MAX_BUF_LENGTH);
			strcpy(current_host, tok);
			printf("Changed host: %s\n", current_host);
		}

		i+=1;
		tok = input_token_buffers[i];
	}
}

void eval_cr_params(char * line){
	char* tok;
	char*  path_tok;
	//const char delims[] = " ";
	char* new_path;
	int count =0, path_tok_cnt=0, path_strlen_cnt=0, i=0, num_tokens=1, new_path_size;

	//clear and then populate tokens buffer
	clearTokBuf(input_token_buffers);
	pop_tokenArray(line, " ", input_token_buffers);

	char cr_cpy[MAX_BUF_LENGTH]; memset(cr_cpy, 0, MAX_BUF_LENGTH);
	strcpy(cr_cpy, current_rsrc);

	//printf("Parsing line: %s\n", line);
	tok = input_token_buffers[i];
	while(tok != NULL && count < 2){
		//printf("line token: %s\n", tok);
		if(strcmp(tok, "cr") != 0){
			//check if the string is valud
			if(strcmp(tok, ".")==0){
			} else if ((strcmp(tok, "../")==0 || strcmp(tok, "..")==0) && strcmp(current_rsrc, "/")!=0 && strcmp(current_rsrc, "/")!=0) {
				//printf("Going back, tok=%s %d\n", tok, (int) (strcmp(tok, "../")==0 || strcmp(tok, "..")==0));
				path_tok = strtok(cr_cpy, "/");
				while(path_tok != NULL) {
					path_tok_cnt +=1;
					path_strlen_cnt += strlen(path_tok) + 1;
					path_tok = strtok(NULL, "/");
				}
				path_strlen_cnt += 1;
				memset(cr_cpy, 0, MAX_BUF_LENGTH); strcpy(cr_cpy, current_rsrc);
				path_tok = strtok(cr_cpy, "/");

				new_path_size = sizeof(char)*path_strlen_cnt;
				new_path = (char*) malloc(new_path_size);
				memset(new_path, 0, new_path_size);
				while(path_tok != NULL && num_tokens < path_tok_cnt){
					strcpy(&new_path[strlen(new_path)], "/");
					strcpy(&new_path[strlen(new_path)], path_tok);
					path_tok = strtok(NULL, "/");
					num_tokens +=1;
				}
				strcpy(&new_path[strlen(new_path)], "/");
				memset(current_rsrc, 0, sizeof(char)*strlen(current_rsrc));
				strcpy(current_rsrc, new_path);
				free(new_path);
			} else if(strlen(tok)>0 && tok[0]== '/'){
				//printf("Override\n");
				memset(current_rsrc, 0, sizeof(char)*strlen(current_rsrc));
				strcpy(current_rsrc, tok);
			} else if(strlen(tok)>0 && tok[0] != '/'){
				//printf("Add child\n");
				if(current_rsrc[strlen(current_rsrc)-1] != '/' && (strlen(current_rsrc)+1)<=MAX_BUF_LENGTH)
					strcpy(&current_rsrc[strlen(current_rsrc)], "/");
				if((strlen(current_rsrc)+strlen(tok)) <= MAX_BUF_LENGTH)
					strcpy(&current_rsrc[strlen(current_rsrc)], tok);
			}
		}

		count += 1;

		//get next token
		i+=1;
		tok = input_token_buffers[i];
	}
}

int handle_get(char * rsrc_path){

	CURL *curlh;
	CURLcode res;
	char* is4url;
	char portStr[10];
	FILE* tempfile;
	long filesize=0;
	char* readData=NULL;
	char* out;
	cJSON* jsonReadData;

	//if . set it to the current resource
	if(strcmp(rsrc_path, ".") == 0) rsrc_path=current_rsrc;

	//convert port to string
	itoa(current_port, portStr, 10);
	
	if(current_host != NULL){
		size_t numbytes = (sizeof(char)*strlen("http://")) + (sizeof(char)*sizeof(current_host)) + (sizeof(char)*sizeof(portStr)) + (sizeof(char)*sizeof(rsrc_path)) +1 ;
		//use malloc to populate the full url for this resource to call GET on it
		is4url = malloc(numbytes);
		memset(is4url, 0, numbytes);
		strcpy(is4url, "http://");
		strcpy(&is4url[(sizeof(char)*strlen(is4url))], current_host);
		strcpy(&is4url[(sizeof(char)*strlen(is4url))], ":");
		strcpy(&is4url[(sizeof(char)*strlen(is4url))], portStr);
		strcpy(&is4url[(sizeof(char)*strlen(is4url))], rsrc_path);
		is4url[(sizeof(char)*strlen(is4url))] = '\0';
		//printf("%s %d %d\n", is4url, (int) strlen(is4url), (int) sizeof(is4url));
		curlh = curl_easy_init();
		if(curlh){
			curl_easy_setopt(curlh, CURLOPT_URL, is4url);
			tempfile = fopen("temp.json","wr+");
			curl_easy_setopt(curlh, CURLOPT_WRITEDATA, tempfile);
			res = curl_easy_perform(curlh);
			curl_easy_cleanup(curlh);
			fclose(tempfile);
		}

		//display the results from tempfile
		tempfile = fopen("temp.json", "r");
		fseek(tempfile, 0L, SEEK_END);
		filesize = ftell(tempfile);
		fseek(tempfile, 0L, SEEK_SET);
		
		readData = (char*)malloc(sizeof(char)*filesize + 5);
		memset(readData, 0, sizeof(char)*filesize + 5);
		fgets ( readData, filesize+1, tempfile );
		jsonReadData=cJSON_Parse(readData);
		out = cJSON_Print(jsonReadData);
		//printf("%s", readData);
		printf("%s", out);
		free(out);
		cJSON_Delete(jsonReadData);

		fclose(tempfile);
		remove("temp.json");
		
		printf("\n");
		free(readData);
		free(is4url);
	}

	return 0;
}

int handle_delete(char *rsrc_path){
	CURL *curlh;
	CURLcode res;
	char* is4url;
	char portStr[10];

	//convert port to string
	itoa(current_port, portStr, 10);
	
	if(current_host != NULL){
		size_t numbytes = (sizeof(char)*strlen("http://")) + (sizeof(char)*sizeof(current_host)) + (sizeof(char)*sizeof(portStr)) + (sizeof(char)*sizeof(rsrc_path));
		//use malloc to populate the full url for this resource to call GET on it
		is4url = (char*)malloc(numbytes);
		memset(is4url, 0, numbytes);
		strcpy(is4url, "http://");
		strcpy(&is4url[(sizeof(char)*strlen(is4url))], current_host);
		strcpy(&is4url[(sizeof(char)*strlen(is4url))], ":");
		strcpy(&is4url[(sizeof(char)*strlen(is4url))], portStr);
		strcpy(&is4url[(sizeof(char)*strlen(is4url))], rsrc_path);
		printf("%s %d %d\n", is4url, (int) strlen(is4url), (int) sizeof(is4url));
		curlh = curl_easy_init();
		if(curlh){
			curl_easy_setopt(curlh, CURLOPT_CUSTOMREQUEST, "DELETE");
			curl_easy_setopt(curlh, CURLOPT_URL, is4url);
			res = curl_easy_perform(curlh);
			curl_easy_cleanup(curlh);
		}
		printf("\n");
		free(is4url);
	}
	return 0;
}

int handle_post(char * rsrc_path, cJSON* jsonObj){

	CURL *curlh;
	CURLcode res;
	char* is4url;
	char portStr[10];
	char* jsonObjStr=NULL;

	FILE* tempfile;
	size_t jsonObjStrSize;


	printf("posting to: %s\n", rsrc_path);

	tempfile = fopen("temp.json","wr+");
	jsonObjStr = cJSON_Print(jsonObj);
	printf("object string: %s\n", jsonObjStr);
	jsonObjStrSize = sizeof(char)*strlen(jsonObjStr);
	if(tempfile != NULL && jsonObjStr != NULL){
		printf("writing to file\n");
		fwrite(jsonObjStr,sizeof(char), strlen(jsonObjStr), tempfile);
		fclose(tempfile);
	}
	free(jsonObjStr);

	tempfile = fopen("temp.json", "rb");

	//if . set it to the current resource
	if(strcmp(rsrc_path, ".") == 0) rsrc_path=current_rsrc;

	//convert port to string
	itoa(current_port, portStr, 10);
	
	if(current_host != NULL){
		size_t numbytes = (sizeof(char)*strlen("http://")) + (sizeof(char)*sizeof(current_host)) + (sizeof(char)*sizeof(portStr)) + (sizeof(char)*sizeof(rsrc_path));
		//use malloc to populate the full url for this resource to call GET on it
		is4url = malloc(numbytes);
		memset(is4url, 0, numbytes);
		strcpy(is4url, "http://");
		strcpy(&is4url[(sizeof(char)*strlen(is4url))], current_host);
		strcpy(&is4url[(sizeof(char)*strlen(is4url))], ":");
		strcpy(&is4url[(sizeof(char)*strlen(is4url))], portStr);
		strcpy(&is4url[(sizeof(char)*strlen(is4url))], rsrc_path);
		printf("%s %d %d\n", is4url, (int) strlen(is4url), (int) sizeof(is4url));
		curlh = curl_easy_init();
		if(curlh){
    			curl_easy_setopt(curlh, CURLOPT_READFUNCTION, read_callback);
			curl_easy_setopt(curlh, CURLOPT_UPLOAD, 1L);
			curl_easy_setopt(curlh, CURLOPT_PUT, 1L);
			curl_easy_setopt(curlh, CURLOPT_URL, is4url);
			curl_easy_setopt(curlh, CURLOPT_READDATA, tempfile);
			curl_easy_setopt(curlh, CURLOPT_INFILESIZE_LARGE,(curl_off_t) jsonObjStrSize);
			res = curl_easy_perform(curlh);
			/*if(res != 0)
				printf("POST Error: %d\n", res);*/
			curl_easy_cleanup(curlh);
		}
		printf("\n");
		free(is4url);
	}
	fclose(tempfile);
	remove("temp.json");
	return 0;
}

char * resolveResource(char * line, char * resBuf, size_t bufsize, int tokenIndicator){
	char* tok;
	char*  path_tok;
	char* new_path;
	int count =0, path_tok_cnt=0, path_strlen_cnt=0, i=1, num_tokens=1, new_path_size;
	int cpy_size = bufsize, lineTokenCnt=0, tok_idx=0;

	char* local_token_buffers[MAX_TOKENS];
	//init local_token_buffers
	while(tok_idx < MAX_TOKENS){
		local_token_buffers[tok_idx] = (char*) malloc(sizeof(char)*MAX_BUF_LENGTH);
		memset(local_token_buffers[tok_idx], 0, sizeof(char)*MAX_BUF_LENGTH);
		tok_idx += 1;
	}

	if(tokenIndicator>=0)
		i = tokenIndicator;

	//clear and then populate tokens buffer
	clearTokBuf(local_token_buffers);
	lineTokenCnt = pop_tokenArray(line, " ", local_token_buffers);

	/*printf("resolveResource():line=%s, lineTokenCnt=%d\n",line, lineTokenCnt);
	i=0;
	while(i<lineTokenCnt){
		printf("resolveResource():i=%d, %s\n", i, local_token_buffers[i]);
		i+=1;
	}
	i=1;*/

	//copy current resource path
	char cr_cpy[MAX_BUF_LENGTH]; memset(cr_cpy, 0, MAX_BUF_LENGTH);
	strcpy(cr_cpy, current_rsrc);

	//copy current resource path to resBuf
	if(cpy_size > strlen(cr_cpy))
		cpy_size = strlen(cr_cpy);
	memset(resBuf, 0, bufsize);
	strncpy(resBuf, cr_cpy, cpy_size);

	//printf("Parsing line: %s\n", line);
	if(tokenIndicator<0 && lineTokenCnt>=2) tok = local_token_buffers[i];
	else if(tokenIndicator>0 && lineTokenCnt>i) tok = local_token_buffers[i];
	while(tok != NULL && count < 1){
		//printf("resolveResource():i=%d, line token: %s\n", i, tok);
		if(strcmp(tok, ".")==0){
		} else if ((strcmp(tok, "../")==0 || strcmp(tok, "..")==0) && strcmp(current_rsrc, "/")!=0 && strcmp(current_rsrc, "/")!=0) {
			//printf("Going back, tok=%s %d\n", tok, (int) (strcmp(tok, "../")==0 || strcmp(tok, "..")==0));
			path_tok = strtok(cr_cpy, "/");
			while(path_tok != NULL) {
				path_tok_cnt +=1;
				path_strlen_cnt += strlen(path_tok) + 1;
				path_tok = strtok(NULL, "/");
			}
			path_strlen_cnt += 1;
			memset(cr_cpy, 0, MAX_BUF_LENGTH); strcpy(cr_cpy, current_rsrc);
			path_tok = strtok(cr_cpy, "/");

			new_path_size = sizeof(char)*path_strlen_cnt;
			new_path = (char*) malloc(new_path_size);
			memset(new_path, 0, new_path_size);
			while(path_tok != NULL && num_tokens < path_tok_cnt){
				strcpy(&new_path[strlen(new_path)], "/");
				strcpy(&new_path[strlen(new_path)], path_tok);
				path_tok = strtok(NULL, "/");
				num_tokens +=1;
			}
			strcpy(&new_path[strlen(new_path)], "/");
			memset(resBuf, 0, sizeof(char)*bufsize);
			if(cpy_size > strlen(cr_cpy))
				cpy_size = strlen(cr_cpy);
			memset(resBuf, 0, bufsize);
			strncpy(current_rsrc, new_path, cpy_size);
			free(new_path);
		} else if(strlen(tok)>0 && tok[0]== '/'){
			//printf("Override\n");
			memset(resBuf, 0, sizeof(char)*bufsize);
			if(strlen(tok) < bufsize)
				strcpy(resBuf, tok);
		} else if(strlen(tok)>0 && tok[0] != '/'){
			//printf("Add child, %s\n", tok);
			if(resBuf[strlen(resBuf)-1] != '/' && (strlen(resBuf)+1)<=MAX_BUF_LENGTH)
				strcpy(&resBuf[strlen(resBuf)], "/");
			if((strlen(resBuf)+strlen(tok)) <= bufsize)
				strcpy(&resBuf[strlen(resBuf)], tok);
		}

		count += 1;

		//get next token
		i+=1;
		tok = local_token_buffers[i];
	}

	//free the local buffer
	tok_idx=0;
	while(tok_idx < MAX_TOKENS){
		free(local_token_buffers[tok_idx]);
		tok_idx += 1;
	}
	return resBuf;
}

void make_resource(char * line){
	int numToks=0, idx=0;
	char* tok;
	char* smap_url=NULL;
	char* alias=NULL;
	char* rsrcParam = NULL;
	char* newRsrcName = NULL;
	char* genpubName = NULL;
	char* symlink_name = NULL;
	char* links_to = NULL;
	char* rsrcParam2 = NULL;
	char* jsonStr = NULL;
	int devices=0, device=0;
	cJSON* jsonObj=cJSON_CreateObject();
	//cJSON* smap_urls;
	//cJSON* aliases;

	//clear and then populate tokens buffer
	clearTokBuf(input_token_buffers);
	numToks = pop_tokenArray(line, " ", input_token_buffers);

	printf("Numtoks: %d\n", numToks);

	if(numToks==2 && strpbrk(input_token_buffers[1], "-") == NULL){
		idx =1;
		tok=input_token_buffers[idx];		//get the resource name parameter
		while(tok!=NULL && idx<2){
			if(strlen(tok)>0 && tok[0]!= '/'){
				jsonObj = create_is4CreateDefaultRsrcReqObj(tok, jsonObj);
				handle_post(current_rsrc, jsonObj);
			}
			idx+=1;
		}
		free(jsonObj);
		return;
	} else if (numToks>=2){ 
		idx =1;
		tok=input_token_buffers[idx];		//get the resource name parameter
		while(tok!=NULL && idx<numToks){
			
			tok=input_token_buffers[idx];
			if(strlen(tok)>0 && tok[0]!= '/'){
				if(strcmp(tok, "--smapurl")==0 && idx+1<numToks)
				{
					smap_url = input_token_buffers[idx+1];
					idx+=2;
					printf("set smapurl=%s\n", smap_url);
				} else if(strcmp(tok, "--alias")==0 && idx+1<numToks &&
					strpbrk(input_token_buffers[idx+1], "/") == NULL &&
					strpbrk(input_token_buffers[idx+1], "-") == NULL)
				{
					alias = input_token_buffers[idx+1];
					idx +=2;
				} else if(strcmp(tok, "--devices")==0){
					devices=1;
					idx = numToks;
				} else if(strcmp(tok, "--device")==0 && idx+1<numToks &&
					strpbrk(input_token_buffers[idx+1], "/") == NULL &&
					strpbrk(input_token_buffers[idx+1], "-") == NULL)
				{
					device=1;
					newRsrcName = (char*)malloc(sizeof(char)*strlen(input_token_buffers[idx+1]));
					strcpy(newRsrcName, input_token_buffers[idx+1]);
					idx = numToks;
				} else if(strcmp(tok, "--genpub")==0 && idx+1<numToks &&
					strpbrk(input_token_buffers[idx+1], "/") == NULL
					)
				{
					genpubName = (char*)malloc(sizeof(char)*strlen(input_token_buffers[idx+1]));
					strcpy(genpubName, input_token_buffers[idx+1]);
					idx = numToks;
				} else if(strcmp(tok, "--symlink")==0 && idx+2<numToks)
				{
					symlink_name = (char*)malloc(sizeof(char)*strlen(input_token_buffers[idx+1]) + 5);
					links_to = (char*)malloc(sizeof(char)*strlen(input_token_buffers[idx+2]) + 5);
					strcpy(symlink_name, input_token_buffers[idx+1]);
					strcpy(links_to, input_token_buffers[idx+2]);
					idx = numToks;
				} else {
					if(idx==1 && rsrcParam == NULL)
						rsrcParam =tok;
					idx +=1;
				}
			} else {
				idx+=1;
			}
			
		}

		if(smap_url !=NULL && strlen(smap_url)>0){
			jsonObj = create_is4CreateSmapPubResourceRequestObj(smap_url, alias, jsonObj);
			if(rsrcParam == NULL){
				handle_post(current_rsrc, jsonObj);
			} else{
				rsrcParam = (char*) malloc(sizeof(char)*MAX_BUF_LENGTH);
				resolveResource(line, rsrcParam, MAX_BUF_LENGTH, -1);
				if(rsrcParam)
					handle_post(rsrcParam, jsonObj);
				free(rsrcParam);
			}
		} else if(devices==1){
			jsonObj = create_is4CreateDevicesRsrcReqObj(jsonObj);
			if(rsrcParam == NULL){
				handle_post(current_rsrc, jsonObj);
			} else{
				rsrcParam = (char*) malloc(sizeof(char)*MAX_BUF_LENGTH);
				resolveResource(line, rsrcParam, MAX_BUF_LENGTH, -1);
				if(rsrcParam)
					handle_post(rsrcParam, jsonObj);
				free(rsrcParam);
			}
		} else if(device ==1 && strlen(newRsrcName)>0 && strlen(newRsrcName)>0){
			jsonObj = create_is4CreateDeviceRsrcReqObj(newRsrcName, jsonObj);
			if(rsrcParam == NULL){
				handle_post(current_rsrc, jsonObj);
			} else{
				rsrcParam = (char*) malloc(sizeof(char)*MAX_BUF_LENGTH);
				resolveResource(line, rsrcParam, MAX_BUF_LENGTH, -1);
				if(rsrcParam)
					handle_post(rsrcParam, jsonObj);
				free(rsrcParam);
			}
			free(newRsrcName);
		} else if(genpubName){
			jsonObj = create_is4CreateGenPubRsrcReqObj(genpubName, jsonObj);
			if(rsrcParam == NULL){
				handle_post(current_rsrc, jsonObj);
			} else{
				rsrcParam = (char*) malloc(sizeof(char)*MAX_BUF_LENGTH);
				resolveResource(line, rsrcParam, MAX_BUF_LENGTH, -1);
				if(rsrcParam)
					handle_post(rsrcParam, jsonObj);
				free(rsrcParam);
			}
			free(genpubName);
		} else if (symlink_name && links_to){
			if(strncmp(links_to, "http://", 7) !=0){
				rsrcParam2 = (char*) malloc(sizeof(char)*MAX_BUF_LENGTH);
				if(rsrcParam == NULL )
					//mkrsrc --symlink <name> rsrcParam2
					resolveResource(line, rsrcParam2, MAX_BUF_LENGTH, 3);
				else 
					//mkrsrc <path> --symlink <name> rsrcParam2
					resolveResource(line, rsrcParam2, MAX_BUF_LENGTH, 4);
				jsonObj = create_is4CreateSymlinkReqObj(symlink_name, rsrcParam2, 1, jsonObj);
			} else if(strncmp(links_to, "http://", 7) ==0 || strncmp(links_to, "https://", 8) ==0 ){
				jsonObj = create_is4CreateSymlinkReqObj(symlink_name, links_to, 0, jsonObj);
			}

			jsonStr = cJSON_Print(jsonObj);
			printf("%s\n", jsonStr);
			if(rsrcParam == NULL){
				handle_post(current_rsrc, jsonObj);
			} else{
				rsrcParam = (char*) malloc(sizeof(char)*MAX_BUF_LENGTH);
				resolveResource(line, rsrcParam, MAX_BUF_LENGTH, -1);
				if(rsrcParam)
					handle_post(rsrcParam, jsonObj);
				free(rsrcParam);
			}
			free(symlink_name);
			free(links_to);
			if(rsrcParam2)
				free(rsrcParam2);
		}
	}
 
	free(jsonObj);
}

void delete_resource(char * line){
	int numToks=0;
	char* thisResource;
	cJSON* jsonObj=cJSON_CreateObject();

	//clear and then populate tokens buffer
	clearTokBuf(input_token_buffers);
	numToks = pop_tokenArray(line, " ", input_token_buffers);

	if(numToks==1){
		thisResource = current_rsrc;
		handle_delete(thisResource);
	} else if(numToks==2){
		thisResource = (char*)malloc(sizeof(char)*MAX_BUF_LENGTH);
		resolveResource(line, thisResource, MAX_BUF_LENGTH, -1);
		handle_delete(thisResource);	
		free(thisResource);
	}
	free(jsonObj);
}

void handle_piping(char * line){
	char line_cpy[MAX_BUF_LENGTH];
	char line_cpy2[MAX_BUF_LENGTH];
	char rsrcName[MAX_BUF_LENGTH];
	char * src=NULL;
	char* dest=NULL;
	int i=0, numToks=0, maxToks=2;
	cJSON* jsonObj=cJSON_CreateObject();
	char* subHandlerRsrc = "/sub";
	const char* pipe = "pipe ";
	char* jsonObjStr;

	strcpy(line_cpy, line);

	//clear and then populate tokens buffer
	clearTokBuf(input_token_buffers);
	numToks = pop_tokenArray(line, " |", input_token_buffers);

	if(numToks >= 2){
		while(i<numToks && i<maxToks){
			if(i%2==0){
				//printf("source: %s\n", input_token_buffers[i]);
				src = input_token_buffers[i];
				sprintf(line_cpy2, "%s%s", pipe, src);
			} else {
				//printf("dest: %s\n", input_token_buffers[i]);
				dest = input_token_buffers[i];
			}
			i+=1;
		}
	}

	if(src && dest){
		resolveResource(line_cpy2, rsrcName, MAX_BUF_LENGTH, -1);
		jsonObj = create_is4SubReqObj(rsrcName, dest, jsonObj);
		jsonObjStr = cJSON_Print(jsonObj);
		printf("Subscription Request:\n%s\npost_url:%s\n", jsonObjStr, subHandlerRsrc);
		handle_post(subHandlerRsrc, jsonObj);
	}

	free(jsonObj);
}
