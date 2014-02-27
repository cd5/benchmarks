package main

import (
	"flag"
	"fmt"
	"os"
)

var n = flag.Int("n", 100, "number of iterations")

func main() {
	flag.Parse()
	for i:=0; i<*n; i++ {
		fmt.Fprintf(os.Stdout, "%d ", i)
	}
}
