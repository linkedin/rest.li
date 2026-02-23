#!/usr/bin/python

import json, urllib
import os
import sys
from stat import *

def usage():
  """
  Prints the script's usage guide.
  """
  print "Usage: stats_diff.py [file1: url or file path] [file2: url or file path] [output file] [report name] [column1 prefix] [column2 prefix]"
  print "Example:./stats_diff.py http://jenkins.n.com/job/R2_PERF_TEST/lastSuccessfulBuild/artifact/pegasus/publish/reports/perftest.json /home/usr/R2_PERF_TEST/pegasus/publish/reports/perftest.json /home/usr/R2_PERF_TEST/pegasus/publish/reports/perftest.diff \"Perf_Test_Results_DIFF_(previos_vs_current_run)\""

def getData(source):
  data=''
  fin = None

  if source.startswith("http"):
     fin = urllib.urlopen(source)
  elif source.endswith(".json"):
     fin = open(source, 'r')

  data = fin.read()
  return data

def diff(value1, value2):
  if (isnum(value1) and isnum(value2)):
    return value2 - value1
  else:
    return "NA"

def isnum(val):
  if (isinstance(val, (int, long, float, complex)) and str(val) != 'nan'):
    return True
  else:
    return False

def percentage(part, whole):
  if (isnum(part) and isnum(whole)):
    if (part != 0 and whole != 0):
      return format((100 * float(part)/float(whole)) - 100, '.2f')
    else:
      if (part == whole):
        return 0
      else:
        return 100
  else:
    return "NA"

def generateHtmlLine (list):
  stringHtml="</th>\n<th>".join(list)
  if ("Percentile" in stringHtml):
    return "<tr><th>"+ "</th>\n<th><font color=#0101DF>".join(list) + "</font></th></tr>\n"
  else:
    return "<tr><th>"+ stringHtml + "</th></tr>\n"

def generateSummaryDiffHtmlTable (headers, values):
  summary=""
  rows=""
  summary = generateHtmlLine(headers)
  for value in values:
    rows += generateHtmlLine (value)

  return "<table border=\"1\" cellpadding=\"5\" cellspacing=\"5\" width=\"100%\">"+summary+rows+"</table>"

def main():
  """
  This is how we are called.
  Example call:
  ./stats_diff.py https://hudson.corp.linkedin.com/job/SI_R2_PERF_PEGASUS_RELEASE/lastSuccessfulBuild/artifact/pegasus/publish/reports/perftest.json /export/home/tester/hudson/data/workspace/SI_R2_PERF_PEGASUS_RELEASE/pegasus/publish/reports/perftest.json /export/home/tester/hudson/data/workspace/SI_R2_PERF_PEGASUS_RELEASE/pegasus/publish/reports/perftest.diff 'Perf Test Results DIFF (previos vs current run)' 'Previous' 'Current'
  """

  print(len(sys.argv))
  print(sys.argv)

  if (len(sys.argv) != 7):
    usage()

  input1 = sys.argv[1]
  input2 = sys.argv[2]
  output = sys.argv[3]
  reportName = sys.argv[4]
  column1NamePrefix = sys.argv[5]
  column2NamePrefix = sys.argv[6]

  headers = ["Name",column1NamePrefix + " Run Value", column2NamePrefix + " Run Value","DIFF"]

  pstats_old = getData(input1)
  pstats_new = getData(input2)

  try:
    pstats_old_map = json.loads(pstats_old)
    pstats_new_map = json.loads(pstats_new)
    stats = []

    for key, value in pstats_old_map.iteritems():
      rdiff = diff(value,pstats_new_map[key])
      pdiff = percentage(pstats_new_map[key],value)
      data = (key,str(value),str(pstats_new_map[key]),str(rdiff)+" ("+str(pdiff)+"%)")
      stats.append(data)

    out = "<p><br> "+reportName+"<br></p>"+generateSummaryDiffHtmlTable (headers, stats)

    outfile = open(output,"wb")
    outfile.write(out)
    outfile.close()

  except ValueError:
    print "ERROR. Diff execution has been aborted. Data source is unavailable."
    print "Check sources:", input1,", ", input2, ", ",output,", ",reportName,", ",column1NamePrefix,", ",column2NamePrefix
    exit
  except:
    print "Error:",sys.exc_info()[0]

if __name__ == "__main__":
    main()
