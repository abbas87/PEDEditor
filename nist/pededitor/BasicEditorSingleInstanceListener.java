/* Eric Boesch, NIST Materials Measurement Laboratory, 2014. This file
 * is placed into the public domain. */

package gov.nist.pededitor;

import java.util.Timer;
import java.util.TimerTask;

import javax.jnlp.SingleInstanceListener;

/** SingleInstanceListener to allow multiple BasicEditors to share the
    same process, reducing startup time. */
class BasicEditorSingleInstanceListener implements SingleInstanceListener {
    BasicEditorCreator ec;
    Timer closer = new Timer("ShutdownChecker", false);
    TimerTask closeTask = null;
    long delay = 60*1000*30; // 30 minutes in msecs

    BasicEditorSingleInstanceListener(BasicEditorCreator ec) {
        this.ec = ec;
    }

    @Override public void newActivation(String[] args) {
        // Turn off exitIfLastWindowCloses on the created BasicEditor
        // object, so the service can continue running.
        BasicEditor.main(new TurnOffExitOnClose(ec), args);
        if (closeTask != null) {
            closeTask.cancel();
        }
        closeTask = new BasicEditorShutdownChecker();
        closer.scheduleAtFixedRate(closeTask, delay, delay);
    }

    static class TurnOffExitOnClose extends BasicEditorCreator {
        BasicEditorCreator ec;

        TurnOffExitOnClose(BasicEditorCreator ec) {
            this.ec = ec;
        }

        @Override public BasicEditor run() {
            BasicEditor e = ec.run();
            e.setExitIfLastWindowCloses(false);
            return e;
        }

        @Override public String getProgramTitle() {
            return ec.getProgramTitle();
        }
    }
}
