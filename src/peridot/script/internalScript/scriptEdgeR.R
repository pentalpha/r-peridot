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
  
  #Load edgeR
  library(edgeR)
  
  edesign <- model.matrix(~peridotConditions$condition)
  peridotCountTable = as.matrix(peridotCountTable)
  
  #Create DGEList
  e <- DGEList(counts=peridotCountTable)
  
  #Calculate nomalization factors
  e <- calcNormFactors(e)
  
  normCounts <- e$counts/e$samples$norm.factors
  
  BaseMeanVect = rowMeans(normCounts)
  
  #Estimates a common negative binomial dispersion parameter for a DGE dataset
  e <- estimateGLMCommonDisp(e, edesign)
  
  #Estimates the abundace-disersion trend
  e <- estimateGLMTrendedDisp(e, edesign) 
  
  #Compute an empirical Bayes estimate of the negative binomial dispersion parameter
  e <- estimateGLMTagwiseDisp(e, edesign)
  
  ## Fit the model, testing the coefficient for the treated vs untreated comparison
  efit <- glmFit(e, edesign)
  
  efit <- glmLRT(efit)#, coef="conditiontreated")
  
  ## Make a table of results
  etable <- topTags(efit, n=nrow(e))$table
  
  ## Create a column FoldChange
  etable$FoldChange = 2^etable$logFC
  
  ## List of conditions A
  factA = peridotConditions$condition==levels(peridotConditions$condition)[2]
  
  ## Columns with condition A
  colA = normCounts[,factA]
  
  ## List of conditions B
  factB = peridotConditions$condition==levels(peridotConditions$condition)[1]
  
  ## Columns with condition B
  colB = normCounts[,factB]
  
  ## Means of condition A
  baseMeanA = rowMeans(colA)
  
  ## Means of condition B
  baseMeanB = rowMeans(colB)
  
  ## Create data frame of baseMeans
  BaseMeandf = as.data.frame(BaseMeanVect)
  
  colnames(BaseMeandf) = "baseMean"
  
  BaseMeandf$baseMeanA = baseMeanA
  
  BaseMeandf$baseMeanB = baseMeanB
  
  df = merge(BaseMeandf, etable, by = 'row.names')
  
  res = df[c("Row.names", "baseMean", "baseMeanA", "baseMeanB", "FoldChange", "logFC", "logCPM", "LR", "PValue", "FDR")]
  
  rownames(res) = res$Row.names
  
  res$Row.names = NULL
  
  res$logCPM = NULL
  
  res$LR = NULL
}else{
  load(file = "edgeR.RData")
}

png(filename = paste(outputFilesDir, "histogram.png", sep = "/"), width=600, height=600)

#Histogram PValue and FDR
p1 <- with(res, hist(PValue, breaks=100, plot = F))
p2 <- with(res, hist(FDR, breaks=100, plot = F))
plot( p1, col="skyblue", main="Histogram", xlab = "Values")  # first histogram
plot( p2, col=scales::alpha('red',.5), add=T)
legend('topleft', c("PValue", "FDR"), fill = c("skyblue", scales::alpha('red',.5)), bty = 'o', border = NA, cex = 0.8, bg = "white")

dev.off()

png(filename = paste(outputFilesDir, "MAPlot.png", sep = "/"), width=600, height=600)

#MA Plot
with(res, plot(log(baseMean), logFC, pch=20, main="MA Plot"))
with(subset(res, FDR<FileConfig$fdr), points(log(baseMean), logFC, pch=20, col="red"))
with(subset(res, FDR<0.4), points(log(baseMean), logFC, pch=20, col="red"))
abline(h=c(FileConfig$log2FoldChange, FileConfig$log2FoldChange*(-1)), col="blue")
legend('bottomright', c(paste("FDR < ", FileConfig$fdr, sep = ""), paste("Log2FoldChange = mod(", FileConfig$log2FoldChange, ")", sep = "")), col = c("red", "blue"), bty = 'o', pch = c(15, NA), lty = c(NA, 1), bg = "white", cex = 0.8)

dev.off()

png(filename = paste(outputFilesDir, "volcanoPlot.png", sep = "/"), width=600, height=600)

#Volcano Plot
with(res, plot(logFC, -log10(PValue), pch=20, main="Volcano plot"))
with(subset(res, FDR<FileConfig$fdr), points(logFC, -log10(PValue), pch=20, col="red"))
abline(v=c(FileConfig$log2FoldChange, FileConfig$log2FoldChange*(-1)), col="blue")
legend('topleft', c(paste("FDR < ", FileConfig$fdr, sep = ""), paste("Log2FoldChange = mod(", FileConfig$log2FoldChange, ")", sep = "")), col = c("red", "blue"), bty = 'o', pch = c(15, NA), lty = c(NA, 1), bg = "white", cex = 0.8)

dev.off()

#Subset com PValue < FileConfig$pValue, Fold Change < FileConfig$log2FoldChange e FDR < FileConfig$fdr
if(FileConfig$log2FoldChange != 0 & FileConfig$pValue != 0 & FileConfig$fdr != 0){
  resSig = subset(res, abs(logFC) > FileConfig$log2FoldChange & PValue < FileConfig$pValue & FDR < FileConfig$fdr)
}else if(FileConfig$log2FoldChange != 0 & FileConfig$pValue != 0){
  resSig = subset(res, abs(logFC) > FileConfig$log2FoldChange & PValue <FileConfig$pValue)
}else if(FileConfig$log2FoldChange != 0 & FileConfig$fdr != 0){
  resSig = subset(res, abs(logFC) > FileConfig$log2FoldChange & FDR < FileConfig$fdr)
}else if(FileConfig$pValue != 0 & FileConfig$fdr != 0){
  resSig = subset(res, PValue <FileConfig$pValue & FDR < FileConfig$fdr)
}else if(FileConfig$log2FoldChange != 0){
  resSig = subset(res, abs(logFC) > FileConfig$log2FoldChange)
}else if(FileConfig$pValue != 0){
  resSig = subset(res, PValue <FileConfig$pValue)
}else if(FileConfig$fdr != 0){
  resSig = subset(res, FDR < FileConfig$fdr)
}
  
##Remove NA
resSig = na.omit(resSig)
#

head(resSig)

if(FileConfig$tops > 0){
  topRes = head(resSig, n = FileConfig$tops)
  
  write.table(topFDR, paste(outputFilesDir, "TopResults.tsv", sep = "/"), sep = "\t")
}

##Create files csv
if(length(resSig$FDR > 0)){
  write.table(resSig, paste(outputFilesDir, "res.tsv", sep = "/"), sep = "\t")
}
#

pdf(file = paste(outputFilesDir, "plots.pdf", sep = "/"))

#Histogram PValue and FDR
p1 <- with(res, hist(PValue, breaks=100, plot = F))
p2 <- with(res, hist(FDR, breaks=100, plot = F))
plot( p1, col="skyblue", main="Histogram", xlab = "Values")  # first histogram
plot( p2, col=scales::alpha('red',.5), add=T)
legend('topleft', c("PValue", "FDR"), fill = c("skyblue", scales::alpha('red',.5)), bty = 'o', border = NA, bg = "white", cex = 0.8)

#MA Plot
with(res, plot(log(baseMean), logFC, pch=20, main="MA Plot"))
with(subset(res, FDR<FileConfig$fdr), points(log(baseMean), logFC, pch=20, col="red"))
abline(h=c(FileConfig$log2FoldChange, FileConfig$log2FoldChange*(-1)), col="blue")
legend('bottomright', c(paste("FDR < ", FileConfig$fdr, sep = ""), paste("Log2FoldChange = mod(", FileConfig$log2FoldChange, ")", sep = "")), col = c("red", "blue"), bty = 'o', pch = c(15, NA), lty = c(NA, 1), bg = "white", cex = 0.8)

#Volcano Plot
with(res, plot(logFC, -log10(PValue), pch=20, main="Volcano plot"))
with(subset(res, FDR<FileConfig$fdr), points(logFC, -log10(PValue), pch=20, col="red"))
abline(v=c(FileConfig$log2FoldChange, FileConfig$log2FoldChange*(-1)), col="blue")
legend('topleft', c(paste("FDR < ", FileConfig$fdr, sep = ""), paste("Log2FoldChange = mod(", FileConfig$log2FoldChange, ")", sep = "")), col = c("red", "blue"), bty = 'o', pch = c(15, NA), lty = c(NA, 1), bg = "white", cex = 0.8)

dev.off()

save(etable, res, file = "edgeR.RData")