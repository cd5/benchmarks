#!/usr/bin/python3

# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
#
#     * Redistributions of source code must retain the above copyright
#     notice, this list of conditions and the following disclaimer.
#
#     * Redistributions in binary form must reproduce the above copyright
#     notice, this list of conditions and the following disclaimer in the
#     documentation and/or other materials provided with the distribution.
#
#     * Neither the name of "The Computer Language Benchmarks Game" nor the
#     name of "The Computer Language Shootout Benchmarks" nor the names of
#     its contributors may be used to endorse or promote products derived
#     from this software without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
# AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
# IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
# ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
# LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
# CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
# SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
# INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
# CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
# ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
# POSSIBILITY OF SUCH DAMAGE.

import sys

class Node:
	def __init__(self, item, left, right):
		self.item = item
		self.left = left
		self.right = right

	def itemCheck(self):
		if self.left == None:
			return self.item
		return self.item + self.left.itemCheck() - self.right.itemCheck()

def bottomUpTree(item, depth):
	if depth <= 0:
		return Node(item, None, None)
	return Node(item, bottomUpTree(2*item-1, depth-1), bottomUpTree(2*item, depth-1))

def main(n, minDepth=4):
	maxDepth = n
	if minDepth+2 > n:
		maxDepth = minDepth + 2
	stretchDepth = maxDepth + 1

	check = bottomUpTree(0, stretchDepth).itemCheck()
	print("stretch tree of depth %d\t check: %d" % (stretchDepth, check))

	longLivedTree = bottomUpTree(0, maxDepth)

	depth = minDepth
	while depth <= maxDepth:
		iterations = 1 << (maxDepth-depth+minDepth)
		check = 0

		for i in range(1, iterations):
			check += bottomUpTree(i, depth).itemCheck()
			check += bottomUpTree(-i, depth).itemCheck()
		print("%d\t trees of depth %d\t check: %d" % (iterations*2, depth, check))
		depth += 2

	print("long lived tree of depth %d\t check: %d" % (maxDepth, longLivedTree.itemCheck()))

if __name__=='__main__':
	main(int(sys.argv[1]))
