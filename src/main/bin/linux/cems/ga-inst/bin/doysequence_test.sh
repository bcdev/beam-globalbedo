#!/bin/bash

startdoy=001
enddoy=365
for doystring in $(seq -w $startdoy 64 $enddoy); do
    iEndDoy=$((10#$doystring + 63))  # 64, 128, 192,...

    intervalStartDoy=$doystring  # 001, 063, 127,...
    intervalEndDoy=`printf '%03d\n' "$(( $iEndDoy > 366 ? 366 : $iEndDoy))"`  # 064, 129, 193,...

    echo "start: $intervalStartDoy"
    echo "end: $intervalEndDoy"

    ### now start inversion... ###

    for doy in $(seq -w $intervalStartDoy $intervalEndDoy); do   # -w takes care for leading zeros
        echo "BRDF computation for DoY $doy ..."
    done
done
