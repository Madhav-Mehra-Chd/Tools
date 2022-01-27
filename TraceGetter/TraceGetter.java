// This utility is used to get Trace from any node of server by providing Identifier.

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;


public class TraceGetter{
    
	String tkprof,mailcmd,userid,passwd;
	static HashMap<String,String> nodeTraceList = new HashMap<String,String>();;
	static String mailSubject,traceFolderOnServerFromProp,envSet,traceName,traceFolderOnServer,serverName,identifier,mailid,hostName,errString,tkpName,nodeName,prefix,prefix1,functionName,buName="",traceLocation,fileSeparator,knownHosts,privateKeyPath,thresholdTime="0",tkpNameWithtime,tkpFileNameWithPath,thresHoldEmail;
	static HashMap<String,String> nodesDetailsList = new HashMap<String,String>(); 
	static int traceFileCount=0,nodeCount=0,customFlag=0,logFlag=0,traceGenerated =0,mailAlreadySent=0; ;
	static Session session = null;
	static Channel channel = null;
	static Logger logger;
	public static void main(String[] args) 
	{
		 functionName = "main";
		 TraceGetter ps = new TraceGetter(); 
		 try
		 {
		 serverName="betsy";
		 
		 System.out.println("\nServer Names---"+args[0]);
		 System.out.println("Identifier or Trace Name---"+args[1]);
		 
		 fileSeparator = File.separator; 
		 
		 if(args[0]!=null)
	     serverName=args[0];
		 if(args[1]!=null)
		 identifier=args[1];
		 if(args.length >2 && args[2]!=null)
	     {
			 mailid=args[2];
			 System.out.println("Email id to recieve Trace---"+args[2]);
			 if(mailid.toLowerCase().equals("log"))
			 {   
				 System.out.println("-------Logs are enabled----");
				 logFlag=1;	 
				 //logger = ps.writeLogs();
				
				 
			}
	     }
		 		 
		 if(args.length >3 && args[3]!=null)
		 {
			 System.out.println("Prefix to be added before TKP file---"+args[3]);
			 prefix=args[3];
			 
			 prefix1=prefix; 
			 prefix=prefix+"_";  
		 }
		 else
	     prefix="";
		 
		 if(args.length > 4 && args[4]!=null)
		 {
	     buName=args[4];
		 // Flag for enabling customization 
	     customFlag=1;
		 }
		 if(logFlag==1)
		 {
			 logger.info("serverName---"+serverName+"---identifier--"+identifier+"---mailid---"+mailid);
			 logger.info("prefix---"+prefix+"---buName---"+buName);
			 logger.info("customFlag---"+customFlag);
		 }
		 
		 if(identifier.contains(".trc"))
		 identifier = identifier.replace(".trc","");	 
		 // Main Logic Starts from Here
		 ps.getNodeDetails(serverName);
		 
		 if(nodeCount == 0)
		 {
		  System.out.println("Nodes Information for server \""+serverName+"\" is not found in Properties files. Kindly check Server Name.");	 
		 }
		 else
		 {	 
		 ps.searchTraceFromAllNodes();
	     	 
	     if(traceFileCount == 0)
		 System.out.println("\nSorry, Trace Not found on any of nodes of "+serverName);	 
		 else if(traceFileCount==1)
			{
			 //------If only 1 trace file is found---------------
	         for (Iterator<String> i = nodeTraceList.keySet().iterator(); i.hasNext(); ) 
	               {
	        	  nodeName = (String) i.next();
	        	  traceName = (String) nodeTraceList.get(nodeName);
	        	   }
	          traceName = traceName.replace(";","");
	          System.out.println("\nTrace file found on Node----"+nodeName+"   With Name---"+traceName);	 
	          ps.createTKP(nodeName,traceName);
			  if(traceGenerated==1)
			  {	  
		      ps.getTKP(nodeName, tkpName);
	          ps.checkTime();
		      if(mailAlreadySent==0)
	          ps.sendMail(mailid);  
	          }
			 /* else
			  {
				  serverName= "betsy1";
				  ps.getNodeDetails(serverName);
				  ps.createTKPOnOtherServer(serverName,traceName);
				  ps.mailTKP(nodeName, tkpName);
		          ps.getTKP(nodeName, tkpName);
			  } */
	          ps.releaseResources(); 
	          System.out.println("\nHappy Tracing from TraceGetter. For any issues/suggestions, feel free to reach at Madhav.Mehra@emerson.com");	 
			}
		 else
		 {
			 //------In case if multiple trace files are found---------------
			 System.out.println("\n-------"+traceFileCount+" trace Files found as below------"); 
			 int tc =1;	 
			 for (Iterator<String> i = nodeTraceList.keySet().iterator(); i.hasNext(); ) 
             {
	      	     nodeName = (String) i.next();
	      	     traceName = (String) nodeTraceList.get(nodeName);
      	      
	      	     System.out.println("\nNode Name---"+nodeName);
				 String[] s = traceName.split("\\;"); 
			     for(int j = 0;j<s.length;j++)
			      {
			    	System.out.println(tc+")"+s[j]);
			        tc++;
			      }
			  }
			 System.out.println("\nKindly enter some Strong Identifier");	 
 	     }
		
		 } //Closing brace of Else of nodeFound if block
		 }
		 catch(ArrayIndexOutOfBoundsException e)
		 {
			 
			 traceFileCount = 1000; // This parameter is read by batch file to run the program again in case if mutiple files are found.
			 System.out.println("One of input paramter is missing.Server Name, Identifier and Email id are mandatory Parameters.");	 
		 }
		 catch(Exception e)
		 {
	     if(logFlag==1) 
	     e.printStackTrace();
		 System.out.println("----Exception In Main---"+e.getMessage()); 
		 }
		 finally
		 {
			 // This function will set errorlevel system variable so that batch file can read.
			 System.exit(traceFileCount);
		
		 }
	}
// This function will read the properties file for various parameters and node details.
public void getNodeDetails(String serverName)	
{
	if(logFlag==1)
	{
	logger.info("---Entering getNodeDetails-----serverName--"+serverName);	
	}  
	
	functionName = "getNodeDetails";
	Properties prop = new Properties();
	InputStream input = null;
	String nodesDetails =null;
	Set<Object> keys =null;
	System.out.println("\nGetting Nodes Details for server---"+serverName);
	try {
			input = new FileInputStream("C:/TraceGetter/helper/Trace.properties");
			prop.load(input);
			
			//--------------Fetches the nodes Names for the server------------------	   
	    	keys = prop.keySet();
	    	
	    	for(Object k:keys)
	        {
	            String key = (String)k;
	            if((key).toLowerCase().equals((serverName+"_server").toLowerCase()))
	            {	
		            //System.out.println("----Node Name-----"+key+"--Value--"+prop.getProperty(key));
		            nodesDetails = prop.getProperty(key);
	            	
		         }
	            if(customFlag==1)
			    {
			     if((key).toLowerCase().equals((prefix1+"_"+serverName).toLowerCase()))	
			     thresholdTime = prop.getProperty(key);
			     if(prop.getProperty("ThreshHoldEmail")!=null)
			     thresHoldEmail = prop.getProperty("ThreshHoldEmail");
			    }
	        }  
	    	//--------- checking the case if instead of server, particular node Name is entered
	    	if(nodesDetails == null)
	    	{
	    		for(Object k:keys)
		        {
		            String key = (String)k;
		            if(key.contains("_server"))
		            {	
		           
		            String valueOfKey = prop.getProperty(key);
		            String[] tempNodesName = valueOfKey.split("\\;");
			        for(int i = 0;i<tempNodesName.length;i++)
			        {   if(tempNodesName[i].equalsIgnoreCase(serverName))
		                 {   
		                	TraceGetter.serverName=key.replace("_server","");
		                	nodesDetails=tempNodesName[i]+";";
		                  }
		            }
		            }
			       if(nodesDetails!=null)
			       break;
		          }
		     }
	    	//-----------Fetching details of all the nodes corresponding to server----------------
	    	if(nodesDetails!=null)
	    	{
			    	System.out.println("-------Nodes found for this Server-----"+nodesDetails);
			    	String[] fetchNodesDetails = nodesDetails.split("\\;");
			    	Set<Object> keys1 = prop.keySet();
			    	for(Object k:keys1)
			        {
			    	for(int j = 0; j< fetchNodesDetails.length ;j++)
			        {
			            String key = fetchNodesDetails[j];
			            if(key.toLowerCase().equals(k.toString().toLowerCase()))
			            {	
				            nodesDetailsList.put(key,prop.getProperty(key));
				            nodeCount++;
				            if(logFlag ==1)
					        logger.info("----Node Name-----"+key+"--Value--"+prop.getProperty(key));
				         }
			        }  }
			    	
	    	}
	    	
	    	// Fetching Trace Location from Properties Files where traces needs to be saved
	    	traceLocation = prop.getProperty("TraceLocation");
	    	if(traceLocation!=null)
	    	{
	    	traceLocation=traceLocation.replace("/", fileSeparator);
	    	traceLocation=traceLocation.replace("\\", fileSeparator);
	    	}
	    	//Getting Private Key Path for Key Login, where private key is placed locally
	    	privateKeyPath = prop.getProperty("PrivateKeyPath");
	    	//Getting Know Hosts File Path for Key Login
	    	knownHosts = prop.getProperty("KnownHosts");
	    	//Getting Trace Folder Location on Server from Properties File. 
	    	traceFolderOnServerFromProp = prop.getProperty("TraceFolderOnServer");
		    if(traceFolderOnServerFromProp==null)
		    traceFolderOnServerFromProp =  "db/db/11.2.0/appsutil/outbound/";
		    if(mailid==null)
			{
		    	if(prop.getProperty("DefaultEmail")!=null)
		    	mailid =  prop.getProperty("DefaultEmail");
			}
		    
		    //System.out.println("---traceFolderOnServer-----"+traceFolderOnServer);
		  
		    	
	    	if(logFlag==1)
	    	{
	    	 logger.info("traceLocation----"+traceLocation);
	    	 logger.info("nodesDetails----"+nodesDetails);
	    	 logger.info("privateKeyPath----"+privateKeyPath);
	    	 logger.info("knownHosts----"+knownHosts);
	    	 logger.info("traceFolderOnServerFromProp----"+traceFolderOnServerFromProp);
	    	 logger.info("nodesDetailsList----"+nodesDetailsList);
	    	}
       }
	catch(Exception e)
	{
	System.out.println("-----Exception in Fetching Node Details--------"+e.getMessage());
	if(logFlag==1) 
    e.printStackTrace();
	}
	if(logFlag==1) 
	logger.info("----Exiting getNodeDetails----");
		
	
}	
	
public void checkTime()
{
	functionName = "checkTime";
	if(logFlag==1)
	 {
		 logger.info("----Entering CheckTime Function---");
		 logger.info("-----traceLocation---"+traceLocation+"\\"+tkpName);
	 }
	tkpFileNameWithPath=traceLocation+fileSeparator+tkpName;
	File file = new File(tkpFileNameWithPath); 
	long lElapsedTimeFromTrace, lthresholdTimeForTransaction=0;
    BufferedReader br;
    String uom= "secs";
	try {
	br = new BufferedReader(new FileReader(file));
	String st; 
	String time="0";
	while ((st = br.readLine()) != null) 
	{ 
	if(st.contains("elapsed seconds in trace file"))
	{
	String[] s = st.split(" ");
	for(int i =0 ;i<s.length;i++)
	{
		if(s[i].equals("elapsed"))
		{
		//System.out.println("----Time taken--"+s[i-2]);	
		time = s[i-2];
		}
		}
	
	} }
	br.close();
	if(logFlag==1)
	 {
		logger.info("-----time read from Trace File---"+time);
	 }
	lElapsedTimeFromTrace = Long.parseLong(time);
	//System.out.println("--lElapsedTimeFromTrace--"+lElapsedTimeFromTrace);
	long hours = lElapsedTimeFromTrace / 3600;
	long minutes = (lElapsedTimeFromTrace % 3600) / 60;
	long seconds = lElapsedTimeFromTrace % 60;
	
	if(hours<=0)
	{
	 if(minutes>0 && seconds !=0)
	 uom = Long.toString(minutes)+"_mins_"+	 Long.toString(seconds)+"secs";
	 if(minutes>0 && seconds ==0)
	 uom = Long.toString(minutes)+"_mins";
     if(minutes<=0)
     uom = Long.toString(seconds)+"_secs";	 
	}
	if(hours>0)
	{
	 if(minutes>0 && seconds !=0)
	 uom = Long.toString(hours)+"_hrs_"+Long.toString(minutes)+"_mins_"+	 Long.toString(seconds)+"secs";
	 if(minutes>0 && seconds ==0)
	 uom =  Long.toString(hours)+"_hrs_"+Long.toString(minutes)+"_mins";
     if(minutes<=0)
     uom = Long.toString(hours)+"_hrs_"+Long.toString(seconds)+"_secs";	 
	}
	
	System.out.println("---Elapsed time in Trace File(in seconds)---"+time);
	if(lElapsedTimeFromTrace>60)
	System.out.println("---Elapsed time in Trace File(in minutes)---"+lElapsedTimeFromTrace/60+"_mins "+lElapsedTimeFromTrace%60+"_secs");
	if(hours>0)
	System.out.println("---Elapsed time in Trace File(in hours)---"+uom);
	uom="_"+uom;
	//--- Renaming Tkp FileName by adding time taken in seconds in it---
	//System.out.println("---uom--"+uom);
	//System.out.println("---tkpFileNameWithPath--"+tkpFileNameWithPath); 
	Path currentFile = Paths.get(tkpFileNameWithPath);
	tkpNameWithtime = (tkpName.replace(".tkp",""))+uom+".tkp";
	//System.out.println("---tkpName--"+tkpName);
	try
	{
	Files.move(currentFile, currentFile.resolveSibling(tkpNameWithtime));
	tkpFileNameWithPath=traceLocation+fileSeparator+tkpNameWithtime;
	}
	catch(IOException ioe)
	{
		tkpFileNameWithPath=traceLocation+fileSeparator+tkpName;
		tkpNameWithtime=tkpName;
		System.out.println("---Error in Renaming file--"+ioe.getMessage());	
	}
	
	mailSubject=tkpNameWithtime;
	if(logFlag==1)
	 {
		logger.info("-----tkpNameWithtime---"+tkpNameWithtime);
	 }
	if(customFlag==1)
	{	
	lthresholdTimeForTransaction = Long.parseLong(thresholdTime);
	System.out.println("----Thresh Hold Time for this Transaction--"+lthresholdTimeForTransaction);
	if(lElapsedTimeFromTrace > lthresholdTimeForTransaction)
	{   
		System.out.println("----Elapsed time is Greater than thresh Hold time by---"+(lElapsedTimeFromTrace-lthresholdTimeForTransaction)+" secs.");
		mailSubject="ALERT : "+"Elapsed time for "+prefix1+" on "+serverName.toUpperCase()+" greater than threshold time by "+(lElapsedTimeFromTrace-lthresholdTimeForTransaction)+" secs.";
		if(thresHoldEmail!=null)
		mailid=thresHoldEmail;	
		sendMail(mailid);
		mailAlreadySent = 1;
	}
	
	}
} catch (Exception e) {
	System.out.println("----Exception in CheckTime---"+e.getMessage());
	e.printStackTrace();
} 
if(logFlag==1) 
	logger.info("----Exiting checkTime----");
}

public void sendMail(String toEmailId)
{   
	if(logFlag==1) 
		logger.info("----Entering sendMail----");
	String to = toEmailId;
	if(to!=null && to.length()!=0 && !("BlankEmail").equals(to))
	{	
	
    Multipart multipart = new MimeMultipart();
    String filename = tkpFileNameWithPath;
    String from = "TraceGetter@emerson.com";
    String host = "INETMAIL.emrsn.net";
    Properties props = new Properties();
    props.put("mail.smtp.host", host);
    props.put("mail.smtp.port", "25");
    //System.out.println("4324324");
    try {
    // Get the Session object.
    	javax.mail.Session session1 = javax.mail.Session.getDefaultInstance(props);
    // Create a default MimeMessage object.
       Message message = new MimeMessage(session1);
       
       message.setFrom(new InternetAddress(from));
       message.setRecipients(Message.RecipientType.TO,InternetAddress.parse(to));
       if(logFlag==1)
       logger.info("--------File to be mailed----"+tkpFileNameWithPath);
       message.setSubject(mailSubject);
       // Create the message part
       BodyPart messageBodyPart = new MimeBodyPart();
       messageBodyPart.setText("PFA your Trace file from TraceGetter. For any issues/suggestions, feel free to reach at Madhav.Mehra@emerson.com.");
       multipart.addBodyPart(messageBodyPart);
       messageBodyPart = new MimeBodyPart();
       
       DataSource source = new FileDataSource(filename);
       messageBodyPart.setDataHandler(new DataHandler(source));
       messageBodyPart.setFileName(tkpNameWithtime);
       multipart.addBodyPart(messageBodyPart);
       message.setContent(multipart);

       // Send message
       Transport.send(message);
       System.out.println("\nTrace File emailed to----"+to);
    } catch (Exception e)
    {
       System.out.println("---Exception in Sending Mail to---"+to);
    }	
	}
	if(logFlag==1) 
		logger.info("----Exiting sendMail----");
}

  public void searchTraceFromAllNodes()
  {	 
	  if(logFlag==1) 
	  logger.info("----Entering searchTraceFromAllNodes----");
	  functionName = "searchTraceFromAllNodes";
	 String traceString = null;
	 System.out.println("\nSearching Trace on All Nodes with identifier---"+identifier);	
	 Iterator<String> keyIterator = nodesDetailsList.keySet().iterator();
     while(keyIterator.hasNext()) 
     {  functionName = "searchTraceFromAllNodes";
        String key = keyIterator.next();
        if(createSession(key)==0)
		{
        traceString = getTraceByIdentifier();
        	
		if(traceString.length()>0)
		{			
			if(logFlag==1)
			logger.info("-----Traces found on Node----"+nodeName+"-----"+traceString);
			nodeTraceList.put(nodeName,traceString);
		}
	 }}
     if(logFlag==1)       
     logger.info("-----Exiting searchTraceFromAllNodes nodeTraceList----"+nodeTraceList);
   
     
  }
        
   
public String getTraceByIdentifier()
{
	 if(logFlag==1)       
	 logger.info("-----Entering getTraceByIdentifier ----");
	functionName = "getTraceByIdentifier";
	StringBuilder sb = new StringBuilder();	
    StringBuilder sb1 = new StringBuilder();
	
    try 
	{
    
    String searchIdentifier = ";ls -lt *"+identifier+"*.trc;";
	String finalCommand ="cd "+ traceFolderOnServer.replace("cd ","")+searchIdentifier;
	int excatName= 0;
	
	//System.out.println("---finalCommand---"+finalCommand);
	Channel channel = session.openChannel("exec");
	channel.setInputStream(null);
    ((ChannelExec)channel).setErrStream(System.err);
     if(logFlag==1)
     {
    	 logger.info("----Searching Trace on---"+nodeName+"---at location---"+traceFolderOnServer);
    	 logger.info("--------Running FinalCmd-------"+finalCommand);
      }
     ((ChannelExec)channel).setCommand(finalCommand);
     InputStream in=channel.getInputStream();
     channel.connect();
    byte[] tmp=new byte[1024];
    
    while(true)
    {
          	while(in.available()>0)
          	{
          		
    	   	  int i=in.read(tmp, 0, 1024);
              if(i<0)break;
              System.out.println(new String(tmp, 0, i));
              sb.append(new String(tmp, 0, i));
              
             }
    	if(channel.isClosed()){
        //System.out.println("exit-status: "+channel.getExitStatus());
        break;
      }
      
    	try{Thread.sleep(1000);}catch(Exception ee){}
    }

    String[] s = (sb.toString().split("\\n")); 
    for(int i = 0;i<s.length;i++)
    {
    String[] s1 = s[i].toString().split(" ");
    //----In case if exact file name is matched----------------
    for(int j=0;j<s1.length;j++)
    {
    
   
    if(s1[j].equals(identifier+".trc"))
    {   
    	sb1.append(s1[j]+";");
    	excatName = 1;
    	traceFileCount++;
    	break;
    }
    }}
  //----In case  exact file name is not found----------------
    if(excatName==0)
    {	
    	for(int i = 0;i<s.length;i++)
        {
        String[] s1 = s[i].toString().split(" ");
        //----In case if exact file name is matched----------------
        for(int j=0;j<s1.length;j++)
        {
             
        if(s1[j].contains(".trc"))
        {   
        	
        	sb1.append(s1[j]+";");
        	
        	excatName = 1;
        	traceFileCount++;
        	break;
        }
        }}
    }
    
	} catch (JSchException e) 
	{
		if(logFlag==1) e.printStackTrace();
	}
	catch (Exception e) 
	{
	   if(logFlag==1) e.printStackTrace();
	}	
	if(logFlag==1)
	logger.info("---Exiting TraceByIdentifier---Traces file String return from---"+nodeName+" as---"+sb1.toString());	
	
	return(sb1.toString());
}
	

public void mailTKP(String nodeName,String traceFileName )
{
	functionName = "mailTKP";
	System.out.println("\nMailing Trace File");
	//System.out.println("------nodeName------"+nodeName+"-----Trace Name----"+traceFileName);
	try{
		createSession(nodeName);
		mailcmd = "echo PFA your Trace file from TraceGetter. | mailx -a "
					+ tkpName + " -s \"" + tkpName
					+ "\" -S from=\"TraceGetter@emerson.com\" " + mailid;
    	//finalCommand = envSet+";"+traceFolderOnServer+";"+tkprof+";"+mailcmd+";";
    	//System.out.println("------finalCmd----"+finalCommand);
    	channel=session.openChannel("exec");
       
        channel.setInputStream(null);
        ((ChannelExec)channel).setErrStream(System.err);
       if(logFlag==1)
       logger.info("--------Running-------"+mailcmd);
        ((ChannelExec)channel).setCommand(mailcmd);
         InputStream in=channel.getInputStream();
         channel.connect();
        byte[] tmp=new byte[1024];
        while(true){
          while(in.available()>0){
            int i=in.read(tmp, 0, 1024);
            //System.out.println("--tmp---"+tmp.);
            if(i<0)break;
            if(logFlag==1)
            logger.info("----Message from Mailx command---"+new String(tmp, 0, i));
          }
          if(channel.isClosed()){
            //System.out.println("exit-status: "+channel.getExitStatus());
            break;
          }
          try{Thread.sleep(1000);}catch(Exception ee){}
        }
        //}       
        
        //System.out.println("--mailid---"+mailid);
        if(mailid.toLowerCase().contains("@emerson.com"))
        {
        if(!tmp.toString().contains("Saved message"))
        System.out.println("Trace File successfully mailed to---"+mailid);
        }
        else
        {
        System.out.println(mailid +" is not valid Emerson email id.");	
        }
        	
    }catch(Exception e){
    	if(logFlag==1) 
    	e.printStackTrace();
    	System.out.println("--Exception in mailing TKP--"+e.getMessage());
    }
}

public void createTKP(String nodeName,String traceFileName )
{
	if(logFlag==1)       
	logger.info("-----Entering createTKP ----nodeName---"+nodeName+"---traceFileName---"+traceFileName);
	functionName = "createTKP"; 
	System.out.println("\nCreating Tkprof of Trace Files---"+traceFileName+"---at Node---"+nodeName);
	try
	{
		createSession(nodeName);
		// Changes done for Customization  - Start
		if(customFlag==1) 
		{	
		DateFormat dateFormat = new SimpleDateFormat("dd-MMM-yy_HH");
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		Date date = new Date();
		tkpName= serverName+"_"+dateFormat.format(date)+"_"+buName+"_"+prefix1+".tkp";
		}
		else
		{
		tkpName= prefix+traceFileName.replaceAll(".trc", "")+".tkp";	
		}
		// Changes done for Customization  - End
		
    	String finalCommand=nodesDetailsList.get(nodeName).toString();
		
    	
		//envSet=cmd[3];
    	//traceFolderOnServer=cmd[4];
    	String traceFolderPath = traceFolderOnServer.replace("cd ", "")+"/";
    	//System.out.println("------traceFolderOnServer---------"+traceFolderOnServer);
    	//System.out.println("------traceFolderPath---------"+traceFolderPath);
    	//tkprof = "cp "+traceFolderPath+traceFileName+" .;tkprof "+traceFolderPath+traceFileName+" "+tkpName+" sys=no sort=prsela,exeela,fchela";
    	tkprof = "cp "+traceFolderPath+traceFileName+" .;tkprof "+traceFileName+" "+tkpName+" sys=no sort=prsela,exeela,fchela"+";rm -f "+traceFileName;

    	finalCommand = envSet+";"+tkprof+";";
    	//System.out.println("----finalCommand------------"+finalCommand);
    	if(logFlag==1)
    	logger.info("----finalCommand---"+finalCommand);	
    	channel=session.openChannel("exec");
        channel.setInputStream(null);
        ((ChannelExec)channel).setErrStream(System.err);
        ((ChannelExec)channel).setCommand(finalCommand);
         InputStream in=channel.getInputStream();
         channel.connect();
        byte[] tmp=new byte[1024];
        
        while(true)
         {
        	if(logFlag==1)
            logger.info("----Entering Outer While loop--");
        	while(in.available()>0){
            int i=in.read(tmp, 0, 1024);
            traceGenerated=1;
            if(logFlag==1)
            logger.info("----i---"+i);
            if(i<0)break;
            if(logFlag==1)
            logger.info("----TKP File getting Genearted---"+new String(tmp, 0, i));
            //System.out.print(new String(tmp, 0, i));
          }
        	
        if(channel.isClosed()){
         break;
          }
       try{Thread.sleep(1000);}catch(Exception ee){}
        }
       if(traceGenerated==1)  
       System.out.println("-----TKP File Generated-----");
       else
       System.out.println("-----Exception in Genrating TKP File. Please check if your user has rights to run Tkprof command.-----");   
    }catch(Exception e){
    	
    	System.out.println("--Exception in Creating Tkprof File--"+e.getMessage());
    }
    if(logFlag==1)       
    	logger.info("-----Exiting createTKP----");
}
	
public void createFolderStructure()
{
	
	 if(logFlag==1)       
	 logger.info("-----Entering createFolderStructure----");
	Date date = new Date();
	DateFormat dateFormat = new SimpleDateFormat("dd-MMM-yy");
	
	String serverPath = traceLocation+fileSeparator+serverName.toUpperCase();
	String buPath = serverPath+fileSeparator+buName.toUpperCase();
	String datePath = buPath+fileSeparator+dateFormat.format(date).toUpperCase();
		
	if(customFlag==1)
	{	
		if(logFlag==1)
		{
			logger.info("----serverPath--"+serverPath);
			logger.info("----buPath--"+buPath);
			logger.info("----datePath--"+datePath);
			
		}
	File file = new File(serverPath);
    //It will check server name folder at location and if not exists, then will create it.
	if (!file.exists()) 
	{	
	if(file.mkdir())
	System.out.println("Created "+serverName.toUpperCase()+" folder at "+traceLocation);	
	else
	System.out.println("Unable to create "+serverName.toUpperCase()+" folder at "+traceLocation);
	}
	//It will check buName name folder at server folder and if not exists, then will create it.
	if (file.exists()) 
	{
	file = new File(buPath);
	if (!file.exists()) 
	{
	if(file.mkdir())
	System.out.println("Created "+buName.toUpperCase()+" folder at "+serverPath);	
	else
	System.out.println("Unable to create "+buName.toUpperCase()+" folder at "+serverPath);	
	}}
	//It will check Current Date folder at Script folder and if not exists, then will create it.
	if(file.exists())
	{	
	file = new File(datePath);
	if (!file.exists()) 
	{
	if(file.mkdir())
	System.out.println("Created "+dateFormat.format(date).toUpperCase()+" folder at "+buPath);	
	else
	System.out.println("Unable to create "+dateFormat.format(date).toUpperCase()+" folder at "+buPath);	
	}
	}
    if(file.exists())
    traceLocation = datePath;	
	
	if(logFlag==1)
	{
		logger.info("----traceLocation on creating folder--"+traceLocation);
		
	}}
	 if(logFlag==1)       
		 logger.info("-----Exiting createFolderStructure----");
}
	

public void getTKP(String nodeName,String tkpFileName )
{
	if(logFlag==1)
	{
	logger.info("---Entering getTKP---nodeName--"+nodeName+"----tkpFileName---"+tkpFileName);	
	}
	functionName = "getTKP";
	System.out.println("\nGetting Tkprof file to Local System");
	
	try
	 {   
		 createSession(nodeName);
		 createFolderStructure();
		 ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
         String destPath =traceLocation; 
		 sftpChannel.connect();
      	 if(logFlag==1)
		 {
         logger.info("----source file---"+tkpFileName+"----------destPath----"+destPath);
         
		 }
      	 sftpChannel.get(tkpFileName,destPath);
         sftpChannel.disconnect();

         System.out.println(tkpFileName+" file is generated at Path---"+destPath);   
         if(logFlag!=1)
         {	 
         
         try 
         {
        	 Runtime.getRuntime().exec("cmd /c start "+ destPath);
         } catch (Exception iae) {
            
        	System.out.println("Exception in opening Trace folder on desktop.") ;
         }
         }
	 }
	catch(Exception e)
	{   
		if(logFlag==1) 
		e.printStackTrace();
		System.out.println("-------Exception in getting TKP File---------"+e.getMessage());
	}
	if(logFlag==1)
	{
	logger.info("---Exiting getTKP-----");	
	}
}
public void releaseResources()
{   
	if(logFlag==1)
	{
	logger.info("---Entering releaseResources-----");	
	} 
	channel.disconnect();
    session.disconnect();
    if(logFlag==1)
	{
	logger.info("---Exiting releaseResources-----");	
	} 
}

public int createSession(String nameOfNode)
{
	if(logFlag==1)
	{
	logger.info("---Entering createSession-----nameOfNode--"+nameOfNode);	
	} 
	String[] parts = nodesDetailsList.get(nameOfNode).toString().split("\\;");
	int keyLogin= 0;
	int success = 0;

	try
	{
	nodeName=nameOfNode;
	hostName=parts[0];
	userid=parts[1];
	passwd=parts[2];
	
	// Fetching TraceLocationOnServer, EnvCmd from Properties file. In case if these are not present, these will be build dynamically.
	//System.out.println("------serverName----"+serverName);
	if(parts.length==3)
	{
		//System.out.println("----------parts lenth 3---------");
		String rawHostName= hostName.substring(0,hostName.indexOf('.'));
		envSet= ". /"+serverName+"db/db/11.2.0/"+nameOfNode+"_"+rawHostName+".env";
		traceFolderOnServer = "/"+serverName+traceFolderOnServerFromProp+serverName;
	}
	
	// If total 4 parameters are passed by user
	if(parts.length==4)
	{
		//System.out.println("----------parts lenth 4---------");
		// if fourth parameter is environment command, then assign to envSet variable and TraceLocation will be build using  TraceFolderOnServer variable in properties file.
		if(parts[3]!=null && parts[3].contains(".env"))
		{
		envSet = parts[3];	
		traceFolderOnServer = "/"+serverName+traceFolderOnServerFromProp+serverName;
		}
		else
		{	
			// it means fourth parameter is TraceLocation Path, it will be assigned to TraceFolder variable and environment command will be build dynamically..
		traceFolderOnServer = parts[3];
		String rawHostName= hostName.substring(0,hostName.indexOf('.'));
		envSet= ". /"+serverName+"db/db/11.2.0/"+nameOfNode+"_"+rawHostName+".env";
		}
	}
	// If total 5 parameters are passed by user
	if(parts.length==5)
	{
		//System.out.println("----------parts lenth 5---------"); 
		if(parts[3]!=null && parts[3].contains(".env"))
		 {
			envSet = parts[3];
		    traceFolderOnServer = parts[4]; 
		 }
		 else
		 {
		     envSet = parts[4];
			 traceFolderOnServer = parts[3]; 
		 }
	}
	
	JSch jsch = new JSch();
	
	if(logFlag==1)
	logger.info("Creating session for----"+nodeName+"---hostname---"+hostName+"---userid---"+userid+"---passowrd----"+passwd);
		
	if(passwd.equalsIgnoreCase("key"))
	{
		keyLogin = 1;
	}
	
	if(keyLogin==1)
	{	     
		if(functionName.equals("searchTraceFromAllNodes")) 
		System.out.println("Connecting via Key Node---"+nodeName+"  Host---"+hostName);
		session=jsch.getSession(userid, hostName);
		 session.setConfig("PreferredAuthentications", "publickey");
		 jsch.setKnownHosts(knownHosts);
		 jsch.addIdentity(privateKeyPath);
		 session.setConfig("StrictHostKeyChecking", "no");
		 session.connect();
	}
	else
	{	
		if(functionName.equals("searchTraceFromAllNodes"))
		System.out.println("Connecting   Node---"+nodeName+"  Host---"+hostName);
		session=jsch.getSession(userid, hostName, 22);
		session.setPassword(passwd);
		Properties config = new Properties();
		config.put("StrictHostKeyChecking", "no");
		session.setConfig(config);
		session.connect();
	}
	} 
	catch(Exception e)
	{
		if(logFlag==1) 
		e.printStackTrace();
		success=-1;
		if(e.getMessage().contains("java.net.UnknownHostException"))
		System.out.println("----Host fetched from properties file does not exists----"+hostName);
		else if(e.getMessage().contains("Auth fail")&& (keyLogin == 0))
		System.out.println("----Username/Password is not correct----User Name---"+userid+"---Password---"+passwd);
		else
		System.out.println("--Exception while creating session--"+e.getMessage());	
	}
	if(logFlag==1)
	{
	logger.info("---Exiting createSession-----success--"+success);	
	} 
	return(success);
}

public Logger writeLogs()
{
	if(logFlag==1)
	{
	logger.info("---Entering writeLogs-----");	
	}  
	Logger logger = Logger.getLogger("MyLog");
     FileHandler fh;
      
     try {
          
         // This block configure the logger with handler and formatter
         fh = new FileHandler("C:/TraceGetter/TGLogs.log");
         logger.addHandler(fh);
         logger.setUseParentHandlers(false);
         SimpleFormatter formatter = new SimpleFormatter();
         fh.setFormatter(formatter);
         
                           
     } catch (SecurityException e) {
         e.printStackTrace();
         
     } catch (IOException e) {
         e.printStackTrace();
     } 
     if(logFlag==1)
 	{
 	logger.info("---Exiting writeLogs-----");	
 	}  
     return(logger); 
   
}

public void createTKPOnOtherServer(String nodeName,String traceFileName )
{
	if(logFlag==1)
	{
	logger.info("---Entering createTKPOnOtherServer-----");	
	}  
	
	functionName = "createTKP"; 
	System.out.println("\nCreating Tkprof of Trace Files at other server---"+traceFileName+"---at Node---"+nodeName);
	try
	{System.out.println("-------1111---");
		if(createSession(nodeName)==0)
		{
			System.out.println("-------222---");
			// Changes done for Customization  - Start
		if(customFlag==1) 
		{	
		DateFormat dateFormat = new SimpleDateFormat("dd-MMM-yy_HH");
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		Date date = new Date();
		tkpName= serverName+"_"+dateFormat.format(date)+"_"+buName+"_"+prefix1+".tkp";
		}
		else
		{
		tkpName= prefix+traceFileName.replaceAll(".trc", "")+".tkp";	
		}
		// Changes done for Customization  - End
		System.out.println("-------222---");
    	String finalCommand;
				//envSet=cmd[3];
    	//traceFolderOnServer=cmd[4];
    	//System.out.println("------traceFolderOnServer---------"+traceFolderOnServer);
    	//System.out.println("------traceFolderPath---------"+traceFolderPath);
    	tkprof = "tkprof "+traceFileName+" "+tkpName+" sys=no sort=prsela,exeela,fchela";
    	   
    	finalCommand = envSet+";"+tkprof+";";
    	//System.out.println("----finalCommand------------"+finalCommand);
    	if(logFlag==1)
    	logger.info("----finalCommand---"+finalCommand);	
    	channel=session.openChannel("exec");
        channel.setInputStream(null);
        ((ChannelExec)channel).setErrStream(System.err);
        ((ChannelExec)channel).setCommand(finalCommand);
         InputStream in=channel.getInputStream();
         channel.connect();
        byte[] tmp=new byte[1024];
        
        while(true)
         {
        	if(logFlag==1)
            logger.info("----Entering Outer While loop--");
        	while(in.available()>0){
            int i=in.read(tmp, 0, 1024);
            traceGenerated=1;
            if(logFlag==1)
            logger.info("----i---"+i);
            if(i<0)break;
            if(logFlag==1)
            logger.info("----TKP File geeting Genearted---"+new String(tmp, 0, i));
            System.out.print(new String(tmp, 0, i));
          }
        	
        if(channel.isClosed()){
         break;
          }
       try{Thread.sleep(1000);}catch(Exception ee){}
        }
       if(traceGenerated==1)  
       System.out.println("-----TKP File Generated-----");
       else
       System.out.println("-----Exception in Genrating TKP File. Please check if your user has rights to run Tkprof command.-----");   
    }}catch(Exception e){
    	
    	System.out.println("--Exception in Creating Tkprof File--"+e.getMessage());
    }
    if(logFlag==1)
	{
	logger.info("---Exiting createTKPOnOtherServer-----");	
	}  
	
}

}
