#!/bin/bash

if [[ $1 == "-all" ]]; then
  hdus=`pan_du -s /group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest/*`
elif [[ $1 == "-auxdata" ]]; then
  hdus=`pan_du -s /group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest/auxdata/*`
elif [[ $1 == "-ga-inst" ]]; then
  hdus=`pan_du -s /group_workspaces/cems2/qa4ecv/vol4/olafd/ga-inst/*`
else
  echo "No option given. Usage:"
  echo "cems_dus.sh [-all|-auxdata|-ga-inst|<path glob>"
  exit 0;
fi

printf "%10s     %5s  %10s     %s\n" "Netto" "Repl" "Brutto" "Path"
printf "=====================================================\n"

let totalNettoSum=0
let totalBruttoSum=0

while read -r hdus_line; do
  echo "BASH_REMATCH[2]: ${BASH_REMATCH[2]}"
  echo "BASH_REMATCH[1]: ${BASH_REMATCH[1]}"
  echo "hdus_line: ${hdus_line}"

  # ours:
  #dir /group_workspaces/cems2/qa4ecv/vol4/olafd/GlobAlbedoTest/auxdata/MVIRI: 5 files, 670480 KiB
  regex='dir ([a-z/0-9]+): ([0-9]+) files, ([0-9]+) ([a-z]+)$'
  if [[ ${hdus_line} =~ $regex ]]; then

  # Calvalus:
  # 502268982319  /calvalus/projects/lc/Ver4
  #if [[ ${hdus_line} =~ ([0-9]+)[^0-9/]+(/[^ ]+)$ ]]; then
    echo "hier 1"
    path=${BASH_REMATCH[2]}
    size=${BASH_REMATCH[1]}
    let "netto=${size}/(1024*1024*1024)"
    if [[ $netto != "0" ]]; then
      echo "hier 2"
      replication=1
      brutto=$(echo "${netto} * ${replication}" | bc)
      printf "%'10.0f GB  %.3f  %'10.0f GB  %s\n" $netto $replication $brutto $path
      totalNettoSum=$(echo "${totalNettoSum} + ${netto}" | bc)
      totalBruttoSum=$(echo "${totalBruttoSum} + ${brutto}" | bc)
    fi
  fi
done <<< "${hdus}"

printf "=====================================================\n"
printf "%'10.0f GB  %5s  %'10.0f GB  %s\n" $totalNettoSum " " $totalBruttoSum "Total"

