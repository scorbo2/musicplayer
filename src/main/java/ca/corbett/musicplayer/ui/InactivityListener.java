package ca.corbett.musicplayer.ui;


import javax.swing.Action;
import javax.swing.Timer;
import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * <p>
 * This is not my code - taken from
 * <a href="https://tips4java.wordpress.com/2008/10/24/application-inactivity/">this blog post</a>
 * from 2008.
 * </p>
 * <p>
 * A class that monitors inactivity in an application.
 * </p>
 * <p>
 * It does this by using a Swing Timer and by listening for specified
 * AWT events. When an event is received the Timer is restarted.
 * If no event is received during the specified time interval then the
 * timer will fire and invoke the specified Action.
 * </p>
 * When creating the listener the inactivity interval is specified in
 * minutes. However, once the listener has been created you can reset
 * this value in milliseconds if you need to.
 * <p>
 * Some common event masks have be defined with the class:
 * </p>
 * <ul>
 *  <li>KEY_EVENTS</li>
 *  <li>MOUSE_EVENTS - which includes mouse motion events</li>
 *  <li>USER_EVENTS - includes KEY_EVENTS and MOUSE_EVENT (this is the default)</li>
 * </ul>
 * <p>
 *  The inactivity interval and event mask can be changed at any time,
 *  however, they will not become effective until you stop and start
 *  the listener.
 * </p>
 *
 * @author Rob Camick
 * @since 2008-11-24
 */
class InactivityListener implements ActionListener, AWTEventListener {
    public final static long KEY_EVENTS = AWTEvent.KEY_EVENT_MASK;

    public final static long MOUSE_EVENTS = AWTEvent.MOUSE_MOTION_EVENT_MASK + AWTEvent.MOUSE_EVENT_MASK;

    public final static long USER_EVENTS = KEY_EVENTS + MOUSE_EVENTS;

    private Window window;
    private Action action;
    private int interval;
    private long eventMask;
    private boolean repeats = true;
    private Timer timer = new Timer(0, this);

    /**
     * Use a default inactivity interval of 1 minute and listen for
     * USER_EVENTS
     */
    public InactivityListener(Window window, Action action) {
        this(window, action, 1);
    }

    /**
     * Specify the inactivity interval and listen for USER_EVENTS
     */
    public InactivityListener(Window window, Action action, int interval) {
        this(window, action, interval, USER_EVENTS);
    }

    /**
     * Specify the inactivity interval and the events to listen for
     */
    public InactivityListener(Window window, Action action, int minutes, long eventMask) {
        this.window = window;
        setAction(action);
        setInterval(minutes);
        setEventMask(eventMask);
    }

    /**
     * The Action to be invoked after the specified inactivity period
     */
    public void setAction(Action action) {
        this.action = action;
    }

    /**
     * The interval before the Action is invoked specified in minutes
     */
    public void setInterval(int minutes) {
        setIntervalInMillis(minutes * 60000);
    }

    /**
     * The interval before the Action is invoked specified in milliseconds
     */
    public void setIntervalInMillis(int interval) {
        this.interval = interval;
        timer.setInitialDelay(interval);
        timer.setDelay(interval);
    }

    /**
     * A mask specifying the events to be passed to the AWTEventListener
     */
    public void setEventMask(long eventMask) {
        this.eventMask = eventMask;
    }

    /**
     * Start listening for events.
     */
    public void start() {
        timer.setInitialDelay(interval);
        timer.setRepeats(repeats);
        timer.start();
        Toolkit.getDefaultToolkit().addAWTEventListener(this, eventMask);
    }

    public void setRepeats(boolean value) {
        repeats = value;
    }

    /**
     * Stop listening for events
     */
    public void stop() {
        Toolkit.getDefaultToolkit().removeAWTEventListener(this);
        timer.stop();
    }

    //  Implement ActionListener for the Timer

    public void actionPerformed(ActionEvent e) {
        ActionEvent ae = new ActionEvent(window, ActionEvent.ACTION_PERFORMED, "");
        action.actionPerformed(ae);
    }

    //  Implement AWTEventListener

    public void eventDispatched(AWTEvent e) {
        if (timer.isRunning()) timer.restart();
    }
}