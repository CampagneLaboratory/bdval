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

#
# This came from KS_test.R
#
# run the KS test
# x <- c(0.1, 0.2, 0.3, 0.4, 0.5)
# y <- c(0.6, 0.7, 0.8, 0.9, 1.0)
q <- ks.test(x,y)
p_value <- q$p.value
test_statistic <- q$statistic[[1]]

#
# This came from rank_features.R
#
# Here x and y are are the same values as in ks.test
#rank_features <- function(training,validation) {
#    n_tr <- length(training)
#    n_va <- length(validation)
#    N <- n_tr + n_va
#    comb <- c(training,validation)
#    rank_comb <- rank(comb)/N
#    rank_tr <- mean(rank_comb[1:n_tr])
#    rank_va <- mean(rank_comb[(n_tr+1):N])
#    if (rank_tr < rank_va) {
#        training <- training*-1
#        validation <- validation*-1
#        comb <- c(training,validation)
#        rank_comb <- rank(comb)/N
#        rank_tr <- mean(rank_comb[1:n_tr])
#        rank_va <- mean(rank_comb[(n_tr+1):N])
#    }
#    c(rank_tr, rank_va)
#}
# output a list with 2 values
#rank_features_val <- rank_features(x, y)

sum_rank <- function(training,validation) {
	n_tr <- length(training)
	n_va <- length(validation)
	N <- n_tr + n_va
	comb <- c(training,validation)
	rank_comb <- rank(comb)
	tr <- sum(rank_comb[1:n_tr])
	va <- sum(rank_comb[(n_tr+1):N])
	if (n_tr <= n_va){
		va <- n_tr*(N+1)-tr
	}
	else {
		tr <- n_va*(N+1)-va
	}
	T1 <- max(tr,va)
	T2 <- min(tr,va)
	c(T1,T2)
}
#output a list with 2 values
sum_rank_features <- sum_rank(x,y)
