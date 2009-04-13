# x <- c(0.1, 0.2, 0.3, 0.4, 0.5)
# y <- c(0.6, 0.7, 0.8, 0.9, 1.0)
q <- ks.test(x,y)
p_value <- q$p.value
test_statistic <- q$statistic[[1]]
