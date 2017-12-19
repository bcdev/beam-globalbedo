#!/bin/bash
# set -x
# set -e
# A bulk tutorial for GPT: http://www.brockmann-consult.de/beam-wiki/display/BEAM/Bulk+Processing+with+GPT
# For a file naming convention see: http://envisat.esa.int/handbooks/meris/CNTR2-2.html
# https://earth.esa.int/documents/10174/1912962/meris.ProductHandbook.2_1.pdf

# NOTE: 
# 2017-03-07: creates l1b files for AVHRR (not relevant for SPP) but for the forward
#             production of input files of AVHRR JRC-FAPAR (see Mirko)
# 2016-12-15: c6 priors Version implemented in ExtractSinglePriors()
#             c6 priors gesture of filenaming BBDR2BRDF (see global variables $pV006, $pv!)

alias echo='echo'

# /// Global variables ///

cmpSnow="false"    # Modified by Christian on 2012-12-06  -PcomputeSnow=$cmpSnow on ga.l3.inversion.single AND ga.l3.albedo.single
usePrior="false"   # Use or not priors in ga.l3.inversion.single step (note that with Snow enabled, prior option is requested to be =true) 
pV006="false"      # use the priors from modis v006
sys=LIN
sensor=MERIS

case $sys in 
WIN)
  BEAM_HOME="/cygdrive/c/Programmi/beam-5.0"
  GPT=gpt.bat ;;
LIN)
  #BEAM_HOME=$HOME/beam-5.0.1-alpha-01
  #GPT=${BEAM_HOME}/bin/gpt.sh ;;
  GPT=$BEAM_HOME/bin/gpt.sh ;;
esac

clear
echo """<sprit2.sh> Overall settings:
cmpSnow  $cmpSnow
usePrior $usePrior
pV006    $pV006
sys      $sys
sensor   $sensor
BEAM_HOME $BEAM_HOME
GPT       $GPT
Beam modules (GA):"""

ls /home/lancoch/beam-5.0.1-alpha-01/modules/beam-globalbedo-*
sleep 2

SITES=( DOME-C JANINA JARVSELJA-BS JARVSELJA-PS LIBYA-4 LOPE NGHOTTO OFENPASS SKUKUZA THIVERVAL-GRIGNON WELLINGTON ZERBOLO )
CODES=( h21v16_0205_0612 \
               h30v12_0567_0009 \
                      h19v03_0525_0207 \
                                   h19v03_0521_0202 \
                                                h20v06_0065_0174 \
                                                        h19v09_0175_0020 \
                                                             h19v08_0871_0736 \
                                                                     h18v04_0842_0400 \
                                                                               h20v11_1025_0602 \
                                                                                        h18v04_0155_0138 \
                                                                                                         h19v12_0692_0432 \
                                                                                                              h18v04_0749_0565 )
                                                                                                              
# Definition of version: this section affects the run
# NOTE: the variable "version" refers to the SPP run (only wrapper step) and its BBDR_single output files. 
#       Not to 6S forward simulation that are defined by the arrays above.
select=a3

case $select in
a)
# this should be associated with version=20161025 
flist_1=( Output/1-Giovanni/MERIS_DOME-C_snow_domec.20160726_1.out \
Output/1-Giovanni/MERIS_JANINA_shrubland.20160726_1.out \
Output/1-Giovanni/MERIS_JARVSELJA-BS_birchstand_mixed.20160726_1.out \
Output/1-Giovanni/MERIS_JARVSELJA-PS_pinestand_mixed.20160726_1.out \
Output/1-Giovanni/MERIS_LIBYA-4_desert.20160726_1.out \
Output/1-Giovanni/MERIS_LOPE_tropical_forest_UCL.20160726_1.out \
Output/1-Giovanni/MERIS_NGHOTTO_tropical_forest_PERU.20160726_1.out \
Output/1-Giovanni/MERIS_OFENPASS_pinestand_mixed.20160726_1.out \
Output/1-Giovanni/MERIS_SKUKUZA_savann1.20160726_1.out \
Output/1-Giovanni/MERIS_THIVERVAL-GRIGNON_wheatcanopy_mixed.20160726_1.out \
Output/1-Giovanni/MERIS_WELLINGTON_citrusorchard.20160726_1.out \
Output/1-Giovanni/MERIS_ZERBOLO_shortrotationforest_mixed.20160726_1.out )
version=20161025 ;;

b)
# this should be associated with version=	20161019
flist_1=( Output/1-Giovanni/MERIS_DOME-C_snow_domec.20160726_1.out \
Output/2-Globalbedo/MERIS_LIBYA-4_desert.20160726_2.out \
Output/2-Globalbedo/MERIS_NGHOTTO_tropical_forest_PERU.20160726_2.out \
Output/2-Globalbedo/MERIS_LOPE_tropical_forest_UCL.20160726_2.out \
Output/2-Globalbedo/MERIS_SKUKUZA_savann1.20160726_2.out \
Output/2-Globalbedo/MERIS_JANINA_shrubland.20160726_2.out \
Output/2-Globalbedo/MERIS_WELLINGTON_citrusorchard.20160726_2.out \
Output/2-Globalbedo/MERIS_JARVSELJA-BS_birchstand_mixed.20160726_2.out \
Output/2-Globalbedo/MERIS_JARVSELJA-PS_pinestand_mixed.20160726_2.out \
Output/2-Globalbedo/MERIS_THIVERVAL-GRIGNON_wheatcanopy_mixed.20160726_2.out \
Output/2-Globalbedo/MERIS_OFENPASS_pinestand_mixed.20160726_2.out \
Output/2-Globalbedo/MERIS_ZERBOLO_shortrotationforest_mixed.20160726_2.out )
version=20161019 ;;

a1)
# this should be associated with version=20170614 (20170111 = old 6S simulation with wrong ALT e.g. INPUT < 9Feb)
# the input files listed here differ from the case a and b, because they contain 
# a column for BSA at TOC. And some date fixing for NGH, JARx2. Moreover, all TOC
# are obtained from extended tables Interpolationext/ (e.g. the Raytran
# simulation as extended for SZA > 60 were taken into account when producing
# TOC inputs for 6S). Mixed scenes for snow are binary (summer or winter).

flist_1=( Output/1-Giovanni/MERIS_DOME-C_snow_domec.20161216_1.out \
Output/1-Giovanni/MERIS_JANINA_shrubland.20161216_1.out \
Output/1-Giovanni/MERIS_JARVSELJA-BS_birchstand_mixed.20161216_1.out \
Output/1-Giovanni/MERIS_JARVSELJA-PS_pinestand_mixed.20161216_1.out \
Output/1-Giovanni/MERIS_LIBYA-4_desert.20161216_1.out \
Output/1-Giovanni/MERIS_LOPE_tropical_forest_UCL.20161216_1.out \
Output/1-Giovanni/MERIS_NGHOTTO_tropical_forest_PERU.20161216_1.out \
Output/1-Giovanni/MERIS_OFENPASS_pinestand_mixed.20161216_1.out \
Output/1-Giovanni/MERIS_SKUKUZA_savann1.20161216_1.out \
Output/1-Giovanni/MERIS_THIVERVAL-GRIGNON_wheatcanopy_mixed.20161216_1.out \
Output/1-Giovanni/MERIS_WELLINGTON_citrusorchard.20161216_1.out \
Output/1-Giovanni/MERIS_ZERBOLO_shortrotationforest_mixed.20161216_1.out )
version=20170614 ;;

a3)
flist_1=( Output/3-Aeronet/MERIS_LOPE_tropical_forest_UCL.20161216_3.out \
Output/3-Aeronet/MERIS_SKUKUZA_savann1.20161216_3.out \
Output/3-Aeronet/MERIS_ZERBOLO_shortrotationforest_LAI03.20161216_3.out \
Output/3-Aeronet/MERIS_ZERBOLO_shortrotationforest_LAI10.20161216_3.out \
Output/3-Aeronet/MERIS_ZERBOLO_shortrotationforest_LAI24.20161216_3.out \
Output/3-Aeronet/MERIS_ZERBOLO_shortrotationforest_LAI32.20161216_3.out \
Output/3-Aeronet/MERIS_ZERBOLO_shortrotationforest_LAI40.20161216_3.out )
version=20170928 ;;

esac

# /// END of global variables ///


# cd  $HOME/Desktop/spp/


#
#
# Functions
#
#

function deftiles(){

# return the $pfile $tile $coordmeris $coordmodis

site=$1

case $site in 

# LAT/LON Coordinates mapping to <tile>/XY arise L1b2SDR_BBDR step: output filenames
# Qui in ordine di latitudineN-S : vedi tables.ods per le coordinate

JARVSELJA-PS|jarp|jar1|ja1)
alt=43 
pfile=Jarvselja1_modis.c5.prior.brdf.bb.csv
tile=h19v03
coordmodis=(1040 0404)
coordmeris=(0521 0202) 
ndsifile=jar.ndsi ;;

JARVSELJA-BS|jarb|jar2|ja2)
alt=43 
pfile=Jarvselja2_modis.c5.prior.brdf.bb.csv
tile=h19v03
coordmodis=(1044 0413)
coordmeris=(0525 0207) 
ndsifile=jar.ndsi ;;

THIVERVAL|THIVERVAL-GRIGNON|thg)
alt=93
pfile=Thiverval_modis.c5.prior.brdf.bb.csv
tile=h18v04
coordmodis=(0309 0275)
coordmeris=(0155 0138) ;;

OFENPASS|ofe)
alt=1890
pfile=Ofenpass_modis.c5.prior.brdf.bb.csv
tile=h18v04
coordmodis=(1684 0800)
coordmeris=(0842 0400) 
ndsifile=ofe.ndsi  ;;

ZERBOLO|zer)
alt=101
pfile=Zerbolo_modis.c5.prior.brdf.bb.csv
tile=h18v04
coordmodis=(1498 1128)
coordmeris=(0749 0565) ;;  # maybe not compatible

LIBYA-4|lib)
alt=117 
pfile=Libia4_modis.c5.prior.brdf.bb.csv
tile=h20v06
coordmodis=(0130 0347)
coordmeris=(0065 0174) ;;

NGHOTTO|ngh)
alt=570
pfile=Nghotto_modis.c5.prior.brdf.bb.csv
tile=h19v08
coordmodis=(1742 1471)
coordmeris=(0871 0736) ;;

LOPE|lop)
alt=317
pfile=Lope_modis.c5.prior.brdf.bb.csv
tile=h19v09
coordmodis=(0349 0040)
coordmeris=(0175 0020) ;;

SKUKUZA|sku)
alt=393
pfile=Skukuza_modis.c5.prior.brdf.bb.csv
tile=h20v11
coordmodis=(2049 1204)
coordmeris=(1025 0602) ;;

JANINA|jan) 
alt=347
pfile=Janina_modis.c5.prior.brdf.bb.csv
tile=h30v12
coordmodis=(1134 0017)
coordmeris=(0567 0009) ;;

WELLINGTON|wel)
alt=100
pfile=Wellington_modis.c5.prior.brdf.bb.csv
tile=h19v12
coordmodis=(1384 0863)
coordmeris=(0692 0432) ;;

DOME-C|dom) 
alt=3233
pfile=DomeC_modis.c5.prior.brdf.bb.csv 
tile=h21v16
coordmodis=(0410 1223)
coordmeris=(0205 0612) ;;

*) echo site not implemented
   exit ;;

esac

}

function pasteExtractionFiles(){
# 2016-10-13: To paste extraction files 
# $1 is one of DOME-C  JANINA  JARVSELJA-BS  JARVSELJA-PS  LIBYA-4  LOPE  NGHOTTO  OFENPASS  SKUKUZA  THIVERVAL-GRIGNON  WELLINGTON  ZERBOLO
if [ $1 ] ; then
  local site=$1 # `echo $1 | tr '[:upper:]' '[:lower:]'`
  local idir="`pwd`/extraction_MERIS/$site/output/extraction" # a sym-link to /home/lancoch/Work/6Ssim/Reflectances/MERIS
  echo "Getting info from $idir ..." > /dev/stderr
  echo "#date time l1.flags detector_index latitude   longitude    dem_alt  dem_rough   lat_corr   lon_corr zonal_wind merid_wind  atm_press      ozone    rel_hum"
 
  paste $idir/'Date(yyyy-MM-dd).txt' $idir/'Time(HH:mm:ss).txt' \
        $idir/l1_flags.txt             $idir/detector_index.txt        $idir/latitude.txt         $idir/longitude.txt \
        $idir/dem_alt.txt              $idir/dem_rough.txt             $idir/lat_corr.txt         $idir/lon_corr.txt  \
        $idir/zonal_wind.txt           $idir/merid_wind.txt            $idir/atm_press.txt        $idir/ozone.txt     \
        $idir/rel_hum.txt |\
  awk '{printf("%s %s %6i %6i %10.6f %10.6f %10.3f %10.3f %10.6f %10.6f %10.2f %10.2f %10.1f %10.1f %10.1f\n",
               $2,$4,$6,$8,$10,$12,$14,$16,$18,$20,$22,$24,$26,$28,$30)}'
               
  # awk '{for (i=2; i<NF; i+=2) printf("%s ",$i) ; print $NF }'
  
else

  echo "Usage: pasteExtractionFiles sitename"
  exit 1
  
fi

}

function n2b(){
# transform narrowband to broadband reflectances using the 
# parameters defined for globalbedo (pag. 141 ATDB)
if [[ $15 ]] ; then
  local a_vis=`echo $* | awk '{print 0.4578 * $2 + 0.3159 * $5 + 0.2248 * $7 + 0.0009}'`
  local a_nir=`echo $* | awk '{print -1.5595 * $3 + 1.1420 * $5 + 0.5412 * $7 + 0.6560 * $13 - 0.0107}'`
  local a_sw=`echo $*  | awk '{print -0.3318 * $2 + 0.4973 * $5 + 0.3737 * $7 + 0.3555 * $13 - 0.0041}'`
  echo $a_vis $a_nir $a_sw
else
  echo """
Usage: n2b list of 15 reflectances
       including the unused reflectances (that can be set to any value)
       Globalbedo uses only R2,R3,R5,R7 and R13 to produce Rvis,Rnir and Rsw
       Returna a vector of lenght 3 with (a_vis a_nir a_sw)
"""
fi
}

function ToASim2sppWrapper(){

# Reformat output file from ToA simulation performed with MakeTOANovember script
# to suite BEAM input required by the SPP included in BEAM.
# Creates L1b time series for the fisrt step of GA chain
# Updates
# 2016-10-13 : Exact additional infos taken from extraction files when converting ToA output 
#              from the MakeToA procedure, to spp input.
#              They include all the NA number of the table below.


L1bDIR=ga_testdata/l1b/
infile=$1
oufile=$L1bDIR/$2
tmpname=$(basename $infile)
SITE=$(echo $tmpname | awk -F_ '{print $2}')
echo $SITE

# defining position of the output variables:
grep ^[1,2] $infile | awk 'NF>20 {print $1,$2}' | sort | uniq --check-char=19 > list_of_dates.txt # list of dates
l=`wc -l < list_of_dates.txt`

# ciclo sulle date che estrae da file di input solo 
# le righe corrispondenti alla data corrente e rimappa 
# i dati in un record unico secondo la tabella seguente:

# Re-Mapping Column Table
# output file # beam # what is 
# he=Header
# NA=not available
# ND=not defined
#-----------------------------
# # ND # 1	featureId	0                         * progressive number of the record starting from 0
# # 1T2 # 2	start_time:time	2010-06-01T12:45:00       * yyyy-mm-ddTHH:MM:SS
# # 1T2 # 3	stop_time:time	2010-07-01T13:57:23
# # 25+26+27 # 4	radiance_1:float	70.38858
# # 25+26+27 # 5	radiance_2:float	63.93897
# # 25+26+27 # 6	radiance_3:float	51.052143
# # 25+26+27 # 7	radiance_4:float	50.016937
# # 25+26+27 # 8	radiance_5:float	49.436176
# # 25+26+27 # 9	radiance_6:float	35.036983
# # 25+26+27 # 10	radiance_7:float	29.30976
# # 25+26+27 # 11   radiance_8:float	27.807388
# # 25+26+27 # 12	radiance_9:float	48.244713
# # 25+26+27 # 13	radiance_10:float	120.38643
# # 25+26+27 # 14	radiance_11:float	36.734013
# # 25+26+27 # 15	radiance_12:float	120.072395
# # 25+26+27 # 16	radiance_13:float	102.15177
# # 25+26+27 # 17	radiance_14:float	99.0389
# # 25+26+27 # 18	radiance_15:float	69.0387
# # NA # 19	l1_flags:short	16                       * from where?
# # NA # 20	detector_index:short	595              * from where?
# # He # 21	latitude:float	53.444145
# # He # 22	longitude:float	10.575718
# # Local Array # 23	dem_alt:float	8375                     * unit?
# # NA # 24	dem_rough:float	7875                     * unit?
# # NA # 25	lat_corr:float	-0.00000275              * unit?
# # NA # 26	lon_corr:float	0.0000225                * unit?
# # 3 # 27	sun_zenith:float	38.642796
# # 4 # 28	sun_azimuth:float	149.3894
# # 5 # 29	view_zenith:float	10.878903
# # 6 # 30	view_azimuth:float	105.04132
# # NA # 31	zonal_wind:float	1.2375001        * unit? (what to do with missing value?)
# # NA # 32	merid_wind:float	-1.3000001       * unit?
# # scaled 1013.25 # 33	atm_press:float	1017.9                   * mbar
# # 10 # 34	ozone:float	334.45                   * DU
# # !9 iwv # 35	rel_hum:float	31.8125                  * %

# TBD: non abbiamo RH per ora impostato a 30%.  // da file extraction si possono prendere queste info oltre a dem / lat corr etc ...
# TODO: pressure=Po*exp(-8.4 * z ) in km
pasteExtractionFiles $SITE > extraction.$SITE
head extraction.$SITE
cat extraction.$SITE | awk '{print NF}' | sort -u 
sleep 1 

lat=`grep Latitude $infile | awk '{print $3}'`
lon=`grep Longitude $infile | awk '{print $3}'`

# TBD: alt=`grep Altitudee $infile | awk '{print $3}'` 
set -x
  deftiles $SITE
  echo $alt # in meters
set +x
sleep 1
# for the pressure we use the barometric formula with ascale heigh of 8.4Km.
# es. Barometric Formula wiki
echo "ToASim2sppWrapper: *** Creating $oufile"
echo "featureId	start_time:time	stop_time:time	radiance_1:float	radiance_2:float	radiance_3:float	radiance_4:float	radiance_5:float	radiance_6:float	radiance_7:float	radiance_8:float	radiance_9:float	radiance_10:float	radiance_11:float	radiance_12:float	radiance_13:float	radiance_14:float	radiance_15:float	l1_flags:short	detector_index:short	latitude:float	longitude:float	dem_alt:float	dem_rough:float	lat_corr:float	lon_corr:float	sun_zenith:float	sun_azimuth:float	view_zenith:float	view_azimuth:float	zonal_wind:float	merid_wind:float	atm_press:float	ozone:float	rel_hum:float" > $oufile
for i in `seq $l`
do
  d=(`awk 'NR=='$i'' list_of_dates.txt`)
  grep -E "${d[0]} +${d[1]}" $infile > tmpfile
  # wc tmpfile
  # definisco un T2 un'ora dopo T1 per vedere se viene presa una statistica
  # aerosol valida (risposta NO)

  ## T2=$(date -d "$d 1 month" +%Y-%m-%dT%H:%M:%S)
  
  # wc -l tmpfile # per meris deve essere lungo 15 per 15 bande
  
  # NOTE: To be verified
  # flag to 16 Land
  # index of the CCD to 595
  # altitude in FEET
  # dem_rough:float	lat_corr:float	lon_corr:float --> setted to alt, 0.,0. 
  
  # set metertofeet to be 0.3048 if alt should be in feet
  # Set default parameter (they will be assumed if no extraction can help)
  
  l1_flags=16          # Land
  detector_index=512   # CCD
  latitude=$lat
  longitude=$lon
  dem_alt=$alt         # in meters integer
  dem_rough=0
  lat_corr=0.0
  lon_corr=0.0
  zonal_wind=0.0
  merid_wind=0.0
  atm_press=1013.25 # `echo $alt | awk '{print 1013.25*exp(-($1*.001) / 8.4 )}'` (see L1b MERIS extractions: the pressure appears to be reported to ground level)
  ozone=`awk 'NR==1 {print $10*1000.}' tmpfile`
  rel_hum=60.0
  
  
  if [[ `wc -l < extraction.$SITE` -gt 2 ]] ; then
    # redefine previous quantities based on 
    # Il nostro dataset dovrebbe essere un subset di questo in quanto esisteva l'AOD e quindi 
    # doveva esser sereno.
    if `grep -E "${d[0]} +${d[1]}" extraction.$SITE > tmpext` ; then
      l1_flags=`awk '{print $3}' tmpext`
      detector_index=`awk '{print $4}' tmpext`
      # latitude=`awk '{print $5}' tmpext`
      # longitude=`awk '{print $6}' tmpext`
      dem_alt=`awk '{print $7}' tmpext`
      dem_rough=`awk '{print $8}' tmpext`
      lat_corr=`awk '{print $9}' tmpext`
      lon_corr=`awk '{print $10}' tmpext`
      zonal_wind=`awk '{print $11}' tmpext`
      merid_wind=`awk '{print $12}' tmpext`
      atm_press=`awk '{print $13}' tmpext`
      ozone=`awk '{print $14}' tmpext`
      rel_hum=`awk '{print $15}' tmpext`
    else
      echo -e "\e[31m[Warning] Instant ${d[*]} not found on extraction.$SITE, maintaining defaults\e[0m" > /dev/stderr
    fi
  fi
  
  # Writing the final formatted file
  
#   awk 'BEGIN {metertofeet = 1. } ; 
#   { r[NR] = $25+$26+$27 } ;
#   END {OFS="\t" ; 
#   print '$((i-1))', $1 "T" $2 , $1 "T" $2 , 
#   r[1], r[2], r[3], r[4], r[5], r[6], r[7], r[8], r[9], r[10], r[11], r[12], r[13], r[14], r[15], 
#   "16","595", '$lat', '$lon', int('$alt'/metertofeet),
#   int('$alt'/metertofeet),0.,0.,$3, $4, $5, $6,
#   "0.0","0.0",1013.25*exp(-('$alt'/1000) / 8.4 ),$10*1000.,60. }' tmpfile 

  awk '{ 
  # defining ToA radiances for all channels 
  r[NR] = $25+$26+$27 } ;
  
  # writing output
  END {OFS="\t" ;
  print '$((i-1))', $1 "T" $2 , $1 "T" $2 , 
  r[1], r[2], r[3], r[4], r[5], r[6], r[7], r[8], r[9], r[10], r[11], r[12], r[13], r[14], r[15], 
  '$l1_flags','$detector_index', '$latitude', '$longitude', '$dem_alt' , '$dem_rough', 
  '$lat_corr','$lon_corr',$3, $4, $5, $6,
  '$zonal_wind','$merid_wind','$atm_press','$ozone','$rel_hum'}' tmpfile 

done >> $oufile

rm tmpfile
rm tmpext

}

function ToASim2JrcFaparWrapper(){

# Reformat output file from ToA simulation performed with MakeTOANovember script
# to suite JRC-FAPAR algorithm (see Mirko Marioni)
# The output format is adopted from ToASim2sppWrapper().
# Creates L1b time series for the fisrt step of JRC-FAPAR chain 
# Updates
# 2017-06-29 : Creates MERIS TOA BRF time series in the same format

if [[ ! $2 ]] ; then echo "Use ToASim2JrcFaparWrapper <file> <instr>" ; return ; fi
infile=$1
instr=$2
avh=${instr:0:3}
tmpname=$(basename $infile)
SITE=$(echo $tmpname | awk -F_ '{print $2}')
echo $SITE

# defining position of the output variables:
case $instr in
"D3-5_") 
  grep ^[1,2] $infile | awk '{print $1,$2}' | sort | uniq --check-char=19 > list_of_dates.txt
  echo Input site ; read SITE
  echo "Input lat" ; read lat
  echo "Input lon" ; read lon
;;
*)
  grep ^[1,2] $infile | awk 'NF>20 {print $1,$2}' | sort | uniq --check-char=19 > list_of_dates.txt # list of dates
  lat=`grep Latitude $infile | awk '{print $3}'`
  lon=`grep Longitude $infile | awk '{print $3}'`

;;

esac

echo $SITE

l=`wc -l < list_of_dates.txt`

# ciclo sulle date che estrae da file di input solo 
# le righe corrispondenti alla data corrente e rimappa 
# i dati in un record unico secondo la tabella seguente:

# Re-Mapping Column Table
# output file # beam # what is 
# he=Header
# NA=not available
# ND=not defined
#-----------------------------
# ND   # 1	featureId	0                         * progressive number of the record starting from 0
# 1T2  # 2	start_time:time	2010-06-01T12:45:00       * yyyy-mm-ddTHH:MM:SS
# 1T2  # 3	stop_time:time	2010-07-01T13:57:23
# 18(15)   # 4	SREFL_CH1:float	[0-1]
# 18(15)   # 5	SREFL_CH2:float	[0-1]
# NA   # 6	l1_flags:short	16bit                     * 
# Here # 7	latitude:float	[-90:90]
# Here # 8	longitude:float	[-180:180]
# 3    # 9	SZEN:float	[0:90]
# 5    # 10	VZEN:float	[0:90]
# 6    # 11	RELAZ:float	[-360:360]
# sum  # 12	SREFL_CH1:float	[]
# sum  # 13	SREFL_CH2:float	[]

# TODO: define the flags (from real measurements?)
#       define a specific flag for MERIS
## pasteExtractionFiles $SITE > extraction.$SITE

deftiles $SITE
echo "$SITE altitude in meter is $alt"
# sleep 2

rm /tmp/${avh}*.tsv # clean previous output files to avoid mixing sites on archive files (see avh.tar) 

for i in `seq $l`
do
  d=(`awk 'NR=='$i'' list_of_dates.txt`)
  grep -E "${d[0]} +${d[1]}" $infile > tmpfile

  case $instr in 
  "AVHRR") 	# TOA radiance as reported for last two column is not used by the processor
			# It is reported here for some check (can be removed?)
			# reflectance are defined from TOC (cols 18's) see below
			
			local ofile=/tmp/${avh}09C1.A`date -u -d "${d[0]}" +%Y%j`.N16.004.`date +%Y%m%d%H%M%S`.tsv #tab separated values
			echo "featureId	start_time:time	stop_time:time	SREFL_CH1:float	SREFL_CH2:float	l1_flags:short	latitude:float	longitude:float	SZEN:float	VZEN:float	RELAZ:float	TOA_RADIANCE_CH1:float	TOA_RADIANCE_CH2:float" > $ofile
            ;;
            
  "D3-5_") 
	# define the input files from TOC Tables6S files
	# latitude and longitude are not included in files so read from stdin ...

	local ofile=/tmp/${avh}09C1.A`date -u -d "${d[0]}" +%Y%j`.N16.004.`date +%Y%m%d%H%M%S`.tsv #tab separated values
	echo "featureId	start_time:time	stop_time:time	SREFL_CH1:float	SREFL_CH2:float	l1_flags:short	latitude:float	longitude:float	SZEN:float	VZEN:float	RELAZ:float	TOA_RADIANCE_CH1:float	TOA_RADIANCE_CH2:float" > $ofile
            ;;
            
  "MERIS")  # TOA radiance is not reported here
			# reflectances are defined from TOA (cols 15's) see below
			# MERIS can have more than 1 product per day!! The filename account for this.
			local ofile=/tmp/${avh}xxxx.A`date -u -d "${d[0]} ${d[1]}" +%Y%j.%H%M`.yyy.zzz.`date +%Y%m%d%H%M%S`.tsv #tab separated values
			echo "featureId	start_time:time	stop_time:time	RTOA_CH1:float	RTOA_CH2:float	RTOA_CH3:float	RTOA_CH4:float	RTOA_CH5:float	RTOA_CH6:float	RTOA_CH7:float	RTOA_CH8:float	RTOA_CH9:float	RTOA_CH10:float	RTOA_CH11:float	RTOA_CH12:float	RTOA_CH13:float	RTOA_CH14:float	RTOA_CH15:float	l1_flags:short	latitude:float	longitude:float	SZEN:float	VZEN:float	RELAZ:float" > $ofile
            ;;
   *) echo "ToASim2JrcFaparWrapper> no rule for $instr is defined" ; exit 99 ;;
  esac
  
  echo "ToASim2JrcFaparWrapper: *** Creating $ofile"

  latitude=$lat
  longitude=$lon
  
  b[0]=0  # unused
  b[1]=0  # cloudy
  b[2]=0  # cloud shadows
  b[3]=0  # land =0 / water=1
  b[4]=0  # sun glint risk
  b[5]=0  # 1 = dense dark veg (see tropical forest and Estonia values)
  b[6]=0  # night (hight solar zenith angle) polar?
  b[7]=0  # unused refl 1-5 are invalid
  b[8]=0  # REFL1 invalid
  b[9]=0  # REFL2 invalid
  b[10]=1  # CH3 invalid
  b[11]=1  # CH4 invalid
  b[12]=1  # CH5 invalid
  b[13]=1  # RHO3 invalid
  b[14]=0  # brdf corr issues
  b[15]=0  # polar flags (only domec) > 60deg over land
 
  bcomp=`echo ${b[*]} | sed -e 's/ //g' | rev` 
  l1_flags=`echo "ibase=2;$bcomp" | bc`
  
  # Writing the final formatted file
  # Jrc-Fapar algh accepts:
  # 1) TOC BRF for AVHRR (ch1 - ch2)
  # 2) TOA BRF for MERIS (ch8 - ch13) l1_flags MERIS?
  
  case $instr in 
    "MERIS") rcol=15 
    
	  awk '{ 
	  # defining ToA radiances for all channels 
	  refl[NR] = $'$rcol'
	  r[NR] = $25+$26+$27};
	  
	  # writing output
	  END {OFS="\t" ;
	  print '$((i-1))', $1 "T" $2 , $1 "T" $2 , 
	  refl[1], refl[2],refl[3],refl[4],refl[5],refl[6],refl[7],refl[8],refl[9],refl[10],refl[11],refl[12],refl[13],refl[14],refl[15],'$l1_flags', '$latitude', '$longitude', $3, $5, $6 - $4}' tmpfile >> $ofile
    
    ;;
    
    
    "AVHRR") rcol=18
    
	  awk '{ 
	  # defining ToA radiances for all channels 
	  refl[NR] = $'$rcol'
	  r[NR] = $25+$26+$27};
	  
	  # writing output
	  END {OFS="\t" ;
	  print '$((i-1))', $1 "T" $2 , $1 "T" $2 , 
	  refl[1], refl[2], '$l1_flags', '$latitude', '$longitude', $3, $5, $6,
	  r[1], r[2]}' tmpfile >> $ofile
	  
	  ;;

      "D3-5_") rcol=16
    
	  awk '{ 
	  # defining ToA radiances for all channels 
	  refl[NR] = $NF
	  r[NR] = -9};
	  
	  # writing output
	  END {OFS="\t" ;
	  print '$((i-1))', $1 "T" $2 , $1 "T" $2 , 
	  refl[1], refl[2], '$l1_flags', '$latitude', '$longitude', $3, $5, $6,
	  r[1], r[2]}' tmpfile >> $ofile
	  
	  ;; 
	  
	  *) return  ;; 
  esac
	  rm tmpfile    

done

# Archiving results (to be processed with spp-fapar IDL)
tar cvf ${infile/out/tar} /tmp/${avh}*.tsv

## To extract use: tar -xf archive.tar -C /target/directory

}

function getyeartile(){
# get the year and tile from a BBDR filename (MERIS)
# get doy
  ifile=$1
  local yea1=${ifile/*PNMAP}
  year=${yea1:0:4}
  month=${yea1:4:2}
  day=${yea1:6:2}
  tile=${ifile/*BBDR_/}
  tile=${tile:0:6}
  # get even the jday
  jd0=${ifile/*PNMAP}
  jd0=${jd0:0:8}
  jd=`date -d "$jd0" +%j`
}

function verif_sdr(){
set -e
# Now incorporate the plot: function p(){} is removed

# Verifica le sdr (spectral surface directional reflectance) retrievata dal primo step del spp (BBDR)
# rispetto a quelle "nascoste" alla procedura ma contenute nel file delle simulazioni 6S.
# Questo confronto e' quello che ci consente di determinare le performance
# del processore nel retrieval delle proprietà della superficie.
# In questa versione vengono confrontati anche i valori di AOT (col 17: BBDR, col 7: 6S file).

# Facciamo le time patterns

local ifile1=$1
local ifileole=/home/lancoch/Work/6Ssim/6s/Outole/`basename ${ifile1/.out/.ole}`   # the 6S independent inversions (dated >2017-06-28)
local ifileln=/home/lancoch/Work/6Ssim/${ifile1/Output/Outputln}                   # 

# Il file e' lo stesso passato al nostro wrapper ed origina dagli Output/
# ex Output/1-Giovanni/
# Il nome del sito viene estratto dal nome del file 
# Il secondo e' un cat dei file con template : 
# "ga_testdata/bbdr/subset_0_of_MER_RR__1PNMAPyyyymmdd_hhmmss
# In questop esempio dato che sappiamo esserci solo una misura per giorno possiamo 
# leggere il primo file riga per riga, definire la variabile yyyymmdd ed estrrarre le 
# colonne delle riflrettanze alla superficie per entrambi. 

local tmpname=$(basename $ifile1)
SITE=$(echo $tmpname | awk -F_ '{print $2}')
deftiles $SITE
echo $SITE $tile ${coordmeris[*]}
echo "File delle inversioni   6S: $ifileole (`wc -l < $ifileole`)"
echo "File dei calcoli localnoon: $ifileln (`wc -l < $ifileln`)"
sleep 3
# read ans
# return

# Estraggo la lista temporale
grep ^2 $ifile1 | awk 'NF>20 {print $1,$2}' | sort -u  > list_of_dates.txt
head list_of_dates.txt
cband=( "VIS" "NIR" "SW" )

for band in {1..15} {-22..-20}
do
##    echo "# 1.date 2.time 13.band aod ozo wv ToA_R ToC_BRF ToC_BHR ToC_HDR srd: ToC_spp aod_spp"
    while read d t 
    do
       ban1=$((band+24))
       local d2=${d//-}
       local t2=${t//:}
       
       # sdr is an array containing spectral sdr and constant aod550 as read from the BBDR file 
       local bbdrfile=subset_0_of_MER_RR__1PNMAP${d2}_${t2}*${tile}_${coordmeris[0]}_${coordmeris[1]}_v${version}.csv
       local tmpdir=ga_testdata/bbdr
       local finaldir=ga_testdata/BBDR_single/$sensor/${d2:0:4}/$tile
       # ga_testdata/Albedo_single/2004/h18v04/NoSnow/GlobAlbedo.albedo.single.2004.025.h18v04.0749.0565.NoSnow.csv
       
       # The above file could be moved to final repository directory with 
       # the action movebbdr. But checking bbdr/ temporary directory first.
       
       if [ -e $tmpdir/$bbdrfile ] ; then 
	      local sdr=( `awk 'NR==2 {print $'$ban1',$17}' $tmpdir/$bbdrfile` )    # column(17) is the retrieved AOD
       elif [ -e $finaldir/$bbdrfile ] ; then 
     	  echo -ne "$bbdrfile exist in bbdr/ or BBDR_single/ dirs structure\r" > /dev/stderr
	      local sdr=( `awk 'NR==2 {print $'$ban1',$17}' $finaldir/$bbdrfile` )
       else
     	 echo -ne "\e[33m$bbdrfile does not exist in bbdr/ or BBDR_single/ dirs structure\e[0m\r" > /dev/stderr
		 local sdr=( "NaN" "NaN" )
       fi
     
       if [[ $band -gt 0 ]] ; then
       
		   # Determines BRF retrieved by 6S inversion (LER) ole files:
		   local ler="NaN"
		   # column(6) contain the 6S inversion in lambertian assumption
		   # column(7) w a evaluation of BRF (?) 
		   local lertentative=`grep -E ^"${d} +${t}" $ifileole | awk --traditional '$3=='$band' {print $6}' | head -n 1`
		   [[ $? -eq 0 ]] && ler=$lertentative
		   echo -e "\nler is $ler" > /dev/stderr
		   [[ $ler == " " ]] && echo ler is null
		   
           # Scrittura
           grep -E ^"${d} +${t}" $ifile1 | awk --traditional '$13=='$band' {NaN="NaN"; print $0,"bbdr_sdr>",'${sdr[0]}','${sdr[1]}','${ler}'}'
       
       elif [[ $band -lt 0 ]] ; then
       
       # to compute R_vis,R_nir,R_sw (with R = dhr, bhr, brf)
       # as a combination of bands (the above files contains 15 time series each relevant
       # to a meris band.
       # here I grep each date of list_of_dates.txt 

       # combining bands to create ToC **original** DHR (from cols 16) and BHR (from cols 17), and brf(cols 18)
       
           # TODO: La colonna 19 e' utilizzata per BSA ma e' robar (hemispherical-directional reflectance with a sun at Theha_s)
           #       per il principio di reciprocita' nn dovrebbe cambiare ma la versione 2 del processore forward metteva in 
           #       colonna 16!! il BSA calcolato con il 6S senza radianze del cielo.
           
           # grep -E ^"${d} +${t}" $ifile1 | awk '{print $19,$17,$18}' | transpose.awk > /tmp/tocdhrbhr.txt
           grep -E ^"${d} +${t}" $ifile1 | awk '{print $16,$17,$18}' | transpose.awk > /tmp/tocdhrbhr.txt
           grep -E ^"${d} "      $ifileln | awk '{print $16,$17,$18}' | transpose.awk > /tmp/tocdhrbhr-ln.txt  # The FIX (2017-06-28) to grep the DHR from localnoon files

           tocdhr=( `awk 'NR==1' /tmp/tocdhrbhr-ln.txt` )  # Directional-Hemispherical Reflectance (from localnoon files)
           tocbhr=( `awk 'NR==2' /tmp/tocdhrbhr.txt` )  # Bi-Hemispherical Reflectance
           tocbrf=( `awk 'NR==3' /tmp/tocdhrbhr.txt` )  # Bidirectional Reflectance
           
           # rm /tmp/tocdhrbhr.txt
           bbTocdhr=( `n2b ${tocdhr[*]}` )  # This should originate from localnoon files ($ifileln)
           bbTocbhr=( `n2b ${tocbhr[*]}` )
           bbTocbrf=( `n2b ${tocbrf[*]}` )
           
#            echo ${bbTocdhr[*]}
#            echo ${bbTocbhr[*]}
#            echo ${bbTocbrf[*]}
           
           # And the results of the spp for albedo as well (the brf are already extracted before)
           ye=${d:0:4}
           jd=`date -d "$d" +%j`
           
           albefile=ga_testdata-v6-nosnow/Albedo_single/$ye/$tile/NoSnow/GlobAlbedo.albedo.single.$ye.$jd.$tile.${coordmeris[0]}.${coordmeris[1]}.NoSnow.csv
           
           local albe=( "NaN" "NaN" ) 
           if [[ -s $albefile ]] ; then
			   # here ban1 = band + 24 then -22(-21,-20) + 24 = 2(3,4)
			   # column 2,3,4      of albe file are taken to extract dhr(vis,nir,sw) as albe(0)
			   # column 5,6,7 (+3) of albe file are taken to extract bhr(vis,nir,sw) as albe(1)
			   
			   albe=( `awk 'NR==2 {print $'$ban1',$'$[ban1+3]'}' $albefile` )
           fi

           # Scrittura (utilizzo il file di input come template inserendo le opportune variazioni laddove
           # ho calcolato i BB reflectance VIS, NIR e SW (ovvero per i primi tre cicli $band)

           grep -E ^"${d} +${t}" $ifile1 | awk --traditional '$13==1 {NaN="NaN"; print $1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,"'${cband[$band+22]}'",NaN,NaN,'${bbTocdhr[$band+22]}','${bbTocbhr[$band+22]}','${bbTocbrf[$band+22]}',NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN,"bbdr_sdr>",'${sdr[0]}','${sdr[1]}','${albe[1]}','${albe[0]}' }'
           fi
    done < list_of_dates.txt
    
    echo -e "\n\n" # this double carriage return separates index block 
    
done 1> to_be_plotted.txt

echo """set term pngcairo enhanced color size 800,400 #960,480
set size square
set format x '%.2g'
set format y '%.2g'
set xtics rotate by -60 # font 'Arial, 9'
set output 'to_be_plotted.png'
set key bottom right
set multiplot layout 1,2 #title '$SITE' font 'Arial,18'
set label 3 '$SITE' at graph 0.1,0.92 font 'Arial,16'
# set xrange [0:*]
# set yrange [0:*]
set xlabel 'TOC BRF (Raytran)'
set ylabel 'TOC SDR (GA Inv)'
set label 2 'script2.sh:verif_sdr(),`date +%Y%m%d`,`hostname`' at screen 0.99,0.5 font 'Arial,8' rotate center noenhanced
arrkey='01 02 03 04 05 06 07 08 09 10 11 12 13 14 15 VIS NIR SW'
arrcol='01 02 03 04 05 06 07 08 09 10 11 12 13 14 15 16  17  18'

set label 1 '(Spectral)' at graph 0.1,0.85 
p for [ j in '2 3 5 7 13' ] 'to_be_plotted.txt' index (j-1) u 18:31 w p lc (j+0) t 'MB'.word(arrkey,j+0),\
                  x lt 0 lw 2 notitle
                  
set xlabel 'TOC BRF (Raytran+n2b)'
set ylabel 'TOC BBDR (GA Inv)'
set label 1 '(Broadband)' font 'Arial,10'
p for [ j in '16 17 18' ] 'to_be_plotted.txt' index (j-1) u 18:31 w p lc (j+0) t 'BB'.word(arrkey,j+0),\
                  x lt 0 lw 2 notitle 
                  
#set label 1 '(spectral LER)'
#set xlabel 'TOC BRF (Inv 6S)'
#set ylabel 'TOC SDR (Inv GA)'
#p for [ j in '2 3 5 7 13' ] 'to_be_plotted.txt' index (j-1) u 33:31 w p lc (j+0) t 'MB'.word(arrkey,j+0),\
                  #x lt 0 lw 2 notitle
                  
unset multiplot""" | gnuplot

if [[ $? -eq 0 ]] ; then 
	# moving files
	mv -v to_be_plotted.png ${ifile1/.out/_perf.png}
	mv -v to_be_plotted.txt ${ifile1/.out/_perf.txt}	
fi

}

function verif_sdr_justplot(){

# NOTE: but even some kind of analysis

local ifile1=$1
local i2file=${ifile1/.out/_perf.txt}
local i2filepng=${ifile1/.out/_perf2.png}

if [ -e $i2file ] ; then

# Time patterns for MERIs bands involved in the N2B conversion rormula
# R2,R3,R5,R7,R13 and AOT, ozone, wv.

echo """set term pngcairo enhanced color size 1024,1024
# set xtics font 'Arial, 9' rotate by 60
set output '$i2filepng'
set key font 'Arial,10' #bottom right
wave='0.412 0.442 0.489 0.509 0.559 0.619 0.664 0.681 0.708 0.753 0.760 0.778 0.865 0.885 0.900 VIS NIR SW'
cnames='    black red      black black cyan black yellow      black black black  black  black magenta       black  black green      red      blue'
cnamesdark='black dark-red black black cyan black dark-yellow black black black  black  black dark-magenta  black  black dark-green dark-red dark-blue'

do for [ i in '2 3 5 7 13 16 17 18' ] {
  stats '$i2file' index (i-1) u 18    name 'E'.sprintf('%02.0f',i+0)                       # E = Expected
  stats '$i2file' index (i-1) u 31    name 'O'.sprintf('%02.0f',i+0)                       # O = Observed (retrieved)
  stats '$i2file' index (i-1) u 18:31 name 'B'.sprintf('%02.0f',i+0)                       # correlation and general statistics
  stats '$i2file' index (i-1) u ((\$31-\$18)**2/\$18) name 'chi2B'.sprintf('%02.0f',i+0)   # chi-squared (the sum is X^2)
  stats '$i2file' index (i-1) u ((\$31-\$18)**2)      name 'rmseB'.sprintf('%02.0f',i+0)   # rmse
}

rmse02 = sqrt(rmseB02_sum/rmseB02_records) / (E02_mean)
rmse05 = sqrt(rmseB05_sum/rmseB05_records) / (E05_mean)
rmse07 = sqrt(rmseB07_sum/rmseB07_records) / (E07_mean)
rmse13 = sqrt(rmseB13_sum/rmseB13_records) / (E13_mean)
rmse16 = sqrt(rmseB16_sum/rmseB16_records) / (E16_mean)
rmse17 = sqrt(rmseB17_sum/rmseB17_records) / (E17_mean)
rmse18 = sqrt(rmseB18_sum/rmseB18_records) / (E18_mean)

cv02 = sqrt(rmseB02_sum/rmseB02_records) / (E02_max - E02_min)
cv05 = sqrt(rmseB05_sum/rmseB05_records) / (E05_max - E05_min)
cv07 = sqrt(rmseB07_sum/rmseB07_records) / (E07_max - E07_min)
cv13 = sqrt(rmseB13_sum/rmseB13_records) / (E13_max - E13_min)
cv16 = sqrt(rmseB16_sum/rmseB16_records) / (E16_max - E16_min)
cv17 = sqrt(rmseB17_sum/rmseB17_records) / (E17_max - E17_min)
cv18 = sqrt(rmseB18_sum/rmseB18_records) / (E18_max - E18_min)

tab=sprintf(\"List bands :                  B02    B05    B07    B13    VIS     NIR    SW\n\
Correl r    : %6.2f %6.2f %6.2f %6.2f %6.2f %6.2f %6.2f\n\
Chi Squared : %6.4f %6.4f %6.4f %6.4f %6.4f %6.4f %6.4f\n\
Samples     : %6.0f %6.0f %6.0f %6.0f %6.0f %6.0f %6.0f\n\
nRMSE       : %6.3f %6.3f %6.3f %6.3f %6.3f %6.3f %6.3f\n\
cvRMSE      : %6.3f %6.3f %6.3f %6.3f %6.3f %6.3f %6.3f\n\
Exp Min     : %6.3f %6.3f %6.3f %6.3f %6.3f %6.3f %6.3f\n\
Exp Max     : %6.3f %6.3f %6.3f %6.3f %6.3f %6.3f %6.3f\",\
B02_correlation, B05_correlation, B07_correlation, B13_correlation, B16_correlation, B17_correlation, B18_correlation,\
chi2B02_sum/chi2B02_records,chi2B05_sum/chi2B02_records,chi2B07_sum/chi2B02_records,chi2B13_sum/chi2B02_records,chi2B16_sum/chi2B02_records,chi2B17_sum/chi2B02_records,chi2B18_sum/chi2B02_records,\
chi2B02_records, chi2B05_records, chi2B07_records,chi2B13_records,chi2B16_records,chi2B17_records,chi2B18_records,\
rmse02,rmse05,rmse07,rmse13,rmse16,rmse17,rmse18,\
cv02,cv05,cv07,cv13,cv16,cv17,cv18,\
E02_min,E05_min,E07_min,E13_min,E16_min,E17_min,E18_min,\
E02_max,E05_max,E07_max,E13_max,E16_max,E17_max,E18_max)

set print 'analysis.txt' append
print \"`date` File : $i2file\"
print tab

# set label tab at screen .65,.30 font 'mono,10'  
set xdata time
set timefmt '%Y-%m-%d %H:%M:%S'
set mxtics 2
set format x '%b'
set format y '%5g'
## set label 'verif_sdr_justplot()' at screen 0.01,0.01 font 'Arial,6'
set style line 2 linetype 2 
set key above
set multiplot layout 3,2 title '$SITE' noenhanced columnsfirst

  set size .6,.3
  set xrange ['2003-01-01 00:00:00' : '2005-01-01 00:00:00']
  set yrange [*:*]
  set ylabel 'BRF'
  
  # p for [ i in '2 5 7 13 16 17 18' ]
  p for [ i in '5 16 17 18' ] '$i2file' index (i-1) u 1:18 w lp t 'B'.sprintf('%02.0f',i+0).'('.(word(wave,i+0)).')' lc rgb word(cnames,i+0) pt 6,\
    for [ i in '5 16 17 18' ] ''        index (i-1) u 1:31 w lp notitle lc rgb word(cnamesdark,i+0) pt 4,\
    '' index 4 u 1:15 t 'ToA (0.559)' w l lt -1
    
    
  set ylabel 'AOT'
  set yrange [0:*]
  set size .6,.3
  p '' index 0 u 1:7 w lp t '6S (forward)' lc 0 pt 6, '' index 0 u 1:32 w lp t 'BEAM (retrieved)' lc 0 pt 4 
  set ylabel 'Angle deg'
  set yrange [0:70]
  set size .6,.3
  p '' index 0 u 1:3 w lp t 'SZA' lc 0, '' index 0 u 1:5 w lp t 'VZA' lc rgb 'red'

  # second column
  
  set size .4,.3 
  set size square
  set origin .6,.66
  set xlabel 'Forward' ; set ylabel 'Retrieved'
  unset xdata 
  set key off
  set xrange [0:*] ; set yrange  [0:*]
  set format x '%g'
  p for [ i in '5 16 17 18' ] '$i2file' index (i-1) u 18:31 w p t 'B'.i.'('.(word(wave,i+0)).')' lc rgb word(cnamesdark,i+0) pt 6,x lt -1 notitle
  set origin .6,.33
  set size .4,.3 
  p '' index 0 u 7:32 w p pt 6 lc 0, x lt -1
  
unset multiplot
""" | gnuplot

fi
echo "$i2filepng created..."
}

function verif_sdr_justplot_a(){

# NOTE: but even som kind of analysis 
# versione per albedo
# molte cose sono superflue

local ifile1=$1
local i2file=${ifile1/.out/_perf.txt}
local i2filepng=${ifile1/.out/_perf3.png}  # file albedo bhr, dhr

if [ -e $i2file ] ; then

# Time patterns for MERIs bands involved in the N2B conversion rormula
# R2,R5,R7,R13 and AOT, ozone, wv.

echo """set term pngcairo enhanced color size 960,640
set output '$i2filepng'
set key font 'Arial,10' #bottom right
wave='0.412 0.442 0.489 0.509 0.559 0.619 0.664 0.681 0.708 0.753 0.760 0.778 0.865 0.885 0.900 VIS NIR SW'
cnames='    black red      black black cyan black yellow      black black black  black  black magenta       black  black green      red      blue'
cnamesdark='black dark-red black black cyan black dark-yellow black black black  black  black dark-magenta  black  black dark-green dark-red dark-blue'

# BHR (WSA) : 17 vs 33
# DHR (BSA) : 16 vs 34

# === statistiche =====================================================================


do for [ i in '16 17 18' ] {
  stats '$i2file' index (i-1) u 17    name 'E'.sprintf('%02.0f',i+0)                       # E = Expected
  stats '$i2file' index (i-1) u 33    name 'O'.sprintf('%02.0f',i+0)                       # O = Observed (retrieved)
  stats '$i2file' index (i-1) u 17:33 name 'B'.sprintf('%02.0f',i+0)                       # correlation and general statistics
  stats '$i2file' index (i-1) u ((\$33-\$17)**2/\$17) name 'chi2B'.sprintf('%02.0f',i+0)   # chi-squared (the sum is X^2)
  stats '$i2file' index (i-1) u ((\$33-\$17)**2)      name 'rmseB'.sprintf('%02.0f',i+0)   # rmse
}

rmse16 = sqrt(rmseB16_sum/rmseB16_records) / (E16_mean)
rmse17 = sqrt(rmseB17_sum/rmseB17_records) / (E17_mean)
rmse18 = sqrt(rmseB18_sum/rmseB18_records) / (E18_mean)
cv16 = sqrt(rmseB16_sum/rmseB16_records) / (E16_max - E16_min)
cv17 = sqrt(rmseB17_sum/rmseB17_records) / (E17_max - E17_min)
cv18 = sqrt(rmseB18_sum/rmseB18_records) / (E18_max - E18_min)

tab=sprintf(\"List bands :   VIS     NIR    SW\n\
Correl r    : %6.2f %6.2f %6.2f\n\
Chi Squared : %6.4f %6.4f %6.4f\n\
Samples     : %6.0f %6.0f %6.0f\n\
nRMSE       : %6.3f %6.3f %6.3f\n\
cvRMSE      : %6.3f %6.3f %6.3f\n\
Exp Min     : %6.3f %6.3f %6.3f\n\
Exp Max     : %6.3f %6.3f %6.3f\",\
B16_correlation, B17_correlation, B18_correlation,\
chi2B16_sum/chi2B16_records,chi2B17_sum/chi2B17_records,chi2B18_sum/chi2B18_records,\
chi2B16_records,chi2B17_records,chi2B18_records,\
rmse16,rmse17,rmse18,\
cv16,cv17,cv18,\
E16_min,E17_min,E18_min,\
E16_max,E17_max,E18_max)

set print 'analysis.txt' append
print \"`date` File : $i2file\"
print tab

# === grafici =========================================================================

set xdata time
set timefmt '%Y-%m-%d %H:%M:%S'
set mxtics 2
set format x '%b'
set format y '%5g'
set key above
set multiplot layout 2,2 title '$SITE' noenhanced columnsfirst

set size .6,.5
set xrange ['2003-01-01 00:00:00' : '2005-01-01 00:00:00']
set yrange [*:*]
set ylabel 'BHR' font 'Arial,18'
p for [ i in '16 17 18' ] '$i2file' index (i-1) u 1:17 w p t word(wave,i+0) lc rgb word(cnames,i+0) pt 6,\
  for [ i in '16 17 18' ] ''        index (i-1) u 1:33 w p notitle lc rgb word(cnamesdark,i+0) pt 7

set ylabel 'DHR' font 'Arial,18'
set size .6,.5
p for [ i in '16 17 18' ] '$i2file' index (i-1) u 1:16 w p t word(wave,i+0) lc rgb word(cnames,i+0) pt 6,\
  for [ i in '16 17 18' ] ''        index (i-1) u 1:34 w p notitle lc rgb word(cnamesdark,i+0) pt 7

# second column

set xlabel 'Forward' font 'Arial,16' ; set ylabel 'Retrieved' font 'Arial,16'
unset xdata 
set key off
set xrange [0:*] ; set yrange  [0:*]
set format x '%g'
set origin .6,.5
set size .4,.45 
p for [ i in '16 17 18' ] '$i2file' index (i-1) u 17:16 w p t 'B'.i.'('.(word(wave,i+0)).')' lc rgb word(cnamesdark,i+0) pt 6,x lt 0 lw 3 notitle
set origin .6,.0
set size .4,.45 
p for [ i in '16 17 18' ] '$i2file' index (i-1) u 16:34 w p t 'B'.i.'('.(word(wave,i+0)).')' lc rgb word(cnamesdark,i+0) pt 6,x lt 0 lw 3 notitle
  
unset multiplot
""" | gnuplot
fi

echo "$i2filepng created..."

}

# Functions related to Snow processing
function isClearSnow(){
# 2016-December
# MERIS version works with b13 and b14
# A simple implementation of the snow flag as described in par 11.5.4 of GA ATDB v4.12m
# It is the reflectance corrected for Rayleigh and Gas Absorption (maybe aerosol/to ask)
if [ $2 ] ; then 
  b13=$1
  b14=$2
  local isBrightWhite=1
  # questo crea un array contenente ( ndsi flag_ndsi ) con il secondo termine 0/1 in accordo ad un valore di soglia
  local ndsiValue=( `echo $b13 $b14 | awk '{ndsi=($1-$2)/($1+$2)} ; print ndsi, ( ndsi>0.68 ? 1 : 0 ) }'` )    # MERIS
  
  if [[ $isBrightWhite -eq 1 && ${ndsiValue[1]} -eq 1 ]] ; then
    echo 1
  else
    echo 0
  fi
  return 0 # no error (echo $?)
else
  echo "[Warning] This function should be used as >isClearSnow b13 b14" 
  return 77 # error code
fi
}

# A statistic plot based on analysis.txt
function verif_sdr_statistiche(){
local sitename=( dom jan jar1 jar2 lib lop ngh ofe sku thg wel zer ) # questo e' l'ordine dei siti in analysis.txt
sorted=0
n=0
for togrep in nRMSE cvRMSE Correl Chi
do
  if [[ $sorted -eq 1 ]] ; then
    grep $togrep analysis.txt | awk '{print $(NF-5),$(NF-2),$(NF-1),$NF}' | sort -k 2  # faccio un sort rispetto al valore alla banda 5 (.559 nm)
    grep $togrep analysis.txt | awk '{print $(NF-5),$(NF-2),$(NF-1),$NF}' | nl -v 0 | sort -k 3 | awk '{print $1}' > /tmp/1.$n # faccio un sort rispetto al valore alla banda 5 (.559 nm)
  else
    # not sorted (alphabetic order)
    grep $togrep analysis.txt | awk '{print $(NF-5),$(NF-2),$(NF-1),$NF}'
    grep $togrep analysis.txt | awk '{print $(NF-5),$(NF-2),$(NF-1),$NF}' | nl -v 0 | awk '{print $1}' > /tmp/1.$n # faccio un sort rispetto al valore alla   
  fi
  
  echo -e "\n\n"                                                         # divido il file in index per eventuali plot multipli
  n=$[$n+1]
done > /tmp/1

# "$i" e' la variabile che indica la posizione del label sull'asse delle y, mentre $n 
# e' l'identificativo che permette di estrarre la label dall'array sitename.
a=`i=0; while read n ; do echo -n \"${sitename[$n]}\" $i, ; i=$[i+1] ; done < /tmp/1.0`
b=`i=0; while read n ; do echo -n \"${sitename[$n]}\" $i, ; i=$[i+1] ; done < /tmp/1.1`
c=`i=0; while read n ; do echo -n \"${sitename[$n]}\" $i, ; i=$[i+1] ; done < /tmp/1.2`
d=`i=0; while read n ; do echo -n \"${sitename[$n]}\" $i, ; i=$[i+1] ; done < /tmp/1.3`

echo """sitelist='dom jan jar1 jar2 lib lop ngh ofe sku thg wel zer'
set term pngcairo enhanced size 800,800
set output 'statistiche.png'
set key off 
set size ratio 1.4
cbt='nRMSE cvRMSE  Correlation Chi^2'
set palette rgbformulae  21,22,23 # black-=red-yellow-white 
set palette rgbformulae  31,32,34 # mine
set palette rgbformulae  33,13,10 # rainbow
# set palette rgbformulae  23,28,3  # ocean (green-blue-white)

set xtics ( 'B02' 0, 'B05' 1, 'B07' 2, 'B13' 3)
set xtics ( 'B05' 0, 'VIS' 1, 'NIR' 2, ' SW' 3)  # occhio qui!!!

# set ytics ( 'dom' 0,'jan' 1, 'jar1' 2, 'jar2' 3, 'lib' 4, 'lop' 5, 'ngh' 6,'ofe' 7,'sku' 8,'thg' 9,'wel' 10,'zer' 11 )
set multiplot layout 2,2
set ytics ( $a )
set cblabel word(cbt,1) font 'Arial,16'
plot '/tmp/1' index 0 matrix with image

set ytics ( $b )
set cblabel word(cbt,2)
plot '/tmp/1' index 1 matrix with image

set cbrange [0:*]
set ytics ( $d )
set cblabel 'reduced {/Symbol C}^2'
plot '/tmp/1' index 3 matrix with image

set palette rgbformulae  33,13,10 negative
set ytics ( $c )
set cblabel word(cbt,3)
plot '/tmp/1' index 2 matrix with image

unset multiplot""" | gnuplot
}

function ExctractSinglePrior(){

###
#  To grep the julian day from the full csv file provided as kernel by MSSL 
#  and reformat the row accordingly to BEAM requests (inversion module)
#  If the exact jd is not found uses the nearest one (for MODIS kernel
#  v5 the priors are provided every 8 days from jd=001).
###

if  [[ $2 ]] ; then 

local psource=$1 # is the fullpath to the source prior
local jd=$2
  

  # grep the header that defines column content of the csv kernel file
  # and write ann extended header with first column diff(erence) of days
  grep ^year $psource |  awk -F, '{OFS="," ; print "diff",$0}' > kjd.list
  
  # sort the kernel file with respect the squared difference of jd and doy.
  grep ^2 $psource | awk -F, '{OFS="," ; print ($2-'$jd')**2,$0}' | sort -n -k1 >> kjd.list
  
  # now the first row contains the nearest kernel parameter priors available for 
  # the day under investigation
  
  head -n 2 kjd.list | sed -e "s/,/ /g" | transpose.awk > kjd.single 

 
  # here I incorporate a template that will be parsed to create an Olaf's priors
  # kjd.singl will be grepped to find out the second column of the following file

  # check the collection version from file name .c5. or .c6.

  if   [[ $psource =~ .c5. ]] ; then pv=0
  elif [[ $psource =~ .c6. ]] ; then pv=1
  else exit 543
  fi
  echo "pv=$pv" > /dev/stderr

# here I define the name of the variable to be grepped on the Priors (c5 is the index [0], c6 index [1])
      Mean_VIS_f0_avr=( Mean_VIS_f0_avr        BRDF_Albedo_Parameters_vis_f0_avr_avr  )
Cov_VIS_f0_VIS_f0_avr=( Cov_VIS_f0_VIS_f0_avr  BRDF_Albedo_Parameters_vis_f0_sd_avr   )
      Mean_VIS_f1_avr=( Mean_VIS_f1_avr        BRDF_Albedo_Parameters_vis_f1_avr_avr  )
Cov_VIS_f1_VIS_f1_avr=( Cov_VIS_f1_VIS_f1_avr  BRDF_Albedo_Parameters_vis_f1_sd_avr   )
      Mean_VIS_f2_avr=( Mean_VIS_f2_avr        BRDF_Albedo_Parameters_vis_f2_avr_avr  )
Cov_VIS_f2_VIS_f2_avr=( Cov_VIS_f2_VIS_f2_avr  BRDF_Albedo_Parameters_vis_f2_sd_avr   )
      Mean_NIR_f0_avr=( Mean_NIR_f0_avr        BRDF_Albedo_Parameters_nir_f0_avr_avr )
Cov_NIR_f0_NIR_f0_avr=( Cov_NIR_f0_NIR_f0_avr  BRDF_Albedo_Parameters_nir_f0_sd_avr  )
      Mean_NIR_f1_avr=( Mean_NIR_f1_avr        BRDF_Albedo_Parameters_nir_f1_avr_avr )
Cov_NIR_f1_NIR_f1_avr=( Cov_NIR_f1_NIR_f1_avr  BRDF_Albedo_Parameters_nir_f1_sd_avr  )
      Mean_NIR_f2_avr=( Mean_NIR_f2_avr        BRDF_Albedo_Parameters_nir_f2_avr_avr )
Cov_NIR_f2_NIR_f2_avr=( Cov_NIR_f2_NIR_f2_avr  BRDF_Albedo_Parameters_nir_f2_sd_avr  )
       Mean_SW_f0_avr=( Mean_SW_f0_avr         BRDF_Albedo_Parameters_shortwave_f0_avr_avr )
  Cov_SW_f0_SW_f0_avr=( Cov_SW_f0_SW_f0_avr    BRDF_Albedo_Parameters_shortwave_f0_sd_avr )
       Mean_SW_f1_avr=( Mean_SW_f1_avr         BRDF_Albedo_Parameters_shortwave_f1_avr_avr )
  Cov_SW_f1_SW_f1_avr=( Cov_SW_f1_SW_f1_avr    BRDF_Albedo_Parameters_shortwave_f1_sd_avr )
       Mean_SW_f2_avr=( Mean_SW_f2_avr         BRDF_Albedo_Parameters_shortwave_f2_avr_avr )
  Cov_SW_f2_SW_f2_avr=( Cov_SW_f2_SW_f2_avr    BRDF_Albedo_Parameters_shortwave_f2_sd_avr )
        Snow_Fraction=( 0.0                    snowFraction_avr )

  echo """featureId	0
latitude:float	Latitude
longitude:float	Longitude
Mean_VIS_f0:float	${Mean_VIS_f0_avr[$pv]}
Cov_VIS_f0_VIS_f0:float	${Cov_VIS_f0_VIS_f0_avr[$pv]}
Cov_VIS_f0_VIS_f1:float	0.0
Cov_VIS_f0_VIS_f2:float	0.0
Cov_VIS_f0_NIR_f0:float	0.0
Cov_VIS_f0_NIR_f1:float	0.0
Cov_VIS_f0_NIR_f2:float	0.0
Cov_VIS_f0_SW_f0:float	0.0
Cov_VIS_f0_SW_f1:float	0.0
Cov_VIS_f0_SW_f2:float	0.0
Mean_VIS_f1:float	${Mean_VIS_f1_avr[$pv]}
Cov_VIS_f1_VIS_f1:float	${Cov_VIS_f1_VIS_f1_avr[$pv]}
Cov_VIS_f1_VIS_f2:float	0.0
Cov_VIS_f1_NIR_f0:float	0.0
Cov_VIS_f1_NIR_f1:float	0.0
Cov_VIS_f1_NIR_f2:float	0.0
Cov_VIS_f1_SW_f0:float	0.0
Cov_VIS_f1_SW_f1:float	0.0
Cov_VIS_f1_SW_f2:float	0.0
Mean_VIS_f2:float	${Mean_VIS_f2_avr[$pv]}
Cov_VIS_f2_VIS_f2:float	${Cov_VIS_f2_VIS_f2_avr[$pv]}
Cov_VIS_f2_NIR_f0:float	0.0
Cov_VIS_f2_NIR_f1:float	0.0
Cov_VIS_f2_NIR_f2:float	0.0
Cov_VIS_f2_SW_f0:float	0.0
Cov_VIS_f2_SW_f1:float	0.0
Cov_VIS_f2_SW_f2:float	0.0
Mean_NIR_f0:float	${Mean_NIR_f0_avr[$pv]}
Cov_NIR_f0_NIR_f0:float	${Cov_NIR_f0_NIR_f0_avr[$pv]}
Cov_NIR_f0_NIR_f1:float	0.0
Cov_NIR_f0_NIR_f2:float	0.0
Cov_NIR_f0_SW_f0:float	0.0
Cov_NIR_f0_SW_f1:float	0.0
Cov_NIR_f0_SW_f2:float	0.0
Mean_NIR_f1:float	${Mean_NIR_f1_avr[$pv]}
Cov_NIR_f1_NIR_f1:float	${Cov_NIR_f1_NIR_f1_avr[$pv]}
Cov_NIR_f1_NIR_f2:float	0.0
Cov_NIR_f1_SW_f0:float	0.0
Cov_NIR_f1_SW_f1:float	0.0
Cov_NIR_f1_SW_f2:float	0.0
Mean_NIR_f2:float	${Mean_NIR_f2_avr[$pv]}
Cov_NIR_f2_NIR_f2:float	${Cov_NIR_f2_NIR_f2_avr[$pv]}
Cov_NIR_f2_SW_f0:float	0.0
Cov_NIR_f2_SW_f1:float	0.0
Cov_NIR_f2_SW_f2:float	0.0
Mean_SW_f0:float	${Mean_SW_f0_avr[$pv]}
Cov_SW_f0_SW_f0:float	${Cov_SW_f0_SW_f0_avr[$pv]}
Cov_SW_f0_SW_f1:float	0.0
Cov_SW_f0_SW_f2:float	0.0
Mean_SW_f1:float	${Mean_SW_f1_avr[$pv]}
Cov_SW_f1_SW_f1:float	${Cov_SW_f1_SW_f1_avr[$pv]}
Cov_SW_f1_SW_f2:float	0.0
Mean_SW_f2:float	${Mean_SW_f2_avr[$pv]}
Cov_SW_f2_SW_f2:float	${Cov_SW_f2_SW_f2_avr[$pv]}
Weighted_Number_of_Samples:float	5.0
Data_Mask:short	1
Snow_Fraction:float	${Snow_Fraction[$pv]}""" > olaf.prior.template

head olaf.prior.template > /dev/stderr

  while read a b 
  do
  
    # obtaining informations/ zeros are replaced by a random number between 0 and 1e-5 (order 1e-6)
    case $b in
      Mean_*|Cov_*|BRDF_*|snowFraction_avr) c=`grep ^$b kjd.single | awk '{print $2}'` ;;
      ## 0.0) c=`echo $RANDOM | awk '{print (($1/32768.)*2.-1.)*1E-4}'` ;;
      0.0) c=0.0 ;;
      *) c=$b ;;
    esac
    
    echo -e "$a $c"
    c=""
    
  done < olaf.prior.template | transpose.awk | sed -e 's/ /\t/g'

else

  echo "Usage: ExctractSinglePrior <sourcefile> <jd>"
  exit 1

fi

  
}

function pALBEDO(){

# This function print the albedo BHR and DHR as retrieved by the 
# last step of the SPP. 
# =============================================================================
# TODO :
# Moreover, would print the albedo as defined by the Priors 
# and Kernel values as BRF = fiso + fvol * Kvol + fgeo * Kgeo.
# The complication is that 1) these information are stored in different files:
# f_i : use the ExctractSinglePrior() function to get prior for a jd (see source for details)
# K_i : cols 11 to 16 of the BBDR files
# 11	Kvol_BRDF_VIS:float	0.004855931
# 12	Kvol_BRDF_NIR:float	0.002225512
# 13	Kvol_BRDF_SW:float	0.0017828757
# 14	Kgeo_BRDF_VIS:float	-0.94513947
# 15	Kgeo_BRDF_NIR:float	-0.80294037
# 16	Kgeo_BRDF_SW:float	-0.88722456
# and 2) the kernel are computed for JD specific geometry configuration
# so that to compute albedo a geometric integration function would be implemented
# this overpass the purpose of this script)
# NOTE: for now Just the BBDR SW as calculated from priors is written!!!
# =============================================================================
# 
# Plot dei prodotti finali serie temporali
# Il nome del file definishe il tempo 
# ------------------------------------------------------
# Header: 
# ------------------------------------------------------
# 1  featureId       0        
#    DHR_VIS:float   0.09609529       
#    DHR_NIR:float   0.24097078       
#    DHR_SW:float    0.16992591       
# 5  BHR_VIS:float   0.078398876      
#    BHR_NIR:float   0.22076663       
#    BHR_SW:float    0.15281998       
# 8  DHR_alpha_VIS_NIR:float 0.0021686696
#    DHR_alpha_VIS_SW:float  1.8281845E-4     
#    DHR_alpha_NIR_SW:float  0.0034722409     
#    DHR_sigma_VIS:float     0.0075834086     
#    DHR_sigma_NIR:float     0.028406989      
#    DHR_sigma_SW:float      0.022546224      
# 14 BHR_alpha_VIS_NIR:float 0.0022542088     
#    BHR_alpha_VIS_SW:float  2.4385507E-5     
#    BHR_alpha_NIR_SW:float  0.003709799      
#    BHR_sigma_VIS:float     0.0030854736     
#    BHR_sigma_NIR:float     0.012713227      
#    BHR_sigma_SW:float      0.00961418       
# 20 Weighted_Number_of_Samples:float        4.1185374
# 21 Relative_Entropy:float  31.465107
# 22 Goodness_of_Fit:float   -42.80324
# 23 Snow_Fraction:float     0.0
# 24 Data_Mask:float 1.0
# 25 Solar_Zenith_Angle:float        64.84188
# 26 latitude:float  45.295   
# 27 longitude:float 8.877
# -----------------------------------------------------


if [ $1 ] ; then
  local site=$1
  deftiles $site
  for f in ga_testdata/Albedo_single/*/${tile}/NoSnow/GlobAlbedo.albedo.single.????.???.${tile}.${coordmeris[0]}.${coordmeris[1]}.NoSnow.csv
  do
    fname=`basename $f`
    y=${fname//GlobAlbedo.albedo.single.}     ;  y=${y:0:4}
    jd=${fname//GlobAlbedo.albedo.single.$y.} ; jd=${jd:0:3}
    ddate=`date -d "$y-01-01 $[10#$jd-1] days" +%Y%m%d`
    # Prior section:
    # deftiles ha definito anche il source di Priors/
    ExctractSinglePrior ga_testdata/Priors/${pfile} $jd | transpose.awk | grep ^Mean  > /tmp/f_i.txt
    
    if ls ga_testdata/BBDR_single/MERIS/$y/$tile/subset_0_of_MER_RR__1PNMAP${ddate}*_000000000000_00000_00000_0001_1x1_BBDR_${tile}_${coordmeris[0]}_${coordmeris[1]}_v$version.csv > /dev/null
    then 
    cat `ls ga_testdata/BBDR_single/MERIS/$y/$tile/subset_0_of_MER_RR__1PNMAP${ddate}*_000000000000_00000_00000_0001_1x1_BBDR_${tile}_${coordmeris[0]}_${coordmeris[1]}_v$version.csv | head -n 1` | transpose.awk |  grep ^K > /tmp/K_i.txt
    
    for b in VIS NIR SW ; do
      F=( `grep Mean_${b}_f0 /tmp/f_i.txt | awk '{print $2}'` \
	  `grep Mean_${b}_f1 /tmp/f_i.txt | awk '{print $2}'` \
	  `grep Mean_${b}_f2 /tmp/f_i.txt | awk '{print $2}'` )
      K=( 1.0 \
	     `grep Kvol_BRDF_${b} /tmp/K_i.txt | awk '{print $2}'` \
	     `grep Kgeo_BRDF_${b} /tmp/K_i.txt | awk '{print $2}'` )
    done
    RossLiR_VIS=`echo ${F[*]} ${K[*]} | awk '{print $1*$4 + $2*$5 + $3*$6}'`
    RossLiR_NIR=`echo ${F[*]} ${K[*]} | awk '{print $1*$4 + $2*$5 + $3*$6}'`
    RossLiR_SW=`echo ${F[*]} ${K[*]} | awk '{print $1*$4 + $2*$5 + $3*$6}'`
    
    fi
    # sleep 1
    
    awk --traditional 'NR==2 {print '$y','$jd',$0,'$RossLiR_VIS','$RossLiR_NIR','$RossLiR_SW'}' $f
  done > /tmp/1

  # The plotter
  echo """set xdata time
  set term x11 noenhanced
  set timefmt '%Y %j'
  set yrange [0:*]
  set ylabel 'Albedo'
  set format x '%b'
  set multiplot layout 2,1
  p 0 lt -1, '/tmp/1' u 1:4 w lp t 'DHR_VIS',\
  '' u 1:5 w lp t 'DHR_NIR',\
  '' u 1:6 w lp t 'DHR_SW',\
  '' u 1:30 w l lw 4 lc rgb 'forest-green' t 'RLiVIS',\
  '' u 1:31 w l lw 4 lc rgb 'dark-red' t 'RLiNIR',\
  '' u 1:32 w l lw 4 lc rgb 'dark-blue' t 'RLiSW'
  

  p 0 lt -1 , '' u 1:7 w lp t 'BHR_VIS',\
  '' u 1:8 w lp t 'BHR_NIR',\
  '' u 1:9 w lp t 'BHR_SW'
  unset multiplot
  """ | gnuplot -persist
  
else
  echo "Function: Usage pALBEDO <site>"
fi
}


#
#
# BEAM operator
#
#

# =============================================================================
function L1b2SDR_BBDR() {

# Environment variables (example):

L1bDIR=ga_testdata/l1b/     # input dir  
BBDRDIR=ga_testdata/bbdr/   # output dir
clear
echo "L1b2SDR_BBDR: *** L1b --> SDR/BBDR ***"
sleep 1

#
# 3) processing L1b --> SDR/BBDR (MERIS, VGT), example for tile: h18v03, pixel [756,787], year: 2006, doy:121.
#
# NOTE: L1b data (radiances) to Spectral Directional Reflectance (SDR) and Broadband Directional Reflectance (BBDR)
#       Implemented so far MERIS, VGT, 
#       The output files (in BBDRDIR) files contain both SDR and BBDR.

case $sensor in 

"MERIS")

# for a definition of input and output file format see the appendix of the script:

# Run the processor: 

# $BEAM_HOME/bin/$GPT ga.l2.single -e \
#     -SsourceProduct="$L1bDIR/subset_0_of_MER_RR__1PNMAP20060511_095146_000003522047_00337_21934_0001_1x1.csv" \
#     -Psensor=MERIS -PbbdrDir=$BBDRDIR
 

lines=`wc -l < $L1bDIR/$1`
for l in `seq 2 $lines`
do
  # get date and time yyyy-mm-ddTHH:MM:SS
  dt=`awk 'NR=='$l' {print $2}' $L1bDIR/$1 | sed -e 's/-//g' -e 's/://g' -e 's/T/_/g'`
  echo $dt
  # input file name should contain header and 1 row of data
  # and the filename format is mandatory (cannot be generic ???)
  
  # Filename convetion
  # Here the filename convention from meris.ProductHandbook.2_1.pdf!
  # l# # is the length of the 
  
  t8=00000000  # duration in seconds of the acquisition  (for Zerbolo is 54 in N1 RR data)
  P1=0         # Phase of the mission                    (    --      is 2               ) 
  c3=000       # number of the cycle in the mission phase(    --      is 12 to 33 from 2003-01-01 to 2004-12-31 )
  o5=00000     # orbit number in the current cycle       (    --      up to 480 for Zerbolo )
  O5=00000     # total orbit                             (    --      is 04385 2003-01-01T10:28:51 to 14834 at 2004-12-31T09:46:25 ) 
  i4=0001      # a progressive number id analysis
  
  # With these numbers one can play to define an exact filename (but, at least for the 
  # first step the above template seems not affecting the processing.
  
  
  SOURCEPRODUCT="$L1bDIR/subset_0_of_MER_RR__1PNMAP${dt}_000000000000_00000_00000_0001_1x1.csv"
  # SOURCEPRODUCT=$L1bDIR/subset_0_of_MER_RR__1PNMAP${dt}_${t8}${P1}${c3}_${o5}_${O5}_${i4}_1x1.csv

  awk 'NR==1||NR=='$l'' $L1bDIR/$1 > $SOURCEPRODUCT
  $GPT ga.l2.single -e -SsourceProduct="$SOURCEPRODUCT" \
                    -Psensor=$sensor \
                    -PbbdrDir=$BBDRDIR
      
#   echo "ctrl+C to exit now!"
#   sleep 3
#   vim target.dim
done


# Pulizia file l1b/ temporanei
local fname=$1
tarfilename=${fname/.csv/.tgz}
tar czvf $L1bDIR/$tarfilename $L1bDIR/subset*.csv
rm $L1bDIR/subset*.csv

;;

"VGT")

# let's VGT stand by (TBD: documented as before)

$GPT ga.l2.single -e \
     -SsourceProduct="$L1bDIR/subset_0_of_subset_saudiarabia_of_V2KRNP____20140511F025_1x1.csv" -Psensor=VGT -PbbdrDir=$BBDRDIR
     
#--> output product is in: 
#$BBDRDIR/subset_0_of_subset_saudiarabia_of_V2KRNP____20140511F025_1x1_BBDR_hXXvYY_<xPos>_<yPos>_vyyMMdd.csv


# [INFO] 2016-05-04T18:33:42.820+0200 - JAI tile scheduler parallelism set to 2
# [INFO] 2016-05-04T18:33:43.038+0200 - Installed GlobAlbedo Meteosat Reader auxiliary data 'org/esa/beam/dataio'
# [WARNING] 2016-05-04T18:33:43.207+0200 - org.esa.beam.framework.dataio.ProductReaderPlugIn: Provider org.esa.beam.dataio.ProbaVNewSynthesisProductReaderPlugIn not found
# [INFO] 2016-05-04T18:33:43.219+0200 - JAI tile cache size is 512 MB
# [INFO] 2016-05-04T18:33:43.219+0200 - JAI tile scheduler parallelism is 2
# [INFO] 2016-05-04T18:33:43.440+0200 - Installed GlobAlbedo Meteosat Reader auxiliary data 'org/esa/beam/dataio'
# [WARNING] 2016-05-04T18:33:43.496+0200 - Could not derive elevation from DEM - set to 0.0
# [INFO] 2016-05-04T18:33:44.207+0200 - All GPF operators will share an instance of com.sun.media.jai.util.SunTileCache with a capacity of 512M
# [INFO] 2016-05-04T18:33:44.458+0200 - Start writing product subset_0_of_subset_saudiarabia_of_V2KRNP____20140511F025_1x1 to /home/lancoch/Desktop/spp/ga_testdata/bbdr/subset_0_of_subset_saudiarabia_of_V2KRNP____20140511F025_1x1_BBDR_h22v07_0404_0221_v20160504.csv
# [INFO] 2016-05-04T18:33:44.475+0200 - End writing product subset_0_of_subset_saudiarabia_of_V2KRNP____20140511F025_1x1 to /home/lancoch/Desktop/spp/ga_testdata/bbdr/subset_0_of_subset_saudiarabia_of_V2KRNP____20140511F025_1x1_BBDR_h22v07_0404_0221_v20160504.csv
# [INFO] 2016-05-04T18:33:44.475+0200 - Time: 0.036 sec. total, 0.036 sec. per line, 0.036 sec. per pixel
# [INFO] 2016-05-04T18:33:44.506+0200 - Start writing product target to target.dim
# [INFO] 2016-05-04T18:33:44.525+0200 - End writing product target to target.dim
# [INFO] 2016-05-04T18:33:44.526+0200 - Time: 0.044 sec. total, 0.044 sec. per line, 0.044 sec. per pixel


;;

*) echo "Sensor not implemented (MERIS or VGT)" ;;

esac


}

# =============================================================================
function BBDR2BRDF(){

  clear
  echo -e "\e[31m *** BBDR --> BRDF *** \e[0m"
  
  if [ $1 ] ; then
  
  local site=$1

  # 4) processing BBDR --> BRDF:
  # - environment variables (example):

  # NOTE: make sure that the following folders exist:

  # $gaRootDir
  # $gaRootDir/BBDR_single
  # $gaRootDir/BBDR_single/<sensor>/<year>/<tile> , and the BBDR CSV files are in there!!
  # $gaRootDir/Inversion_single
  # $gaRootDir/Albedo_single
  
  gaRootDir="$HOME/Desktop/spp/ga_testdata"
  bbdrSingleDir="$gaRootDir/BBDR_single"          # input folder

  deftiles $site
  echo "PriorFile(base) tile coordmeris after deftiles():"
  echo $pfile $tile ${coordmeris[*]}

  inversionSingleDir=$gaRootDir/Inversion_single                     # output folder   
  priorPixelProduct=/tmp/tmpPrior.csv # priors/$pfile
  priorDir=$gaRootDir/Priors

  # for f in 2003{001..365..8} 2004{001..366..8} 
  for f in 2003{1..100..8} 2004{1..100..8} 
  
  ##for f in `ls $bbdrSingleDir/MERIS/*/$tile/subset_0_of_MER_RR__1PNMAP*_${tile}_${coordmeris[0]}_${coordmeris[1]}_v${version}.csv` 
  # $bbdrSingleDir/MERIS/2004/h18v04/subset_0_of_MER_RR__1PNMAP20040616_100943_000000000000_00000_00000_0001_1x1_BBDR_h18v04_0749_0565_v${version}.csv #  
  # `seq -f "%03.0f" 1 8 365` ga_testdata/BBDR_single/MERIS/2004/h18v04/subset_0_of_MER_RR__1PNMAP20040616_100943_000000000000_00000_00000_0001_1x1_BBDR_h18v04_0749_0565_v${version}.csv  
  
  do 
    case $f in
    # jd is actually defined by a list of dates
    2*)
      year=${f:0:4}
      jd=${f:4:3}
      
    ;;
    *)
    # Get $jd/ from the BBDR product name
    getyeartile $f
    ;;
    esac
    
    echo "Input arguments: " $f $year $jd $tile ${coordmeris[*]} bbdrversion $version
    # sleep 5
    
    # creates output default directories
    mkdir -p $inversionSingleDir/$year/$tile/Snow
    mkdir -p $inversionSingleDir/$year/$tile/NoSnow
    
    echo "&now the gpt call"
    case $usePrior in
    
      "false")
      $GPT ga.l3.inversion.single -Ptile="$tile" \
                                  -Pyear=$year \
                                  -Pdoy=$((10#$jd)) \
                                  -PusePrior=false \
                                  -PcomputeSnow=$cmpSnow \
                                  -PpixelX=${coordmeris[0]} \
                                  -PpixelY=${coordmeris[1]} \
                                  -PgaRootDir="$gaRootDir" \
                                  -PversionString="v$version" \
                                  -PbbdrRootDir="$bbdrSingleDir" 2>&1 | egrep -v "No BBDR source|timeGet"
                                  
                                  #-t "$gaRootDir/tmpinv/ga.noprior.$year.$((10#$jd)).$tile.${coordmeris[0]}.${coordmeris[1]}.csv"
                                  
                                  # -PbbdrRootDir="$bbdrSingleDir"
      
      # This option only if the default path would be changed: -PinversionDir=$inversionSingleDir
      
      ;;

      "true")
      # Define the priors file
      # Creates the single line prior files from the database files provided by Said 
      # converting them in the format suitable to be used here : Olaf format
      rm $priorPixelProduct > /dev/null 2>&1  # clean from a previous run
      
      lat=`transpose.awk $f | grep latitude | cut -f2`
      lon=`transpose.awk $f | grep longitude | cut -f2`
      
      # the exact prior from Said
      local cpv="c5"
      if [[ $pV006 == "true" ]] ; then
      cpv="c6"
      # redefine the Prior source name
      # ex.   Zerbolo_modis.c5.prior.brdf.bb.csv
      #       Zerbolo_modis.c6.prior.brdf.nosnow.csv
      #       Zerbolo_modis.c6.prior.brdf.snow.csv
      #       Zerbolo_modis.c6.prior.brdf.snownosnow.csv

      pfile=${pfile/c5/c6}
      # Snow priors exist only for .c6.
      [[ $cmpSnow == "true" ]] && pfile=${pfile/bb.csv/snow.csv} || pfile=${pfile/bb.csv/nosnow.csv}
      
      fi
      
      ExctractSinglePrior ${priorDir}/${pfile} $jd | sed -e 's/Latitude/'$lat'/' -e 's/Longitude/'$lon'/' > $priorPixelProduct
      
      # A smart reformat for some artifacts occurring on tmp prior file
      sed -e 's/\t\t//g' -e 's/e-0/E-/g' $priorPixelProduct > /tmp/1
      mv /tmp/1 $priorPixelProduct
      dos2unix $priorPixelProduct
      
      # backupping Priors
      case $cmpSnow in 
        true)  local fid=Snow   ;;
        false) local fid=NoSnow ;; 
      esac
      mkdir -p $priorDir/$fid/$tile
      
      # Just as a backup (plotting):
      cp -v $priorPixelProduct $priorDir/$fid/$tile/subset_0_of_kernel.$cpv.$jd.---.$tile.$fid.csv
      
      # A fake prior from olaf example 
      # sed -e 's/LAT/'$lat'/' -e 's/LON/'$lon'/' pinco-template.csv > $priorPixelProduct
      
      # Call the inversion tool
      # set -x 
      $GPT ga.l3.inversion.single -SpriorPixelProduct="$priorPixelProduct" \
                                  -Ptile="$tile" \
                                  -Pyear=$year \
                                  -Pdoy=$((10#$jd)) \
                                  -PusePrior=true \
                                  -PcomputeSnow=$cmpSnow \
                                  -PpixelX=${coordmeris[0]} \
                                  -PpixelY=${coordmeris[1]} \
                                  -PgaRootDir="$gaRootDir" \
                                  -PversionString="v$version" \
                                  -PbbdrRootDir="$bbdrSingleDir" 2>&1 | egrep -v "No BBDR source|timeGet"
                                  
      # This option only if the default path would be changed: -PinversionDir="$inversionSingleDir" 
      
      ;;
      
    esac
  echo "Error code $?"
  
  # $GPT ga.l3.inversion.single -e -SpriorPixelProduct=$priorPixelProduct -Ptile="h18v03" -Pyear=2006 -Pdoy=121 -PusePrior=false -PcomputeSnow=true -PpixelX=0756 -PpixelY=0787 -PgaRootDir=$gaRootDir -PbbdrRootDir=$bbdrSingleDir -PinversionDir=$inversionSingleDir
  #--> output products are in:
  #$inversionSingleDir/GlobAlbedo.brdf.single.2006.121.h18v03.0756.0787.NoSnow.csv
  #$inversionSingleDir/GlobAlbedo.brdf.single.2006.121.h18v03.0756.0787.Snow.csv
  done
  
  else
  
  echo "Function: Usage BBDR2BRDF <site>"
  fi
  
}

# =============================================================================
function BRDF2ALBEDO() {
  clear
  echo -e "\e[31;42m *** BRDF --> Albedo *** \e[0m"

  # 5) processing BRDF --> Albedo:
  # - environment variables (example):
  
  if [ $1 ] ; then 
    local site=$1
    deftiles $site
    local fid=
    case $cmpSnow in 
      false) brdfDir="ga_testdata/Inversion_single/*/$tile/NoSnow" ; fid=NoSnow ;;
      true)  brdfDir="ga_testdata/Inversion_single/*/$tile/Snow"   ; fid=Snow   ;;
    esac
    
    for f in `ls $brdfDir/GlobAlbedo.brdf.single.????.???.$tile.${coordmeris[0]}.${coordmeris[1]}.$fid.csv`
    do
      albedoDir=`dirname $f`
      albedoDir=${albedoDir/Inversion_single/Albedo_single}
      mkdir -p $albedoDir
      fname=`basename $f`
      y=${fname//GlobAlbedo.brdf.single.}     ;  y=${y:0:4}
      jd=${fname//GlobAlbedo.brdf.single.$y.} ; jd=${jd:0:3}
           
      echo -e "${f} ... \e[32m[ga.l3.albedo.single]\e[0m ...\n $albedoDir/${fname/brdf/albedo}" 
      # echo $y $jd
      
      $GPT ga.l3.albedo.single    -SbrdfProduct=$f \
                                  -Ptile=$tile \
                                  -Pyear=$y \
                                  -Pdoy=$[10#$jd] \
                                  -PcomputeSnow=$cmpSnow \
                                  -t $albedoDir/${fname/brdf/albedo} 2> /dev/null
                                  
    done
    
    # targetProductSnow=$targetDir/GlobAlbedo.albedo.single.2006.121.h18v03.0756.0787.Snow.csv
    # $GPT ga.l3.albedo.single -e -Ptile="h18v03" -Pyear=2006 -Pdoy=121 -PcomputeSnow=true -SbrdfProduct=$brdfProduct -t $targetProductSnow
  
  else
    echo "Usage BRDF2ALBEDO <site>"
  fi

}


# =============================================================================
# Main            Inputfile  [Outputfile]
# =============================================================================



action=$1

# set -e 

case $action in 

avhrrwrapper)
# from 2017-06-29 it works even for MERIS:
if [ $2 ] ; then
  flist=$2
  instr=`basename $flist`
  instr=${instr:0:5}
  echo $flist $instr
  sleep 5
  ToASim2JrcFaparWrapper $flist $instr
fi

;;

l1bbbdr) 

if [[ $2 ]] ; then 
  flist=$2
else
  local flist=( ${flist_1[*]} )
fi

rm analysis.txt
for f in ${flist[*]}
do
    of=$(basename $f)
    of=${of/.out/.csv}
    echo "ToASim2sppWrapper : $f $of"
    
    ToASim2sppWrapper $f $of
    L1b2SDR_BBDR $of                         # internal loop on each record of the file
    
done

;;

bbdrmove)
# move the BBDR files in the correct folder to be processed BBDR -> BRDF
# by checking the correctess of their content (without NaNs)
infolder=ga_testdata/bbdr/
outfolder=ga_testdata/BBDR_single/MERIS/
for f in ga_testdata/bbdr/subset*.csv
do
  if ! grep NaN $f > /dev/null ; then 
    ff=`basename $f`
    getyeartile $ff
    mkdir -p ga_testdata/BBDR_single/MERIS/$year/$tile/
    mv -v $f ga_testdata/BBDR_single/MERIS/$year/$tile/
  else
    echo -e "\e[31m$f contains NaNs (not moved)\e[0m"
    usleep 100000 # sleep .300 sec
  fi
done         ;;

bbdrfixsnow)

# Lo snow_mask non e' settato nel passaggio L1B2BBDR del SPP. Bisogna farlo a manona.
# I tentativi di farlo tramire PixClass (abilitando l'algoritmo per il calcolo di isBrightWhite
# sono meramente Falliti!
# Noi decidiamo di definirlo sulla base dle valore del NDSI MODIS imponentdolo a 1
# per valori di NDSI maggiori di 0.25. Per il tile h21v16 (Dome-C) il valore e' sempre 1.
# I siti interessati sono JARVSELJA e OFENPASS.
# Lo script controlla tutti i file BBDR prodotti per un certo sito, identificato da $tile e 
# $coordmeris e imposta lo snow_mask controllando il file jar.ndsi o ofe.ndsi, per 
# verificando il valore di ndsi per la data relativa al nomefile.

for site in JARVSELJA-PS JARVSELJA-BS OFENPASS
do 
  deftiles $site
  # questa chiamata ha definito anche il nome del file ndsi 
  for f in ga_testdata/BBDR_single/MERIS/*/$tile/subset_0_of_MER_RR__1PNMAP*BBDR_${tile}_${coordmeris[0]}_${coordmeris[1]}_v$version.csv
  do
    echo "Setting snow_mask for $f ..."
    # define the date to be grepped on ndsifile
    
    d=${f/*MER_RR__1PNMAP}
    d=${d:0:4}-${d:4:2}-${d:6:2}
    snow_mask=`grep $d Output/$ndsifile | awk '{print ($3 > 25 ? 1 : 0 )}'`
    echo $site $d \> $snow_mask
    [[ $snow_mask -ne 1 ]] && snow_mask=0
    
    # From PixClass.awk:
    # Si ridefinisce la colonna 40 relativa allo snow_mask.
    
    awk '{ if (FNR==1) print $0
      else 
      {
      $40='$snow_mask' #iscs
      for (c=1;c<NF;c++) printf("%s\t",$c) ; print $NF
      }}' $f > /tmp/1 
      mv /tmp/1 $f
  done
done

;;

bbdr2brdf)

  if [ $2 ] ; then
    site=$2
    clear
    echo "Processing bbdr file to produce brdf for $site"
    sleep 2
    BBDR2BRDF $site 
  else
  for site in ${SITES[*]}
  do
    clear
    echo "Processing bbdr file to produce brdf for $site"
    sleep 2
    BBDR2BRDF $site 
  done
  fi
    
;; 

brdf2albedo)

  if [ $2 ] ; then
    site=$2
    clear
    echo "Processing bbdr file to produce brdf for $site"
    sleep 2
    BRDF2ALBEDO $site
  else
  for site in ${SITES[*]}
  do
    clear
    echo "Processing bbdr file to produce brdf for $site"
    sleep 2
    BRDF2ALBEDO $site
  done
  fi
  ;;

verif)

# scorporato da l1bbbdr
   
    if [ $2 ] ; then 
		f=$2
			verif_sdr $f
			# verif_sdr_justplot $f
			# verif_sdr_justplot_a $f
    else
		echo "verif action will be ran for all files out in the array f_list1 (C^c to stop)"
		read ans
		for f in ${flist_1[*]}
		do
			verif_sdr $f
			# verif_sdr_justplot $f
			# verif_sdr_justplot_a $f
		done
	fi
	
	;;   
    
    pALBEDO)
		pALBEDO $2 ;;

*)
echo -e """
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
\e[32mAvailable option are:\e[0m

$0 avhrrwrapper 6Sofile : AVHRR wrapper (jrc-fapar)
$0 l1bbbdr 6Sofile      : L1b->BBDR production
$0 bbdrmove             : To be performed after previous step
$0 bbdrfixsnow          : fis Snow_mask of specific BBDR files
$0 bbdr2brdf   [site]   : BBDR->BRDF production
$0 brdf2albedo          : BRDF->Albedo
$0 verif  [6Sofile]     : the performance analisys for ref file F
$0 pALBEDO     <site>   : plot the albedo product for site <site>

Notes: 
<> mandatory argument
[] optional argument
versioning (vYYYYmmdd) is specified on the header script
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~"""

exit ;;

esac


# Verifica dei file inversione
## verif_sdr_statistiche



exit


# *********************************************************************************************************************
# *Transposed* example of input file (subset_0_of_MER_RR__1PNMAP20060511_095146_000003522047_00337_21934_0001_1x1.csv):
# *********************************************************************************************************************
# 1	featureId	0                         * progressive number of the record starting from 0
# 2	start_time:time	2010-06-01T12:45:00       * yyyy-mm-ddTHH:MM:SS
# 3	stop_time:time	2010-07-01T13:57:23
# 4	radiance_1:float	70.38858
# 5	radiance_2:float	63.93897
# 6	radiance_3:float	51.052143
# 7	radiance_4:float	50.016937
# 8	radiance_5:float	49.436176
# 9	radiance_6:float	35.036983
# 10	radiance_7:float	29.30976
# 11	radiance_8:float	27.807388
# 12	radiance_9:float	48.244713
# 13	radiance_10:float	120.38643
# 14	radiance_11:float	36.734013
# 15	radiance_12:float	120.072395
# 16	radiance_13:float	102.15177
# 17	radiance_14:float	99.0389
# 18	radiance_15:float	69.0387
# 19	l1_flags:short	16                       * from where?
# 20	detector_index:short	595              * from where?
# 21	latitude:float	53.444145
# 22	longitude:float	10.575718
# 23	dem_alt:float	8375                     * unit feet?
# 24	dem_rough:float	7875                     * unit?
# 25	lat_corr:float	-0.00000275              * unit?
# 26	lon_corr:float	0.0000225                * unit?
# 27	sun_zenith:float	38.642796
# 28	sun_azimuth:float	149.3894
# 29	view_zenith:float	10.878903
# 30	view_azimuth:float	105.04132
# 31	zonal_wind:float	1.2375001        * unit?
# 32	merid_wind:float	-1.3000001       * unit?
# 33	atm_press:float	1017.9                   * mbar @ sea level
# 34	ozone:float	334.45                   * DU
# 35	rel_hum:float	31.8125                  * %


# *********************************************************************************************************************
# *Transposed* example of $BBDRDIR/subset_0_of_MER_RR__1PNMAP20060511_095146_000003522047_00337_21934_0001_1x1_BBDR_hXXvYY_<xPos>_<yPos>_vyyMMdd.csv
# *********************************************************************************************************************
# 1	featureId	0
# 2	BB_VIS:float	0.05568666              * BBDR
# 3	BB_NIR:float	0.33715737
# 4	BB_SW:float	0.20002827
# 5	sig_BB_VIS_VIS:float	0.0021501498
# 6	sig_BB_VIS_NIR:float	0.0015676065
# 7	sig_BB_VIS_SW:float	0.0001319814
# 8	sig_BB_NIR_NIR:float	0.01505054
# 9	sig_BB_NIR_SW:float	0.0046105604
# 10	sig_BB_SW_SW:float	0.010910063
# 11	Kvol_BRDF_VIS:float	0.004855931
# 12	Kvol_BRDF_NIR:float	0.002225512
# 13	Kvol_BRDF_SW:float	0.0017828757
# 14	Kgeo_BRDF_VIS:float	-0.94513947
# 15	Kgeo_BRDF_NIR:float	-0.80294037
# 16	Kgeo_BRDF_SW:float	-0.88722456
# 17	AOD550:float	0.15
# 18	sig_AOD550:float	0
# 19	NDVI:float	0.7609817
# 20	sig_NDVI:float	0.007156293
# 21	VZA:float	10.878903
# 22	SZA:float	38.642796
# 23	RAA:float	44.348083
# 24	DEM:float	0.001
# 25	sdr_1:float	0.031862486             * spectral surface direction reflectance (atmospherically corrected)
# 26	sdr_2:float	0.035276804
# 27	sdr_3:float	0.038345277
# 28	sdr_4:float	0.048553426
# 29	sdr_5:float	0.0808065
# 30	sdr_6:float	0.064721584
# 31	sdr_7:float	0.058459878
# 32	sdr_8:float	0.057208087
# 33	sdr_9:float	0.13307649
# 34	sdr_10:float	0.38421634
# 35	sdr_11:float	0.32234535
# 36	sdr_12:float	0.40916532
# 37	sdr_13:float	0.4325162
# 38	sdr_14:float	0.43823308
# 39	sdr_15:float	0.42257395
# 40	snow_mask:short	0
# 41	l1_flags:short	16
# 42	latitude:float	53.444145
# 43	longitude:float	10.575718

# *********************************************************************************************************************
# **Transposed** example of exp output file Inversion_single/2003/h18v04/NoSnow/GlobAlbedo.brdf.single.2003.085.h18v04.0749.0565.NoSnow.csv
# *********************************************************************************************************************
#      1  featureId       0        
#      2  mean_VIS_f0:float       0.13114871       
#      3  mean_VIS_f1:float       -0.0029864423    
#      4  mean_VIS_f2:float       0.030335193      
#      5  mean_NIR_f0:float       0.21790189       
#      6  mean_NIR_f1:float       0.08067595       
#      7  mean_NIR_f2:float       0.0136301955     
#      8  mean_SW_f0:float        0.17451048       
#      9  mean_SW_f1:float        0.048401847      
#     10  mean_SW_f2:float        0.019028364      
#     11  VAR_VIS_f0_VIS_f0:float 5.531458E-4      
#     12  VAR_VIS_f0_VIS_f1:float -4.88576E-4      
#     13  VAR_VIS_f0_VIS_f2:float 5.151944E-4      
#     14  VAR_VIS_f0_NIR_f0:float 4.078826E-6      
#     15  VAR_VIS_f0_NIR_f1:float -7.8972635E-6    
#     16  VAR_VIS_f0_NIR_f2:float 4.3735313E-6     
#     17  VAR_VIS_f0_SW_f0:float  1.2778426E-6     
#     18  VAR_VIS_f0_SW_f1:float  -1.5983325E-6    
#     19  VAR_VIS_f0_SW_f2:float  1.2575609E-6     
#     20  VAR_VIS_f1_VIS_f1:float 0.0014382026     
#     21  VAR_VIS_f1_VIS_f2:float -4.2136983E-4    
#     22  VAR_VIS_f1_NIR_f0:float -3.9471265E-6    
#     23  VAR_VIS_f1_NIR_f1:float 1.4874416E-5     
#     24  VAR_VIS_f1_NIR_f2:float -3.892595E-6     
#     25  VAR_VIS_f1_SW_f0:float  -1.0437785E-6    
#     26  VAR_VIS_f1_SW_f1:float  3.4902687E-6     
#     27  VAR_VIS_f1_SW_f2:float  -9.2103E-7       
#     28  VAR_VIS_f2_VIS_f2:float 4.8311974E-4     
#     29  VAR_VIS_f2_NIR_f0:float 3.7729474E-6     
#     30  VAR_VIS_f2_NIR_f1:float -7.1117393E-6    
#     31  VAR_VIS_f2_NIR_f2:float 4.0886034E-6     
#     32  VAR_VIS_f2_SW_f0:float  1.1877396E-6     
#     33  VAR_VIS_f2_SW_f1:float  -1.3998684E-6    
#     34  VAR_VIS_f2_SW_f2:float  1.1797458E-6     
#     35  VAR_NIR_f0_NIR_f0:float 0.0062780534     
#     36  VAR_NIR_f0_NIR_f1:float -0.012214849     
#     37  VAR_NIR_f0_NIR_f2:float 0.0066904505     
#     38  VAR_NIR_f0_SW_f0:float  1.9883035E-5     
#     39  VAR_NIR_f0_SW_f1:float  -2.775451E-5     
#     40  VAR_NIR_f0_SW_f2:float  1.9423267E-5     
#     41  VAR_NIR_f1_NIR_f1:float 0.03435013       
#     42  VAR_NIR_f1_NIR_f2:float -0.012537335     
#     43  VAR_NIR_f1_SW_f0:float  -3.8187514E-5    
#     44  VAR_NIR_f1_SW_f1:float  8.8184264E-5     
#     45  VAR_NIR_f1_SW_f2:float  -3.620091E-5     
#     46  VAR_NIR_f2_NIR_f2:float 0.0072578294     
#     47  VAR_NIR_f2_SW_f0:float  2.126891E-5      
#     48  VAR_NIR_f2_SW_f1:float  -2.799506E-5     
#     49  VAR_NIR_f2_SW_f2:float  2.1069545E-5     
#     50  VAR_SW_f0_SW_f0:float   0.005304087      
#     51  VAR_SW_f0_SW_f1:float   -0.0070221024    
#     52  VAR_SW_f0_SW_f2:float   0.005212863      
#     53  VAR_SW_f1_SW_f1:float   0.018793823      
#     54  VAR_SW_f1_SW_f2:float   -0.0065070195    
#     55  VAR_SW_f2_SW_f2:float   0.0051822327     
#     56  Entropy:float   -17.92928        
#     57  Relative_Entropy:float  31.328896        
#     58  Weighted_Number_of_Samples:float        2.9513035        
#     59  Time_to_the_Closest_Sample:float        3.0      
#     60  Goodness_of_Fit:float   43.37411         
#     61  latitude:float  45.295   
#     62  longitude:float 8.877
    
# *********************************************************************************************************************
# **Transposed** example of ga_testdata/Albedo_single/2004/h18v04/NoSnow/GlobAlbedo.albedo.single.2004.025.h18v04.0749.0565.NoSnow.csv
# *********************************************************************************************************************
#      1  featureId       0        
#      2  DHR_VIS:float   0.08952747       
#      3  DHR_NIR:float   0.20644109       
#      4  DHR_SW:float    0.15284094       
#      5  BHR_VIS:float   0.08879329       
#      6  BHR_NIR:float   0.21438722       
#      7  BHR_SW:float    0.15745345       
#      8  DHR_alpha_VIS_NIR:float 0.0022000738     
#      9  DHR_alpha_VIS_SW:float  7.127252E-4      
#     10  DHR_alpha_NIR_SW:float  0.003458038      
#     11  DHR_sigma_VIS:float     0.008527993      
#     12  DHR_sigma_NIR:float     0.051521465      
#     13  DHR_sigma_SW:float      0.034575842      
#     14  BHR_alpha_VIS_NIR:float 0.0021996102     
#     15  BHR_alpha_VIS_SW:float  6.917013E-4      
#     16  BHR_alpha_NIR_SW:float  0.0034805129     
#     17  BHR_sigma_VIS:float     0.01169596       
#     18  BHR_sigma_NIR:float     0.06900153       
#     19  BHR_sigma_SW:float      0.046732355      
#     20  Weighted_Number_of_Samples:float        2.9513035        
#     21  Relative_Entropy:float  32.49182         
#     22  Goodness_of_Fit:float   43.37411         
#     23  Snow_Fraction:float     0.0      
#     24  Data_Mask:float 1.0      
#     25  Solar_Zenith_Angle:float        43.782276        
#     26  latitude:float  45.295   
#     27  longitude:float 8.877
