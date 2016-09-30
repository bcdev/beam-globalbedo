#!/bin/bash

while true
do
  echo "`date` : removing core files..."
  rm -f $GA_INST/core.*
  rm -f $GA_INST/hs*.log
  sleep 1200
done
