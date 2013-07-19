#!/bin/bash
./perf_eval.sh $(find data/dict -maxdepth 1 -type f -not -name "*.*")
