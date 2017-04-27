args = commandArgs(trailingOnly = F)

localDir <- args[length(args)-3]
localDir

inputFilesDir <- args[length(args)-2]
inputFilesDir

outputFilesDir <- args[length(args)-1]
outputFilesDir

notFirstRun <- args[length(args)]
notFirstRun

setwd(localDir)

#Get file config
FileConfigPath = paste(localDir, "config.txt", sep = "/")

#Read file config
FileConfig = read.table(FileConfigPath, header = TRUE, row.names = 1, sep = "|")

if(notFirstRun == "0"){

  peridotConditions = read.table(paste(inputFilesDir, "condition-input.tsv", sep = "/"), header=TRUE, row.names=1)
  peridotConditions

  #Read Path file
  inputTableFile = paste(inputFilesDir, "rna-seq-input.tsv", sep = "/")

  #Read file
  peridotCountTable = read.table(inputTableFile, header=TRUE, row.names=1 )

  #Ignore samples with "not-use" indicated
  #first, remove they from the conditions table
  peridotConditions <- subset(peridotConditions, condition != "not-use")
  #then, remove from the counts table
  for(i in colnames(peridotCountTable)){
    iContainsNotUse = length(grep("not.use", as.name(i))) > 0
    if(iContainsNotUse){
      #erases the column
      peridotCountTable[, i] = NULL
    }
  }
  #Finally, drop unused levels (not-use levels)
  peridotConditions = droplevels(peridotConditions)
  peridotConditions
  
  #Create data matrix
  Data = data.matrix(peridotCountTable)
  
  #Head matrix
  head(Data)
  
  #Load EBSeq
  library(EBSeq)
  
  #Specifies the median normalization function
  Sizes = MedianNorm(Data)

  peridotConditions$condition
  #Get the posterior probability of being DE
  EBOut = EBTest(Data = Data, Conditions = peridotConditions$condition, sizeFactors = Sizes, maxround = 5)
  
  #Obtain DE analysis results in a two-condition test using the output of EBTest()
  EBDERes = GetDEResults(EBOut, FDR = FileConfig$fdr)
  
  str(EBDERes$DEfound)
  
  str(EBDERes$Status)
  
  x = as.data.frame(EBOut$PPMat)
  x$MeanList = unlist(EBOut$MeanList)
  
  #Calculates the posterior fold change for each transcript across conditions.
  GeneFC = PostFC(EBOut)
  
  res = as.data.frame(unlist(EBOut$MeanList))
  
  colnames(res) = "baseMean"
  
  res$baseMeanA = unlist(EBOut$C2Mean)
  
  res$baseMeanB = unlist(EBOut$C1Mean)
  
  res$foldChange = GeneFC$RealFC
  
  res$log2FoldChange = log2(GeneFC$RealFC)
  
  res$pval = x$PPEE
  
  res$FDR = x$PPDE
}else{
  load(file = "EBSeq.RData")
}

png(filename = paste(outputFilesDir, "histogram.png", sep = "/"), width=600, height=600)

#Histogram PValue
p1 <- with(res, hist(pval, breaks=100, plot = F))
p2 <- with(res, hist(FDR, breaks=100, plot = F))
plot( p1, col="skyblue", main="Histogram", xlab = "Values")  # first histogram
plot( p2, col=scales::alpha('red',.5), add=T)
legend('topleft', c("PValue", "FDR"), fill = c("skyblue", scales::alpha('red',.5)), bty = 'o', border = NA, cex = 0.8, bg = "white")

dev.off()

png(filename = paste(outputFilesDir, "MAPlot.png", sep = "/"), width=600, height=600)

#MA Plot
with(res, plot(log(baseMean), log2FoldChange, pch=20, main="MA Plot"))
with(subset(res, 1-FDR<FileConfig$fdr), points(log(baseMean), log2FoldChange, pch=20, col="red"))
abline(h=c(FileConfig$log2FoldChange, FileConfig$log2FoldChange*(-1)), col="blue")
legend('bottomright', c(paste("FDR < ", FileConfig$fdr, sep = ""), paste("Log2FoldChange = mod(", FileConfig$log2FoldChange, ")", sep = "")), col = c("red", "blue"), bty = 'o', pch = c(15, NA), lty = c(NA, 1), bg = "white", cex = 0.8)

dev.off()

png(filename = paste(outputFilesDir, "volcanoPlot.png", sep = "/"), width=600, height=600)

#Volcano Plot
with(res, plot(log2FoldChange, -log10(pval), pch=20, main="Volcano plot"))
with(subset(res, 1-FDR<FileConfig$fdr), points(log2FoldChange, -log10(pval), pch=20, col="red"))

with(res, plot(log2FoldChange, -log10(pval), pch=20, main="Volcano plot"))
with(subset(res, 1-FDR<FileConfig$fdr), points(log2FoldChange, -log10(pval), pch=20, col="red"))
abline(v=c(FileConfig$log2FoldChange, FileConfig$log2FoldChange*(-1)), col="blue")
legend('topleft', c(paste("FDR < ", FileConfig$fdr, sep = ""), paste("Log2FoldChange = mod(", FileConfig$log2FoldChange, ")", sep = "")), col = c("red", "blue"), bty = 'o', pch = c(15, NA), lty = c(NA, 1), bg = "white", cex = 0.8)

dev.off()

#Subset com PValue < FileConfig$pValue, Fold Change < FileConfig$log2FoldChange e FDR < FileConfig$fdr
if(FileConfig$log2FoldChange != 0 & FileConfig$pValue != 0 & FileConfig$fdr != 0){
  resSig = subset(res, abs(log2FoldChange) > FileConfig$log2FoldChange & pval <FileConfig$pValue & 1-FDR < FileConfig$fdr)
}else if(FileConfig$log2FoldChange != 0 & FileConfig$pValue != 0){
  resSig = subset(res, abs(log2FoldChange) > FileConfig$log2FoldChange & pval <FileConfig$pValue)
}else if(FileConfig$log2FoldChange != 0 & FileConfig$fdr != 0){
  resSig = subset(res, abs(log2FoldChange) > FileConfig$log2FoldChange & 1-FDR < FileConfig$fdr)
}else if(FileConfig$pValue != 0 & FileConfig$fdr != 0){
  resSig = subset(res, pval <FileConfig$pValue & 1-FDR < FileConfig$fdr)
}else if(FileConfig$log2FoldChange != 0){
  resSig = subset(res, abs(log2FoldChange) > FileConfig$log2FoldChange)
}else if(FileConfig$pValue != 0){
  resSig = subset(res, pval <FileConfig$pValue)
}else if(FileConfig$fdr != 0){
  resSig = subset(res, 1-FDR < FileConfig$fdr)
}


##Remove NA
resSig = na.omit(resSig)

if(FileConfig$tops > 0){
  topRes = head(resSig, n = FileConfig$tops)
  
  write.table(topRes, paste(outputFilesDir, "/TopResults.tsv", sep = ""), sep = "\t")
}

##Create files csv
if(length(resSig$FDR > 0)){
  write.table(resSig, paste(outputFilesDir, "/res.tsv", sep = ""), sep = "\t")
}
#

pdf(file = paste(outputFilesDir, "plots.pdf", sep = "/"))

#Histogram PValue
p1 <- with(res, hist(pval, breaks=100, plot = F))
p2 <- with(res, hist(FDR, breaks=100, plot = F))
plot( p1, col="skyblue", main="Histogram", xlab = "Values")  # first histogram
plot( p2, col=scales::alpha('red',.5), add=T)
legend('topleft', c("PValue", "FDR"), fill = c("skyblue", scales::alpha('red',.5)), bty = 'o', border = NA, cex = 0.8, bg = "white")

#MA Plot
with(res, plot(log(baseMean), log2FoldChange, pch=20, main="MA Plot"))
with(subset(res, 1-FDR<FileConfig$fdr), points(log(baseMean), log2FoldChange, pch=20, col="red"))
abline(h=c(FileConfig$log2FoldChange, FileConfig$log2FoldChange*(-1)), col="blue")
legend('bottomright', c(paste("FDR < ", FileConfig$fdr, sep = ""), paste("Log2FoldChange = mod(", FileConfig$log2FoldChange, ")", sep = "")), col = c("red", "blue"), bty = 'o', pch = c(15, NA), lty = c(NA, 1), bg = "white", cex = 0.8)

#Volcano Plot
with(res, plot(log2FoldChange, -log10(pval), pch=20, main="Volcano plot"))
with(subset(res, 1-FDR<FileConfig$fdr), points(log2FoldChange, -log10(pval), pch=20, col="red"))

with(res, plot(log2FoldChange, -log10(pval), pch=20, main="Volcano plot"))
with(subset(res, 1-FDR<FileConfig$fdr), points(log2FoldChange, -log10(pval), pch=20, col="red"))
abline(v=c(FileConfig$log2FoldChange, FileConfig$log2FoldChange*(-1)), col="blue")
legend('topleft', c(paste("FDR < ", FileConfig$fdr, sep = ""), paste("Log2FoldChange = mod(", FileConfig$log2FoldChange, ")", sep = "")), col = c("red", "blue"), bty = 'o', pch = c(15, NA), lty = c(NA, 1), bg = "white", cex = 0.8)

dev.off()

save(res, file = "EBSeq.RData")
