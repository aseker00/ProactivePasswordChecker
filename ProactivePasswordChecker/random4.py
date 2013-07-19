# -*- coding: utf-8 -*-
"""
Created on Sun Jul 14 01:21:44 2013

@author: Amit
"""

import sys
from random import randrange

infile = sys.argv[1]
f = open(infile)
base = ord(' ')
for line in f.readlines():
    l = list(line.strip())
    if (len(l) > 2):
        r = randrange(95)
        pos = randrange(1,len(l)-1)
        l[pos] = chr(base+r)
    word = "".join(l)
    print word
f.close()