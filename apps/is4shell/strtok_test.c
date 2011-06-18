#include <stdio.h>
#include <string.h>
#include <stdlib.h>

void testM(char*);

void main(void){
	char string[] = "words separated by spaces -- and, punctuation!";
	const char delimiters[] = " .,;:!-";
	char *token;
	char * b;

	/*token = strtok (string, delimiters); 
	printf("%s\n", token);
	token = strtok (NULL, delimiters);    
	printf("%s\n", token);
	token = strtok (NULL, delimiters);   
	printf("%s\n", token);
	token = strtok (NULL, delimiters);  
	printf("%s\n", token);
	token = strtok (NULL, delimiters); 
	printf("%s\n", token);
	token = strtok (NULL, delimiters);
	printf("%s\n", token);
	token = strtok (NULL, delimiters);
	printf("%s\n", token);*/

	testM(b);
	printf("b=%s\n", b);
}

void testM(char * blah){
	blah = (char*)malloc(sizeof(char)*strlen("hello"));
	if(blah != NULL){
		printf("setting blah\n");
		strcpy(blah, "hello");
		printf("set b to %s\n", blah);
	} else {
		printf("blah is NULL after malloc\n");
	}
}
