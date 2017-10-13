# clusterGenesColors.R
# author: Hannah Blau
# last modified on: 12 Oct 2017

# contains variables and functions to color cluster ellipses and data points in visualizations of phenocluster.R

early.color <- "green"
late.color <-  "blue"
eight.colors <- c("red", "navy", "orange", "cyan", "purple", "olivedrab", "magenta", "black")
five.colors <- c("magenta", "cyan",  "red", "purple", "orange")

early.genes <- c("DPM2", "PIGY", "PIGA", "PIGC", "PIGH", "PIGP", "PIGQ", "PIGL", "PIGM", "PIGX", "PIGV", "PIGN",
                 "PIGB", "PIGO", "PIGF", "PIGG", "PIGW", "DPM3", "DPM1", "MPDU1")
late.genes <- c("PIGT", "PIGK", "PIGS", "PIGU", "GPAA1", "PGAP1", "PGAP3", "PGAP2", "PGAP5")

# param: list of the labels for leaves of the dendrogram
# return: list of colors corresponding to early/late category for each label of input list
genes.color <- function(vectorOfLabels) {
  return(unlist(lapply(vectorOfLabels, label.color)))
}

# param: label of leaf in clustering tree
# return: color for this label (early or late according to the gene mentioned in the label)
# relies on format of label PNUM.GENE, will fail if label format is different
label.color <- function(label) {
  geneAsList <- strsplit(label, ".", fixed = TRUE)
  gene <- unlist(geneAsList)[2]
  return(ifelse(gene %in% late.genes, late.color, early.color))
}
