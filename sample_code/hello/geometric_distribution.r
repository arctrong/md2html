x <- c(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28)
y <- c(2441, 1914, 1426, 1034, 803, 615, 450, 322, 247, 202, 142, 89, 84, 65, 42, 29, 19, 20, 14, 6, 8, 5, 5, 5, 6, 3, 2, 0, 2)
xLabel = "Random number"
yLabel = "Number of hits"
png(file = "geometric_distribution.png", width = 800, height = 600)
plot(x, y, type = "o", xlab=xLabel, ylab=yLabel, main = "Geometric distribution")
dev.off()
