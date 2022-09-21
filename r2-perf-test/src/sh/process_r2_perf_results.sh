#!/bin/sh

currentNodeWorkspaceDir=
perfLoadGeneratorHudsonJobURL=
targetServerHost=
targetServerUser=
targerServerHudsonJobName=
rampupTime=
gcLog=

usage(){
  echo "USAGE: $0 -j <load generator hudson job url> -h <target server host> -u <target server user> -t <target server hudson job name> -w <current node workspace dir> -r <rampup time> -g <Http server gc log>"
  echo "Example #1: ./process_r2_perf_results.sh -w /home/usr/R2_PERF_TEST -j http://jenkins.n.com/job/R2_PERF_TEST/lastSuccessfulBuild/artifact/pegasus/publish/reports -h qa36.n.com -u tester -t R2_PERF_RunHttpServer -r 10 -g /home/usr/R2_PERF_TEST/remote_HTTP_server_logs/gc/gc.log"
  echo "Example #2 (used in jenkins job config setup): $WORKSPACE/pegasus/r2-perf-test/src/sh/process_r2_perf_results.sh -w $WORKSPACE -j ${JOB_URL}lastSuccessfulBuild/artifact/pegasus/publish/reports -h ${TARGET_PERF_SERVER} -u tester -t ${TARGET_PERF_SERVER_JOB_NAME} -r 10"
}

for arg in "$@"
do
  case "$1" in
    -h | --targethost ) shift
                        targetServerHost=$1
                        ;;
    -u | --targetuser ) shift
                        targetServerUser=$1
                        ;;
    -j | --job )        shift
                        perfLoadGeneratorHudsonJobURL=$1
                        ;;
    -t | --targetjob )  shift
                        targerServerHudsonJobName=$1
                        ;;
    -w | --workspace )  shift
                        currentNodeWorkspaceDir=$1
                        ;;
    -r | --workspace )  shift
                        rampupTime=$1
                        ;;
    -g | --gclog )      shift
                        gcLog=$1
                        ;;
    esac
    shift
done

if [ ! -n "$targetServerHost" ] || [ ! -n "$targetServerUser" ] || [ ! -n "$targerServerHudsonJobName" ] || [ ! -n "$currentNodeWorkspaceDir" ]  || [ ! -n "$perfLoadGeneratorHudsonJobURL" ]
then
  usage
  exit 1
fi

srcDir="$currentNodeWorkspaceDir/r2-perf-test/src"
buildDir="$currentNodeWorkspaceDir/build/r2-perf-test"
publishDir="$currentNodeWorkspaceDir/publish/reports"

mkdir -p $publishDir
#mkdir -p $buildDir/logs/$targetServerHost/gc

if [ ! -d "$buildDir/logs/$targetServerHost/gc" ]
then
  mkdir -p "$buildDir/logs/$targetServerHost/gc"
fi

# Parse gc log
command="$srcDir/sh/parse_gclog.py  $currentNodeWorkspaceDir/build/r2-perf-test/logs/$targetServerHost/gc/gc.log  $buildDir/logs/$targetServerHost/parsed_gc.csv"
echo "Executing $command"
$command

# Graph test results
command="$srcDir/sh/run_graphing_R_script.sh -r $srcDir/R_scripts/R2_summary_and_density_plot.R -d $buildDir/logs -g $publishDir/perftest.png -s $publishDir/perftest.summary -j $publishDir/perftest.json -m $rampupTime"
echo "Executing $command"
$command

echo "Total number of requests:" `cat $publishDir/perftest.json | sed 's/,//g;s/^.*Total_Responses..\([0-9]*\).*$/\1/'`

# Graph GC log
command="$srcDir/sh/run_graphing_R_script.sh -r $srcDir/R_scripts/plot_gc.R -d $buildDir/logs/$targetServerHost/parsed_gc.csv -g $publishDir/gcstats.png -s $publishDir/gcstats.summary -j $publishDir/gcstats.json -n $rampupTime -m `cat $publishDir/perftest.json | sed 's/,//g;s/^.*Total_Responses..\([0-9]*\).*$/\1/'` "
echo "Executing $command"
$command

# Generate Perf Test Results and GC diffs (current vs previous run)

command="$srcDir/sh/stats_diff.py $perfLoadGeneratorHudsonJobURL/perftest.json $publishDir/perftest.json $publishDir/perftest.diff Perf_Test_Results_DIFF_(current_vs_previos_run) Previous_R2 Current_R2"
echo "Executing $command"
$command

command="$srcDir/sh/stats_diff.py $perfLoadGeneratorHudsonJobURL/gcstats.json $publishDir/gcstats.json $publishDir/gcstats.diff GC_Stats_DIFF_(current_vs_previos_run) Previous_GC Current_GC"
echo "Executing $command"
$command

# Generate summary report
command="$srcDir/sh/generate_graph_report.sh -d $publishDir -r $publishDir/report.html -t Test_Report"
echo "Executing $command"
$command
