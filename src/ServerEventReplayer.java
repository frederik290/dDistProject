import javax.swing.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Takes the event recorded by the DocumentEventCapturer and replays
 * them in a JTextArea. The delay of 1 sec is only to make the individual
 * steps in the reply visible to humans.
 *
 * @author Jesper Buus Nielsen
 */
public class ServerEventReplayer implements Runnable {

    private LinkedBlockingQueue<MyTextEvent> incomingQueue;
    private LinkedBlockingQueue<MyTextEvent> outgoingQueue;
    private JTextArea serverTextArea;
    private ConcurrentHashMap<MyTextEvent, TextEventSender> senderMap;

    public ServerEventReplayer(LinkedBlockingQueue<MyTextEvent> incomingQueue, LinkedBlockingQueue<MyTextEvent> outgoingQueue, JTextArea serverTextArea, ConcurrentHashMap<MyTextEvent, TextEventSender> senderMap) {
        this.incomingQueue = incomingQueue;
        this.outgoingQueue = outgoingQueue;
        this.serverTextArea = serverTextArea;
        this.senderMap = senderMap;

    }

    public void run() {
        boolean wasInterrupted = false;
        while (!wasInterrupted) {
            try {
                MyTextEvent mte = incomingQueue.take();
                if (mte instanceof TextInsertEvent) {
                    final TextInsertEvent tie = (TextInsertEvent) mte;

                    try {
                        serverTextArea.insert(tie.getText(), tie.getOffset());

                        if (!isSameAreaTextHash(tie)) {
                            TextEventSender sender = senderMap.get(tie);
                            outgoingQueue.put(new TextSyncEvent(tie.getOffset() + tie.getText().length(), serverTextArea.getText()));
                        } else {
                            outgoingQueue.put(tie);
                        }
                        senderMap.remove(tie);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } else if (mte instanceof TextRemoveEvent) {
                    final TextRemoveEvent tre = (TextRemoveEvent) mte;
                    try {
                        serverTextArea.replaceRange(null, tre.getOffset(), tre.getOffset() + tre.getLength());
                        outgoingQueue.put(tre);
                        if (!isSameAreaTextHash(tre)) {
                            TextEventSender sender = senderMap.get(tre);
                            sender.put(new TextSyncEvent(tre.getOffset(), serverTextArea.getText()));
                        }
                        senderMap.remove(tre);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }

            } catch (Exception e) {
                wasInterrupted = true;
            }
        }
    }

    private boolean isSameAreaTextHash(MyTextEvent remoteEvent) {
        int localHash = serverTextArea.getText().hashCode();
        int remoteHash = remoteEvent.getTextHash();
        return localHash == remoteHash;


    }

}
