#!/bin/bash

while true
do
  echo "`date` : `bqueues |grep "lotus "` // `bjobs -r |wc`"
  sleep 60
done
