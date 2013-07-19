# -*- coding: utf-8 -*-
"""
Created on Sun Jul 14 00:23:51 2013

@author: Amit
"""

from random import randrange

base = ord('A')
for i in range(0, 100000):
    word = ''
    for j in range(0, 8):
        r = randrange(26)
        word += chr(base+r)
    print word