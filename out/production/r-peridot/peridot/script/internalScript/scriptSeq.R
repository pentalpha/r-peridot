args = commandArgs(trailingOnly = F)

localDir <- args[length(args)-3]

localDir

inputFilesDir <- args[length(args)-2]

inputFilesDir

outputFilesDir <- args[length(args)-1]

outputFilesDir

notFirstRun <- args[length(args)]

notFirstRun

localDir
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
  
  library(sSeq)
  
  res = nbTestSH(peridotCountTable, peridotConditions$condition, levels(peridotConditions$condition)[1], levels(peridotConditions$condition)[2])
  
  resFinal <- data.frame(baseMean = res$Mean, row.names =  rownames(res))
  
  resFinal$baseMeanA <- res$rawMeanA
  
  resFinal$baseMeanB <- res$rawMeanB
  
  resFinal$foldChange <- 2^res$rawLog2FoldChange
  
  resFinal$log2FoldChange <- res$rawLog2FoldChange
  
  resFinal$pvalue <- res$pval
  
  resFinal$padj <- p.adjust(res$pval, method = "fdr", n = length(res$pval))
  
  head(p.adjust(res$pval, method = "fdr", n = length(res$pval)))
  
  head(resFinal)

}else{
  load(file = "sSeq.RData")
}
  
jpeg(filename = paste(outputFilesDir, "histogram.jpg", sep = "/"))

#Histogram
p1 <- with(resFinal, hist(pvalue, breaks=100, plot = F))
p2 <- with(resFinal, hist(padj, breaks=100, plot = F))
plot( p1, col="skyblue", main="Histogram", xlab = "Values")  # first histogram
plot( p2, col=scales::alpha('red',.5), add=T)
legend('topleft', c("PValue", "FDR(padjust)"), fill = c("skyblue", scales::alpha('red',.5)), bty = 'o', border = NA, cex = 0.8, bg = "white")

dev.off()

jpeg(filename = paste(outputFilesDir, "MAPlot.jpg", sep = "/"))

#MA Plot
with(resFinal, plot(log(baseMean), log2FoldChange, pch=20, main="MA Plot"))
with(subset(resFinal, padj<FileConfig$fdr), points(log(baseMean), log2FoldChange, pch=20, col="red"))
abline(h=c(FileConfig$log2FoldChange, FileConfig$log2FoldChange*(-1)), col="blue")
legend('bottomright', c(paste("FDR(padj) < ", FileConfig$fdr, sep = ""), paste("Log2FoldChange = mod(", FileConfig$log2FoldChange, ")", sep = "")), col = c("red", "blue"), bty = 'o', pch = c(15, NA), lty = c(NA, 1), bg = "white", cex = 0.8)

dev.off()

jpeg(filename = paste(outputFilesDir, "volcanoPlot.jpg", sep = "/"))

#Volcano Plot
with(resFinal, plot(log2FoldChange, -log10(pvalue), pch=20, main="Volcano plot"))
with(subset(resFinal, padj<FileConfig$fdr), points(log2FoldChange, -log10(pvalue), pch=20, col="red"))
abline(v=c(FileConfig$log2FoldChange, FileConfig$log2FoldChange*(-1)), col="blue")
legend('topleft', c(paste("FDR(padj) < ", FileConfig$fdr, sep = ""), paste("Log2FoldChange = mod(", FileConfig$log2FoldChange, ")", sep = "")), col = c("red", "blue"), bty = 'o', pch = c(15, NA), lty = c(NA, 1), bg = "white", cex = 0.8)

dev.off()

#Subset com PValue < FileConfig$pValue, Fold Change < FileConfig$log2FoldChange e FDR < FileConfig$fdr
if(FileConfig$log2FoldChange != 0 & FileConfig$pValue != 0 & FileConfig$fdr != 0){
  resSub = subset(resFinal, abs(log2FoldChange) > FileConfig$log2FoldChange & pvalue <FileConfig$pValue & padj < FileConfig$fdr)
}else if(FileConfig$log2FoldChange != 0 & FileConfig$pValue != 0){
  resSub = subset(resFinal, abs(log2FoldChange) > FileConfig$log2FoldChange & pvalue <FileConfig$pValue)
}else if(FileConfig$log2FoldChange != 0 & FileConfig$fdr != 0){
  resSub = subset(resFinal, abs(log2FoldChange) > FileConfig$log2FoldChange & padj < FileConfig$fdr)
}else if(FileConfig$pValue != 0 & FileConfig$fdr != 0){
  resSub = subset(resFinal, pvalue <FileConfig$pValue & padj < FileConfig$fdr)
}else if(FileConfig$log2FoldChange != 0){
  resSub = subset(resFinal, abs(log2FoldChange) > FileConfig$log2FoldChange)
}else if(FileConfig$pValue != 0){
  resSub = subset(resFinal, pvalue <FileConfig$pValue)
}else if(FileConfig$fdr != 0){
  resSub = subset(resFinal, padj < FileConfig$fdr)
}

##Remove NA
resSub = na.omit(resSub)

##Create Files Top DGE
if(FileConfig$tops > 0 & length(resSub$padj > 0)){
  topRes = head(resSub, n = FileConfig$tops)
  
  write.table(topRes, paste(outputFilesDir, "TopResults.tsv", sep = "/"), sep = "\t")
}

##Create files tsv
if(length(resSub$padj > 0)){
  write.table(resSub, paste(outputFilesDir, "res.tsv", sep = "/"), sep = "\t")
}

pdf(file = paste(outputFilesDir, "plots.pdf", sep = "/"))
  
#Histogram
p1 <- with(resFinal, hist(pvalue, breaks=100, plot = F))
p2 <- with(resFinal, hist(padj, breaks=100, plot = F))
plot( p1, col="skyblue", main="Histogram", xlab = "Values")  # first histogram
plot( p2, col=scales::alpha('red',.5), add=T)
legend('topleft', c("PValue", "FDR(padjust)"), fill = c("skyblue", scales::alpha('red',.5)), bty = 'o', border = NA, cex = 0.8, bg = "white")

#MA Plot
with(resFinal, plot(log(baseMean), log2FoldChange, pch=20, main="MA Plot"))
with(subset(resFinal, padj<FileConfig$fdr), points(log(baseMean), log2FoldChange, pch=20, col="red"))
abline(h=c(FileConfig$log2FoldChange, FileConfig$log2FoldChange*(-1)), col="blue")
legend('bottomright', c(paste("FDR(padj) < ", FileConfig$fdr, sep = ""), paste("Log2FoldChange = mod(", FileConfig$log2FoldChange, ")", sep = "")), col = c("red", "blue"), bty = 'o', pch = c(15, NA), lty = c(NA, 1), bg = "white", cex = 0.8)

#Volcano Plot
with(resFinal, plot(log2FoldChange, -log10(pvalue), pch=20, main="Volcano plot"))
with(subset(resFinal, padj<FileConfig$fdr), points(log2FoldChange, -log10(pvalue), pch=20, col="red"))
abline(v=c(FileConfig$log2FoldChange, FileConfig$log2FoldChange*(-1)), col="blue")
legend('topleft', c(paste("FDR(padj) < ", FileConfig$fdr, sep = ""), paste("Log2FoldChange = mod(", FileConfig$log2FoldChange, ")", sep = "")), col = c("red", "blue"), bty = 'o', pch = c(15, NA), lty = c(NA, 1), bg = "white", cex = 0.8)

dev.off()

save(resFinal, file = "sSeq.RData")