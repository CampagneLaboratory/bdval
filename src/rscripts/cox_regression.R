# Create the simplest test data set

#time<-c(0.2,2.4,0.1,0.5)
#censor<-c(1.0,1.0,1.0,1.0)
#cov1<-c(43.55,47.59,54.52,54.52)
#cov2<-c(1.0,0.2,1.3,2.3)

library(survival)
dataset <- list(time, censor, cov1, cov2 )
result0 <- coxph( Surv(time, censor) ~ cov1 + cov2, dataset)  #stratified model
result1 <- data.frame(summary(result0)$coef) #get the data
result2<-data.frame(   t(       summary(result0)$rsq         )             )
coef<-result1$exp.coef.
p_value <- result1$p
R2<- result2$rsq