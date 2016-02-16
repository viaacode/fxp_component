package fxp.thread;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by dieter on 15/02/2016.
 */
public class ThreadManager implements IThreadManager {
    private static final Logger logger = LogManager.getLogger(ThreadManager.class);
    private Map<Runnable, Thread> runningThreads;

    public ThreadManager() {
        runningThreads = new HashMap<>();
        logger.info("Initialized thread manager.");
    }

    @Override
    public boolean runThread(Runnable r) {
        Thread t = new Thread(r);
        t.start();
        runningThreads.put(r, t);
        logger.info("Added thread to list. Current size is " + runningThreads.size());
        return true;
    }

    @Override
    public int getThreadCount() {
        return runningThreads.size();
    }

    @Override
    public void notifyFinish(Runnable r) {
        //Delete thread from list
        runningThreads.remove(r);
        System.out.println("Removed thread from list after finish.");
    }
}
