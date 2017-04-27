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

#Get directory
FileConfig = getwd()

#Read file example
peridotCountTable = read.table(paste(inputFilesDir, "rna-seq-input.tsv", sep = "/"), header=TRUE, row.names=1 )

geneNames = rownames(peridotCountTable)

peridotConditions = read.table(paste(inputFilesDir, "condition-input.tsv", sep = "/"), header=TRUE, row.names=1)
peridotConditions

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

# Normalizar o dado de entrada #
peridotCountTable = as.data.frame(lapply(peridotCountTable, function(x) (x/sum(x))*10000000))

rownames(peridotCountTable) = geneNames

#############################

# Gerar boxplot #

lev = levels(peridotConditions$condition)

color.code<-colorRampPalette(c('blue','red'), space="rgb")(length(lev))

peridotCountTableNA = as.data.frame(apply(peridotCountTable, c(1, 2), function(x){
  #print(x)
  if(x == 0){
    x = NA
  }else{
    x = x
  }
}))

png(filename = paste(outputFilesDir, "BoxPlot.png", sep = "/"), width=600, height=600)

boxplot(log2(peridotCountTableNA), outline = F, col=color.code[peridotConditions$condition], main = "Boxplot", las=2)

dev.off()  
  
###############################

library(pvclust)

library(gplots)

# Abrir o arquivo de miRNAs achados nos pacotes do R-peridot #
intersectFile = paste(inputFilesDir, "VennDiagram.PostAnalysisScript/Intersect.tsv", sep = "/")

inter = read.table(file = intersectFile, header = F, sep = "\t")

################################

# Calcular o PCA #
tperidot = t(peridotCountTable)

pca = prcomp(tperidot)

png(filename = paste(outputFilesDir, "PCA.png", sep = "/"), width=600, height=600)

plot(pca$x[,1], pca$x[,2], xlab="PCA 1", ylab="PCA 2",type="p", pch=19, col=color.code[peridotConditions$condition] , cex=1.0, xlim=c(min(pca$x[,1])*1.1, max(pca$x[,1])*1.1), ylim=c(min(pca$x[,2]*1.1), max(pca$x[,2])*1.1), main = "All Samples TCGA vs mirBase")

text(pca$x[,1] -4,pca$x[,2]-1, rownames(tperidot),cex=0.7, pos = 1)

dev.off()

################################

# Salvar arquivos normalizados #

rownames(peridotCountTable) = geneNames

write.table(peridotCountTable, paste(outputFilesDir, "NormalizedCounts.tsv", sep = "/"), sep = "\t", row.names = T)

###############################

length(inter[,1])

if(length(inter[,1]) > 6){
  # Calcular dendrograma e heatmap só funciona com mais de 6 miRNAs encontrados #
  
  peridotCountTable[] = lapply(peridotCountTable, function(x) as.integer(x))
  
  subInter = intersect(rownames(peridotCountTable), inter[,1])
  
  d = as.matrix(peridotCountTable[subInter,])
  
  d.pv = pvclust(d, nboot = 1000, parallel = TRUE, method.hclust = "complete", method.dist = "euclidean")
  
  png(filename = paste(outputFilesDir, "Dendrogram.png", sep = "/"), width=600, height=600)
  
  plot(d.pv)
  
  dev.off()
  
  clusters = list()
  
  clusters$samples = hclust(dist(t(d)))
  
  clusters$genes = hclust(dist(d))
  
  d2 = as.matrix(apply(d, c(1,2), function(x){
    if(x == 0){
      x = 1
    }else{
      x = x
    }
  }))
  
  png(filename = paste(outputFilesDir, "HeatMap.png", sep = "/"), width=600, height=600)
  
  heatmap.2(log2(d2), Rowv = as.dendrogram(clusters$genes), Colv = as.dendrogram(clusters$samples), dendrogram = "both", key = T, keysize = 1.4, key.par=list(mar=c(3,1,3,1)), col = greenred(200), scale = "none", trace = "none", cexRow = 0.1, cexCol = 0.4, srtCol = 90, labRow = "", density.info = 'histogram', main = "HeatMap", margins = c(10, 5))
  
  dev.off()
  
  PDFheight = nrow(d)/8
  PDFwidth = ncol(d)/8
  if(PDFheight <= 8) {PDFheight = 10}
  if(PDFwidth <= 8) {PDFwidth = 10}
  
  pdf(file = paste(outputFilesDir, "aux1.pdf", sep = "/"))
  
  par(cex.axis=0.8)
  
  boxplot(log2(peridotCountTableNA), outline = F, col=color.code[peridotConditions$condition], main = "Boxplot", las=2)
  
  plot(pca$x[,1], pca$x[,2], xlab="PCA 1", ylab="PCA 2",type="p", pch=19, col=color.code[peridotConditions$condition] , cex=1.0, xlim=c(min(pca$x[,1])*1.1, max(pca$x[,1])*1.1), ylim=c(min(pca$x[,2]*1.1), max(pca$x[,2])*1.1), main = "PCA")
  
  text(pca$x[,1] -4,pca$x[,2]-1, rownames(tperidot),cex=0.7, pos = 1)
  
  plot(d.pv, cex = 0.8, print.pv = F, main = "Dendrogram")
  
  dev.off()
  
  pdf(file = paste(outputFilesDir, "aux2.pdf", sep = "/"), height = PDFheight, width = PDFwidth)
  
  heatmap.2(log2(d2), Rowv = as.dendrogram(clusters$genes), Colv = as.dendrogram(clusters$samples), dendrogram = "both", key = T, keysize = 1.4, key.par=list(mar=c(3,1,3,1)), col = greenred(200), scale = "none", trace = "none", cexRow = 0.75, cexCol = 0.9, srtCol = 90, density.info = 'histogram', main = "HeatMap", margins = c(10, 7))
  
  dev.off()
  
  #system(paste("pdfunite", paste(outputFilesDir, "aux1.pdf", sep = "/"), paste(outputFilesDir, "aux2.pdf", sep = "/"), paste(outputFilesDir, "HeatMap.pdf", sep = "/"), sep = " "), wait = T)
}

