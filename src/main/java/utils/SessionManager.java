
package utils;

import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.InputEvent;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.SwingUtilities;

public final class SessionManager {

    private static final long IDLE_TIMEOUT_MILLIS = 15 * 60 * 1000L;
    private static final long CHECK_INTERVAL_MILLIS = 1000L;

    private static long lastActivityTime;
    private static boolean running;
    private static AWTEventListener activityListener;
    private static Timer idleTimer;
    private static TimerTask idleCheckTask;
    private static Runnable timeoutAction;

    private SessionManager() {
    }

    public static synchronized void start(Runnable onTimeout) {
        stop();

        timeoutAction = onTimeout;
        lastActivityTime = System.currentTimeMillis();
        running = true;

        activityListener = event -> {
            if (event instanceof InputEvent) {
                synchronized (SessionManager.class) {
                    if (running) {
                        lastActivityTime = System.currentTimeMillis();
                    }
                }
            }
        };

        long eventMask = AWTEvent.MOUSE_EVENT_MASK
                | AWTEvent.MOUSE_MOTION_EVENT_MASK
                | AWTEvent.MOUSE_WHEEL_EVENT_MASK
                | AWTEvent.KEY_EVENT_MASK;

        Toolkit.getDefaultToolkit()
                .addAWTEventListener(activityListener, eventMask);

        idleTimer = new Timer("MotorPH-Session-Idle-Timeout", true);

        idleCheckTask = new TimerTask() {
            @Override
            public void run() {
                Runnable action = null;

                synchronized (SessionManager.class) {
                    if (!running) {
                        return;
                    }

                    long idleTime = System.currentTimeMillis()
                            - lastActivityTime;

                    if (idleTime >= IDLE_TIMEOUT_MILLIS) {
                        running = false;

                        if (activityListener != null) {
                            Toolkit.getDefaultToolkit()
                                    .removeAWTEventListener(activityListener);
                            activityListener = null;
                        }

                        action = timeoutAction;
                        timeoutAction = null;

                        idleCheckTask.cancel();
                        idleTimer.cancel();
                    }
                }

                if (action != null) {
                    SwingUtilities.invokeLater(action);
                }
            }
        };

        idleTimer.scheduleAtFixedRate(
                idleCheckTask,
                CHECK_INTERVAL_MILLIS,
                CHECK_INTERVAL_MILLIS
        );
    }

    public static synchronized void stop() {
        running = false;

        if (idleCheckTask != null) {
            idleCheckTask.cancel();
            idleCheckTask = null;
        }

        if (idleTimer != null) {
            idleTimer.cancel();
            idleTimer = null;
        }

        if (activityListener != null) {
            Toolkit.getDefaultToolkit()
                    .removeAWTEventListener(activityListener);
            activityListener = null;
        }

        timeoutAction = null;
    }
}
