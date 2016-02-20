#include <stdio.h>
#include <string.h>

#include "kit.h"


int main(int argc, char *argv[])
{
	std::string sessionId{argv[1]}, devCmt{argv[2]};
	std::string cliCmt, authCode;

	create_auth_code("hello", sessionId, devCmt, cliCmt, authCode);

	return 0;
}
