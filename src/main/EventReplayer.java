package main;

import events.InitTextEvent;
import events.MyTextEvent;
import events.TextInsertEvent;
import events.TextRemoveEvent;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.LinkedBlockingQueue;


public class EventReplayer implements Runnable {
    private LinkedBlockingQueue<MyTextEvent> incomingQueue;
    private JTextArea area;
    private DocumentEventCapturer dec;

    public EventReplayer(LinkedBlockingQueue<MyTextEvent> incomingQueue, JTextArea area, DocumentEventCapturer dec) {
        this.incomingQueue = incomingQueue;
        this.area = area;
        this.dec = dec;
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
                            area.insert(tie.getText(), tie.getOffset());
                            dec.enable();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                } else if (mte instanceof TextRemoveEvent) {
                    final TextRemoveEvent tre = (TextRemoveEvent) mte;
                    EventQueue.invokeLater(() -> {
                        try {
                            dec.disable();
                            area.replaceRange(null, tre.getOffset(), tre.getOffset() + tre.getLength());
                            dec.enable();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                } else if(mte instanceof InitTextEvent) {
                    final InitTextEvent ite = (InitTextEvent) mte;
                    EventQueue.invokeLater(() -> {
                        try {
                            dec.disable();
                            int position = area.getCaret().getDot();
                            area.replaceRange(null, 0, area.getText().length());
                            area.insert(ite.getText(), ite.getOffset());
                            area.getCaret().setDot(position);
                            dec.enable();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                }

            } catch (Exception e) {
                wasInterrupted = true;
            }
        }
    }

}
