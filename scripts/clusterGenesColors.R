# clusterGenesColors.R
# author: Hannah Blau
# last modified on: 12 Oct 2017

# contains variables and functions to color cluster ellipses and data points in visualizations of phenocluster.R

early.color <- hyperphos.color <- "green"
late.color <- gpid.color <- "blue"
eight.colors <- c("red", "navy", "orange", "cyan", "purple", "olivedrab", "magenta", "black")
five.colors <- c("magenta", "cyan",  "red", "purple", "orange")

early.genes <- c("DPM2", "PIGY", "PIGA", "PIGC", "PIGH", "PIGP", "PIGQ", "PIGL", "PIGM", "PIGX", "PIGV", "PIGN",
                 "PIGB", "PIGO", "PIGF", "PIGG", "PIGW", "DPM3", "DPM1", "MPDU1")
late.genes <- c("PIGT", "PIGK", "PIGS", "PIGU", "GPAA1", "PGAP1", "PGAP3", "PGAP2", "PGAP5")
hyperphos.genes <- c("PIGV", "PIGY", "PIGO", "PGAP2", "PIGW", "PGAP3")
gpid.gene <- "PIGM"

earlyLate.subtitle = "green for early genes, blue for late genes"
hyperGpid.subtitle = "green for Hyperphosphatasia, blue for Glycosylphosphatidylinositol Deficiency"

# params: list of the labels for leaves of the dendrogram, function to color each label
# return: list of colors corresponding to category of each label in input list
genes.color <- function(vectorOfLabels, color.fn) {
  return(unlist(lapply(vectorOfLabels, color.fn)))
}

# param: label of patient
# return: gene part of this label
# for example, "P108.PIGV" maps to "PIGV"
# relies on format of label PNUM.GENE, will fail if label format is different
get.gene <- function(label) {
  geneAsList <- strsplit(label, ".", fixed = TRUE)
  return(unlist(geneAsList)[2])
}

# param: label of patient
# return: early/late color for this label (according to the gene mentioned in the label)
earlyLate <- function(label) {
  gene <- get.gene(label)
  return(ifelse(gene %in% late.genes, late.color, early.color))
}

# param: label of patient
# return: color for this label (according to the gene mentioned in the label); default black, change to
# disease subgroup color if gene matches that subgroup
hyperGpid <- function(label) {
  gene <- get.gene(label)
  if (gene %in% hyperphos.genes)
    return(hyperphos.color)
  else if (gene %in% gpid.gene)
    return(gpid.color)
  else return("black")
}
