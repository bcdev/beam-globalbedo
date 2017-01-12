#!/bin/bash

# TODO: count PROCESSED files and create final marker file if we are done for all 46 DoYs:
cnt_files=`ls -1 blubb* |wc -l`
echo "cnt_files: $cnt_files"

if [ $cnt_files -eq 2 ]
then
    echo "all files done"
fi

