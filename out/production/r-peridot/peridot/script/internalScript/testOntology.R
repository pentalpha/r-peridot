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
params

setwd(inputFilesDir)
genelistinput = read.table("gene-list-input.txt", sep="\t", header=TRUE)
genelistinput

setwd(outputFilesDir)
file.create("plot.jpg")
