#!/bin/bash

myArray=$1
IFS=';' read -a myArray <<< "$myArray"

for i in "${myArray[@]}"
do
    echo "arg1=$i"
done
