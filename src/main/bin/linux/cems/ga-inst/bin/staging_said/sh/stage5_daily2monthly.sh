#BSUB -o %J.o
#BSUB -e %J.e
#BSUB -q lotus


#########################################################################
#This program is about creating monthely albedo products from 8-daily ones
#author: Said Kharbouche, MSSL.UCL(2014)
#########################################################################



################################################# INPUTs ###########################################
####################################################################################################
#text file that contains list of absulute paths of 8-daily products for a given year
LIST=$1

# target year
YEAR=$2

# output directory
OUTDIR=$3
###################################################################################################



# the position of date in file name (splitted by ".")
IDXDATE=4

#JARFILE
JARFILE=/home/users/saidkharbouche/jar2/daily2monthly.jar

#create output sub directories
mkdir -p $OUTDIR

#launch java program
java -jar $JARFILE $YEAR $LIST $OUTDIR $IDXDATE

echo 'Done.'
