#!/bin/bash

index=-1
index2=-1
while [ $index -lt 5 ] && [ $index2 -lt 2 ]
do
  echo "increasing: index = $index"
  echo "increasing: index2 = $index2"
  index=$(($index+1))
  index2=$(($index2+1))
done
echo "done: index = $index"
echo "done: index2 = $index2"
