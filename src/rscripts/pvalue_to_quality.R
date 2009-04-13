#
# This came from Combined_p_fz.R
# Performs fisher and stouffer calculations
#
# data_in <- c(0.7314737598889861,0.3279058343153105,0.44593582173613167,0.13591326378264545,0.6741945793137526,0.6966164516055036,0.8821685621029981,0.9285991553585969,0.03499331059671895,0.595591497976657,0.019822396723213953,0.2254288086895525,0.9571059800957111,0.27427078742309274,0.8880258593029959,0.6490101658869945,0.07492392148820437,0.10998176053724673,0.0489795691972823,0.9926184579529816)

fisher <- function(model_data){
	for (i in 1:length(model_data)){
		if (model_data[i]==0) model_data[i] <- 2.2E-16
		if (model_data[i]==1) model_data[i] <- 0.9999999999999999
	}
	log_fn <- NULL
	for (i in 1:length(model_data)){
		log_fn[i] <- log(model_data[i])
	}
	f <- -2*sum(log_fn)
	pf <- 1-pchisq(f,2*length(model_data))
	c(f,pf)
}

stouffer <- function(model_data){
    for (i in 1:length(model_data)){
		if (model_data[i]==0) model_data[i] <- 2.2E-16
		if (model_data[i]==1) model_data[i] <- 0.9999999999999999
	}
	zn <- NULL
	for (i in 1:length(model_data)){
		zn[i] <- qnorm(model_data[i])
	}
	z <- sum(zn)/sqrt(length(model_data))
	pz <- pnorm(z)
	c(z,pz)
}

stouffer_vals <- stouffer(data_in)
fisher_vals <- fisher(data_in)

#
# This came from ratio_rank.R
#
# these values come from rank_features.R. Each is a vector of the values that come
# for all of the features for a specific model
# x <- c(..., ...)
# y <- c(..., ...)
# average the ratios of x/y. x and y have equal length.

ratio_rank <- function(x, y){
    ratio  <- sum(x/y)/length(x)
    c(ratio)
}

#output: 1 value for each model
ratio_rank_val <- ratio_rank(x, y)
