# -*- coding: utf-8 -*-
"""
Created on Sun Jul 14 00:37:26 2013

@author: Amit
"""

from random import randrange

base = ord(' ')
for i in range(0, 100000):
    word = ''
    for j in range(0, 8):
        r = randrange(95)
        word += chr(base+r)
    print word