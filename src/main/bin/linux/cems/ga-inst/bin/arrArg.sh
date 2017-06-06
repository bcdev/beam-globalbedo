#!/bin/bash

arg1="$1"
arg2=("${!2}")
arg3="$3"
arg4=("${!4}")

echo "arg1=$arg1"
echo "arg2 array=${arg2[@]}"
echo "arg2 #elem=${#arg2[@]}"
echo "arg3=$arg3"
echo "arg4 array=${arg4[@]}"
echo "arg4 #elem=${#arg4[@]}"
