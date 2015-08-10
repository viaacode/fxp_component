package fxp.thread;

import java.io.IOException;

import org.apache.commons.net.ftp.*;


public class StorProcedure implements Runnable{
	
	FTPClient ftpClient;
	String command;

	public StorProcedure(FTPClient ftp, String command){
		ftpClient = ftp;
		this.command = command;
	}

	public void run() {
		
		try {
			ftpClient.sendCommand(command);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
