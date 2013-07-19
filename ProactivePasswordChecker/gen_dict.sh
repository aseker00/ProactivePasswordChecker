#!/bin/bash
python process.py data/dict/crack/dict/1/bad_pws.dat.dwg > data/dict/bp1
python process.py data/dict/crack/dict/1/jargon.dwg > data/dict/bp2
cp data/dict/wordlist/american-english data/dict/bp3
python process.py data/dict/crack/dict/1/family-names.dwg > data/dict/family.dict
python process.py data/dict/crack/dict/1/given-names.dwg > data/dict/given.dict
sort data/dict/bp3 data/dict/family.dict data/dict/given.dict | uniq > data/dict/bp4
sort data/dict/bp* | uniq > data/dict/bp5
sort -R data/dict/bp5 > data/dict/bp5.rand
a=$(wc -l data/dict/bp5.rand | cut -d' ' -f1)
b=$(($a*7/10))
c=$(($a-$b))
head -n$b data/dict/bp5.rand > data/dict/bp5.train
tail -n$c data/dict/bp5.rand > data/dict/bp5.test
python random1.py > data/dict/gp1
python random2.py > data/dict/gp3
python random3.py data/dict/wordlist/american-english > data/dict/nbp1
python random4.py data/dict/wordlist/american-english > data/dict/nbp2
python random3.py data/dict/bp4 > data/dict/nbp3

