args = commandArgs(trailingOnly = F)

localDir <- args[length(args)-3]
setwd(localDir)
localDir
inputFilesDir <- args[length(args)-2]
inputFilesDir
outputFilesDir <- args[length(args)-1]
outputFilesDir
notFirstRun <- args[length(args)]
notFirstRun

paramFile = paste(getwd(), "config.txt", sep = "/")
params = read.table(paramFile, header = TRUE, row.names = 1, sep = "|")

setwd(inputFilesDir)
#input = read.table("testPack.AnalysisScript/res.csv", sep="\t", header=TRUE)
#

setwd(outputFilesDir)
file.create("result1.pdf")
file.create("result2.jpg")