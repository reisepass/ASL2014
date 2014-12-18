library(ggplot2)
source('/media/mort/Seagate Expansion Drive/Dropbox/College/ASL2/aslCSVtools.R')
library(zoo)
library(mgcv)

source('E:/Dropbox/College/ASL2/StatisticalAnalysis/aslCSVtools.R')
#Report story:

#Basic Setup



wdir<-"E:/Dropbox/College/ASL2/firstLoadTest"
rwDat<-combineFiles(workDir=wdir,fileRegExNamePattern="*.csv")
rwDat<-rwDat[complete.cases(rwDat),]
rwDat<-factorizeDF2014(rwDat)



ggplot(rwDat[sample.int(nrow(rwDat),size=10000,replace=FALSE),])+geom_smooth(aes(x=clEnterQ,y=clRoundTime,color=experiment_name))
#The line color indicates the experiment performed. In this case we are running a 5minute experiment on T2 Middle sized machine. With 1 middleware 1 Client Simulator. db_connection_limit: 5 message_length:100 num_queues: 5 mw_message_handlers_pool_size: 10  The client composition is mixed with equal parts : num_send_only: num_send_private: num_peekclients: num_pullclients: num_pullPrivate:num_findAuthor :num_relQueue. The graph displays a smoothed regression line with its 95% confidence interval for the Client Response time vs Time. Here we can cearly see that our data violates the IID assumption since it is autocorrelated. To minimize this model error we will exclude the first 120seconds of the experiment. We choose 120seconds because above 49clients has an unacceptable Response time and we will keep the number of clients below this in future experiments. 


ggplot(rwDat)+geom_smooth(aes(x=clEnterQ,y=mwRoundTime,color=experiment_description))
ggplot(rwDat)+geom_smooth(aes(x=clEnterQ,y=dbRoundTime,color=experiment_description))


ggplot(rwDat)+geom_smooth(aes(x=clEnterQ,y=mwThinkTime,color=experiment_description))
ggplot(rwDat)+geom_smooth(aes(x=clEnterQ,y=dbThinkTime,color=experiment_description))
ggplot(rwDat)+geom_smooth(aes(x=clEnterQ,y=clThinkTime,color=experiment_description))
ggplot(rwDat[sample.int(nrow(rwDat),size=10000,replace=FALSE),])+geom_smooth(aes(x=clEnterQ,y=clThinkTime,linetype=experiment_description))+geom_point(aes(x=clEnterQ,y=clThinkTime,color=request_type))
  #The client thinktime seems to be the biggest producer of the warmup effect. At exactly 120min in all experiments there is a sudden speedup. It does not appea to be related to request times. 




############################# Scale up experiment #########################################33

wdir<-"E:/ASL_Data/ScalueUpExperiment"
wdir<-"/media/mort/Seagate Expansion Drive/ASL_Data/ScalueUpExperiment"
rwDat<-combineFiles(workDir=wdir,fileRegExNamePattern="*.csv")
rwDat<-rwDat[complete.cases(rwDat),]
rwDat$num_client_machines[rwDat$experiment_name=="csvClients_ScaleUpExperiment3_6C_3M_150MH_25DB_200C.csv"]<-6
rwDat$num_middlewares[rwDat$experiment_name=="csvClients_ScaleUpExperiment3_6C_3M_150MH_25DB_200C.csv"]<-3
rwDat$request_type[rwDat$request_type=="readPull"]<-"readPeek"
rwDat<-factorizeDF2014(rwDat)



through<-quickTroughput2014(rwDat,splitOn="num_middlewares")

through2<-quickTroughput2014(rwDat[rwDat$num_middlewares==3,],splitOn="request_type")

#Scaling up the system has increaseed the throughput as expected on a logerithmic scale with the number of machines
unstableboxPlot<-ggplot(through$raw)+geom_boxplot(notch=TRUE,aes(x=cLabel,y=throughput))+ylab("Throughput per Second")+xlab("Client And Middleware Scale Multiplier")+scale_colour_hue(name="Middleware and Client Multiplier")+ggtitle(bquote(atop(.("Mixed Client Scale Up Experiment"), atop(italic(.("Client Simulators: 2-6, Middlewares: 1-3, Message Handler Threads: 150 each, DB Connections: 25 each, Number of Clients: Mixed Types 200 each")), "")))) 
unstableboxPlot
ggsave("Unstable Scale Up Exp Throughput boxplot.png",unstableboxPlot)

#But it turns out that over the course of the experiment the throughput drastically decreases in each scale condition.
unstableTHrough<-ggplot(through$raw,aes(x=time/60+2,y=throughput,color=cLabel))+geom_point()+stat_smooth(method = "lm")+ylab("Throughput last Second")+xlab("Time (min)")+scale_colour_hue(name="Middleware and Client Multiplier")+ggtitle(bquote(atop(.("Mixed Client Scale Up Experiment"), atop(italic(.("Client Simulators: 2-6, Middlewares: 1-3, Message Handler Threads: 150 each, DB Connections: 25 each, Number of Clients: Mixed Types 200 each")), "")))) 
unstableTHrough
ggsave("Unstable Scale Up Exp Throughput Over Time.png",unstableTHrough)






through2Normal<-through2$raw
through2Normal$throughput[through2Normal$cLabel=="sendMpub"]<-through2Normal$throughput[through2Normal$cLabel=="sendMpub"]/50
through2Normal$throughput[through2Normal$cLabel=="sendMpriv"]<-through2Normal$throughput[through2Normal$cLabel=="sendMpriv"]/50
through2Normal$throughput[through2Normal$cLabel=="readPeek"]<-through2Normal$throughput[through2Normal$cLabel=="readPeek"]/30
through2Normal$throughput[through2Normal$cLabel=="readPop"]<-through2Normal$throughput[through2Normal$cLabel=="readPop"]/20
through2Normal$throughput[through2Normal$cLabel=="findAuthor"]<-through2Normal$throughput[through2Normal$cLabel=="findAuthor"]/25
through2Normal$throughput[through2Normal$cLabel=="queueRequest"]<-through2Normal$throughput[through2Normal$cLabel=="queueRequest"]/25

unstableTHroughType<-ggplot(through2Normal,aes(x=time/60+2,y=throughput,color=cLabel))+geom_point()+stat_smooth(method = "lm")+ylab("Throughput last Second per Client")+xlab("Time (min)")+scale_colour_hue(name="Type Of Request")+ggtitle(bquote(atop(.("Mixed Client Scale Up Experiment"), atop(italic(.("Client Simulators: 2-6, Middlewares: 1-3, Message Handler Threads: 150 each, DB Connections: 25 each, Number of Clients: Mixed Types 200 each")), "")))) 
unstableTHroughType

ggsave("Unstable Scale Up Exp Throughput Over Time By RequestType.png",unstableTHroughType)
#As expected the different Request types have different throughputs. Writing one Private message, one Public message or Reading one message without removal have the same throughput. Second down in rank is the request to find all relevant queues for a given client. 3rd in rank is finding a request sent from a particular client. The slowest request is reading a message and deleting it, this may be because no other request requires an reorganization of all entries above the one found (Above interms of the BTree Postrgresql implements internally). We Can also see that the decrease in throughput is resent for all Request types. 


rwSubDat<-rwDat[rwDat$clEnterQ>120000&rwDat$clEnterQ<(max(rwDat$clEnterQ)-10000),]
rwSubDat<-rwSubDat[sample.int(nrow(rwSubDat),10000),]

#And seeing that the "DB Thinktime" is increaseing over time would indicate that the DB is increasing in size and this is why its lookup is increasing in time. We will now conduct an experiment with more clients removing messages than clients sending messages. This should reverse the problem
ggplot(rwSubDat,aes(x=clEnterQ,y=dbThinkTime, color=num_middlewares))+geom_point()+geom_smooth()
ggplot(rwSubDat,aes(x=request_type,y=dbRoundTime, color=num_middlewares))+geom_boxplot()

#We can also see that the amount thinktime of the Middleware does not change throughout indicating that the problem arises from the DB. 
ggplot(rwSubDat,aes(x=clEnterQ,y=mwThinkTime, color=num_middlewares))+geom_point()

####WTFFFF why does clConnectionInit time go up
ggplot(rwSubDat,aes(x=clEnterQ,y=clConnInitTime, color=num_middlewares))+geom_point(aes(y=clConnInitTime-mwRoundTime))
ggplot(rwSubDat)+geom_point(aes(x=clEnterQ,y=clConnInitTime, color=num_middlewares))
ggplot(rwSubDat)+geom_point(aes(x=clEnterQ,y=clCloseTime, color=num_middlewares))








############## Scale Up STABLE ############################

wdir<-"/media/mort/Seagate Expansion Drive/ASL_Data/StableScalUp"
rwDat<-combineFiles(workDir=wdir,fileRegExNamePattern="*.csv")
rwDat<-rwDat[complete.cases(rwDat),]
rwDat<-factorizeDF2014(rwDat)

rwSubDat<-rwDat[rwDat$clEnterQ>120000&rwDat$clEnterQ<(max(rwDat$clEnterQ)-30000),]
rwSubDat<-rwSubDat[sample.int(nrow(rwSubDat),10000),]

tmpthrough<-quickTroughput2014(rwDat,splitOn = "num_middlewares")
throughputOverTime<-ggplot(tmpthrough$raw,aes(x=(time/60)+2,y=throughput,color=cLabel))+geom_point()+ stat_smooth(method = "lm")+ggtitle(bquote(atop(.("Stable Scale Up Experiment"), atop(italic(.("Client Simulators: 2-6, Middlewares: 1-3, Message Handler Threads: 150 each, Database Connections: 25 each, Number of Clients: 200 each")), ""))))+scale_colour_hue(name="Middleware and Client Multiplier")+ylab("Throughput per 1sec")+xlab("Time (min)")
ggsave("Stable ScaleUp Exp Throughput Over Time.png",throughputOverTime)
#The Stable Scale experiment confirms our hypothesis that the last Scale Experiment was loosing throughput overtime because they database was filling up. 
tmpThr<-quickTroughput2014(rwDat,splitOn = "num_middlewares",trimFront=190000)$raw
oneThru<-lm(throughput~time,data=tmpThr[tmpThr$cLabel==1,])
summary(oneThru)
twoThru<-lm(throughput~time,data=tmpThr[tmpThr$cLabel==2,])
summary(twoThru)
threeThru<-lm(throughput~time,data=tmpThr[tmpThr$cLabel==3,])
summary(threeThru)
#Statistically at all three scales we see no significant positive relationship between Throughput and time. For the model Throughput as a function of time we calculated: At Scale 1: Coef=4.531e-02 p-value=0.544, At Scale2: Coef=-0.1596 p-value=0.267, At Scale3: Coef=0.2058 p-value=0.43


responseOverTime<-ggplot(rwSubDat,aes(y=clRoundTime,x=(clEnterQ/60000)+2))+ coord_cartesian(ylim=c(0,1000))+geom_point()+ stat_smooth(method = "lm")+ylab("Response time (ms)")+xlab("Time (min)")+scale_colour_hue(name="Middleware and Client Multiplier")+ggtitle(bquote(atop(.("Stable Scale Up Experiment"), atop(italic(.("Client Simulators: 2-6, Middlewares: 1-3, Message Handler Threads: 150 each, Database Connections: 25 each, Number of Clients: 200 each")), "")))) 
ggsave("Stable ScaleUp Exp Response Time over Time.png",responseOverTime)
# Additionally we also see that the response time does not increase over time. The blue line is a linear regression. 




throughPutBoxPlot<-ggplot(tmpthrough$raw,aes(x=cLabel,y=throughput))+geom_boxplot(outlier.size=NaN,notch = TRUE)+ggtitle(bquote(atop(.("Stable Scale Up Experiment"), atop(italic(.("Client Simulators: 2-6, Middlewares: 1-3, Message Handler Threads: 150 each, Database Connections: 25 each, Number of Clients: 200 each")), "")))) +xlab("Middleware and Client Multiplier")+ylab("Throughput per 1sec")+stat_summary(fun.y=median, geom="line", aes(group=1))  + stat_summary(fun.y=median, geom="point")
ggsave("Stable ScaleUp Exp Throughput Boxplot vs Scale Factor.png",throughPutBoxPlot)
#After aleviating the DB fill level as a factor we are again able to evaluate the Scale Up ability of our system. Again, we started with 2 Client machines simulating 200 Clients each and one middleware machine. This condition was then scalled up to 4 CLient machines with 2 Middlewares totalling 800 Client threads and Scalled to 6 Client Machines with 3 Middlewares tottaling 1200 Client threads on one Database. The boxplot shows both 50% quantiles and 95% quantiles and for no condition do these overlap hence we can conclude that scaling up increases throughput significantly.

fitBox<-lm(throughput~as.numeric(cLabel),data=tmpthrough$raw)
summary(fitBox)
# The Scaling factor is highly significant in a linear model with a Coef= 1555.196, Intercept= 108.543 and p-value<2e-16.
#############################################################



############### Increasing Numbler of Clients Exp2 #########
wdir<-"/media/mort/Seagate Expansion Drive/ASL_Data/Increasing Client Exp2"
rwDat<-combineFiles(workDir=wdir,fileRegExNamePattern="*.csv")

rwDat$num_clients_perMachien[rwDat$experiment_name=="csvClients_cliInc5_1C_1M_100MH_40DB_30C_stable_L100.csv"]<-30
rwDat$totalCliants<-rwDat$num_clients_perMachien*rwDat$num_client_machines
rwDat<-factorizeDF2014(rwDat)

rwSubDat<-rwDat[rwDat$clEnterQ>120000&rwDat$clEnterQ<(max(rwDat$clEnterQ)-30000),]
rwSubDat<-rwSubDat[sample.int(nrow(rwSubDat),50000),]

clientsRespond<-ggplot(rwSubDat,aes(y=clRoundTime,x=as.factor(totalCliants)))+geom_boxplot(outlier.size = NaN)+ylab("Response time (ms)")+xlab("Number of Simulated Clients")+ggtitle(bquote(atop(.("Client Number to Response time"), atop(italic(.("Client Simulators: 1-4, Middlewares: 1, Message Handler Threads: 100 each, DB Connections: 40 each, Number of Clients: Stable Types 30-400")), "")))) +ylim(0,400)+stat_summary(fun.y=median, geom="line", aes(group=1))+stat_summary(fun.y=median, geom="point")
clientsRespond
ggsave("Client Number Response time boxplot.png",clientsRespond)
#The Response time is a linear function of the number of clients in the closed system
summary(lm(clRoundTime~totalCliants,data=rwSubDat))
#In a linear model of Responsetime the total number of clients has a Coef = 1/8 Intercept 2.14 and p-value<2e^-16

pairwise.t.test(rwSubDat$clRoundTime,as.factor(rwSubDat$totalCliants))
# Additionally running a T-Test between each group gives a significant difference for every group pair with a p-value<2e-16


ggplot(rwSubDat,aes(y=dbThinkTime,x=as.factor(totalCliants)))+geom_boxplot(outlier.size = NaN)+ylim(0,50)


tmpthrough<-quickTroughput2014(rwDat,splitOn = "totalCliants",trimBack = 10000, trimFront = 80000 ,removeOutlier = FALSE)


ggplot(tmpthrough$raw,aes(x=time,y=throughput,color=cLabel))+geom_point()+geom_smooth()
numclieTHrough<-ggplot(tmpthrough$raw,aes(x=cLabel,y=throughput))+geom_boxplot(outlier.size = NaN)+ylab("Throughput per Second")+xlab("Number of Simulated Clients")+ggtitle(bquote(atop(.("Client Number to Throughput"), atop(italic(.("Client Simulators: 1-8, Middlewares: 1, Message Handler Threads: 100 each, DB Connections: 25 each, Number of Clients: Stable Types 100-800")), "")))) +ylim(0,2250)+stat_summary(fun.y=median, geom="line", aes(group=1))+stat_summary(fun.y=median, geom="point")
##TODO Todo explain the drop off in throughput for 400 clients,  see what happens with 500 and 600 If you have time
numclieTHrough
ggsave("Client Number vs Throughput one mw.png",numclieTHrough)

#TODO REMOVE THIS 
pairwise.t.test(tmpthrough$raw$throughput,as.factor(tmpthrough$raw$cLabel))
# Additionally running a T-Test between each group gives a significant difference for every group pair with a p-value<2e-16

#########################Increasing Numbler  Scale MW Try 3 EX3 ########################## TODO not so good 

wdir<-"/media/mort/Seagate Expansion Drive/ASL_Data/Middleware Scaleup"
rwDat<-combineFiles(workDir=wdir,fileRegExNamePattern="*.csv")
rwDat$totalClients<-rwDat$num_client_machines*rwDat$num_clients_perMachien
rwDat<-factorizeDF2014(rwDat)

tmpthrough<-quickTroughput2014(rwDat,splitOn = "totalClients",trimBack = 10000, trimFront = 60000 )
mhTHrough<-ggplot(tmpthrough$raw,aes(x=cLabel,y=throughput))+geom_boxplot(outlier.size = NaN)+ylab("Throughput per Second")+xlab("Total Number of Clients")+ggtitle(bquote(atop(.("Varying Message Handler Thread Throughput"), atop(italic(.("Client Simulators: 1-10, Middlewares: 1, Message Handler Threads: 100 , DB Connections: 15, Number of Clients: 100 Stable Types ")), ""))))+stat_summary(fun.y=median, geom="line", aes(group=1))+stat_summary(fun.y=median, geom="point") 
mhTHrough


############################################################

############### Var Message Length Exp3  #######################  

wdir<-"/media/mort/Seagate Expansion Drive/ASL_Data/VarMesgLengthExp3"
rwDat<-combineFiles(workDir=wdir,fileRegExNamePattern="*.csv")

rwDat<-factorizeDF2014(rwDat)
rwDat$messageLength[rwDat$experiment_name=="csvClients_mesgLengthExp3_medM3__4MWXL_100000L.csv"]<-100000
rwDat$messageLength[rwDat$experiment_name=="csvClients_mesgLengthExp3_medM3__4MWXL_500000L.csv"]<-500000
rwDat$messageLength[rwDat$experiment_name=="csvClients_mesgLengthExp3_medM3__4MWXL_10000L.csv"]<-10000
rwDat$messageLength[rwDat$experiment_name=="csvClients_mesgLengthExp3_medM3__4MWXL_50000L.csv"]<-50000
rwDat$messageLength[rwDat$experiment_name=="csvClients_mesgLengthExp3_medM3__4MWXL_5000L.csv"]<-5000
rwDat$messageLength[rwDat$experiment_name=="csvClients_mesgLengthExp3_medM3__4MWXL_1000L.csv"]<-1000
rwDat$messageLength[rwDat$experiment_name=="csvClients_mesgLengthExp3_medM3__4MWXL_100L.csv"]<-100

msgLengthResp<-ggplot(rwDat[rwDat$clEnterQ>60000,],aes(x=as.factor(messageLength),y=dbRoundTime))+geom_boxplot(outlier.size = NaN)+ylim(0,10000)+ylab("Response time (ms)")+xlab("Message Length")+ggtitle(bquote(atop(.("Message Length vs Response time"), atop(italic(.("Client Simulators: 10, Middlewares: 5, Message Handler Threads: 100 each, DB Connections: 15 each, Number of Clients: Stable Types 100")), ""))))+stat_summary(fun.y=median, geom="line", aes(group=1))+stat_summary(fun.y=median, geom="point")
msgLengthResp
ggsave("Message Length Varying vs Response time.png",msgLengthResp)

tmpthrough<-quickTroughput2014(rwDat,splitOn = "messageLength",trimFront =60000,trimBack = 5000 )

msgThrough<-ggplot(tmpthrough$raw,aes(x=cLabel,y=throughput))+geom_boxplot(outlier.size = NaN)+stat_summary(fun.y=mean, geom="line", aes(group=1))+stat_summary(fun.y=mean, geom="point")+ylab("Throughput per Second")+xlab("Message Length")+ggtitle(bquote(atop(.("Message Length  vs Throughput"), atop(italic(.("Client Simulators: 8, Middlewares: 4, Message Handler Threads: 150 , DB Connections: 20 each, Number of Clients: 100 Stable Types ")), ""))))+ylim(0,2000)
ggsave("Message Length Varying vs Throughput.png",msgThrough)



##############################################################################


############### VaryDB Vary DB Conn  TODO #####################################

wdir<-"/media/mort/Seagate Expansion Drive/ASL_Data/VaryDBEX2"
rwDat<-combineFiles(workDir=wdir,fileRegExNamePattern="*.csv")
rwDat<-factorizeDF2014(rwDat)



dbResp<-ggplot(rwDat,aes(x=as.factor(num_db_handlers),y=clRoundTime))+geom_boxplot(outlier.size = NaN)+ylab("Response time (ms)")+xlab("DB connections")+ggtitle(bquote(atop(.("Varying DB Connectionpool vs Response time"), atop(italic(.("Client Simulators: 4, Middlewares: 1, Message Handler Threads: 150 ,  ")), "DB Connections: 1 - 15 each, Number of Clients: 140 Stable Types"))))+ylim(0,800)+stat_summary(fun.y=median, geom="line", aes(group=1))+stat_summary(fun.y=median, geom="point")
dbResp
ggsave("Varying DB Handlers ResponseTime.png", dbResp)

tmpthrough<-quickTroughput2014(rwDat,splitOn = "num_db_handlers",trimBack = 60000, trimFront =60000 )
dbThru<-ggplot(tmpthrough$raw,aes(x=cLabel,y=throughput))+geom_boxplot(outlier.size = NaN)+stat_summary(fun.y=mean, geom="line", aes(group=1))+stat_summary(fun.y=mean, geom="point")+ylab("Throughput per second")+xlab("DB connections")+ggtitle(bquote(atop(.("Varying DB Connectionpool vs Throughput"), atop(italic(.("Client Simulators: 4, Middlewares: 1, Message Handler Threads: 150 ,  ")), "DB Connections: 1 - 15 each, Number of Clients: 140 Stable Types"))))+ylim(0,2000)
dbThru
ggsave("Varying DB Handlers Throughput.png", dbThru)

############### VaryMH Vary MH Threads ###################################

wdir<-"/media/mort/Seagate Expansion Drive/ASL_Data/FindingBestMW"
rwDat<-combineFiles(workDir=wdir,fileRegExNamePattern="*.csv")

rwDat<-factorizeDF2014(rwDat)

mhResp<-ggplot(rwDat,aes(x=as.factor(num_mh_threads),y=clRoundTime))+geom_boxplot(outlier.size = NaN)+ylab("Response time (ms)")+xlab("Message Handler threads")+ggtitle(bquote(atop(.("Varying Message Handler Thread Response time"), atop(italic(.("Client Simulators: 1, Middlewares: 1, Message Handler Threads: 5-95 each, DB Connections: 70 each, Number of Clients: 90 Stable Types ")), ""))))+stat_summary(fun.y=median, geom="line", aes(group=1))+stat_summary(fun.y=median, geom="point")
mhResp
ggsave("Varry Message Handler Threads Resp.png",mhResp)
tmpthrough<-quickTroughput2014(rwDat,splitOn = "num_mh_threads",trimBack = 30000 )

through<-tmpthrough$raw
mhTHrough<-ggplot(through,aes(x=cLabel,y=throughput))+geom_boxplot(outlier.size = NaN)+ylab("Throughput per Second")+xlab("Message Handler threads")+ggtitle(bquote(atop(.("Varying Message Handler Thread Throughput"), atop(italic(.("Client Simulators: 1, Middlewares: 1, Message Handler Threads: 5-95 each, DB Connections: 70 each, Number of Clients: 90 Stable Types ")), ""))))+stat_summary(fun.y=median, geom="line", aes(group=1))+stat_summary(fun.y=median, geom="point") 
mhTHrough
ggsave("Varry Message Handler Threads THroughput.png",mhTHrough)


#################

################ Long Term Experiment ####################################
wdir<-"/media/mort/Seagate Expansion Drive/ASL_Data/LongTermStability_moreconsumers_4C_2M_100MH_35DB_100C/client"
LongTermStability_4c_2M_100MH_35DB_100C <- read.csv("~/Seagate/ASL_Data/LongTermStability_moreconsumers_4C_2M_100MH_35DB_100C/client/LongTermStability_4c_2M_100MH_35DB_100C.csv")
rwDat<-factorizeDF2014r(LongTermStability_4c_2M_100MH_35DB_100C)
rwDat <- rwDat[rwDat$clEnterQ<(33*60*1000),]


tmpthrough<-findThroughPutV2014(rwDat)
through<-data.frame(throughput_last_second=tmpthrough, time=1:length(tmpthrough))

throughputLong<-ggplot(through,aes(x=time/60,y=throughput_last_second))+geom_point()+geom_hline(yintercept=mean(through$throughput_last_second),color = "darkblue" )+xlab("Time (min)")+ylab("Throughput in last Second")+ggtitle(bquote(atop(.("Longterm Throughput"), atop(italic(.("Client Simulators: 4, Middlewares: 2, Message Handler Threads: 100 each, DB Connections: 25 each, Number of Clients: 100 Stable Types ")), ""))))
throughputLong
responseTimeLong<-ggplot(rwDat[sample.int(nrow(rwDat),size=5000,replace=FALSE),],aes(x=clEnterQ/(60*1000),y=clRoundTime))+scale_y_continuous(limits = c(-10, 700))+geom_point()+geom_smooth()+xlab("Time (min)")+ylab("Response time (ms)")+ggtitle(bquote(atop(.("Longterm Responsetime"), atop(italic(.("Client Simulators: 4, Middlewares: 2, Message Handler Threads: 100 each, DB Connections: 25 each, Number of Clients: 100 Stable Types ")), ""))))
responseTimeLong
ggsave("LongTerm Response_Time_LongTermStability_moreConsumers_4C_2M_100MH_35DB_100C.png",responseTimeLong)
ggsave("Throughput_LongTermStability_moreConsumers_4C_2M_100MH_35DB_100C.png",throughputLong)
############################################################################################





######################### DB Scalability #################  OLD, UNUSED

wdir<-"/media/mort/Seagate Expansion Drive/ASL_Data/checkDBScale_xl_MW"
rwDat<-combineFiles(workDir=wdir,fileRegExNamePattern="*.csv")

rwDatF<-factorizeDF2014(rwDat)



ggplot(rwDatF[rwDatF$clEnterQ>60000,],aes(color=experiment_name ,x= num_client_machines, y=dbRoundTime))+geom_boxplot()
ggplot(rwDatF[rwDatF$clEnterQ>60000,],aes(x= num_client_machines, y=dbThinkTime))+geom_boxplot()+ylim(0,40)

through8<- quickTroughput2014(rwDatF[rwDatF$num_client_machines==8,],splitOn = "experiment_name",trimFront = 50000, trimBack = 10000 )$raw 
ggplot(through8,aes(x=cLabel,y=throughput))+geom_boxplot()+coord_flip()
tmpthrough<-quickTroughput2014(rwDatF,splitOn = "experiment_name",trimFront = 50000, trimBack = 10000 )
ggplot(tmpthrough$raw,aes(x=cLabel, y = throughput))+geom_boxplot()+coord_flip()

summary(rwDatF)


######################   Middleware Scaling ################
wdir <- "/media/mort/Seagate Expansion Drive/ASL_Data/Middleware Scaleup"
rwDat<-combineFiles(workDir=wdir,fileRegExNamePattern="*.csv")
rwDat<-factorizeDF2014(rwDat)
rwDat$mwThinkTime2 <- (rwDat$mwNoQRound -rwDat$dbRoundTime)
rwDat$mwThinkTime3<- (rwDat$mwRoundTime-rwDat$clTimeinQ-rwDat$dbRoundTime)
rwDat$numMidl2 <- as.numeric(rwDat$num_middlewares)
summary(rwDat)

  mhResp<-ggplot(rwDat[rwDat$clEnterQ>45000,],aes(x=num_middlewares,y=mwThinkTime3))+geom_boxplot(outlier.size = NaN)+ylab("Middleware Latency (ms)")+xlab("Client, Middleware Scale")+ggtitle(bquote(atop(.("Scaling Middleware vs Latency"), atop(italic(.("Client Simulators: 2-10, Middlewares: 1-5, Message Handler Threads: 100 each, DB Connections: 15 each, Number of Clients: 100 Stable Types each")), ""))))+stat_summary(fun.y=median, geom="line", aes(group=1))+stat_summary(fun.y=median, geom="point")+ylim(0,75)
mhResp
ggsave("Size up XL db m3 MW m3 Cli Latency.png",mhResp)

nme(rwDat[rwDat$clEnterQ>45000,]$mwThinkTime3)

tmpthrough<-quickTroughput2014(rwDat,splitOn = "num_middlewares",trimFront = 60000,trimBack = 5000 )



through<-tmpthrough$raw
mhTHrough<-ggplot(through,aes(x=cLabel,y=throughput))+geom_boxplot(outlier.size = NaN)+ylab("Throughput per Second")+xlab("Client, Middleware Scale")+ggtitle(bquote(atop(.("Scaling Middleware vs Throughput"), atop(italic(.("Client Simulators: 2-10, Middlewares: 1-5, Message Handler Threads: 100 each, DB Connections: 15 each, Number of Clients: 100 Stable Types each")), ""))))+stat_summary(fun.y=median, geom="line", aes(group=1))+stat_summary(fun.y=median, geom="point")
mhTHrough
ggsave("Size up XL db m3 MW m3 Cli Throughput.png",mhTHrough)


throughMean<-c(mean(through[through$cLabel==1,]$throughput),mean(through[through$cLabel==2,]$throughput),mean(through[through$cLabel==3,]$throughput),mean(through[through$cLabel==4,]$throughput),mean(through[through$cLabel==5,]$throughput))
dif<-throughMean[-1]-throughMean[-5]
throughMedi<-c(median(through[through$cLabel==1,]$throughput),median(through[through$cLabel==2,]$throughput),median(through[through$cLabel==3,]$throughput),median(through[through$cLabel==4,]$throughput),median(through[through$cLabel==5,]$throughput))

summary(rwDat)

#########################################



#########################################
wdir<-"/media/mort/Seagate Expansion Drive/ASL_Data/Varry MW nodes const CLients"
rwDat<-combineFiles(workDir=wdir,fileRegExNamePattern="*.csv")
rwDat<-factorizeDF2014(rwDat)

tmpthrough<-quickTroughput2014(rwDat,splitOn = "num_middlewares",trimFront = 60000,trimBack = 5000 )
through<-tmpthrough$raw
mhTHrough<-ggplot(through,aes(x=cLabel,y=throughput))+geom_boxplot(outlier.size = NaN)+ylab("Throughput per Second")+xlab("Client, Middleware Scale")+ggtitle(bquote(atop(.("Scaling Middleware vs Throughput"), atop(italic(.("Client Simulators: 2-10, Middlewares: 1-5, Message Handler Threads: 100 each, DB Connections: 15 each, Number of Clients: 100 Stable Types each")), ""))))+stat_summary(fun.y=median, geom="line", aes(group=1))+stat_summary(fun.y=median, geom="point")
mhTHrough
ggsave("Size up XL db m3 MW m3 Cli Throughput.png",mhTHrough)




######################################## Sanity checks   ############################
wdir<-"/media/mort/Seagate Expansion Drive/ASL_Data/sanityCheckWriteMessage1/client"
rwDat<-combineFiles(workDir=wdir,fileRegExNamePattern="*.csv")
rwDat<-factorizeDF2014(rwDat)
nrow(rwDat[rwDat$request_type=="sendMpub"&rwDat$result==" [s]",]); # Compare this number to the number of messages found in the database dump.  It matches 
# nrow = 6652
### Sanity Check removal (This experiment must be run directly after sanityCheckWriteMessage with --skipDrop setting)
wdir<-"/media/mort/Seagate Expansion Drive/ASL_Data/sanityCheckRemoveMessage1/client"
rw2Dat<-combineFiles(workDir=wdir,fileRegExNamePattern="*.csv")
rw2Dat<-factorizeDF2014(rw2Dat)
###################  
nrow(rw2Dat[rw2Dat$request_type=="readPop"&rw2Dat$result==" [s]",])
#nrow = 6842 
#The double retrivals of one message at a time are very likely in this scenario because I only created 3 queues and at everypoint in time there are 30 DB connections open some of which are sending messages at the same time to remove the same top element from one fo these 3 queues. If request retrieves the message then there is a short period of time where a second client could read this message before it is removed. The problem could be eliviated by changing the consistency level at the database for example by adding "FOR UPDATE" into a transation which includes the read and remove 

######################### Checking the effect which the Atomic counters have on throughput #############333
wdir<-"/media/mort/Seagate Expansion Drive/ASL_Data/vary Atomic Counters"
rwDat<-combineFiles(workDir=wdir,fileRegExNamePattern="*.csv")
rwDat<-factorizeDF2014(rwDat)


ggplot(rwDat,aes(x=clEnterQ,y=mwRoundTime,color=experiment_name))+geom_point()

tmpthrough<-quickTroughput2014(rwDat,splitOn = "experiment_name",trimFront = 50000,trimBack = 5000 )
through<-tmpthrough$raw
through$names<-rep("a",nrow(through))
through$names[through$cLabel=="csvClients_testingAtomicCounterOptions1_Long.csv"]<-"No"
through$names[through$cLabel=="csvClients_testingAtomicCounterOptions1_LongMWON_DBON.csv"]<-"Yes"
mhTHrough<-ggplot(through,aes(x=names,y=throughput))+geom_boxplot(outlier.size = NaN)+ylab("Throughput per Second")+xlab("AtomicIntegers Used")+ggtitle(bquote(atop(.("Throughput impact of using AtomicIntegers to log queue sizes"), atop(italic(.("Client Simulators: 2, Middlewares: 1, Message Handler Threads: 100 each, ")), "DB Connections: 25 each, Number of Clients: 100 Stable Types each"))))
mhTHrough
ggsave("Atomic Integer used Throughput.png",mhTHrough)


############################# Delete Queue performance with and without SQL CASCADE ON DELTE 

wdir<-"/home/mort/Seagate/ASL_Data/queueDelete"
rwDat<-combineFiles(workDir=wdir,fileRegExNamePattern="*.csv")
rwDat<-factorizeDF2014(rwDat)
justQ <- rwDat[rwDat$request_type=="deleteQueueRequest" &rwDat$result==" [s]",]
justQ$cascade <- "no"
justQ$cascade[justQ$experiment_name=="csvClients_queueDelete_cascOn_LargeTable_CascOff.csv"]<-"Off"
justQ$cascade[justQ$experiment_name=="csvClients_queueDelete_cascOn_LargeTable_CascOn.csv"]<-"On"
deleteQueue <- ggplot(justQ, aes(x=cascade,y=dbThinkTime))+geom_boxplot(outlier.size = NaN)+ylab("DB Service Time (ms)")+xlab("Cascade on Delete")+ggtitle(bquote(atop(.("Foreign key delete Queue performance"), atop(italic(.("Client Simulators: 4, Middlewares: 2, Message Handler Threads: 100, DB Connections: 25")), " Number of Clients: 100 send Only, Num Queues: 66 with ~5000 entries"))))
ggsave("Delete queue.png",deleteQueue)
t.test(justQ$dbThinkTime[justQ$experiment_name=="csvClients_queueDelete_cascOn_LargeTable_CascOff.csv"],justQ$dbThinkTime[justQ$experiment_name=="csvClients_queueDelete_cascOn_LargeTable_CascOn.csv"])


#################################################  Database Isolation  and then scaling 
wdir<-"/media/mort/Seagate Expansion Drive/ASL_Data/database_isolation"
rwDat<-combineFiles(workDir=wdir,fileRegExNamePattern="*.csv")
rwDat<-factorizeDF2014(rwDat)

tmpthrough<-quickTroughput2014(rwDat,splitOn = "experiment_name",trimFront = 80000,trimBack = 5000 )
through<-tmpthrough$raw
ggplot(through,aes(x=cLabel, y=throughput))+geom_boxplot()+coord_flip()


#### Try 10   <---- Max throughput  >10000
wdir<-"~/Seagate/ASL_Data/database_isolation_200cl_100XLmh_4db_xlDB"
rwDat<-combineFiles(workDir=wdir,fileRegExNamePattern="*.csv")
rwDat<-factorizeDF2014(rwDat)

tmpthrough<-quickTroughput2014(rwDat,splitOn = "experiment_name",trimFront = 80000,trimBack = 5000 )
through<-tmpthrough$raw
ggplot(through,aes(x=cLabel, y=throughput))+geom_boxplot()+coord_flip()
ggplot(through,aes(color=cLabel, x=time,y=throughput))+geom_point()

#### try 11 
wdir<-"/media/mort/Seagate Expansion Drive/tmpDBSIO"
rwDat<-combineFiles(workDir=wdir,fileRegExNamePattern="*.csv")
rwDat<-factorizeDF2014(rwDat)

tmpthrough<-quickTroughput2014(rwDat,splitOn = "experiment_name",trimFront = 80000,trimBack = 5000 )
through<-tmpthrough$raw
ggplot(through,aes(x=cLabel, y=throughput))+geom_boxplot()+coord_flip()
ggplot(through,aes(color=cLabel, x=time,y=throughput))+geom_point()

#### try 12    ############################ THis one also works for scaling DB, prob even better since the client configuration is not so strange 

wdir <- "/home/mort/Seagate/ASL_Data/database_isolation_fix"
rwDat<-combineFiles(workDir=wdir,fileRegExNamePattern="*.csv")
rwDat<-factorizeDF2014(rwDat)
rwDat<-rwDat[complete.cases(rwDat),]

saveOldNames<-levels(rwDat$experiment_name)
levels(rwDat$experiment_name)<-c("m3 large",
                                 "t2 medium",
                                 "t2 small",
                                 "m3 large",
                                 "t2 medium",
                                 "t2 small",
                                 "m3 medium",
                                 "m3 medium")



tttmp<-quickTroughput2014(rwDat[rwDat$num_client_machines==8,],splitOn = "experiment_name",trimFront = 0,trimBack = 0 )$raw
ggplot(tttmp,aes(x=time,color=cLabel,y=throughput))+geom_point()

rwDat$experiment_name <- factor(rwDat$experiment_name,levels(rwDat$experiment_name)[c(4,3,1,2)])

tmpthrough9<-quickTroughput2014(rwDat[rwDat$num_client_machines==9,],splitOn = "experiment_name",trimFront = 65000,trimBack = 5000 )$raw
tmpthrough9$ClientMachines<-9
tmpthrough8<-quickTroughput2014(rwDat[rwDat$num_client_machines==8,],splitOn = "experiment_name",trimFront = 65000,trimBack = 5000 )$raw #We are trimming away warmup and 
tmpthrough8$ClientMachines<-8
through<-rbind(tmpthrough8,tmpthrough9)
through$ClientMachines<-as.factor(through$ClientMachines)


dbIsolat<-ggplot(through,aes(x=ClientMachines,y=throughput))+geom_boxplot(outlier.size=NaN)+ylab("Throughput per Second")+xlab("Number of Client and Middleware Instances")+ggtitle(bquote(atop(.("Database Throughput Isolation Evidence"), atop(italic(.("Client Simulators: 8-9, Middlewares: 8-9, Message Handler Threads: 100, DB Connections: 10")), " Number of Clients: 100send, 50 peek, 100 find by Author , Num Queues: 4 "))))+scale_color_discrete(name="Experiment")
dbIsolat
ggsave("Database Isolation Evidence.png",dbIsolat)
t.test(through$throughput[through$ClientMachines==8],through$throughput[through$ClientMachines==9])




#Database Isolated Throughput Scaling
dbThrough<-ggplot(tmpthrough8, aes(x=cLabel,y=throughput))+geom_boxplot(outlier.size=NaN)+stat_summary(fun.y=median, geom="line", aes(group=1))+stat_summary(fun.y=median, geom="point")+ylab("Throughput per Second")+xlab("Database AWS Instance")+ggtitle(bquote(atop(.("Database Throughput Scaling"), atop(italic(.("Client Simulators: 8, Middlewares: 8, Message Handler Threads: 100, DB Connections: 10")), " Number of Clients: 100send, 50 peek, 100 find by Author , Num Queues: 4 "))))
dbThrough
ggsave("Database Throughput Scaling.png",dbThrough)
pairwise.t.test(tmpthrough8$throughput,tmpthrough8$cLabel)

jdatd<-rwDat[rwDat$num_client_machines==8&rwDat$request_type=="deleteQueueRequest",]
ggplot(jdatd,aes(x=clEnterQ,y=dbRoundTime,color=experiment_name))+geom_point()

#Database Isolated Response time Scaling
jdat8<-rwDat[rwDat$num_client_machines==8&rwDat$result==" [s]"&rwDat$result==" [s]"&rwDat$clEnter>65000&rwDat$clEnter<95000,]


dbIsol_thinktime<-ggplot(jdat8,aes(x=experiment_name,y=dbThinkTime))+geom_boxplot(outlier.size=NaN)+ylim(0,40)+ylab("DB Query Roundtime (ms)")+xlab("Database AWS Instance")+ggtitle(bquote(atop(.("Database Latency (After connection is established)"), atop(italic(.("Client Simulators: 8, Middlewares: 8, Message Handler Threads: 100, DB Connections: 10")), " Number of Clients: 100send, 50 peek, 100 find by Author , Num Queues: 4 "))))
dbIsol_thinktime
ggsave("Database Thinktime Scaling.png",dbIsol_thinktime)
pairwise.t.test(jdat8$dbThinkTime,jdat8$experiment_name)


dbRound<-ggplot(jdat8,aes(x=experiment_name,y=dbRoundTime))+geom_boxplot(outlier.size=NaN)+ylim(0,550)+ylab("DB Query Roundtime (ms)")+xlab("Database AWS Instance")+ggtitle(bquote(atop(.("Database Latency (including connection-pool)"), atop(italic(.("Client Simulators: 8, Middlewares: 8, Message Handler Threads: 100, DB Connections: 10")), " Number of Clients: 100send, 50 peek, 100 find by Author , Num Queues: 4 "))))
dbRound
ggsave("Database Roundtime Scaling.png",dbRound)
pairwise.t.test(jdat8$dbRoundTime,jdat8$experiment_name)


################################################## SUmmary togetther
wdir<-"/media/mort/Seagate Expansion Drive/ASL_Data/db_isolation_xl"
rwDat<-combineFiles(workDir=wdir,fileRegExNamePattern="*.csv")
rwDat<-factorizeDF2014(rwDat)


oldNames<-levels(rwDat$experiment_name)
rwDat$num_client_machines[rwDat$experiment_name=="csvClients_database_isolation_200cl_100XLmh_10db_8XLmachines_lDB3.csv"]<-8
levels(rwDat$experiment_name)<-c("m3 Large DB",
                "m3 medium DB",
                "t2 medium DB",
                "t2 small DB",
                "m3 Large DB",
                "m3 medium DB",
                "t2 medium DB",
                "t2 small DB")


  
rwDat$experiment_name <- factor(rwDat$experiment_name,levels(rwDat$experiment_name)[c(2,4,1,3)])
  
  


dels<-rwDat[rwDat$request_type=="deleteQueueRequest",]
summary(dels) #note that the deleteQUeues is triggered early 

tmpthrough9<-quickTroughput2014(rwDat[rwDat$num_client_machines==9,],splitOn = "experiment_name",trimFront = 65000,trimBack = 5000 )$raw
tmpthrough9$ClientMachines<-9
tmpthrough8<-quickTroughput2014(rwDat[rwDat$num_client_machines==8,],splitOn = "experiment_name",trimFront = 65000,trimBack = 5000 )$raw #We are trimming away warmup and 
tmpthrough8$ClientMachines<-8
through<-rbind(tmpthrough8,tmpthrough9)
through$ClientMachines<-as.factor(through$ClientMachines)



dbIsolat<-ggplot(through,aes(x=ClientMachines,y=throughput))+geom_boxplot(outlier.size=NaN)+ylab("Throughput per Second")+xlab("Number of Client and Middleware Instances")+ggtitle(bquote(atop(.("Database Throughput Isolation Evidence"), atop(italic(.("Client Simulators: 8-9, Middlewares: 8-9, Message Handler Threads: 100, DB Connections: 10")), " Number of Clients: 100send, 50 peek, 100 find by Author , Num Queues: 4 "))))+scale_color_discrete(name="Experiment")
dbIsolat
ggsave("Database Isolation Evidence.png",dbIsolat)
t.test(through$throughput[through$ClientMachines==8],through$throughput[through$ClientMachines==9])




#Database Isolated Throughput Scaling
dbThrough<-ggplot(tmpthrough8, aes(x=cLabel,y=throughput))+geom_boxplot(outlier.size=NaN)+stat_summary(fun.y=median, geom="line", aes(group=1))+stat_summary(fun.y=median, geom="point")+ylab("Throughput per Second")+xlab("Database AWS Instance")+ggtitle(bquote(atop(.("Database Throughput Scaling"), atop(italic(.("Client Simulators: 8, Middlewares: 8, Message Handler Threads: 100, DB Connections: 10")), " Number of Clients: 100send, 50 peek, 100 find by Author , Num Queues: 4 "))))
dbThrough
ggsave("Database Throughput Scaling.png",dbThrough)
pairwise.t.test(tmpthrough8$throughput,tmpthrough8$cLabel)

jdatd<-rwDat[rwDat$num_client_machines==8&rwDat$request_type=="deleteQueueRequest",]
ggplot(jdatd,aes(x=clEnterQ,y=dbRoundTime,color=experiment_name))+geom_point()

#Database Isolated Response time Scaling
jdat8<-rwDat[rwDat$num_client_machines==8&rwDat$result==" [s]"&rwDat$result==" [s]"&rwDat$clEnter>65000&rwDat$clEnter<95000,]


dbIsol_thinktime<-ggplot(jdat8,aes(x=experiment_name,y=dbThinkTime))+geom_boxplot(outlier.size=NaN)+ylim(0,40)+ylab("DB Query Roundtime (ms)")+xlab("Database AWS Instance")+ggtitle(bquote(atop(.("Database Latency (After connection is established)"), atop(italic(.("Client Simulators: 8, Middlewares: 8, Message Handler Threads: 100, DB Connections: 10")), " Number of Clients: 100send, 50 peek, 100 find by Author , Num Queues: 4 "))))
dbIsol_thinktime
ggsave("Database Thinktime Scaling.png",dbIsol_thinktime)
pairwise.t.test(jdat8$dbThinkTime,jdat8$experiment_name)


dbRound<-ggplot(jdat8,aes(x=experiment_name,y=dbRoundTime))+geom_boxplot(outlier.size=NaN)+ylim(0,550)+ylab("DB Query Roundtime (ms)")+xlab("Database AWS Instance")+ggtitle(bquote(atop(.("Database Latency (including connection-pool)"), atop(italic(.("Client Simulators: 8, Middlewares: 8, Message Handler Threads: 100, DB Connections: 10")), " Number of Clients: 100send, 50 peek, 100 find by Author , Num Queues: 4 "))))

ggsave("Database Roundtime Scaling.png",dbRound)
pairwise.t.test(jdat8$dbRoundTime,jdat8$experiment_name)



################################################# MW isolation and varing mw nodes 
wdir<-"/media/mort/Seagate Expansion Drive/ASL_Data/scaleMW_xlDB_midM3MW_midm3CL"
rwDat<-combineFiles(workDir=wdir,fileRegExNamePattern="*.csv")
rwDat<-factorizeDF2014(rwDat)
rwDat$mwThinkTime3<- (rwDat$mwRoundTime-rwDat$clTimeinQ-rwDat$dbRoundTime)
tmpthrough<-quickTroughput2014(rwDat,splitOn = "num_middlewares",trimFront = 60000,trimBack = 5000 )
through<-tmpthrough$raw
ggplot(through,aes(x=cLabel, y=throughput))+geom_boxplot()

### MW isolation try 3 with xl Client XL DB t2Mid MW 
wdir<-"/media/mort/Seagate Expansion Drive/ASL_Data/scaleMW_xlDB_xlCL_midT2_MW"
rwDat<-combineFiles(workDir=wdir,fileRegExNamePattern="*.csv")
rwDat<-factorizeDF2014(rwDat)

tmpthrough<-quickTroughput2014(rwDat,splitOn = "num_middlewares",trimFront = 60000,trimBack = 5000 )
through<-tmpthrough$raw
scaleMiddleware<-ggplot(through,aes(x=cLabel, y=throughput))+geom_boxplot(outlier.size = NaN)+ylab("Throughput per Second")+xlab("Number of Middleware Instances")+ggtitle(bquote(atop(.("Middleware  Throughput Scaling"), atop(italic(.("Client Simulators: 8, Middlewares: 1-8, Message Handler Threads: 50, DB Connections: 8")), " Number of Clients: 55 Send/Pop stable, Num Queues: 4 "))))+stat_summary(fun.y=median, geom="line", aes(group=1))+stat_summary(fun.y=median, geom="point")
scaleMiddleware 
ggsave("Middlware Isolation Scaling Throughput.png",scaleMiddleware)
rwDat$mwThinkTime3<- (rwDat$mwRoundTime-rwDat$clTimeinQ-rwDat$dbRoundTime)

mwLatency<-ggplot(rwDat[rwDat$clEnterQ>65000,],aes(x=num_middlewares,y=mwThinkTime3))+geom_boxplot(outlier.size = NaN)+ylab("Middleware Latency (ms)")+xlab(" Middleware Scale")+ggtitle(bquote(atop(.("Middleware  Latency Scaling"), atop(italic(.("Client Simulators: 8, Middlewares: 1-8, Message Handler Threads: 50, DB Connections: 8")), " Number of Clients: 55 Send/Pop stable, Num Queues: 4 "))))+ylim(20,60)
mwLatency
ggsave("Middlware Isolation Scaling Latency.png",mwLatency)



### Evidence 
wdir<-"/media/mort/Seagate Expansion Drive/ASL_Data/scaleMW_xlDB_xlCL_midT2_MW"
rwDat<-combineFiles(workDir=wdir,fileRegExNamePattern="*.csv")

wdir<-"/home/mort/Seagate/ASL_Data/scaleMW_xlDB_isolationEvidence"
rwDat2<-combineFiles(workDir=wdir,fileRegExNamePattern="*.csv")

rwDat<-rbind(rwDat,rwDat2)
rm(rwDat2)

through<-quickTroughput2014(rwDat,splitOn = "experiment_name",trimFront = 60000,trimBack = 5000 )$raw
levels(through$cLabel)<-c("1mw 150cl",
                          "1mw 90cl",
                          "2mw 150cl",
                          "2mw 90cl",
                          "4mw 150cl",
                          "4mw 90cl",
                          "8mw 150cl",
                          "8mw 90cl")
ggplot(through,aes(x=cLabel, y=throughput))+geom_boxplot(outlier.size = NaN)+ylab("Middleware Latency (ms)")+xlab(" Middleware Scale")+ggtitle(bquote(atop(.("Middleware Isolation Evidence, Client simulators are not maxed out"), atop(italic(.("Client Simulators: 8, Middlewares: 1-8, Message Handler Threads: 50, DB Connections: 8")), " Number of Clients: 55 Send/Pop stable, Num Queues: 4 "))))+stat_summary(fun.y=median, geom="line", aes(group=1))+stat_summary(fun.y=median, geom="point")















mwResp<-ggplot(rwDat[rwDat$clEnterQ>65000,],aes(x=num_middlewares,y=mwRoundTime))+geom_boxplot(outlier.size = NaN)+ylab("Middleware Roundtime (ms)")+xlab("Middleware Scale")+ggtitle(bquote(atop(.("Middleware Latency Scaling"), atop(italic(.("Client Simulators: 8, Middlewares: 1-8, Message Handler Threads: 50, DB Connections: 8")), " Number of Clients: 55 Send/Pop stable, Num Queues: 4 "))))+ylim(75,100)
mwResp

mwQue<-ggplot(rwDat[rwDat$clEnterQ>65000,],aes(x=num_middlewares,y=clTimeinQ))+geom_boxplot(outlier.size = NaN)+ylab("Clients Waiting in Queue (ms)")+xlab(" Middleware Scale")+ggtitle(bquote(atop(.("Middleware Isolation Evidence"), atop(italic(.("Client Simulators: 8, Middlewares: 1-8, Message Handler Threads: 50, DB Connections: 8")), " Number of Clients: 55 Send/Pop stable, Num Queues: 4 "))))+ylim(35,45)
mwQue 
ggsave("Middwlare Isolation Evidence Client queue.png",mwQue)

mwQue<-ggplot(rwDat[rwDat$clEnterQ>65000,],aes(x=num_middlewares,y=mwTimeInDBQ))+geom_boxplot(outlier.size = NaN)+ylab("Clients Waiting in Queue (ms)")+xlab("Middleware Scale")+ggtitle(bquote(atop(.("Middleware Isolation Evidence"), atop(italic(.("Client Simulators: 8, Middlewares: 1-8, Message Handler Threads: 50, DB Connections: 8")), " Number of Clients: 55 Send/Pop stable, Num Queues: 4 "))))
mwQue
pairwise.t.test(rwDat$mwThinkTime3[rwDat$clEnterQ>65000],rwDat$num_middlewares[rwDat$clEnterQ>65000])

mwQue<-ggplot(rwDat[rwDat$clEnterQ>65000,],aes(x=num_middlewares,y=clThinkTime))+geom_boxplot(outlier.size = NaN)+ylab("Clients Waiting in Queue (ms)")+xlab(" Middleware Scale")+ggtitle(bquote(atop(.("Middleware Isolation Evidence"), atop(italic(.("Client Simulators: 8, Middlewares: 1-8, Message Handler Threads: 50, DB Connections: 8")), " Number of Clients: 55 Send/Pop stable, Num Queues: 4 "))))
mwQue 

mwQue<-ggplot(rwDat[rwDat$clEnterQ>65000,],aes(x=num_middlewares,y=clRoundTime))+geom_boxplot(outlier.size = NaN)+ylab("Clients Waiting in Queue (ms)")+xlab(" Middleware Scale")+ggtitle(bquote(atop(.("Middleware Isolation Evidence"), atop(italic(.("Client Simulators: 8, Middlewares: 1-8, Message Handler Threads: 50, DB Connections: 8")), " Number of Clients: 55 Send/Pop stable, Num Queues: 4 "))))
mwQue 

mwQue<-ggplot(rwDat[rwDat$clEnterQ>65000,],aes(x=num_middlewares,y=mwRoundTime))+geom_boxplot(outlier.size = NaN)+ylab("Clients Waiting in Queue (ms)")+xlab(" Middleware Scale")+ggtitle(bquote(atop(.("Middleware Isolation Evidence"), atop(italic(.("Client Simulators: 8, Middlewares: 1-8, Message Handler Threads: 50, DB Connections: 8")), " Number of Clients: 55 Send/Pop stable, Num Queues: 4 "))))
mwQue 

# Explination why Middlware latency is not equal,, maybe
mhResp<-ggplot(rwDat[rwDat$clEnterQ>65000,],aes(x=num_middlewares,y=dbThinkTime))+geom_boxplot(outlier.size = NaN)+ylab("Middleware Latency (ms)")+xlab("Client, Middleware Scale")+ggtitle(bquote(atop(.("Middleware  Latency Scaling"), atop(italic(.("Client Simulators: 8, Middlewares: 1-8, Message Handler Threads: 50, DB Connections: 8")), " Number of Clients: 55 Send/Pop stable, Num Queues: 4 "))))+ylim(20,60)
mhResp
##########################################################################################################################################################################




################################################  Max throughput Experiment    ############################################################################################# 



wdir<-"/home/mort/College/ASL2/maxThroughputExp_9XLClients_9XLmw_XLDB_4_nodeleteQ/client"
rwDat<-combineFiles(workDir=wdir,fileRegExNamePattern="*.csv")
rwDat<-factorizeDF2014(rwDat)
through<-quickTroughput2014(rwDat,splitOn = "experiment_name",trimFront = 60000,trimBack = 5000 )$raw # we cut out the warmup time. 
ggplot(through,aes(x=cLabel,y=throughput))+geom_boxplot(outlier.size=NaN)

# Mean Message Throughput per second 
mean(through$throughput)
# Standard deviation of message throughput per second 
sd(through$throughput)

# Response time mean
mean(rwDat[rwDat$clEnterQ>60000,]$clRoundTime) # we cut out the warmup time. 
# Response time Standard Deviation 
sd(rwDat[rwDat$clEnterQ>60000,]$clRoundTime) # we cut out the warmup time.  


