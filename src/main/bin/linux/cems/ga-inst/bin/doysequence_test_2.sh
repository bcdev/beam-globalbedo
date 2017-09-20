#!/bin/bash

startdoy=001
enddoy=365
for doystring in $(seq -w $startdoy 96 $enddoy); do
    iEndDoy=$((10#$doystring + 95))  # 96, 192,...

    intervalStartDoy=$doystring  # 001, 095, 191,...
    intervalEndDoy=`printf '%03d\n' "$(( $iEndDoy > 366 ? 366 : $iEndDoy))"`  # 096, 192,..., 366

    echo "start: $intervalStartDoy"
    echo "end: $intervalEndDoy"

    ### now start inversion... ###

    #for doy in $(seq -w $intervalStartDoy $intervalEndDoy); do   # -w takes care for leading zeros
    #    echo "BRDF computation for DoY $doy ..."
    #done
done
