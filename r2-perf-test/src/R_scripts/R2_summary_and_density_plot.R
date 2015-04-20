rm(list = ls())      # Clear all variables
graphics.off()    # Close graphics windows

# Parse and use command line arguments
# Command Line Example:
# Rscript scripts/R2_summary_and_density_plotmultifiles.R data/logs_Nov10_qa31_build8 results/R2_Nov10_qa31_build8.png

args <- commandArgs(TRUE)      # retrieve args

dataFileOrDir         <- args[1]
output_graph_file     <- "response_time"
outputSummaryHtmlFile <- "summary.html"
outputSummaryJsonFile <- "summary.json"

ttlresponses <- 0
successcount <- 0
failedcount <- 0


# --- Functions

getAxisTimeFrequency <- function(ttlexecutiontime)
{
  atfrequency <- 1;
  if (ttlexecutiontime/60 > 1 )
  {
	atfrequency <- ttlexecutiontime/60;
  }
  atfrequency
}

readCsvFile <- function(datafile)
{
  cat ("\nReading data from ",datafile, "\n\n")
  data <- read.csv(datafile)

  # Get Time
  dates <- strptime(data$date, "%Y/%m/%d %H:%M:%OS", tz="")
  # Get response times
  resptimes <- data$latency_nano / 10E6

  # Create data frame with all responses (including failed)
  alldata <- data.frame(date=dates, resptime=resptimes)
  alldata
}

plotResponseTimeVsTime <- function (data, starttime, endtime, axistimefrequency,gtitle)
{
  x <- strptime(data$date,"%Y-%m-%d %H:%M:%OS", tz="")
  y <- data$resptime

  xpos <- plot(
    x,                                    # Timestamp
    y,                                    # Data to plot
    type="p",				  # Plot lines and points. Use "p" for points only, "l" for lines only
    xlab="",                              # Label for the x-axis
    ylab="Response Time (msec)",          # Label for the y-axis
    font.lab=2,		                  # Font to use for the axis labels: 1=plain text, 2=bold, 3=italic, 4=bold italic
    xaxt = "n",
    xlim=c(starttime, endtime),
    col = "green",
    cex = 0.7,
    cex.axis=1.2,
    cex.lab=1.5,
    bty="l"				  # Box around plot to contain only left and lower lines
  )

  # Plot failed requests
  failedResponsesLineWidth = 5
  if (length(x[is.na(y)]) > length(x)/10)
  {
    failedResponsesLineWidth = 1
  }
  rug(as.POSIXct(x[is.na(y)]), side=1, lwd=failedResponsesLineWidth, col="red")

  # Title
  title(gtitle, cex.main=2, font.main=4)

  # Setup x-axis with time labels in "%Y/%m/%d %H:%M:%S" format ,etc.
  dr <- c(starttime, endtime, axistimefrequency);
  drseq <- seq(starttime, endtime, by=axistimefrequency);
  axis(1, drseq, at=drseq ,labels = format(drseq, "%Y/%m/%d %H:%M:%S", tz=""), cex.axis = 1.2, las=2)
  par(xaxp = dr)
  grid()

  # Add mean response time line to the graph
  abline(h=resp_mean, col="blue",lwd=4)
  text(as.numeric(endtime),pos=3, adj = 1,resp_median,paste("Median = ",resp_median),font=2, cex=1.7 );

  # Add legend for successful and failed responses
  legend("topright","(x,y)",legend=c("successful","failed"), pch=1, col=c(rep('green',1), rep('red', 1)), lwd=c(1.5, 3.0))

  xpos
}

plotDensityGraph <- function (resptimes, density)
{
  # margin smaller than default
  par( las = 1, mar = c(5,5,5,5)+.1 )

  # calculate histogram (but do not display)
  h <- hist( resptimes, plot = FALSE)  #, breaks = 15 )

  # just set the scene and axes
  plot( h , border = NA, freq = FALSE, xlab = "Response Time", ylab = "Density", cex.axis = 1.1, cex.lab = 1.1, cex = 0.3, main="")
  title(paste("Response Time Density"), cex.main=2, font.main=4)

  # horizontal line
  abline( h = axTicks(2) , col = "gray", lwd = .5 )

  # just to get the boxes right
  dgraph <- plot( h, add = TRUE, lwd = .5 , freq = FALSE, xlab = "", ylab = "", axes = FALSE , col="grey")

  # adds a tick and translucent line to display the density estimate
  lines( density, lwd = 4, col = "orange" )

  # add a rug (response time)
  rug( resptimes, col = "blue" )

  # box around the plot
  box()

  dgraph
}

generateSummaryHtmlTable <- function (htmlTableHeaders, values)
{
  # Generate header
  summary <- paste("<th>",htmlTableHeaders,sep="</th>\n<th>",collapse="")
  # Generate row
  rows <- paste("<th>",values,sep="</th>\n<th>",collapse="")
  summary <- paste("<table border=\"1\" cellpadding=\"5\" cellspacing=\"5\" width=\"100%\"><tr>",summary,"</tr>\n","<tr>",rows,"</tr>\n</table>")

  summary
}

# --- End of functions

op <- options(digits.secs=4)

r2stats <- NULL

r2stats <- readCsvFile(dataFileOrDir)

dates <- strptime(r2stats$date,"%Y-%m-%d %H:%M:%OS", tz="")
starttime <- min(as.POSIXct(dates))

attach(r2stats)

endtime <- max(dates)
successData <- r2stats[!is.na(r2stats$resptime),]

ttlresponses <- length(r2stats$resptime)

### Calculate total number of responses received
respcount <- length(r2stats$resptime)

# Get test details for summary report in graph legend
successcount <- length(successData$resptime)    # only successful responses
failedcount <- ttlresponses - successcount    #length(r2stats[is.na(r2stats$resptime),]) #  ttlresponses - successcount

### Calculate mean and median response time values for all responses
sums <- c(summary(successData$resptime))

resp_mean <- sums[4]
resp_median <- sums[3]
minresptime <- sums[1]
maxresptime <- sums[6]
frsq <- sums[2] # 1st. Qu
trdq <- sums[5] # 3rd Qu.
percentile50 <- quantile(successData$resptime, 0.5)
percentile90 <- quantile(successData$resptime, 0.9)
percentile95 <- quantile(successData$resptime, 0.95)
percentile99 <- quantile(successData$resptime, 0.99)
standard_deviation <- sapply(successData,sd)

#Calculate throughput
howLongRunning <- difftime(as.POSIXct(endtime), as.POSIXct(starttime), tz="", units="secs")
axistimefrequency <- getAxisTimeFrequency (as.numeric(howLongRunning, units="secs"))
throughput <- format(((ttlresponses / as.numeric(howLongRunning, units="secs"))),digits=2,scientific=FALSE)
resptimedensity <- density(successData$resptime)
successData <- NULL

graphHeight=1100
ttlPlots=2

cat("\nTotal Responses:",ttlresponses)
cat("\nTotal Successful Responses:",successcount)
cat("\nTotal Failed Responses:",failedcount)
cat("\nStats:")
cat("\n\tMean latency (in millis): ", mean(r2stats$resptime))
cat("\n\tReqs / Sec: ", nrow(r2stats) / as.numeric(howLongRunning))
cat("\n\tMin Latency: ", min(r2stats$resptime))
cat("\n\t50% Latency: ", percentile50)
cat("\n\t90% Latency: ", percentile90)
cat("\n\t95% Latency: ", percentile95)
cat("\n\t99% Latency: ", percentile99)
cat("\n\tMax Latency: ", max(r2stats$resptime))

# If test run is 4+ minutes long, plot 1 minute graph in addition to main and density graphs
mid <- round(min(length(dates)/2))
if ((endtime - (dates[mid]+60)) > 0)
{
  graphHeight=2200
  ttlPlots=3
}

# Define png format file
png(output_graph_file,width=1600,height=graphHeight)

# Plotting graphs ...
plot.new()                              # start a new plot
                                        # set parameters for this graph
par(mar=c(24,5,5,5)+0.1)                # set the size of the outer margins
par(mfrow=c(ttlPlots,1))		# plot three graphs in one page

cat("\n\nPlotting first graph: Response Time vs Execution Time\n")

# First plot: response time vs time
#
#plotResponseTimeVsTime (dates, r2stats$resptime, as.POSIXct(starttime), as.POSIXct(endtime), axistimefrequency, paste("Response Time vs Test Execution Time (",starttime," - ",endtime,")"))
plotResponseTimeVsTime (r2stats, as.POSIXct(starttime), as.POSIXct(endtime), axistimefrequency, paste("Response Time vs Test Execution Time (",starttime," - ",endtime,")"))


cat("\nPlotting second graph: Density\n")

# Second plot : response time density
#
plotDensityGraph(r2stats$resptime,resptimedensity)

options(op)
dev.off()

#------------------ Print summary stats into a file (file naming convention: *.summary name)
out <- file(outputSummaryHtmlFile,"w") # open an output txt file

htmldata <- c("<table border=\"1\" cellpadding=\"5\" cellspacing=\"5\" width=\"100%\">")

# Summary html table
summaryHeader <- c("Test Start Time","Test Stop Time","Total Execution Time(sec)","Total Responses","Successful Responses","Failed Responses","% Failed Responses","Throughput (r/sec")
summaryData <- c(format(starttime,"%Y/%m/%d %H:%M:%S", tz=""),format(endtime,"%Y/%m/%d %H:%M:%S", tz=""), howLongRunning, ttlresponses, successcount, failedcount, paste((failedcount*100)/ttlresponses," %"),throughput)

# Response Stats html table
responsesStatsHeader <- c("Min Response Time(millisec)","1st. Qu(millisec)","Median(millisec)","Mean(millisec)","3rd Qu.(millisec)","90%(millisec)","95%(millisec)","99%(millisec)","Max Response(millisec)","Standard Deviation")
responsesStatsData <- c(minresptime, frsq, resp_median, resp_mean, trdq, percentile90, percentile95, percentile99, maxresptime, toString(standard_deviation))

cat(htmldata,"<p><br>SERVER REQUESTS<br></p><p><br>SUMMARY<br></p>",generateSummaryHtmlTable(summaryHeader,summaryData ),"<p><br>RESPONSE STATS<br></p>",generateSummaryHtmlTable(responsesStatsHeader,responsesStatsData ),c("</table"), sep="", file=out)

close(out)

#-------- Generate json report (file naming convention: *.json name)

jout <- file(outputSummaryJsonFile,"w") # open an output txt file

jdata <- c("{\"Total_Responses\":",ttlresponses,",\"Successful_Responses\":",successcount,",\"Failed_Responses\":",failedcount,",\"Throughput(r/sec)\":",throughput,
",\"Min_Response_Time\":",minresptime,",\"1st_Qu_RespTime\":",frsq,",\"Median_RespTime\":",resp_median,",\"Mean_RespTime\":",resp_mean,",\"3rd_Qu_RespTime\":",trdq,
",\"Max_Response\":",maxresptime,",\"90_Percentile(millisec)\":",percentile90,",\"95_Percentile(millisec)\":",percentile95,",\"99_Percentile(millisec)\":",percentile99,"}")

cat(jdata, sep="", file=jout)
close(jout)

detach(r2stats)


