#!/bin/bash

while true
do
  echo "`date` : `bjobs -rw |grep avhrrgeo`"
  sleep 60
done
