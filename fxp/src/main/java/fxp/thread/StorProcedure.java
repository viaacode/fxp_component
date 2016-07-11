package fxp.thread;

import java.io.IOException;

import org.apache.commons.net.ftp.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StorProcedure implements Runnable {

    private static final Logger logger = LogManager.getLogger(StorProcedure.class);
    FTPClient ftpClient;
    String command;

    public StorProcedure (FTPClient ftp, String command) {
        ftpClient = ftp;
        this.command = command;
    }

    public void run() {
        try {
            ftpClient.sendCommand(command);
            throw new Exception("Ooops");
        } catch (Exception e) {
            logger.error("Sending the command '" + command + "'failed.");
            logger.error(e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}
