@echo off

set BEAM4_HOME=C:\Users\olafd\beam_snapshots\beam-5.0

gpt Meteosat.Netcdf.Read ^
-SsourceProduct="C:\Users\olafd\QA4ECV\from_eumetsat\W_XX-EUMETSAT-Darmstadt,SOUNDING+SATELLITE,MET7+MVIRI_C_BRF_EUMP_20010501000000.nc" ^
-PlatBandName=lat ^
-PlonBandName=lon ^
-t "C:\Users\olafd\QA4ECV\from_eumetsat\W_XX-EUMETSAT-Darmstadt,SOUNDING+SATELLITE,MET7+MVIRI_C_BRF_EUMP_20010501000000_latlon.dim"

exit /B %ERRORLEVEL%