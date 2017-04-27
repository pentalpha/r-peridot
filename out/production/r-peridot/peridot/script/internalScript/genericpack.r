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
params$p.value
params$fdr
params$log2.fold.change
params$tops

setwd(inputFilesDir)
rnaseqinput = read.table("rna-seq-input.tsv", sep="\t", header=TRUE)
#rnaseqinput

setwd(outputFilesDir)
file.create("plots.pdf")
file.create("histogram.jpg")
file.create("volcanoPlot.jpg")
file.create("MAPlot.jpg")
file.create("res.csv")
