#!/bin/bash
set -e

marker=$1
rootDir=$2

# add marker file in a directory and in each of its subdirectory (max depth = 4)
OLDDIR=$PWD
cd $rootDir
echo "touch .$marker"
touch .$marker
for subDir1 in `ls`; do
    echo "subDir1: $subDir1"
    if [ -d "${subDir1}" ] ; then
        echo "touch $subDir1/.$marker"
        touch $subDir1/.$marker
        for subDir2 in `ls $subDir1`; do 
            echo "subDir2: $subDir2"
            if [ -d "${subDir1}/${subDir2}" ] ; then
                echo "touch $subDir1/$subDir2/.$marker"
                touch $subDir1/$subDir2/.$marker
                for subDir3 in `ls $subDir1/$subDir2`; do
                    echo "subDir3: $subDir3"
                    if [ -d "${subDir1}/${subDir2}/${subDir3}" ] ; then
                        echo "touch $subDir1/$subDir2/$subDir3/.$marker"
                        touch $subDir1/$subDir2/$subDir3/.$marker
                        for subDir4 in `ls $subDir1/$subDir2/$subDir3`; do
                            echo "subDir4: $subDir4"
                            if [ -d "${subDir1}/${subDir2}/${subDir3}/${subDir4}" ] ; then
                                echo "touch $subDir1/$subDir2/$subDir3/$subDir4/.$marker"
                                touch $subDir1/$subDir2/$subDir3/$subDir4/.$marker
                            fi
                        done
                    fi
                done
            fi
        done
    fi
done

cd $OLDDIR

