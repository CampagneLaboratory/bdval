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

#base <- 2.0
#values <- c(1.0, 2.0, 3.0, 4.0, 5.0)

# Returns sum(values) + base
sumfx <- function(base, values) {
    result <- base
	for (i in 1:length(values)){
		result <- result + values[i]
	}
    result
}

# Returns product(values) + base
prodfx <- function(base, values) {
    result <- values[1]
	for (i in 2:length(values)){
		result <- result * values[i]
	}
    result + base
}

# Perform the calculations based on the inputs. Store the values
# in the outputs
sum <- sumfx(base, values)
prod <- prodfx(base, values)
comb <- c(sum,prod)
