#!/bin/bash

while true
do
  #echo "`date` : `bqueues |grep "short-serial "`   | `bjobs -r |wc`   | `bjobs -a |grep PEND |wc`"
  echo "`date` : `bqueues |grep "short-serial "`   | `bjobs -r |wc`   | `bjobs -a |grep PEND |wc`"
  sleep 60
done
