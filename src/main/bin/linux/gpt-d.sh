#! /bin/sh

# working script for usage in Globalbedo
export BEAM4_HOME=/opt/beam-4.9.0.1

. "$BEAM4_HOME/bin/detect_java.sh"


# settings for GA include
# - a bigger HEAP (6GB)
# - a concurrent GC algorithm (gives 25 % better timing, found by testing)

"$app_java_home/bin/java" \
    -Xms6g -Xmx6g  \
    -XX:+UnlockExperimentalVMOptions -XX:+UseCompressedOops -XX:+UseConcMarkSweepGC -XX:NewRatio=3 -XX:+AggressiveOpts \
    -Dceres.context=beam \
    -Dbeam.consoleLog=true -Dbeam.logLevel=ALL \
    "-Dbeam.mainClass=org.esa.beam.framework.gpf.main.Main" \
    "-Dbeam.home=$BEAM4_HOME" \
    "-Dncsa.hdf.hdflib.HDFLibrary.hdflib=$BEAM4_HOME/modules/lib-hdf-2.7/lib/libjhdf.so" \
    "-Dncsa.hdf.hdf5lib.H5.hdf5lib=$BEAM4_HOME/modules/lib-hdf-2.7/lib/libjhdf5.so" \
    -jar "$BEAM4_HOME/bin/ceres-launcher.jar" "$@"

exit $?