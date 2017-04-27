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
  
  countData <- data.matrix(peridotCountTable)

  head(countData)

  condFac <- as.factor(peridotConditions$condition)
  
  condFac
  
  library(DESeq2)
  
  dds = DESeqDataSetFromMatrix(countData = countData, DataFrame(condFac), ~condFac)
  
  dds
  
  dds <- DESeq(dds)
  
  res <- results(dds)
  
  res
}else{
  load(file = "DESeq2.RData")
}

if(notFirstRun == "0"){
  resFinal <- data.frame(baseMean = res$baseMean, row.names =  rownames(res))
  
  baseMeanPerLvl <- sapply( levels(condFac), function(lvl) rowMeans( counts(dds,normalized=TRUE)[,condFac == lvl] ) )
  
  colnames(baseMeanPerLvl) <- c("baseMeanB", "baseMeanA")
  
  resFinal$baseMeanA <- baseMeanPerLvl[,1]
  
  resFinal$baseMeanB <- baseMeanPerLvl[,2]
  
  resFinal$foldChange <- 2^res$log2FoldChange
  
  resFinal$log2FoldChange <- res$log2FoldChange
  
  resFinal$pvalue <- res$pvalue
  
  resFinal$padj <- res$padj
}

head(resFinal)

png(filename = paste(outputFilesDir, "histogram.png", sep = "/"), width=600, height=600)

#Histogram
p1 <- with(resFinal, hist(pvalue, breaks=100, plot = F))
p2 <- with(resFinal, hist(padj, breaks=100, plot = F))
plot( p1, col="skyblue", main="Histogram", xlab = "Values")  # first histogram
plot( p2, col=scales::alpha('red',.5), add=T)
legend('topleft', c("PValue", "FDR(padjust)"), fill = c("skyblue", scales::alpha('red',.5)), bty = 'o', border = NA, cex = 0.8, bg = "white")

dev.off()

png(filename = paste(outputFilesDir, "MAPlot.png", sep = "/"), width=600, height=600)

#MA Plot
with(resFinal, plot(log(baseMean), log2FoldChange, pch=20, main="MA Plot"))
with(subset(resFinal, padj<FileConfig$fdr), points(log(baseMean), log2FoldChange, pch=20, col="red"))
abline(h=c(FileConfig$log2FoldChange, FileConfig$log2FoldChange*(-1)), col="blue")
legend('bottomright', c(paste("FDR(padj) < ", FileConfig$fdr, sep = ""), paste("Log2FoldChange = mod(", FileConfig$log2FoldChange, ")", sep = "")), col = c("red", "blue"), bty = 'o', pch = c(15, NA), lty = c(NA, 1), bg = "white", cex = 0.8)

dev.off()

png(filename = paste(outputFilesDir, "volcanoPlot.png", sep = "/"), width=600, height=600)

#Volcano Plot
with(resFinal, plot(log2FoldChange, -log10(pvalue), pch=20, main="Volcano plot"))
with(subset(resFinal, padj<FileConfig$fdr), points(log2FoldChange, -log10(pvalue), pch=20, col="red"))
abline(v=c(FileConfig$log2FoldChange, FileConfig$log2FoldChange*(-1)), col="blue")
legend('topleft', c(paste("FDR(padj) < ", FileConfig$fdr, sep = ""), paste("Log2FoldChange = mod(", FileConfig$log2FoldChange, ")", sep = "")), col = c("red", "blue"), bty = 'o', pch = c(15, NA), lty = c(NA, 1), bg = "white", cex = 0.8)

dev.off()

#Subset com PValue < FileConfig$pValue, Fold Change < FileConfig$log2FoldChange e FDR < FileConfig$fdr
if(FileConfig$log2FoldChange != 0 & FileConfig$pValue != 0 & FileConfig$fdr != 0){
  resSig = subset(resFinal, c(abs(log2FoldChange) > FileConfig$log2FoldChange & pvalue <FileConfig$pValue & padj < FileConfig$fdr))
}else if(FileConfig$log2FoldChange != 0 & FileConfig$pValue != 0){
  resSig = subset(resFinal, c(abs(log2FoldChange) > FileConfig$log2FoldChange & pvalue <FileConfig$pValue))
}else if(FileConfig$log2FoldChange != 0 & FileConfig$fdr != 0){
  resSig = subset(resFinal, c(abs(log2FoldChange) > FileConfig$log2FoldChange & padj < FileConfig$fdr))
}else if(FileConfig$pValue != 0 & FileConfig$fdr != 0){
  resSig = subset(resFinal, c(pvalue <FileConfig$pValue & padj < FileConfig$fdr))
}else if(FileConfig$log2FoldChange != 0){
  resSig = subset(resFinal, abs(log2FoldChange) > FileConfig$log2FoldChange)
}else if(FileConfig$pValue != 0){
  resSig = subset(resFinal, pvalue <FileConfig$pValue)
}else if(FileConfig$fdr != 0){
  resSig = subset(resFinal, padj < FileConfig$fdr)
}

##Remove NA
resSig = na.omit(resSig)

if(FileConfig$tops > 0){
  topRes = head(resSig, n = FileConfig$tops)
  
  write.table(topRes, paste(outputFilesDir, "/TopResults.tsv", sep = ""), sep = "\t")
}

##Create files csv
if(length(resSig$padj > 0)){
  write.table(resSig, paste(outputFilesDir, "/res.tsv", sep = ""), sep = "\t")
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

save(resFinal, file = "DESeq2.RData")