# these values come from rank_features.R. Each is a vector of the values that come
# for all of the features for a specific model
# rank_tr <- c(4.0, ...)
# rank_va <- c(1.5, ...)

ratio_rank <- function(rank_tr, rank_va){
    ratio  <- sum(rank_tr)/sum(rank_va)
    c(ratio)
}

#output: 1 value for each model
ratio_rank_val <- ratio_rank(rank_tr, rank_va)
