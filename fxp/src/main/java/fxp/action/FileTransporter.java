package fxp.action;



import java.io.IOException;
import java.net.SocketException;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;


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
	public static final String MOV_ACTIE= "MOV";


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
	public FXPResponse transport(String sourcePath,String sourceFilename,String destinationPath,String destinationFilename,boolean move){

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
			this.doFileCheck(sourcePath, sourceFilename,response);
			if(response.getStatus() == FileTransporter.STATUS_NOK){
				return response;

			}
			ftp1.connect(this.destinationHost, this.destinationPort);
			ftp2.connect(this.sourceHost, this.sourcePort);

			if(!ftp1.login(this.destinationUser, this.destinationPassword)){
				System.out.println("ftp1 login false");
			}
			if(!ftp2.login(this.sourceUser, this.sourcePassword)){
				System.out.println("ftp2 login false");
			}

			ftpCreateDirectoryTree(ftp1, destinationPath);
			ftpCreateDirectoryTree(ftp2, sourcePath);

			/*
			ftp1.changeWorkingDirectory(destinationPath);
			ftp2.changeWorkingDirectory(sourcePath);
			*/
			ftp1.sendCommand("TYPE I");
			ftp2.sendCommand("TYPE I");
			ftp1.sendCommand("PASV");

			String reply = ftp1.getReplyString();;
			ftp2.sendCommand("PORT " + this.getPorts(reply));

			StorProcedure storPro = new StorProcedure(ftp1, "STOR " + destinationFilename);
			StorProcedure retrProdecure = new StorProcedure(ftp2, "RETR " + sourceFilename);

			Thread thread = new Thread(storPro);
			Thread thread2 = new Thread(retrProdecure);

			thread.start();
			thread2.start();

			thread.join();
			thread2.join();


			if (!this.filesizeEqual(sourcePath, sourceFilename, destinationPath, destinationFilename)) {
				response.setMessage("Files weren't of the same size. Transfer failed.");
				response.setStatus(STATUS_NOK);
			} else {
				if (move) {
					this.deleteFile(sourcePath, sourceFilename);
				}
			}

			response.setStatus(FileTransporter.STATUS_OK);
			response.setMessage("The file " + sourceFilename + " has been copied as file " + destinationFilename + " on the destination " + this.destinationHost  );
		}catch(IOException e){
			e.printStackTrace();
			response.setStatus(FileTransporter.STATUS_NOK);
			response.setMessage("There was an error within an IO-action: " + e.getMessage() + ". INFO SOURCE Filename: " + sourceFilename + ". FilePath: " + sourcePath);
		} catch (Exception e) {
			// TODO Auto-generated catch block
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
		return response;
	}

	public FXPResponse moveFile(String sourcePath, String sourceFilename, String destinationPath, String destinationFilename) {

		FXPResponse response = new FXPResponse();
		response.setId(this.getId(destinationFilename));
		response.setActie(FileTransporter.MOV_ACTIE);
		this.checkData(response);

		if(response.getStatus() == FileTransporter.STATUS_NOK){
			return response;
		}

		FTPClient ftp1 = new FTPClient();

		try{
			this.doFileCheck(sourcePath.replace("\\", ""), sourceFilename, response);
			if(response.getStatus() == FileTransporter.STATUS_NOK){
				return response;

			}
			ftp1.connect(this.destinationHost, this.destinationPort);

			if(!ftp1.login(this.destinationUser, this.destinationPassword)){
				System.out.println("ftp1 login false");
			}

			//ftp1.changeWorkingDirectory(destinationPath);
			ftp1.sendCommand("TYPE I");
			//ftp1.sendCommand("PASV");


			String reply = ftp1.getReplyString();;

			StorProcedure storPro = new StorProcedure(ftp1, "RNFR " + sourcePath + sourceFilename);
			StorProcedure retrProdecure = new StorProcedure(ftp1, "RNTO " + destinationPath + destinationFilename);

			Thread thread = new Thread(storPro);
			Thread thread2 = new Thread(retrProdecure);


			thread.start();
			thread2.start();

			thread.join();
			thread2.join();

			response.setStatus(FileTransporter.STATUS_OK);
			response.setMessage("The file " + sourceFilename + " has been moved as file " + destinationFilename + " on the destination " + this.destinationHost  );
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
			} catch (IOException e) {
				e.printStackTrace();
				response.setStatus(FileTransporter.STATUS_NOK);
				response.setMessage("There was an error when closing de ftp-connection : " + e.getMessage() + ". INFO SOURCE Filename: " + sourceFilename + ". FilePath: " + sourcePath);

			}

		}
		return response;
	}



	private void checkData(FXPResponse response){

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



	}

	private void doFileCheck(String sourcePath,String sourceFilename, FXPResponse response) throws SocketException, IOException{

		FTPClient ftpClient = new FTPClient();
		try{

			ftpClient.connect(this.sourceHost, this.destinationPort);
			ftpClient.login(this.sourceUser, this.sourcePassword);

			ftpClient.changeWorkingDirectory(sourcePath);
			FTPFile[] files = ftpClient.listFiles(sourceFilename);

			if (files.length == 0){
				response.setStatus(FileTransporter.STATUS_NOK);
				response.setMessage("File does not exist");

			}else if (files[0].getSize() == 0){
				response.setStatus(FileTransporter.STATUS_NOK);
				response.setMessage("File is empty");
			}

		}finally{
			ftpClient.disconnect();
		}

	}

	public boolean checkFreeSpace(String destinationPath, int maxFilesOnLocation) throws SocketException, IOException{

		FTPClient ftpClient = new FTPClient();
		boolean result = false;
		try{

			ftpClient.connect(this.destinationHost, this.destinationPort);
			ftpClient.login(this.destinationUser, this.destinationPassword);

			ftpClient.changeWorkingDirectory(destinationPath);
			FTPFile[] files = ftpClient.listFiles();

			if (files.length < maxFilesOnLocation){
				result = true;

			}
		}finally{
			ftpClient.disconnect();
		}

		return result;
	}

	private String getId(String sourceFile){
		int location = sourceFile.indexOf(".");
		return sourceFile.substring(0, (location == -1 ? sourceFile.length() - 1 : location));
	}

	private String getPorts(String reply){
		int location = reply.indexOf('(');
		int location2 = reply.indexOf(')');
		return reply.substring(location+1, location2);
	}

	private boolean filesizeEqual(String sourcePath, String sourceFilename, String destinationPath, String destinationFilename) throws IOException {
		FTPClient ftpClient1 = new FTPClient();
		FTPClient ftpClient2 = new FTPClient();
		try{

			ftpClient1.connect(this.sourceHost, this.sourcePort);
			ftpClient2.connect(this.destinationHost, this.destinationPort);

			ftpClient1.login(this.sourceUser, this.sourcePassword);
			ftpClient2.login(this.destinationUser, this.destinationPassword);

			ftpClient1.changeWorkingDirectory(sourcePath);
			ftpClient2.changeWorkingDirectory(destinationPath);


			FTPFile[] files1 = ftpClient1.listFiles(sourceFilename);
			FTPFile[] files2 = ftpClient2.listFiles(destinationFilename);

			if (files1.length == 0 || files2.length == 0){
				throw new IOException("Transfer failed. One of the files doesn't exist anymore.");

			}else{
				if (files1[0].getSize() == files2[0].getSize()) return true;
			}
			return false;

		}finally{
			ftpClient1.disconnect();
			ftpClient2.disconnect();
		}
	}

	private void deleteFile(String sourcePath, String sourceFilename) throws IOException {
		FTPClient ftpClient = new FTPClient();
		ftpClient.connect(this.sourceHost, this.sourcePort);
		ftpClient.login(this.sourceUser, this.sourcePassword);
		ftpClient.changeWorkingDirectory(sourcePath);
		ftpClient.deleteFile(sourceFilename);
	}

	private static void ftpCreateDirectoryTree( FTPClient client, String dirTree ) throws IOException {

		boolean dirExists = true;

		//tokenize the string and attempt to change into each directory level.  If you cannot, then start creating.
		String[] directories = dirTree.replaceFirst(client.printWorkingDirectory(), "").split("/");
		for (String dir : directories ) {
			if (dir != null && !dir.isEmpty()) {
				if (!dir.isEmpty()) {
					if (dirExists) {
						dirExists = client.changeWorkingDirectory(dir);
					}
					if (!dirExists) {
						if (!client.makeDirectory(dir)) {
							throw new IOException("Unable to create remote directory '" + dir + "'.  error='" + client.getReplyString() + "'");
						}
						if (!client.changeWorkingDirectory(dir)) {
							throw new IOException("Unable to change into newly created remote directory '" + dir + "'.  error='" + client.getReplyString() + "'");
						}
					}
				}
			}
		}
	}
}
