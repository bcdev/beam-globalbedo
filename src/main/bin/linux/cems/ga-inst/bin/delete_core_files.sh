#!/bin/bash
# deletes core files and hs_err logs every 15min

while true
do
  rm -f $GA_INST/core.* $GA_INST/hs_err*
  sleep 900
done
