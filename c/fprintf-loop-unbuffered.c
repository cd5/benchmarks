#include <stdio.h>

int main(int argc, char *argv[]) {
	int n = atoi(argv[1]);
	int i;
	for(i=0; i<n; i++) {
		fprintf(stdout, "%d ", i);
		fflush(stdout);
	}
}
