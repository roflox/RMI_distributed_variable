package nodes;

import org.apache.logging.log4j.Logger;

public class QueueChecker extends Thread {

    private static Logger logger;
    private NodeImpl node;

    public QueueChecker(Logger logger, NodeImpl node) {
        QueueChecker.logger = logger;
        this.node = node;
    }

    @Override
    public void run() {
        try {
            synchronized (this) {
                while (!isInterrupted()) {
                    wait(100);
                    if (!node.working) {
                        node.executeQueue();
                    }
                }
            }
        } catch (InterruptedException e) {
            logger.warn("Cannot wait.");
        }
    }


}
