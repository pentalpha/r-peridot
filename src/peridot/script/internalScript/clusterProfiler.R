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

#Get directory
paramFile = paste(getwd(), "config.txt", sep = "/")
params = read.table(paramFile, header = TRUE, row.names = 1, sep = "|")
params

#Get Temp Diretory
FileTemp = outputFilesDir

genelistinput = paste(inputFilesDir, "VennDiagram.PostAnalysisScript/Intersect.tsv", sep = "/");

genelist = inter = read.table(file = genelistinput, header = F, sep = "\t")

library(clusterProfiler)
library(ggplot2)

eg = bitr(genelist[,1], fromType= as.character(params$geneIdType), toType = c("SYMBOL","ENTREZID", "UNIPROT"), OrgDb = "org.Mm.eg.db", drop = T)

head(eg$ENTREZID)

universe = org.Mm.egENSEMBL

mappedGenes = mappedkeys(universe)

ego = enrichGO(gene = eg$ENTREZID, universe = mappedGenes, OrgDb = "org.Mm.eg.db", ont = "MF", readable = T, pAdjustMethod = "BH", pvalueCutoff  = 1, qvalueCutoff = 1)
ego@result = subset(ego@result, (pvalue < params$pValue & qvalue < params$fdr))
ego@result$Description = strtrim(ego@result$Description, 20)
ego@result$Description = lapply(ego@result$Description, function(x) paste(x, "...", sep = ""))
head(ego)

ego2 = enrichGO(gene = eg$ENTREZID, universe = mappedGenes, OrgDb = "org.Mm.eg.db", ont = "CC", readable = T, pAdjustMethod = "BH", pvalueCutoff  = 1, qvalueCutoff = 1)
ego2@result = subset(ego2@result, (pvalue < params$pValue & qvalue < params$fdr))
ego2@result$Description = strtrim(ego2@result$Description, 20)
ego2@result$Description = lapply(ego2@result$Description, function(x) paste(x, "...", sep = ""))
head(ego2)

ego3 = enrichGO(gene = eg$ENTREZID, universe = mappedGenes, OrgDb = "org.Mm.eg.db", ont = "BP", readable = T, pAdjustMethod = "BH", pvalueCutoff  = 1, qvalueCutoff = 1)
ego3@result = subset(ego3@result, (pvalue < params$pValue & qvalue < params$fdr))
ego3@result$Description = strtrim(ego3@result$Description, 20)
ego3@result$Description = lapply(ego3@result$Description, function(x) paste(x, "...", sep = ""))
head(ego3)

if(length(ego@result$ID) > 0){
  #jpeg(filename = paste(outputFilesDir, "enrichGOMF.jpg", sep = "/"))
  
  dotplot(ego, title = "Ontology = MF", showCategory = 100, colorBy = "pvalue")
  
  ggsave(filename = paste(outputFilesDir, "enrichGOMF.jpg", sep = "/"))
  
  #dev.off()
}

if(length(ego2@result$ID) > 0){
  dotplot(ego2, title = "Ontology = CC", showCategory = 100, colorBy = "pvalue")
  
  ggsave(filename = paste(outputFilesDir, "enrichGOCC.jpg", sep = "/"))
}

if(length(ego3@result$ID) > 0){
  dotplot(ego3, title = "Ontology = BP", showCategory = length(ego3@result$ID), colorBy = "pvalue")
  
  ggsave(filename = paste(outputFilesDir, "enrichGOBP.jpg", sep = "/"))
}


pdf(file = paste(outputFilesDir, "enrich.pdf", sep = "/"))
if(length(ego@result$ID) > 0){
  dotplot(ego, title = "Ontology = MF", showCategory = length(ego@result$ID), colorBy = "pvalue")
}

if(length(ego2@result$ID) > 0){
  dotplot(ego2, title = "Ontology = CC", showCategory = length(ego2@result$ID), colorBy = "pvalue")
}

if(length(ego3@result$ID) > 0){ 
  dotplot(ego3, title = "Ontology = BP", showCategory = length(ego3@result$ID), colorBy = "pvalue")
}

dev.off()

##### KEGG #####
eg2np <- bitr_kegg(eg$ENTREZID, fromType='kegg', toType='ncbi-geneid', organism='mmu')

kk <- enrichKEGG(eg2np$kegg, organism = 'mmu', pvalueCutoff = 1, qvalueCutoff = 1)

#kk@result = subset(kk@result, (pvalue < params$pValue & qvalue < params$fdr))

kk@result = subset(kk@result, (pvalue < 0.01 & qvalue < 0.05))

dotplot(kk)
kk@result$ID[1]
testeKEGG <- browseKEGG(kk, kk@result$ID[1])
kk@result


teste = pathview(gene.data = kk@result$geneID, pathway.id = kk@result$ID, species = "mmu", kegg.dir = outputFilesDir, kegg.native = T)

gene = list(gene=max(abs(testeGene)), cpd=1)
View(genelist)
geneIDpvalue
View(geneList)

str(teste$mmu00190)
