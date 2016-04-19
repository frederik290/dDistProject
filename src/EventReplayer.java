import javax.swing.*;
import java.awt.*;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Takes the event recorded by the DocumentEventCapturer and replays
 * them in a JTextArea. The delay of 1 sec is only to make the individual
 * steps in the reply visible to humans.
 *
 * @author Jesper Buus Nielsen
 */
public class EventReplayer implements Runnable {

    private LinkedBlockingQueue<MyTextEvent> incomingQueue;
    private JTextArea area;

    public EventReplayer(LinkedBlockingQueue<MyTextEvent> incomingQueue, JTextArea area) {
        this.incomingQueue = incomingQueue;
        this.area = area;

    }

    public void run() {
        boolean wasInterrupted = false;
        while (!wasInterrupted) {
            try {
                MyTextEvent mte = incomingQueue.take();
                if (mte instanceof TextInsertEvent) {
                    final TextInsertEvent tie = (TextInsertEvent) mte;
                    EventQueue.invokeLater(() -> {
                        try {
                            area.insert(tie.getText(), tie.getOffset());
                        } catch (Exception e) {
                            System.err.println(e);
                /* We catch all axceptions, as an uncaught exception would make the
                 * EDT unwind, which is now healthy.
                 */
                        }
                    });
                } else if (mte instanceof TextRemoveEvent) {
                    final TextRemoveEvent tre = (TextRemoveEvent) mte;
                    EventQueue.invokeLater(() -> {
                        try {
                            area.replaceRange(null, tre.getOffset(), tre.getOffset() + tre.getLength());
                        } catch (Exception e) {
                            System.err.println(e);
                /* We catch all axceptions, as an uncaught exception would make the
                 * EDT unwind, which is now healthy.
                 */
                        }
                    });
                }
            } catch (Exception e) {
                wasInterrupted = true;
            }
        }
    }

}
