package fxp.thread;

/**
 * Created by dieter on 15/02/2016.
 */
public interface IThreadManager {
    public boolean runThread(Runnable r);
    public int getThreadCount();
    public void notifyFinish(Runnable r);
}
