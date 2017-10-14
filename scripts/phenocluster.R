# phenocluster.R
# author: Hannah Blau
# last modified on: 12 Oct 2017

# Function rcp *r*eads an n x n dissimilarity matrix, *c*lusters the data, and *p*lots the resulting clustering.
# requires the cluster library (https://CRAN.R-project.org/package=cluster)
# and the dendextend library (https://CRAN.R-project.org/package=dendextend)

# load the packages
suppressPackageStartupMessages(library(cluster))
suppressPackageStartupMessages(library(dendextend))

# You should create R project with all files in project directory, or set the RStudio working directory
# so that call to source will not result in "No such file" error
source("clusterGenesColors.R")

# inputFile: file containing dissimilarity matrix
# outputDirectory: directory for output files (without separator at the end)
rcp <- function(inputFile, outputDirectory) {
  dissimfrm <- read.table(inputFile, header = TRUE)
  dissimdist <- as.dist(dissimfrm)
  
  # Create output directory if necessary, and two sub-directories for plots
  earlyLateDir <- paste(outputDirectory, "earlyLate", sep = "/")
  diseaseDir <- (paste(outputDirectory, "diseases", sep = "/"))
  if (!dir.exists(outputDirectory))
    dir.create(outputDirectory)
  if (!dir.exists(earlyLateDir))
    dir.create(earlyLateDir)
  if (!dir.exists(diseaseDir))
    dir.create(diseaseDir)
  
  cp(dissimdist, earlyLateDir, earlyLate, earlyLate.subtitle)
  cp(dissimdist, diseaseDir, hyperGpid, hyperGpid.subtitle)
}

# Function cp clusters from the distance matrix and plots the results.
# params: dissimilarity distance matrix, directory for plots,
#         function to color the patient labels, subtitle for plot that explains the coloring
cp <- function(dissdist, plotdir, coloring.fn, subtitle) {
  # agglomerative hierarchical clustering, diss argument tells agnes we are passing a dissimilarity matrix
  # default method is "average"; other choices are "complete", "flexible", "gaverage", "single", "ward", "weighted"  
  # "flexible" and "gaverage" require par.method argument
  ahclus <- agnes(dissdist, diss = TRUE)
  dend <- as.dendrogram(ahclus)
  dend <- color_branches(dend, k = 8, col = eight.colors)
  labels_colors(dend) <- genes.color(labels(dend), coloring.fn)
  
  pdf(paste(plotdir, "agnes8.pdf", sep = "/"), width = 18, height = 8)
  plot(dend, main = "Hierarchical clustering, average method, 8 clusters",
       sub = subtitle)
  dev.off()
  
  # pam (partitioning around medoids clustering, related to k-means but you can start with a dissimilarity matrix
  # instead of the dataset from which the dissimilarity values are derived
  for (i in 2:5) {
    pamclus <- pam(dissdist, i, diss = TRUE)
    pdf(paste(plotdir, paste0("pam", i, ".pdf"), sep = "/"), width = 10, height = 10)
    # RStudioGD() interactive device
    
    # labels = 2, cex.txt = 0.4 to check all the point labels
    clusplot(dissdist, pamclus$clustering, diss = TRUE, color = TRUE, lines = 0, labels = 4, col.clus = five.colors,
             main = paste0("Partitioning Around Medoids, k = ", i), 
             sub = subtitle, col.p = genes.color(names(pamclus$clustering), coloring.fn))
    dev.off()
  }
  
  # classical multi-dimensional scaling. k is the number of dim. If add == TRUE, cmdscale computes a constant
  # that is added to the non-diagonal dissimilarities to make them Euclidean (according to cmdscale help)
  mds <- cmdscale(dissdist, add = TRUE, k = 2)
  
  # if add == TRUE, cmdscale computes a constant
  # mds <- c(cmdscale(dissdist, add = TRUE, k = 2),
  #         cmdscale(dissdist, eig = TRUE, k = 2))
  # addNoAdd = c("Add", "NoAdd")
  
  # for (i in 1:2) {
  #   pdf(paste(outputDirectory, paste0("cms", addNoAdd[i], ".pdf"), sep = "/"), width = 10, height = 10)
  #   x <- mds[i]$points[,1]
  #   y <- mds[i]$points[,2]
  #   plot(x, y, xlab = "Coordinate 1", ylab = "Coordinate 2", main = paste("Classical MDS,", addNoAdd[i]),
  #        asp = 1, type = "n")
  #   text(x, y, labels = labels(dissdist), cex=.6, col = genes.color(labels(dissdist), coloring.fn))    
  #   #  points(x, y,  col = genes.color(labels(dissdist)), pch = 20)
  #   dev.off()
  # }
  
  pdf(paste(plotdir, "cms.pdf", sep = "/"), width = 10, height = 10)
  x <- mds$points[,1]
  y <- mds$points[,2]
  plot(x, y, xlab = "Coordinate 1", ylab = "Coordinate 2", main = "Classical MDS", 
       sub = subtitle, asp = 1, type = "n")
  text(x, y, labels = labels(dissdist), cex=.6, col = genes.color(labels(dissdist), coloring.fn))    
  #  points(x, y,  col = genes.color(labels(dissdist)), pch = 20)
  dev.off()
  
  return(pamclus$clustering)
}
