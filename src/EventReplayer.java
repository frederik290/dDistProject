import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ConcurrentHashMap;
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
    private DocumentEventCapturer dec;
    private ConcurrentHashMap<MyTextEvent,MyTextEvent> localBuffer;

    public EventReplayer(LinkedBlockingQueue<MyTextEvent> incomingQueue, JTextArea area, DocumentEventCapturer dec, ConcurrentHashMap<MyTextEvent,MyTextEvent> localBuffer) {
        this.incomingQueue = incomingQueue;
        this.area = area;
        this.dec = dec;
        this.localBuffer = localBuffer;

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
                            dec.disable();
                            MyTextEvent localEvent = localBuffer.get(tie);
                            if(localEvent == null){
                                area.insert(tie.getText(), tie.getOffset());
                            }
                            localBuffer.remove(tie);
                            dec.enable();
                        } catch (Exception e) {
                            e.printStackTrace();
                /* We catch all axceptions, as an uncaught exception would make the
                 * EDT unwind, which is now healthy.
                 */
                        }
                    });
                } else if (mte instanceof TextRemoveEvent) {
                    final TextRemoveEvent tre = (TextRemoveEvent) mte;
                    EventQueue.invokeLater(() -> {
                        try {
                            dec.disable();
                            MyTextEvent localEvent = localBuffer.get(tre);
                            if(localEvent == null){
                                area.replaceRange(null, tre.getOffset(), tre.getOffset() + tre.getLength());
                            }else{
                                System.out.println("Local remove detected");
                            }
                            localBuffer.remove(tre);
                            dec.enable();
                        } catch (Exception e) {
                            e.printStackTrace();
                /* We catch all axceptions, as an uncaught exception would make the
                 * EDT unwind, which is now healthy.
                 */
                        }
                    });
                } else if(mte instanceof TextSyncEvent) {
                    final TextSyncEvent tse = (TextSyncEvent) mte;
                    EventQueue.invokeLater(() -> {
                        try {
                            dec.disable();
                            area.setText("");
                            area.insert(tse.getAreaText(), 0);
                            area.getCaret().setDot(tse.getOffset());
                            System.out.println("Text reset");
                            dec.enable();
                        } catch (Exception e) {
                            e.printStackTrace();
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
