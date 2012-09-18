#!/usr/bin/python
import os
import sys
import re
import datetime
import csv
from stat import *

def usage():
    """
    Prints the script's usage guide.
    """
    print "Usage: parse_gclog.py input output [time-start] [time-end]"
    print "input = path to gc.log file"
    print "output = path to gc.csv file"
    print "time-start = date in yyyy-MM-dd HH:mm:ss format"
    print "time-end = date in yyyy-MM-dd HH:mm:ss format"
    sys.exit(-1)

def parse(line):
    """
    Parses an input line from gc.log into a set of tokens and returns them.
    There are two patterns we have to look for:
    "2011-11-15T09:16:37.054+0000: 1160.384: [GC [PSYoungGen: 38266K->384K(38144K)] 87474K->49592K(725312K), 0.0021760 secs] [Times: user=0.00 sys=0.00, real=0.00 secs]"
    "2011-11-15T20:18:18.483+0000: 55654.452: [Full GC [PSYoungGen: 384K->0K(986112K)] [PSOldGen: 687029K->17354K(275264K)] 687413K->17354K(1261376K) [PSPermGen: 8849K->8849K(21248K)], 0.1061210 secs] [Times: user=0.10 sys=0.01, real=0.10 secs]"
    """
    pgre = re.compile("^(\d{4}\-\d{2}\-\d{2})T(\d{2}:\d{2}:\d{2}.\d{3})\+\d+:\s\d+\.\d+:\s\[[GC].*\s+\[([a-zA-Z].*):\s+(\d+)K->(\d+)K\((\d+)K\)\]\s+(\d+)K->(\d+)K\((\d+)K\),\s+(\d+\.\d+)\ssecs\]\s+\[Times: user=(\d+.\d+) sys=(\d+.\d+), real=(\d+.\d+) secs\]")
    fre = re.compile("^(\d{4}\-\d{2}\-\d{2})T(\d{2}:\d{2}:\d{2}.\d{3})\+\d+:\s\d+\.\d+:\s\[[Full GC].*\s+\[([a-zA-Z].*):\s+(\d+)K->(\d+)K\((\d+)K\)\]\s+\[([a-zA-Z].*):\s+(\d+)K->(\d+)K\((\d+)K\)\]\s+(\d+)K->(\d+)K\((\d+)K\)\s+\[([a-zA-Z].*):\s+(\d+)K->(\d+)K\((\d+)K\)\],\s+(\d+\.\d+)\ssecs\]\s+\[Times: user=(\d+.\d+) sys=(\d+.\d+), real=(\d+.\d+) secs\]")

    gcdata = {}

    # First try matching with the partial GC pattern pgre
    isFullGc = False
    m = pgre.match(line)

    # Then match with the full GC pattern
    if (m == None):
        m = fre.match(line)
        isFullGc = True

    if ( m != None):
        gcdata['Time'] = str(m.group(1)+" "+m.group(2))
        gcdata['FullGC'] = isFullGc
        gcdata['YoungUsedBeforeGc'] = int(m.group(4))
        gcdata['YoungUsedAfterGc'] = int(m.group(5))
        gcdata['YoungSize'] = int(m.group(6))
        if ( not isFullGc ):
            gcdata['HeapUsedBeforeGc'] = int(m.group(7))
            gcdata['HeapUsedAfterGc'] = int(m.group(8))
            gcdata['HeapSize'] = int(m.group(9))
            gcdata['ElapsedTime'] = float(m.group(10))
            gcdata['UserTime'] = float(m.group(11))
            gcdata['SystemTime'] = float(m.group(12))
            gcdata['RealTime'] = float(m.group(13))
            gcdata['PSOldGenBeforeGc'] = str("NA")
            gcdata['PSOldGenAfterGc'] = str("NA")
            gcdata['PSOldGenSize'] = str("NA")
            gcdata['PSPermGenBeforeGc'] = str("NA")
            gcdata['PSPermGenAfterGc'] = str("NA")
            gcdata['PSPermGenSize'] = str("NA")
        else:
            gcdata['PSOldGenBeforeGc'] = int(m.group(8))
            gcdata['PSOldGenAfterGc'] = int(m.group(9))
            gcdata['PSOldGenSize'] = int(m.group(10))
            gcdata['HeapUsedBeforeGc'] = int(m.group(11))
            gcdata['HeapUsedAfterGc'] = int(m.group(12))
            gcdata['HeapSize'] = int(m.group(13))
            gcdata['PSPermGenBeforeGc'] = int(m.group(15))
            gcdata['PSPermGenAfterGc'] = int(m.group(16))
            gcdata['PSPermGenSize'] = int(m.group(17))
            gcdata['ElapsedTime'] = float(m.group(18))
            gcdata['UserTime'] = float(m.group(19))
            gcdata['SystemTime'] = float(m.group(20))
            gcdata['RealTime'] = float(m.group(21))

    return gcdata, isFullGc

def isWithinSpecifiedTime(logtime, starttime, endtime):
    printToFileFlag = False
    if (endtime > starttime):
        if ( logtime >= starttime and logtime <= endtime ):
           printToFileFlag = True
    return printToFileFlag

def main():
    """
    This is how we are called. Reads the command line args, reads the input
    file line by line, calling out to parse() for each line, processing and
    pushing the tokens into arrays that are passed into the drawGraph() method.
    Example call:
    ./parse_gclog.py ../tmp/gc.log gc.csv
    ./parse_gclog.py ../tmp/gc.log gc.csv "2006-08-13 05:00:00" "2006-08-13 11:00:00"
    """

    sliceLogFile = False
    startTime = 0
    endTime = 0

    print(sys.argv)

    if (len(sys.argv) != 3 and len(sys.argv) != 5):
        usage()
    input = sys.argv[1]
    output = sys.argv[2]
    # optional start and end times provided
    if (len(sys.argv) == 5):
        sliceLogFile = True
        startTime = datetime.datetime.strptime(sys.argv[3],"%Y-%m-%d %H:%M:%S")
        endTime = datetime.datetime.strptime(sys.argv[4],"%Y-%m-%d %H:%M:%S")

    # initialize local variables
    header = ['Time','FullGC','YoungUsedBeforeGc','YoungUsedAfterGc','YoungSize','PSOldGenBeforeGc','PSOldGenAfterGc','PSOldGenSize','PSPermGenBeforeGc','PSPermGenAfterGc','PSPermGenSize','HeapUsedBeforeGc','HeapUsedAfterGc','HeapSize','ElapsedTime','UserTime','SystemTime','RealTime']

    #read input and parse line by line
    fin = open(input, 'r')
    csvout = csv.writer(open(output, 'wb'), delimiter=',')
    try:
        csvout.writerow(header)
        while (True):
            line = fin.readline()
            if (line == ""):
                break
            (parsedgcdata, isFullGc) = parse(line.rstrip())
            if ( parsedgcdata.has_key('Time') ):
                ltime = str(parsedgcdata['Time'])
                # remove milliseconds from log time string
                logtime = datetime.datetime.strptime(ltime[:ltime.find(".")],"%Y-%m-%d %H:%M:%S")
                if ((sliceLogFile and isWithinSpecifiedTime(logtime, startTime, endTime)) or (not sliceLogFile)):
                    data = []
                    for name in header:
                        data.append(parsedgcdata[name])
                    csvout.writerow(data)

    except csv.Error, e:
        sys.exit('file %s, Exception: %s' % (output, e))
    fin.close

if __name__ == "__main__":
    main()
