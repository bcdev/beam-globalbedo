#!/bin/bash

while true
do
  echo "`date` : `bqueues |grep "lotus "`   | `bjobs -r |wc`   | `bjobs -a |grep PEND |wc`"
  sleep 60
done
