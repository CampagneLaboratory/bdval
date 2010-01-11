#
# Copyright (C) 2008-2010 Institute for Computational Biomedicine,
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
