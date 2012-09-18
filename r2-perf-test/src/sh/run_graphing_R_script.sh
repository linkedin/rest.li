#!/bin/sh

RscriptFile=
dataFileOrDir=
outputGraphFile=
outputSummaryFile=
outputJsonFile=
miscParam1=
miscParam2=

usage(){
  echo "USAGE: $0 -r <R script> -d <data file or directory> -g <outputGraphFile> -s <outputSummaryFile.summary> -j <output json file> -m <some parameter required by R script> -m2 <some parameter required by R script>"
  echo "Example1: ./run_graphing_R_script.sh Rscript /home/usr/R2_PERF_TEST/pegasus/r2-perf-test/src/R_scripts/R2_summary_and_density_plot.R /home/usr/R2_PERF_TEST/pegasus/build/r2-perf-test/logs /home/usr/R2_PERF_TEST/pegasus/publish/reports/perftest.png /home/usr/R2_PERF_TEST/pegasus/publish/reports/perftest.summary /home/usr/R2_PERF_TEST/pegasus/publish/reports/perftest.json"
  echo "Example2: ./run_graphing_R_script.sh Rscript /home/usr/R2_PERF_TEST/pegasus/r2-perf-test/src/R_scripts/plot_gc.R /home/usr/R2_PERF_TEST/pegasus/build/r2-perf-test/logs/qa36.n.com/parsed_gc.csv /home/usr/R2_PERF_TEST/pegasus/publish/reports/gcstats.png /home/usr/R2_PERF_TEST/pegasus/publish/reports/gcstats.summary /home/usr/R2_PERF_TEST/pegasus/publish/reports/gcstats.json 50000"
}

for arg in "$@"
do
  case "$1" in
    -r | --rscript )   shift
                       RscriptFile=$1
                       ;;
    -d | --data )      shift
                       dataFileOrDir=$1
                       ;;
    -g | --graph )     shift
                       outputGraphFile=$1
                       ;;
    -s | --summary )   shift
                       outputSummaryFile=$1
                       ;;
    -j | --jsonfile )  shift
                       outputJsonFile=$1
                       ;;
    -m | --miscparam ) shift
                       miscParam1=$1
                       ;;     
    -n | --miscparam2 ) shift
                       miscParam2=$1
                       ;;
    esac
    shift
done

if [ ! -n "$RscriptFile" ] || [ ! -n "$dataFileOrDir" ] || [ ! -n "$outputGraphFile" ] || [ ! -n "$outputSummaryFile" ] || [ ! -n "$outputJsonFile" ]
then
  usage
  exit 1
fi

#mkdir -p "outputDir"

command="Rscript $RscriptFile $dataFileOrDir $outputGraphFile $outputSummaryFile $outputJsonFile $miscParam1 $miscParam2"
echo "Executing $0 $command"
$command
