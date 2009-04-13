library(survival)
require(survival)
#time<-c(0.2,2.4,0.1,0.5)
#censor<-c(1.0,1.0,1.0,1.0)
#group<-c(2.0,2.0,1.0,1.0)


dataset <- list(time, censor, group )
r<-survdiff( Surv(time, censor) ~ group, dataset)
p_value <- 1 - pchisq(r$chisq, length(r$n) - 1)
