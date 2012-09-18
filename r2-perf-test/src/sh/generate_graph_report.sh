#!/bin/sh

graphResultsDir=
outputDir=
summaryHTMLFile=
HTMLReportTitle=


usage(){
  echo "USAGE: $0 -d <graph results directory> -r <html report file> -t <report title>"
  echo "Example1: ./generate_graph_report.sh -d somedatadir -r results/html/TestReport.html -t Test_Report"

}

for arg in "$@"
do
  case "$1" in
    -d | --dir )       shift
                       graphResultsDir=$1
                       ;;
    -r | --report )    shift
                       summaryHTMLFile=$1
                       ;;
    -t | --reporttitle ) shift
                       HTMLReportTitle=$1
                       ;;
    esac
    shift
done

if [ ! -n "$graphResultsDir" ] || [ ! -n "$summaryHTMLFile" ] || [ ! -n "$HTMLReportTitle" ]
then
  usage
  exit 1
fi

#------- Functions -------
#-------------------------
printImageFileToHtml()
{
  declare -a filesToPrint
  # passing array params to the function
  filesToPrint=( `echo "$1"` )
  summaryHTMLFile=$2

  for file in ${filesToPrint[@]}
  do
    echo "$file"
    echo "<p></p>"  >> $summaryHTMLFile
    echo "<img src=\"$file\"></img>" >> $summaryHTMLFile
  done

}
#-------------------------
#-------------------------
printFileToHtml()
{
  declare -a filesToPrint
  # passing array params to the function
  filesToPrint=( `echo "$1"` )
  summaryHTMLFile=$2
  graphResultsDir=$3

  for file in ${filesToPrint[@]}
  do
    echo "$file"
    echo "<p>"  >> $summaryHTMLFile
    echo "<br>" >> $summaryHTMLFile
    cat "$graphResultsDir/$file" >> $summaryHTMLFile
    echo "<br>" >> $summaryHTMLFile
  done

}

#-------------------------
#---  End of Functions ---

# Generate HTML report

echo "<html>" >  $summaryHTMLFile
echo "<head>" >>  $summaryHTMLFile
echo "<title>$HTMLReportTitle</title>" >>  $summaryHTMLFile
echo "<body>" >> $summaryHTMLFile
echo "<basefont size="5">" >> $summaryHTMLFile
echo "<p><br>$HTMLTitle<br></p>" >> $summaryHTMLFile
echo ""

### Add R2 Perf graphs

perfPNGFiles=`ls $graphResultsDir|grep "\.png"| grep perftest`
echo "PErf png files: $perfPNGFiles"
# Set args variables to pass arrays to function
args=`echo ${perfPNGFiles[@]}`
# Call printImageFileToHtml function
printImageFileToHtml "$args" "$summaryHTMLFile"

### Add Perf summary

perfSummaryFiles=`ls $graphResultsDir|grep "\.summary"|grep perftest`
echo "Perf Summary files: $perfSummaryFiles"
# Set args variables to pass arrays to function
args=`echo ${perfSummaryFiles[@]}`
# Call printFileToHtml function
printFileToHtml "$args" "$summaryHTMLFile" "$graphResultsDir"

### Add Perf diff with previous run

diffiles=`ls $graphResultsDir|grep "\.diff"|grep perftest`
echo "Diff files: $diffiles"
# Set args variables to pass arrays to function
args=`echo ${diffiles[@]}`
# Call printFileToHtml function
printFileToHtml "$args" "$summaryHTMLFile" "$graphResultsDir"

### Graph GC

printGCMetricsLegend "$summaryHTMLFile"

gcPNGFiles=`ls $graphResultsDir|grep "\.png"| grep gc`
echo "gcPNGFiles files: $gcPNGFiles"
# Set args variables to pass arrays to function
args=`echo ${gcPNGFiles[@]}`
# Call printImageFileToHtml function
printImageFileToHtml "$args" "$summaryHTMLFile"

### Add GC summary
gcSummaryFiles=`ls $graphResultsDir|grep "\.summary"|grep gc`
echo "GC Summary files: $gcSummaryFiles"
# Set args variables to pass arrays to function
args=`echo ${gcSummaryFiles[@]}`
# Call printFileToHtml function
printFileToHtml "$args" "$summaryHTMLFile" "$graphResultsDir"

### Add GC diff with previous run

diffiles=`ls $graphResultsDir|grep "\.diff"|grep gc`
echo "Diff files: $diffiles"
# Set args variables to pass arrays to function
args=`echo ${diffiles[@]}`
# Call printFileToHtml function
printFileToHtml "$args" "$summaryHTMLFile" "$graphResultsDir"

echo "</p>" >> $summaryHTMLFile
echo "</body>" >> $summaryHTMLFile
echo "</head>" >> $summaryHTMLFile
echo "</html>" >> $summaryHTMLFile
