package fxp.runnables;

import fxp.action.FileTransporter;
import fxp.response.FXPResponse;
import fxp.thread.IThreadManager;
import fxp.thread.StorProcedure;
import java.io.IOException;
import javax.naming.AuthenticationException;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by dieter on 02/02/2016.
 */
public class TransportRunnable implements Runnable {

    private static final Logger logger = LogManager.getLogger(TransportRunnable.class);
    private String sourcePath;
    private String sourceFilename;
    private String destinationPath;
    private String destinationFilename;
    private boolean move;
    private FileTransporter transporter;
    private IThreadManager observer;

    private FXPResponse response = new FXPResponse();

    private FTPClient ftp1 = new FTPClient();
    private FTPClient ftp2 = new FTPClient();

    int MAX_RETRY = 10; // when exception happens, retry transfer

    private static final class Lock {
    }
    private final Object lock = new Lock();

    public TransportRunnable(String sourcePath, String sourceFilename, String destinationPath, String destinationFilename, boolean move, FileTransporter transporter, IThreadManager observer) {
        this.sourcePath = sourcePath;
        this.sourceFilename = sourceFilename;
        this.destinationPath = destinationPath;
        this.destinationFilename = destinationFilename;
        this.move = move;
        this.transporter = transporter;
        this.observer = observer;
    }

    @Override
    public void run() {
        try {
            int retry = 0;

            while (!transferData() && retry < MAX_RETRY) {
                retry++;
            }
        } catch (Exception e) {
            logger.info("Exception happened while transferring data.");
            e.printStackTrace();
        } finally {
            try {
                observer.notifyFinish(this);
                ftp1.disconnect();
                ftp2.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                response.setStatus(FileTransporter.STATUS_NOK);
                response.setMessage("There was an error when closing the ftp-connection : " + e.getMessage() + ". INFO SOURCE Filename: " + sourceFilename + ". FilePath: " + sourcePath);
                logger.info("There was an error when closing the ftp-connection : " + e.getMessage() + ". INFO SOURCE Filename: " + sourceFilename + ". FilePath: " + sourcePath);
            }
        }

        logger.info("TRANSFER DONE");
    }

    private boolean transferData() {
        synchronized (lock) {
            try {
                logger.info("Starting file transfer for files:");
                logger.info("Source:\t\t" + sourcePath + "/" + sourceFilename);
                logger.info("Destination:\t\t" + destinationPath + "/" + destinationFilename);
                String originalDestinationFileName = destinationFilename;
                destinationFilename = originalDestinationFileName + ".part";

                transporter.doFileCheck(sourcePath, sourceFilename, response);
                if (response.getStatus() == FileTransporter.STATUS_NOK) {
                    return false;
                }
                ftp1.connect(transporter.destinationHost, transporter.destinationPort);
                logger.info("Connected to server " + transporter.getSourceHost());
                ftp2.connect(transporter.sourceHost, transporter.sourcePort);
                logger.info("Connected to server " + transporter.getDestinationHost());

                if (!ftp1.login(transporter.destinationUser, transporter.destinationPassword)) {
                    logger.error("ftp1 login false");
                    throw new AuthenticationException("FTP1 login was incorrect");
                }
                if (!ftp2.login(transporter.sourceUser, transporter.sourcePassword)) {
                    logger.error("ftp1 login false");
                    throw new AuthenticationException("FTP2 login was incorrect");
                }

                transporter.ftpCreateDirectoryTree(ftp1, destinationPath);
                transporter.ftpCreateDirectoryTree(ftp2, sourcePath);


                /*
                            ftp1.changeWorkingDirectory(destinationPath);
                            ftp2.changeWorkingDirectory(sourcePath);
                 */
                ftp1.sendCommand("TYPE I");
                ftp2.sendCommand("TYPE I");
                ftp1.sendCommand("PASV");

                String reply = ftp1.getReplyString();
                ftp2.sendCommand("PORT " + transporter.getPorts(reply));

                StorProcedure storPro = new StorProcedure(ftp1, "STOR " + destinationFilename);
                StorProcedure retrProdecure = new StorProcedure(ftp2, "RETR " + sourceFilename);

                Thread thread = new Thread(storPro);
                Thread thread2 = new Thread(retrProdecure);

                thread.start();
                thread2.start();

                logger.info("Started file transfer.");

                // throw new Exception("something went wrong");
                int count = 0;
                long previousSize = 0;
                int identicalSizeCount = 0;
                long[] filesizes = transporter.getFileSizes(sourcePath, sourceFilename, destinationPath, destinationFilename);
                long sourcefilesize = filesizes[0];

                // As long as transferring data makes progress
                while (!transporter.filesizeEqual(filesizes) && identicalSizeCount < 10) {
                    lock.wait(30000);
                    try {
                        filesizes[1] = transporter.getFileSize(destinationPath, destinationFilename);
                    } catch (IOException e) {
                        logger.error("IOException occurred when getting file sizes. Continuing to poll.");
                    }
                    logger.info("Progress for file " + sourceFilename + " : " + String.format("%10.2f", ((double) filesizes[1] / (double) filesizes[0]) * 100) + "%");
                    if (previousSize == filesizes[1]) {
                        identicalSizeCount++;
                        logger.info("File size is equal. Transfer is being held up.");
                    } else {
                        identicalSizeCount = 0;
                    }

                    previousSize = filesizes[1];
                    count++;
                }

            if (!transporter.filesizeEqual(sourcePath, sourceFilename, destinationPath, destinationFilename)) {
                logger.error("Files weren't of the same size, transfer failed. Files:");
                logger.error("Source:\t\t" + sourcePath + "/" + sourceFilename + "\nDestination:\t\t" + destinationPath + "/" + destinationFilename);
                response.setMessage("Files weren't of the same size. Transfer failed.");
                if (identicalSizeCount > 10) {
                    response.setMessage(String.format("%s\n%s", response.getMessage(), "File size didn't increase for 10 seconds."));
                    logger.error(String.format("%s\n%s", response.getMessage(), "File size didn't increase for 10 seconds."));
                }
                response.setStatus(transporter.STATUS_NOK);
                thread.interrupt();
                thread2.interrupt();
                return false;
            } else {
                transporter.renameFile(destinationPath, destinationFilename, originalDestinationFileName);
                logger.info("Renamed file from " + destinationFilename + " to " + originalDestinationFileName);
                if (move) {
                    transporter.deleteFile(sourcePath, sourceFilename);
                    logger.info("Deleted file @ " + sourcePath + "/" + sourceFilename);
                }
                response.setStatus(FileTransporter.STATUS_OK);
                response.setMessage("The file " + sourceFilename + " has been copied as file " + destinationFilename + " on the destination " + transporter.destinationHost);
                return true;
            }
            }catch (IOException e) {
                logger.error("IOException occurred");
                e.printStackTrace();
                response.setStatus(FileTransporter.STATUS_NOK);
                response.setMessage("There was an error within an IO-action: " + e.getMessage() + ". INFO SOURCE Filename: " + sourceFilename + ". FilePath: " + sourcePath);
                logger.error("There was an error within an IO-action: " + e.getMessage() + ". INFO SOURCE Filename: " + sourceFilename + ". FilePath: " + sourcePath);
                return false;
            }catch (InterruptedException e) {
                logger.error("InterruptedException occurred");
                e.printStackTrace();
                return false;
            }catch (AuthenticationException e) {
                logger.error("AuthenticationException occurred");
                e.printStackTrace();
                return false;
            }catch (OutOfMemoryError e) {
                logger.error("OutOfMemoryError occurred");
                e.printStackTrace();
                return false;
            } catch (Exception e) {
                logger.error("Some unknown exception happened.");
                e.printStackTrace();
                return false;
            }
        }
    }
}
