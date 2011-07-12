#!/bin/tcsh -f

set FUNC = convert_to_julian_day

if ($#argv != 3) then
  echo ""
  echo "Usage: $FUNC yyyy mm dd"
  exit 1
endif

set year = $1
set month = $2
set day = $3

#--- Function to transform YYYYMMDD to julian day ---#
if ( $year == 2000 || $year == 2004 || $year == 2008 || $year == 2012 ) then
        set months_days = (31 29 31 30 31 30 31 31 30 31 30 31)
else
        set months_days = (31 28 31 30 31 30 31 31 30 31 30 31)
endif

set julian_day = 0
set counter_months = 0

while ( $counter_months < `echo $month - 1 | bc` )
        set counter_months = `echo $counter_months + 1 | bc`
        set julian_day = `echo $julian_day + $months_days[$counter_months] | bc`
end

set julian_day = `echo $julian_day + $day | bc`

set julian_day = `echo $julian_day | awk '{if (length($1)==1) $1="00"$1; else if (length($1)==2) $1="0"$1} {print $1}'`

echo $julian_day
