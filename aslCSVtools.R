

#Hard Code defaults

checkDefaults<- function(dataHere, dfa.num_middlewares=1,
                dfa.num_db_handlers= 30,
                dfa.num_mh_threads = 30,
                dfa.num_sender     = 0,
                dfa.num_peek       = 0,
                dfa.num_pop        =  0,
                dfa.num_createq    =  0,
                dfa.num_request_resonse  =0,
                dfa.num_deleteq   =0,
                dfa.num_repr    =0,
                dfa.num_relevant_queues =0,
                dfa.num_send_messagemulti=0,
                dfa.num_readmultiPeek=0,
                dfa.num_readmultipop=0,
                dfa.message_length=100,
                dfa.dbpre_allocation_size=0){
  
  idx<-array(data=TRUE,dim=nrow(dataHere))
  if(('num_middlewares'%in% colnames(dataHere))){
    idx<-idx& (dataHere$num_middlewares %in% dfa.num_middlewares)
  }
  if(('num_db_handlers'%in% colnames(dataHere))){
    idx<-idx& (dataHere$num_db_handlers%in% dfa.num_db_handlers ) 
  }
  if(('num_mh_threads' %in% colnames(dataHere))){
    idx<-idx&(dataHere$num_mh_threads%in% dfa.num_mh_threads ) 
  }
  if(('num_sender' %in% colnames(dataHere))){
    idx<-idx&(dataHere$num_sender%in% dfa.num_sender )
  }
  if(('num_peek' %in% colnames(dataHere))){
    idx<-idx&dataHere$num_peek%in% dfa.num_peek  
  }
  if(('num_pop' %in% colnames(dataHere))){
    idx<-idx&dataHere$num_pop %in% dfa.num_pop
  }
  if(('num_createq' %in% colnames(dataHere))){
    idx<-idx&dataHere$num_createq %in%  dfa.num_createq
  }
  if(('num_request_resonse' %in% colnames(dataHere))){
    idx<-idx& (dataHere$num_request_resonse%in%dfa.num_request_resonse )
  }
  
  if(('num_deleteq' %in% colnames(dataHere))){
    idx<-idx& dataHere$num_deleteq%in%  dfa.num_deleteq  
  }
 if('num_repr' %in% colnames(dataHere)){
   idx<-idx&dataHere$num_repr %in% dfa.num_repr  
 }
 if(('num_relevant_queues' %in% colnames(dataHere))){
   idx<-idx& dataHere$num_relevant_queues%in% dfa.num_relevant_queues
 }
if('num_send_messagemulti' %in% colnames(dataHere)){
  idx<-idx& dataHere$num_send_messagemulti%in% dfa.num_send_messagemulti
}
if('num_readmultiPeek' %in% colnames(dataHere)){
  idx<-idx& dataHere$num_readmultiPeek%in% dfa.num_readmultiPeek
}
if('num_readmultipop' %in% colnames(dataHere)){
  idx<-idx&  dataHere$num_readmultipop%in% dfa.num_readmultipop
}
if('message_length' %in% colnames(dataHere)){
  idx<-idx& dataHere$message_length%in% dfa.message_length
}
if('dbpre_allocation_size' %in% colnames(dataHere)){
  idx<-idx& dataHere$dbpre_allocation_size %in% dfa.dbpre_allocation_size
}

  return(  dataHere[idx,] );
  
}

cleanAndPlot<-function(dataHere){
  return(rw.plot.All(cleanUpData(dataHere)));
}


throughPutCalc<-function(dataHere){
  rw.throughPut<-nrow(dataHere)/((max(dataHere$time_rel_send_begin)-min(dataHere$time_rel_send_begin))/1000)
   return(rw.throughPut)
}

readFileAddExperiName<-function(workDir,fileName){
  setwd(workDir)
  rw.data<- read.csv(fileName)
  experimentName<- array(data=substr(fileName,start=1,stop=nchar(fileName)-4),dim=nrow(rw.data))
  rw.data$experiment_name<-experimentName
  return(rw.data);
}

combineFiles<-function( workDir, fileRegExNamePattern="*.csv", fileNameList=list.files(workDir,pattern=fileRegExNamePattern)){
  setwd(workDir)
  rw.data<- read.csv(fileNameList[[1]])
  rw.data<-rw.data[,!(names(rw.data) == "request_id")]
  experimentName<- array(data=fileNameList[[1]],dim=nrow(rw.data))
  rw.data$experiment_name<-experimentName
  if(length(fileNameList)>=2){
    for ( i in 2:length(fileNameList)){
      if(file.info(fileNameList[[i]])$size>2){
      
        result = tryCatch({
          tmp.rw.data <- read.csv(fileNameList[[i]]);
          tmp.rw.data<-tmp.rw.data[,!(names(tmp.rw.data) == "request_id")]
          experimentName<- array(data=fileNameList[[i]],dim=nrow(tmp.rw.data))
          tmp.rw.data$experiment_name<-experimentName
          rw.data<-rbind(rw.data,tmp.rw.data)
        }, warning = function(w) {
          print(paste(" Warning when reading file: ",getwd(),fileNameList[[i]]))
          message(e)
        }, error = function(e) {
           print(paste(" ERROR file not formatted properly: ",getwd(),fileNameList[[i]]))
           message(e)
        });
                          
     
      
        
     
      }
      else{
        print(paste("Warning file is empty:",fileNameList[[i]]))
      }
    }
  }
  return (rw.data);
}


removeCols <- function(dataHere,badCols=c("dbpre_allocation_size","client_type","num_clients_per_middleware")){
  
  return ( dataHere[,!(names(dataHere) %in% badCols)])
}

removeHeadTail<-function(dataHere, warmupDuration=(120*1000), destroyDuration=10*1000){
   #  num miliseconds to cut away
  dataHere$time_rel_send_begin<-dataHere$time_client_send_to_api-dataHere$time_start_experiment
  dataHere$time_rel_return_before_end<-dataHere$time_end_experiment-dataHere$time_api_arrival_at_client #### Name confusion 
  
  if(min(dataHere$time_rel_send_begin)<0){
    print("WARNING, some experiments start after first message was sent")  
  }
  if(min(dataHere$time_rel_return_before_end)<0){
    print("WARNING, some experiments end before last message was sent")  
  }
  
  dataHere$time_rel_send_begin<-abs(dataHere$time_rel_send_begin)
  dataHere$time_rel_return_before_end<-abs(dataHere$time_rel_return_before_end)

  rw.data.fltr<-dataHere[dataHere$time_rel_send_begin>warmupDuration,]
  rw.data.fltr<-rw.data.fltr[rw.data.fltr$time_rel_return_before_end >(destroyDuration),]
  return (rw.data.fltr)
}

cleanUpData<-function(dataHere){
  dataHere<-removeCols(dataHere);
  dataHere<-dataHere[complete.cases(dataHere),]
  dataHere<-removeHeadTail(dataHere);
  dataHere<- dataHere[order(dataHere$time_client_send_to_api), ]
  return (dataHere);
}

findRoundTripTimes<-function(dataHere){
  dataHere$time_dif_client_roundtrip<-dataHere$time_api_arrival_at_client-dataHere$time_client_send_to_api;
  dataHere$time_dif_server_roundtrip<-dataHere$time_mesg_handler_end-dataHere$time_mesg_handler_start;
  dataHere$time_dif_db_roundtrip_withNetwork<-dataHere$time_db_arrived_at_server -dataHere$time_server_send_to_db;
  
  
  dataHere$timeSpent_Step1_Init_Connection<-dataHere$time_api_send_to_server-dataHere$time_client_send_to_api;
  ## time_api_send_to_server   right after opening a Socket and Object stream 
  ## time_client_send_to_api   right before ProcSend calls .sendMessage()
  dataHere$timeSpent_Step2_API_Send_to_Message_Handler<- dataHere$time_mesg_handler_star-dataHere$time_api_send_to_server;
  #time_mesg_handler_star  Right after MH reads in an object 
  #Time to write object and MH picking it up

  dataHere$timeSpent_Step3_Message_Handler_think_before_DB<-dataHere$time_server_send_to_db-dataHere$time_mesg_handler_star
  ### time_server_send_to_db right before calling the SQLUtil function
  dataHere$timeSpent_Step4_DB_Network_and_think<-  dataHere$time_db_arrived_at_server -dataHere$time_server_send_to_db
  ###time_db_arrived_at_server    Right after recieving SQLUtil time 
 
  dataHere$timeSpent_Step5_Message_Handler_Send_to_Client<-dataHere$time_server_arrival_at_api-dataHere$time_db_arrived_at_server
  # time_server_arrival_at_api  After opening an input stream and then receiving the object 
  timeShift<-min(dataHere$timeSpent_Step5_Message_Handler_Send_to_Client)
  if(timeShift<0){
    dataHere$timeSpent_Step5_Message_Handler_Send_to_Client<- dataHere$timeSpent_Step5_Message_Handler_Send_to_Client+abs(timeShift)
    dataHere$timeSpent_Step2_API_Send_to_Message_Handler<-dataHere$timeSpent_Step2_API_Send_to_Message_Handler+timeShift
  }
  dataHere$timeSpent_Step6_API_Close_Socket_and_return_to_Client <- dataHere$time_api_arrival_at_client-dataHere$time_server_arrival_at_api  
  # time_api_arrival_at_client   in the client when a request is back at client and is done 
  dataHere$networkTime
  dataHere$timeSpent_MH_timeToCloseSocket <- dataHere$time_mesg_handler_end-dataHere$time_server_send_to_api 
  dataHere$timeSpent_MH_SendAndClose <- dataHere$time_mesg_handler_end-dataHere$time_db_arrived_at_server ##Just added

  
  ###time_server_send_to_api   After it finished writing the object to the out stream. 
  # timeSpent_Step5_Server_think_after   can be considered the time it takes for the response to be moved accross the network from Middle ware to client  
  
  return(dataHere);
}




findThroughPutV2<-function(dataHere,bucketSize=1000){
  
  
  
  cl.range<-range(dataHere$time_mesg_handler_end)

 
  rw.str.throughputHist<- hist(dataHere$time_mesg_handler_end,breaks=seq(from=cl.range[1],to=cl.range[2]+bucketSize,by=bucketSize),plot=FALSE)
  
  return(rw.str.throughputHist)
  
  
}

findThroughPutV2014<-function(dataHere,bucketSize=1000){
  
  
  
  cl.range<-range(dataHere$mwLeaveQ)
  dataHere$time_mesg_handler_end_shifted<-dataHere$mwLeaveQ-cl.range[1];
  cl.s.range<-range(dataHere$time_mesg_handler_end_shifted)
  rw.str.throughputHist<- hist(dataHere$time_mesg_handler_end_shifted,breaks=seq(from=cl.s.range[1],to=cl.s.range[2]+bucketSize,by=bucketSize),plot=FALSE)
  
  return(rw.str.throughputHist$counts[-length(rw.str.throughputHist$counts)])
  
  
}

findThroughPut<-function(dataHere,bucketSize=1000){
  
 
  
  cl.range<-range(dataHere$time_mesg_handler_end)
  dataHere$time_mesg_handler_end_shifted<-dataHere$time_mesg_handler_end-cl.range[1];
  cl.s.range<-range(dataHere$time_mesg_handler_end_shifted)
  rw.str.throughputHist<- hist(dataHere$time_mesg_handler_end_shifted,breaks=seq(from=cl.s.range[1],to=cl.s.range[2]+bucketSize,by=bucketSize),plot=FALSE)
  
  return(rw.str.throughputHist$counts[-length(rw.str.throughputHist$counts)])
  
  
}


convert2014CSVback <- function(d){
  names(d)[names(d)=="num_queues"] <- "num_relevant_queues"
  dout<-data.frame(num_peek=rep(0,times=nrow(d)),check.names = FALSE)
  dout$time_start_experiment<-d$time_start_experiment
  dout$time_end_experiment<-d$time_start_experiment+d$exp_duration
  dout$time_client_send_to_api<-d$clEnterQ-d$clConnInitTime+d$time_start_experiment
  dout$time_api_send_to_server<-d$clEnterQ+d$time_start_experiment
  dout$time_mesg_handler_start<-d$clLeaveQ+d$time_start_experiment
  dout$time_server_send_to_db<-d$mwEnterQ+d$time_start_experiment
  dout$time_db_arrived_at_server<-d$mwLeaveQ+d$time_start_experiment
  dout$time_server_send_to_api<-d$mwLeaveQ+d$time_start_experiment
  dout$time_mesg_handler_end<-d$mwLeaveQ+d$time_start_experiment+d$clCloseTime #kinda fake but i dont need thsi number anyway
  dout$time_api_arrival_at_client <- d$clEnterQ+d$time_start_experiment+d$clRoundTime
  dout$time_server_arrival_at_api <- d$clEnterQ+d$time_start_experiment+d$clRoundTime-d$clThinkTime
  dout$result<-d$result
  dout$result[d$result=="[s]"]<-"PASS"
  dout$result[d$result=="[f]"]<-"FAIL"
  dout$result<-as.factor(dout$result)
  dout$message_length<-100 #super fake 
  dout$num_readmultipop<-d$num_clients_perMachien
  dout$num_middlewares<-d$num_middlewares
  dout$num_db_handlers<-d$num_db_handlers
  dout$num_mh_threads<-d$num_mh_threads
  dout$client_id<-d$client_id
  dout$request_id<-d$request_id
  dout$request_type<-d$request_type
  dout$num_sender<-0
  dout$num_peek<-0
  dout$num_pop<-0
  dout$num_createq<-0
  dout$num_request_response_pool<-0
  dout$num_request_response_pairs<-0
  dout$num_deleteq<-0
  dout$num_repr<-0
  dout$num_send_messagemulti<-0
  
  return(dout);
}


factorizeDF2014<-function(dataHere){
  
  if( "experiment_description" %in% names(dataHere)){
    dataHere$experiment_description <- as.factor(dataHere$experiment_description)
  }
  if( "time_start_experiment" %in% names(dataHere)){
    dataHere$time_start_experiment <- as.factor(dataHere$time_start_experiment)
  }
  if( "exp_duration" %in% names(dataHere)){
    dataHere$exp_duration <- as.factor(dataHere$exp_duration)
  }
  if( "num_mh_threads" %in% names(dataHere)){
    dataHere$num_mh_threads <- as.factor(dataHere$num_mh_threads)
  }
  if( "num_middlewares" %in% names(dataHere)){
    dataHere$num_middlewares <- as.factor(dataHere$num_middlewares)
  }
  if( "num_db_handlers" %in% names(dataHere)){
    dataHere$num_db_handlers <- as.factor(dataHere$num_db_handlers)
  }
  if( "num_client_machines" %in% names(dataHere)){
    dataHere$num_client_machines <- as.factor(dataHere$num_client_machines)
  }
  if( "num_queues" %in% names(dataHere)){
    dataHere$num_queues <- as.factor(dataHere$num_queues)
  }
  if( "num_clients_perMachien" %in% names(dataHere)){
    dataHere$num_clients_perMachien <- as.factor(dataHere$num_clients_perMachien)
  }
  if( "client_id" %in% names(dataHere)){
    dataHere$client_id <- as.factor(dataHere$client_id)
  }
  if( "request_type" %in% names(dataHere)){
    dataHere$request_type <- as.factor(dataHere$request_type)
  }
  if( "Label" %in% names(dataHere)){
    dataHere$Label <- as.factor(dataHere$Label)
  }
  if( "experiment_name" %in% names(dataHere)){
    dataHere$experiment_name <- as.factor(dataHere$experiment_name)
  }
  if( "result" %in% names(dataHere)){
    dataHere$result <- as.factor(dataHere$result)
  }
  return(dataHere)
  
}

remove_outliers <- function(x, na.rm = TRUE, ...) {
  qnt <- quantile(x, probs=c(.25, .75), na.rm = na.rm, ...)
  H <- 1.5 * IQR(x, na.rm = na.rm)
  y <- x
  y[x < (qnt[1] - H)] <- NA
  y[x > (qnt[2] + H)] <- NA
  y
}

quickTroughput2014 <- function(rw.bigData.rt,trimFront=120000,splitOn="num_clients_perMachien",trimBack=60000, removeOutlier=FALSE){
  
  saveResults_1SecondBucket<-data.frame(check.names = FALSE,check.rows=FALSE);
  saveRawCounts_1Sec<-data.frame(check.names = FALSE,check.rows=FALSE);   
  rw.bigData.rt<-rw.bigData.rt[rw.bigData.rt$clEnterQ>trimFront,]
  maxTime<-rw.bigData.rt$clEnterQ[length(rw.bigData.rt$clEnterQ)-1]
  rw.bigData.rt<-rw.bigData.rt[rw.bigData.rt$clEnterQ<(maxTime-trimBack),]
  varName<-splitOn
  for( lvlName in levels(as.factor(rw.bigData.rt[varName][[1]]))){
    
    
    rw.filterData<-rw.bigData.rt[rw.bigData.rt[varName]==lvlName,]
    if(nrow(rw.filterData)>0) {
      ### 1 second bucket 
      rw.str.throughputHist<-findThroughPutV2014(rw.filterData,bucketSize=1000)
      if(removeOutlier){
        rw.str.throughputHist<-remove_outliers(rw.str.throughputHist)
      }
      for( i in 1:length(rw.str.throughputHist)){
        saveRawCounts_1Sec<-rbind(saveRawCounts_1Sec,data.frame(cLabel=lvlName,throughput=rw.str.throughputHist[i],time=i))
      }
      
      
      outRecord<-rw.filterData[1,1:21]
      outRecord$SDTrhoughPutPerSecond<-sd(rw.str.throughputHist)
      rw.t.thr<-t.test(rw.str.throughputHist)
      outRecord$MeanThroughPutPerSecond<-mean(rw.t.thr$estimate)
      outRecord$MeanThroughPutPerSecond_ConfInt_Uper<-(rw.t.thr$conf.int[2])
      outRecord$MeanThroughPutPerSecond_ConfInt_Lowr<-(rw.t.thr$conf.int[1])
      outRecord$ThroughPutPer_num_sample<-length(rw.str.throughputHist)
      outRecord$ThroughPut_sem <-  outRecord$SDTrhoughPutPerSecond/sqrt(outRecord$ThroughPutPer_num_sample)
      saveResults_1SecondBucket<-rbind(saveResults_1SecondBucket,outRecord )
      
      
    }

  }
  return(list("buckets"=saveResults_1SecondBucket,"raw"=saveRawCounts_1Sec ))
}

###################

