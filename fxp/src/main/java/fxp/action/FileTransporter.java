package fxp.action;


import java.io.IOException;

import org.apache.commons.net.ftp.FTPClient;

import fxp.response.FXPResponse;
import fxp.thread.StorProcedure;

/*
 * @author:Bart Devis
 * @version: 1.0
 * @description: The FileTransporter helps to move a file from a ftp-server to another ftp-server
 * 				 To use the method Transport() you need to activate FXP on both servers
 * 
 */

public class FileTransporter {

	
	private String sourceHost;
	private String destinationHost;
	private String sourceUser;
	private String destinationUser;
	private int sourcePort = 21;
	private int destinationPort = 21;
	private String sourcePassword;
	private String destinationPassword;
	
	public static final String STATUS_NOK = "NOK";
	public static final String STATUS_OK = "OK";
	public static final String ACTIE = "FXP";
	
	
	/*
	 * Empty constructor for FileTransporter
	 */
	public FileTransporter() {
		super();
	}
	
	
/*
 * Constructor for FileTransporter
 * You can give all the information that the class need to make a connection
 * @param: String sourceHost, String destinationHost,
			String sourceUser, String destinationUser, int sourcePort,
			int destinationPort, String sourcePassword,
			String destinationPassword
 * 
 */

	public FileTransporter(String sourceHost, String destinationHost,
			String sourceUser, String destinationUser, String sourcePassword,
			String destinationPassword) {
		super();
		this.sourceHost = sourceHost;
		this.destinationHost = destinationHost;
		this.sourceUser = sourceUser;
		this.destinationUser = destinationUser;
		this.sourcePassword = sourcePassword;
		this.destinationPassword = destinationPassword;
	}




	/*
	 * get the hostname of the source 
	 * @return String: hostname source
	 */
	public String getSourceHost() {
		return sourceHost;
	}


	/*
	 * set the hostname of the source 
	 * @param String: hostname source
	 */
	public void setSourceHost(String sourceHost) {
		this.sourceHost = sourceHost;
	}


	/*
	 * get the hostname of the destination
	 * @return String: hostname destination
	 */
	public String getDestinationHost() {
		return destinationHost;
	}

	/*
	 * set the hostname of the destination
	 * @param String: hostname destination
	 */
	public void setDestinationHost(String destinationHost) {
		this.destinationHost = destinationHost;
	}

	/*
	 * get the user of the source 
	 * @return String: user source
	 */
	public String getSourceUser() {
		return sourceUser;
	}

	/*
	 * set the user of the source 
	 * @param String: user source
	 */
	public void setSourceUser(String sourceUser) {
		this.sourceUser = sourceUser;
	}


	/*
	 * get the user of the destination 
	 * @return String: user destination
	 */
	public String getDestinationUser() {
		return destinationUser;
	}

	/*
	 * set the user of the destination 
	 * @param String: user destination
	 */
	public void setDestinationUser(String destinationUser) {
		this.destinationUser = destinationUser;
	}

	

	/*
	 * get the port of the source 
	 * @return int: port source
	 */
	public int getSourcePort() {
		return sourcePort;
	}

	/*
	 * set the port of the source 
	 * @param int: port source (default 21)
	 */
	public void setSourcePort(int sourcePort) {
		this.sourcePort = sourcePort;
	}

	/*
	 * get the port of the destination 
	 * @return int: port destination 
	 */
	public int getDestinationPort() {
		return destinationPort;
	}


	/*
	 * get the port of the destination 
	 * @param String: port destination (default 21)
	 */
	public void setDestinationPort(int destinationPort) {
		this.destinationPort = destinationPort;
	}

	/*
	 * get the password of the source 
	 * @return String: password source
	 */
	public String getSourcePassword() {
		return sourcePassword;
	}

	/*
	 * set the password of the source 
	 * @param String: password source
	 */
	public void setSourcePassword(String sourcePassword) {
		this.sourcePassword = sourcePassword;
	}

	/*
	 * get the password of the destination 
	 * @return String: password destination
	 */
	public String getDestinationPassword() {
		return destinationPassword;
	}


	/*
	 * set the password of the destination 
	 * @param String: password destination
	 */
	public void setDestinationPassword(String destinationPassword) {
		this.destinationPassword = destinationPassword;
	}


	/*This method will transport a file from a ftp-server to another ftp-server.
	 *FXP needs to be activated on the two servers.
	@param none
	@return boolean true= file is copied, false=file is not copied 
	@throws IOException - if the method can not do a command on the servers*/
	public FXPResponse Transport(String sourcePath,String sourceFilename,String destinationPath,String destinationFilename){
		
		FXPResponse response = new FXPResponse();
		response.setId(this.getId(destinationFilename));
		response.setActie(FileTransporter.ACTIE);
		this.checkData(response);
		
		if(response.getStatus() == FileTransporter.STATUS_NOK){
			return response;
		}
	
		
		FTPClient ftp1 = new FTPClient();
		FTPClient ftp2 = new FTPClient();
		
		try{
		
		ftp1.connect(this.destinationHost, this.destinationPort);
		ftp2.connect(this.sourceHost, this.sourcePort);
		
		if(!ftp1.login(this.destinationUser, this.destinationPassword)){
				System.out.println("ftp1 login false");
		}
		if(!ftp2.login(this.sourceUser, this.sourcePassword)){
			System.out.println("ftp2 login false");
		}
			
		ftp1.changeWorkingDirectory(destinationPath);
		ftp2.changeWorkingDirectory(sourcePath);
		
		ftp1.sendCommand("TYPE I");
		ftp2.sendCommand("TYPE I");
		
		ftp1.sendCommand("PASV");
		
		String reply = ftp1.getReplyString();;
		ftp2.sendCommand("PORT " + this.getPorts(reply));
		
		
		StorProcedure storPro = new StorProcedure(ftp1, "STOR " + destinationFilename);
		StorProcedure retrProdecure = new StorProcedure(ftp2, "RETR " + sourceFilename);
		
		/*if(ftp1.sendCommand("STOR " + destinationFilename) == 550){
			response.setStatus(FileTransporter.STATUS_NOK);
			response.setMessage("no rights");
			throw new Exception();
		};*/
		
		
		//ftp2.sendCommand("RETR " + sourceFilename);
		
		Thread thread = new Thread(storPro);
		Thread thread2 = new Thread(retrProdecure);
		
		thread.start();
		thread2.start();
		
		thread.join();
		thread2.join();
		
		
		response.setStatus(FileTransporter.STATUS_OK);
		response.setMessage("The file " + sourceFilename + " has been copied as file " + destinationFilename + "on the destination " + this.destinationHost  );
		
		
		}catch(IOException e){
			e.printStackTrace();
			response.setStatus(FileTransporter.STATUS_NOK);
			response.setMessage("There was an error within an IO-action: " + e.getMessage() + ". INFO SOURCE Filename: " + sourceFilename + ". FilePath: " + sourcePath);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			try {
				ftp1.disconnect();
				ftp2.disconnect();
			} catch (IOException e) {
				e.printStackTrace();
				response.setStatus(FileTransporter.STATUS_NOK);
				response.setMessage("There was an error when closing de ftp-connection : " + e.getMessage() + ". INFO SOURCE Filename: " + sourceFilename + ". FilePath: " + sourcePath);
				
			}
			
		}
		
		return response;
		
		
	}

	public FXPResponse Transport2(String sourcePath,String sourceFilename,String destinationPath,String destinationFilename){
		
		FXPResponse response = new FXPResponse();
		response.setId(this.getId(destinationFilename));
		response.setActie(FileTransporter.STATUS_OK);
		this.checkData(response);
		
		if(response.getStatus() == FileTransporter.STATUS_NOK){
			return response;
		}
		
		
		
		FTPClient ftp1 = new FTPClient();
		FTPClient ftp2 = new FTPClient();
		
		try{
			
			ftp1.connect(this.destinationHost, this.destinationPort);
			ftp2.connect(this.sourceHost, this.sourcePort);
			
			if(!ftp1.login(this.destinationUser, this.destinationPassword)){
					System.out.println("ftp1 login false");
			}
			if(!ftp2.login(this.sourceUser, this.sourcePassword)){
				System.out.println("ftp2 login false");
			}
				
			ftp1.changeWorkingDirectory(destinationPath);
			ftp2.changeWorkingDirectory(sourcePath);
			
			
			ftp1.setFileType(FTPClient.BINARY_FILE_TYPE);
			ftp2.setFileType(FTPClient.BINARY_FILE_TYPE);
			
			//ftp1.pasv();
			//String reply = ftp1.getReplyString();;
			
			ftp1.enterRemotePassiveMode();
			ftp2.port(ftp1.getRemoteAddress(), ftp2.getPassivePort());
			
			//ftp2.sendCommand("PORT " + this.getPorts(reply));
			
			
			
			if(ftp1.stor(destinationFilename) == 550){
				response.setStatus(FileTransporter.STATUS_NOK);
				response.setMessage("The user '" + this.destinationUser + "' has no rights to write on destination: " + this.destinationHost);
			};
			ftp2.retr(sourceFilename);
			
			
			
			//StorProcedure storPro = new StorProcedure(ftp1, "STOR " + destinationFilename);
			//StorProcedure retrProdecure = new StorProcedure(ftp2, "RETR " + sourceFilename);
			
			/*Thread thread = new Thread(storPro);
			Thread thread2 = new Thread(retrProdecure);
			
			thread.start();
			thread2.start();*/
			
		}catch(IOException e){
			e.printStackTrace();
			response.setStatus(FileTransporter.STATUS_NOK);
			response.setMessage("There was an error within an IO-action: " + e.getMessage() + ". INFO SOURCE Filename: " + sourceFilename + ". FilePath: " + sourcePath);
		}finally{
			try {
				ftp1.disconnect();
				ftp2.disconnect();
			} catch (IOException e) {
				e.printStackTrace();
				response.setStatus(FileTransporter.STATUS_NOK);
				response.setMessage("There was an error when closing de ftp-connection : " + e.getMessage() + ". INFO SOURCE Filename: " + sourceFilename + ". FilePath: " + sourcePath);
				
			}
			
		}
		
		response.setStatus(FileTransporter.STATUS_OK);
		response.setMessage("The file " + sourceFilename + " has been copied as file " + destinationFilename + "on the destination " + this.destinationHost  );
		return response;
	}
	
	private FXPResponse checkData(FXPResponse response){
		
		if((this.sourceHost.isEmpty() || this.destinationHost.isEmpty())||(this.sourceHost == "" || this.destinationHost == "")){
			response.setStatus(FileTransporter.STATUS_NOK);
			response.setMessage("No hostname for the source or/and destination.");
		}
		
		if((this.sourceUser.isEmpty() || this.destinationHost.isEmpty())||(this.sourceUser == "" || this.destinationHost == "")){
			response.setStatus(FileTransporter.STATUS_NOK);
			response.setMessage("No user for the source or/and destination.");
		}
		
		
		if((this.sourcePassword.isEmpty() || this.destinationPassword.isEmpty())||(this.sourcePassword == "" || this.destinationPassword == "")){
			response.setStatus(FileTransporter.STATUS_NOK);
			response.setMessage("No password for the source or/and destination.");
		}
		
		
		return response;
	}
	
	private String getId(String sourceFile){
		int location = sourceFile.indexOf(".");
		return sourceFile.substring(0, location);
	}
	
	private String getPorts(String reply){
		int location = reply.indexOf('(');
		int location2 = reply.indexOf(')');
		return reply.substring(location+1, location2);
	}
}
