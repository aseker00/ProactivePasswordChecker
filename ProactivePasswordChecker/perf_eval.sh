#!/bin/bash
while test $# -gt 0; do
	a=$(java -cp 'bin;../LanguageModel/bin' project.ppc.PasswordChecker test gt data/model -s $1 | awk '$2 == "strong" {print $2}' | wc -l)
	b=$(wc -l $1 | cut -d' ' -f1)
	echo -ne $1 | sed 's/.*\///'
	echo -ne '\t'
	echo -ne $a
	echo -ne '\t'
	echo -ne $b
	echo -ne '\t'
	echo "scale = 4; (($a/$b)*100)" | bc | sed 's/..$//'
	shift
done
