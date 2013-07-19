# -*- coding: utf-8 -*-
"""
Created on Sun Jul 14 01:09:22 2013

@author: Amit
"""

import sys
from random import randrange

infile = sys.argv[1]
f = open(infile)
base = ord(' ')
for line in f.readlines():
    l = list(line.strip())
    r = randrange(95)
    pos = randrange(len(l))
    l[pos] = chr(base+r)
    word = "".join(l)
    print word
f.close()