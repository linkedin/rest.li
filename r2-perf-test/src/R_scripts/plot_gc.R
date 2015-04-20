# Parse and use command line arguments

rm(list = ls())      # Clear all variables
graphics.off()    # Close graphics windows

args <- commandArgs(TRUE)

dataFile              <- args[1]
output_graph_file     <- "gc_plot"
outputSummaryHtmlFile <- "gc.html"
outputSummaryJsonFile <- "gc.json"
totalResponses        <- args[2]

print (args)


# --- Functions

getAxisTimeFrequency <- function(ttlexecutiontime) {
  print(ttlexecutiontime)
  atfrequency <- 1;
  if (ttlexecutiontime/10 > 1)
  {
	atfrequency <- ttlexecutiontime/10;
  }
  cat("\natfrequency:",atfrequency)
  atfrequency
}

plotTimeSeriesGraph <- function(x,y,starttime, endtime, axistimefrequency, gtype, gtitle, xlabel,ylabel){

  plot(x,
       y,
       type=gtype,
       xaxt = "n",
       xlim=c(starttime, endtime),
	   xlab=xlabel,
       ylab=ylabel,
	   font.lab=1,				# Font to use for the axis labels: 1=plain text, 2=bold, 3=italic, 4=bold italic
       col = "blue",
       #cex = 1.5,
       cex.axis=1.7,
       cex.lab=3,
       bty="l"                     # Box around plot to contain only left and lower lines
      )

  title(gtitle, cex.main=3, font.main=8)

  # Setup x-axis with time labels in "%Y/%m/%d %H:%M:%S" format ,etc.
  dr <- c(starttime, endtime, axistimefrequency);
  drseq <- seq(starttime, endtime, by=axistimefrequency);
  axis(1, drseq, at=drseq ,labels = format(drseq, "%Y/%m/%d %H:%M:%S", tz=""), cex.axis = 1.7, las=2)
  par(xaxp = dr)
  grid()
}

generateSummaryHtmlTable <- function (htmlTableHeaders, values){
  # Generate header
  summary <- paste("<th>",htmlTableHeaders,sep="</th>\n<th>",collapse="")
  # Generate row
  rows <- paste("<th>",values,sep="</th>\n<th>",collapse="")
  summary <- paste("<table border=\"1\" cellpadding=\"5\" cellspacing=\"5\" width=\"100%\"><tr>",summary,"</tr>\n","<tr>",rows,"</tr>\n</table>")

  summary
}
print ("Reading from data file:")
print (dataFile)

data <- read.csv(dataFile, header = TRUE,sep=",")
times <- strptime(data$Time,"%Y/%m/%d %H:%M:%OS", tz="")

gengcdata <- subset(data,data$FullGC =="False",c("Time","YoungUsedBeforeGc","YoungUsedAfterGc","YoungSize","HeapUsedBeforeGc","HeapUsedAfterGc","HeapSize","ElapsedTime"))
fullgcdata <- subset(data,data$FullGC =="True")

summary(gengcdata)

heapDiff <- gengcdata$HeapUsedBeforeGc - gengcdata$HeapUsedAfterGc

# Gen GC graph

### Calcualte graph boundaries
starttime <- min(as.POSIXct(data$Time))

endtime <- max(as.POSIXct(data$Time))
howLongRunning <- difftime(endtime,starttime , tz="", units="secs")
cat("howlongRunning:",howLongRunning)

cat("\nstarttime:")
print(starttime)
cat(" endtime:")
print(endtime)

#Calculate plot axisfrequency
axistimefrequency <- getAxisTimeFrequency (as.numeric(howLongRunning, units="secs"))
cat("\naxistimefrequency:",axistimefrequency)

graphHeight=1700
ttlPlots=3

if (length(fullgcdata$Time) > 0)
{
  graphHeight=2200
  ttlPlots=4
}

# Define png format file
png(output_graph_file,width=1600,height=graphHeight)

# Plotting graphs ...
plot.new()                              # start a new plot

par(mar=c(15,5,5,2)+0.1)                # set the size of the outer margins
par(mfrow=c(ttlPlots,1))	            # plot three graphs in one page

# Graph GC Stats
plotTimeSeriesGraph (as.POSIXct(gengcdata$Time),heapDiff,as.POSIXct(starttime), as.POSIXct(endtime), axistimefrequency,'l',"Heap Diff (Before GC - After GC)","","Memory(KBytes)")
plotTimeSeriesGraph (as.POSIXct(gengcdata$Time),gengcdata$HeapSize,as.POSIXct(starttime), as.POSIXct(endtime), axistimefrequency,'l',"HeapSize","","Memory(KBytes)")
plotTimeSeriesGraph (as.POSIXct(gengcdata$Time),gengcdata$ElapsedTime,as.POSIXct(starttime), as.POSIXct(endtime), axistimefrequency,'l',"Pause Time", "","ElapsedTime(sec)")

if (length(fullgcdata$Time) > 0)
{
  plotTimeSeriesGraph (as.POSIXct(fullgcdata$Time),fullgcdata$HeapUsedBeforeGc,as.POSIXct(starttime), as.POSIXct(endtime), axistimefrequency,'h',"Full GC","","Full GC")
}

dev.off()

# Summary

footprint <- max(data$HeapSize)/1024
freedmem <- data$HeapUsedBeforeGc - data$HeapUsedAfterGc
freedmemtotal <- sum(as.numeric(freedmem))/1024
avgFreedMemGC <- mean(freedmem)/1024
freedmemperresp <- freedmemtotal/as.numeric(totalResponses)
freedpermin <- toString(freedmemtotal/(as.double(howLongRunning/60)))
sumPause <- sum(data$ElapsedTime)
throughput <- ((howLongRunning-sumPause)/as.numeric(howLongRunning))*100
gcPerformance <- paste(toString(freedmemtotal/sumPause,"M/sec"))

# Memory

avgMemAfterGC <- mean(data$HeapUsedAfterGc)/1024

# Pause

minPause <- min(data$ElapsedTime)
maxPause <- max(data$ElapsedTime)
avrgGcPause <- mean(data$ElapsedTime)

# Full GC

if (length(fullgcdata) > 0 ){

  fullGcFreedMem <- fullgcdata$HeapUsedBeforeGc - fullgcdata$HeapUsedAfterGc
  fullGcFreedMemTtl <- sum(fullGcFreedMem)/1024
  fullGcSumPause <- sum(fullgcdata$ElapsedTime)
  pauseTtl <- fullGcSumPause + sumPause
  fullGcSumPausePercntg <- as.numeric((fullGcSumPause*100)/pauseTtl)
  sumPausePercntg <- as.numeric((sumPause*100)/pauseTtl)
  fullGcPerformance <- paste(toString(fullGcFreedMemTtl/fullGcSumPause,"M/sec"))

  avgMemAfterFullGC <- mean(fullgcdata$HeapUsedAfterGc)/1024
  avgFreedMemFullGC <- mean(fullGcFreedMem)/1024
  fullGcSumPause <- sum(fullgcdata$ElapsedTime)
  fillGcAvgPause <- mean(fullgcdata$ElapsedTime)

  cat("fullGcFreedMem:")
  print(fullGcFreedMem)
  cat("\n AVERAGE Full GC Acc Pause:",fullGcSumPause,"\n")
  cat("\n:",fullGcFreedMemTtl,"\n")
  cat("\n fullGcPerformance:",  fullGcPerformance,"\n")
}

#-------- End of Final list

#-------- Generate Html Report Table
out <- file(outputSummaryHtmlFile,"w") # open an output txt file

htmldata <- c("<table border=\"1\" cellpadding=\"5\" cellspacing=\"5\" width=\"100%\">")

# Summary html table
summaryHeader <- c("Footprint","Freed Memory","Freed Mem/Response","Freed Mem/Min","Total Time(sec)","Acc pauses","Throughput","Full GC Performance","GC Performace")
summaryData <- c(footprint, freedmemtotal, paste(freedmemperresp,"M/obj"),paste(freedpermin,"M/min"), howLongRunning, sumPause, throughput,fullGcPerformance,gcPerformance)

# Memory html table
memoryHeader <- c("Footprint","Avg after full GC","Avg after GC","Freed Memory ","Freed by full GC","Freed by GC","Avg freed full FC","Avg freed GC")
memoryData <- c(footprint, avgMemAfterFullGC, avgMemAfterGC, freedmemtotal, fullGcFreedMemTtl, freedmemtotal,avgFreedMemFullGC,avgFreedMemGC)

# Pause html table
pauseHeader <- c("Acc pauses","Acc full GC","Acc GC","Min Pause","Max Pause","Avg Pause","Avg Full GC","Avg GC")
pauseData <- c(sumPause, paste(fullGcSumPause,"(",fullGcSumPausePercntg,"%)"), paste(sumPause,"(",sumPausePercntg,"%)"), min(data$ElapsedTime), max(data$ElapsedTime), mean(data$ElapsedTime),fillGcAvgPause,mean(data$ElapsedTime))

cat(htmldata,"<p><br>GARBAGE COLLECTION<br></p><p><br>SUMMARY<br></p>",generateSummaryHtmlTable(summaryHeader,summaryData ),"<p><br>MEMORY<br></p>",generateSummaryHtmlTable(memoryHeader,memoryData ),"<p><br>PAUSE<br></p>",generateSummaryHtmlTable(pauseHeader, pauseData),c("</table"), sep="", file=out)

close(out)

#-------- Generate json report (file naming convention: *.json name)

jout <- file(outputSummaryJsonFile,"w") # open an output txt file

jdata <- c("{\"Footprint\":",footprint,",\"Freed_Memory\":",freedmemtotal,",\"Freed_Mem/Response\":",freedmemperresp,",\"Freed_Mem/Min\":",freedpermin,",\"Total_Time(sec)\":",howLongRunning,",\"Acc_pauses\":",sumPause,",\"Throughput\":",throughput,
",\"Full_GC_Performance\":",fullGcPerformance,",\"GC_Performace\":",gcPerformance,",\"Avg_after_full_GC\":",avgMemAfterFullGC,",\"Avg_after_GC\":",avgMemAfterGC,",\"Freed_by_full_GC\":",fullGcFreedMemTtl,
",\"Freed_by_GC\":",freedmemtotal,",\"Avg_freed_full_FC\":",avgFreedMemFullGC,",\"Avg_freed_GC\":",avgFreedMemGC,",\"Acc_pauses\":",sumPause,",\"Acc_full_GC\":",fullGcSumPause,",\"Acc_full_GC(%)\":",
fullGcSumPausePercntg,",\"Acc_GC\":",sumPause,",\"Acc_GC(%)\":",sumPausePercntg,",\"Min_Pause\":",min(data$ElapsedTime),",\"Max_Pause\":",max(data$ElapsedTime),",\"Avg_Pause\":",mean(data$ElapsedTime),
",\"Avg_Full_GC\":",fillGcAvgPause,",\"Avg_GC\":",mean(data$ElapsedTime),"}")

cat(jdata, sep="", file=jout)
close(jout)
