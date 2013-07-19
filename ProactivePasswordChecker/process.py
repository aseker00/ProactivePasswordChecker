# -*- coding: utf-8 -*-
"""
Created on Sat Jul 13 20:38:56 2013

@author: Amit
"""
import sys

f = open(sys.argv[1])
lines = f.readlines()[1:]
prev = ''
for line in lines:
    l = line.strip()
    if l[0:1].isdigit():
        num = int(l[0:1])
        curr = l[1:]
        word = prev[0:num] + curr
        prev = word
        print word
f.close()