package main

import (
	"bufio"
	"flag"
	"fmt"
	"os"
)

var n = flag.Int("n", 100, "number of iterations")

func main() {
	flag.Parse()
	buf := bufio.NewWriter(os.Stdout)
	for i:=0; i<*n; i++ {
		fmt.Fprintf(buf, "%d ", i)
	}
	buf.Flush()
}
