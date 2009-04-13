# Here x and y are are the same values as in ks.test
# x <- c(0.1, 0.2, 0.3, 0.4, 0.5)
# y <- c(0.6, 0.7, 0.8, 0.9, 1.0)
rank_features <- function(training,validation) {
    n_tr <- length(training)
    n_va <- length(validation)
    N <- n_tr + n_va
    comb <- c(training,validation)
    rank_comb <- rank(comb)/N
    rank_tr <- mean(rank_comb[1:n_tr])
    rank_va <- mean(rank_comb[(n_tr+1):N])
    if (rank_tr < rank_va) {
        training <- training*-1
        validation <- validation*-1
        comb <- c(training,validation)
        rank_comb <- rank(comb)/N
        rank_tr <- mean(rank_comb[1:n_tr])
        rank_va <- mean(rank_comb[(n_tr+1):N])
    }
    c(rank_tr, rank_va)
}
#output a list with 4 values
rank_features_val <- rank_features(x, y)
