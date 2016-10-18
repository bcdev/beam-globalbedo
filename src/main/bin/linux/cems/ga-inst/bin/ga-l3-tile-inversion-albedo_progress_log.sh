#!/bin/bash

while true
do
  echo "`date` : `cat ga-l3-tile-inversion-albedo-leo.status |grep created`"
  sleep 60
done
