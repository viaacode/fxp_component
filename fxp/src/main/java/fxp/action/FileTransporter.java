package fxp.action;

import java.io.IOException;
import java.net.SocketException;

import fxp.runnables.TransportRunnable;
import fxp.thread.IThreadManager;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import fxp.response.FXPResponse;
import fxp.thread.StorProcedure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.naming.AuthenticationException;

/*
 * @author:Bart Devis
 * @version: 1.0
 * @description: The FileTransporter helps to move a file from a ftp-server to another ftp-server
 * 				 To use the method Transport() you need to activate FXP on both servers
 * 
 */
public class FileTransporter {

    private static final Logger logger = LogManager.getLogger(FileTransporter.class);

    public String sourceHost;
    public String destinationHost;
    public String sourceUser;
    public String destinationUser;
    public int sourcePort = 21;
    public int destinationPort = 21;
    public String sourcePassword;
    public String destinationPassword;

    public static final String STATUS_NOK = "NOK";
    public static final String STATUS_OK = "OK";
    public static final String ACTIE = "FXP";
    public static final String MOV_ACTIE = "MOV";

    private static final class Lock {
    }
    private final Object lock = new Lock();
    private IThreadManager manager;


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
            String destinationPassword, IThreadManager manager) {
        super();
        this.sourceHost = sourceHost;
        this.destinationHost = destinationHost;
        this.sourceUser = sourceUser;
        this.destinationUser = destinationUser;
        this.sourcePassword = sourcePassword;
        this.destinationPassword = destinationPassword;
        this.manager = manager;

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
    public FXPResponse transport(String sourcePath, String sourceFilename, String destinationPath, String destinationFilename, boolean move) throws Exception {
        FXPResponse response = new FXPResponse();

        response.setId(this.getId(destinationFilename));
        response.setActie(FileTransporter.ACTIE);
        this.checkData(response);

        if (response.getStatus() == FileTransporter.STATUS_NOK) {
            return response;
        }

        Runnable r = new TransportRunnable(sourcePath, sourceFilename, destinationPath, destinationFilename, move, this, manager);
        manager.runThread(r);

        response.setStatus(STATUS_OK);
        response.setMessage("File is being transferred");

        return response;
    }

    public FXPResponse moveFile(String sourcePath, String sourceFilename, String destinationPath, String destinationFilename) {
        logger.info("Request for moving a file received");
        FXPResponse response = new FXPResponse();
        response.setId(this.getId(destinationFilename));
        response.setActie(FileTransporter.MOV_ACTIE);
        this.checkData(response);

        if (response.getStatus() == FileTransporter.STATUS_NOK) {
            return response;
        }

        FTPClient ftp1 = new FTPClient();

        try {
            this.doFileCheck(sourcePath.replace("\\", ""), sourceFilename, response);
            if (response.getStatus() == FileTransporter.STATUS_NOK) {
                return response;

            }
            ftp1.connect(this.destinationHost, this.destinationPort);
            logger.info("Connected to FTP server");

            if (!ftp1.login(this.destinationUser, this.destinationPassword)) {
                logger.error("ftp login false");
                throw new AuthenticationException("FTP login was incorrect");
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

            logger.info("File move requested");

            thread.join();
            thread2.join();

            response.setStatus(FileTransporter.STATUS_OK);
            response.setMessage("The file " + sourceFilename + " has been moved as file " + destinationFilename + " on the destination " + this.destinationHost);
            logger.info("The file " + sourceFilename + " has been moved as file " + destinationFilename + " on the destination " + this.destinationHost);
        } catch (IOException e) {
            logger.error("IOException occurred");
            e.printStackTrace();
            response.setStatus(FileTransporter.STATUS_NOK);
            response.setMessage("There was an error within an IO-action: " + e.getMessage() + ". INFO SOURCE Filename: " + sourceFilename + ". FilePath: " + sourcePath);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            try {
                ftp1.disconnect();
                logger.info("Disconnected");
            } catch (IOException e) {
                e.printStackTrace();
                response.setStatus(FileTransporter.STATUS_NOK);
                response.setMessage("There was an error when closing the ftp-connection : " + e.getMessage() + ". INFO SOURCE Filename: " + sourceFilename + ". FilePath: " + sourcePath);
                logger.error("There was an error when closing the ftp-connection : " + e.getMessage() + ". INFO SOURCE Filename: " + sourceFilename + ". FilePath: " + sourcePath);
            }
        }
        
        return response;
    }

    public FXPResponse deleteFile(String sourcePath, String sourceFilename) throws SocketException, IOException {
        FTPClient ftpClient = new FTPClient();
        FXPResponse response = new FXPResponse();
        
        try {
            ftpClient.connect(this.sourceHost, this.destinationPort);
            ftpClient.login(this.sourceUser, this.sourcePassword);

            ftpClient.changeWorkingDirectory(sourcePath);
            FTPFile[] files = ftpClient.listFiles(sourceFilename);

            if (files.length == 0) {
                response.setStatus(FileTransporter.STATUS_NOK);
                response.setMessage("File does not exist");

            } else {
                ftpClient.deleteFile(sourceFilename);
                response.setStatus(FileTransporter.STATUS_OK);
                response.setMessage(sourcePath + "/" + sourceFilename + " has been deleted");
            }
        } finally {
            ftpClient.disconnect();
        }
        
        return response;
    }

    private void checkData(FXPResponse response) {
        if ((this.sourceHost.isEmpty() || this.destinationHost.isEmpty()) || (this.sourceHost == "" || this.destinationHost == "")) {
            response.setStatus(FileTransporter.STATUS_NOK);
            response.setMessage("No hostname for the source or/and destination.");
        }

        if ((this.sourceUser.isEmpty() || this.destinationHost.isEmpty()) || (this.sourceUser == "" || this.destinationHost == "")) {
            response.setStatus(FileTransporter.STATUS_NOK);
            response.setMessage("No user for the source or/and destination.");
        }

        if ((this.sourcePassword.isEmpty() || this.destinationPassword.isEmpty()) || (this.sourcePassword == "" || this.destinationPassword == "")) {
            response.setStatus(FileTransporter.STATUS_NOK);
            response.setMessage("No password for the source or/and destination.");
        }
    }

    public boolean doFileCheck(String sourcePath, String sourceFilename, FXPResponse response) throws SocketException, IOException {
        FTPClient ftpClient = new FTPClient();
        boolean file_exists_and_not_empty = false;
        
        try {
            ftpClient.connect(this.sourceHost, this.destinationPort);
            ftpClient.login(this.sourceUser, this.sourcePassword);

            ftpClient.changeWorkingDirectory(sourcePath);
            FTPFile[] files = ftpClient.listFiles(sourceFilename);

            if (files.length == 0) {
                response.setStatus(FileTransporter.STATUS_NOK);
                response.setMessage("File does not exist");
            } else if (files[0].getSize() == 0) {
                response.setStatus(FileTransporter.STATUS_NOK);
                response.setMessage("File is empty");
            } else {
                response.setStatus(FileTransporter.STATUS_OK);
                file_exists_and_not_empty = true;
            }
        } finally {
            ftpClient.disconnect();
            return file_exists_and_not_empty;
        }
    }

    private String getId(String sourceFile) {
        int location = sourceFile.indexOf(".");
        return sourceFile.substring(0, (location == -1 ? sourceFile.length() - 1 : location));
    }

    public String getPorts(String reply) {
        int location = reply.indexOf('(');
        int location2 = reply.indexOf(')');
        return reply.substring(location + 1, location2);
    }

    public boolean filesizeEqual(String sourcePath, String sourceFilename, String destinationPath, String destinationFilename) throws IOException {
        long[] sizes = getFileSizes(sourcePath, sourceFilename, destinationPath, destinationFilename);
        return filesizeEqual(sizes);
    }

    public boolean filesizeEqual(long sourceSize, String destinationPath, String destinationFilename) throws IOException {
        long size = getFileSize(destinationPath, destinationFilename);
        long[] sizes = {sourceSize, size};
        return filesizeEqual(sizes);
    }

    public long getFileSize(String destinationPath, String destinationFilename) throws IOException {
        FTPClient ftpClient1 = new FTPClient();
        try {
            ftpClient1.connect(this.destinationHost, this.destinationPort);
            ftpClient1.login(this.destinationUser, this.destinationPassword);
            ftpClient1.changeWorkingDirectory(destinationPath);
            FTPFile[] files1 = ftpClient1.listFiles(destinationFilename);
            if (files1.length == 0) {
                throw new IOException("File size check failed, file doesn't exist anymore.");
            } else {
                return files1[0].getSize();
            }
        } finally {
            ftpClient1.disconnect();
        }
    }

    public boolean filesizeEqual(long[] sizes) {
        if (sizes[0] != sizes[1]) {
            return false;
        }
        return true;
    }

    public long[] getFileSizes(String sourcePath, String sourceFilename, String destinationPath, String destinationFilename) throws IOException {
        long[] sizes = new long[2];
        
        FTPClient ftpClient1 = new FTPClient();
        FTPClient ftpClient2 = new FTPClient();
        
        try {

            ftpClient1.connect(this.sourceHost, this.sourcePort);
            ftpClient2.connect(this.destinationHost, this.destinationPort);

            ftpClient1.login(this.sourceUser, this.sourcePassword);
            ftpClient2.login(this.destinationUser, this.destinationPassword);

            ftpClient1.changeWorkingDirectory(sourcePath);
            ftpClient2.changeWorkingDirectory(destinationPath);

            FTPFile[] files1 = ftpClient1.listFiles(sourceFilename);
            FTPFile[] files2 = ftpClient2.listFiles(destinationFilename);

            if (files1.length == 0 || files2.length == 0) {
                throw new IOException("Transfer failed. One of the files doesn't exist anymore.");

            } else {
                sizes[0] = files1[0].getSize();
                sizes[1] = files2[0].getSize();
            }
            return sizes;

        } finally {
            ftpClient1.disconnect();
            ftpClient2.disconnect();
        }
    }

    public boolean checkFreeSpace(String destinationPath, int maxFilesOnLocation) throws SocketException, IOException {
        FTPClient ftpClient = new FTPClient();
        boolean result = false;
        try {

            ftpClient.connect(this.destinationHost, this.destinationPort);
            ftpClient.login(this.destinationUser, this.destinationPassword);

            ftpClient.changeWorkingDirectory(destinationPath);
            FTPFile[] files = ftpClient.listFiles();

            if (files.length < maxFilesOnLocation) {
                result = true;

            }
        } finally {
            ftpClient.disconnect();
        }

        return result;
    }

    public void renameFile(String destinationPath, String destinationFileName, String originalDestinationFileName) throws SocketException, IOException {
        FTPClient ftpClient = new FTPClient();
        boolean result = false;
        
        try {
            ftpClient.connect(this.destinationHost, this.destinationPort);
            ftpClient.login(this.destinationUser, this.destinationPassword);

            ftpClient.changeWorkingDirectory(destinationPath);
            FTPFile[] files = ftpClient.listFiles();

            ftpClient.rename(destinationFileName, originalDestinationFileName);
        } finally {
            ftpClient.disconnect();
        }
    }

    public static void ftpCreateDirectoryTree(FTPClient client, String dirTree) throws IOException {
        String tempTree = dirTree; // /vrt/temp

        while (!client.changeWorkingDirectory(tempTree)) {
            tempTree = tempTree.substring(0, tempTree.lastIndexOf("/"));// /vrt
            logger.info("Temptree: " + tempTree);
        }

        String toCreateDirs = dirTree.replaceFirst(tempTree, "");

        if (!toCreateDirs.equals("")) {
            logger.info("To create directories: " + toCreateDirs);
        }

        String[] directories = toCreateDirs.split("/");
        boolean dirExists = true;
        for (String dir : directories) {
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
