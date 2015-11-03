#!/bin/bash

## we want this:
#index = -1
#while (index < 3 and index < 6) do
#    print 'increasing ' + index
#    index++
#
#print 'done: index = ' + index

index=-1
while [ $index -lt 3 ] && [ $index -lt 6 ]
do
  echo "increasing: index = $index"
  index=$(($index+1))
done
echo "done: index = $index"
