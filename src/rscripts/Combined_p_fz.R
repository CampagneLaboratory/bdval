#
# Copyright (C) 2008-2009 Institute for Computational Biomedicine,
#                         Weill Medical College of Cornell University
#
#  This program is free software; you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation; either version 3 of the License, or
#  (at your option) any later version.
#
#  This program is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU General Public License for more details.
#
#  You should have received a copy of the GNU General Public License
#  along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
