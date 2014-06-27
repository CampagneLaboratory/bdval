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

library(survival)
require(survival)
#time<-c(0.2,2.4,0.1,0.5)
#censor<-c(1.0,1.0,1.0,1.0)
#group<-c(2.0,2.0,1.0,1.0)


dataset <- list(time, censor, group )
r<-survdiff( Surv(time, censor) ~ group, dataset)
p_value <- 1 - pchisq(r$chisq, length(r$n) - 1)
