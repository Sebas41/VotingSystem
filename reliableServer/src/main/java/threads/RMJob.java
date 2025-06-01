package threads;

import communication.Notification;
import model.ReliableMessage;
import model.Vote;
import repository.PendingMessageStoreMap;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RMJob extends Thread {

    public static final String PENDING = "Pending";
    public static final String SENDED  = "Sended";

    private final PendingMessageStoreMap store;

    private final Map<String, ReliableMessage> forConfirm = new ConcurrentHashMap<>();

    private long sequenceNumber = 0L;
    private final Object lock = new Object();
    private boolean enable = true;
    private final Notification notification;

    public RMJob(Notification notification) {
        this.notification = notification;
        this.store = new PendingMessageStoreMap();

        if (!store.findAll().isEmpty()) {
            long maxSeq = store.findAll().values().stream()
                               .mapToLong(ReliableMessage::getNumberMessage)
                               .max()
                               .orElse(-1L);
            this.sequenceNumber = maxSeq + 1;
        }
    }


    public void add(Vote message) {
        synchronized (lock) {
            ReliableMessage rm = new ReliableMessage(
                UUID.randomUUID().toString(),
                sequenceNumber++,
                PENDING,
                message
            );
            store.add(rm);
        }
    }


    public void confirmMessage(String uuid) {
        ReliableMessage removed = store.findById(uuid);
        if (removed != null) {
            store.remove(uuid);
            forConfirm.put(uuid, removed);
        }
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    @Override
    public void run() {
        while (enable) {
            for (Map.Entry<String, ReliableMessage> entry : store.findAll().entrySet()) {
                String uuid = entry.getKey();
                ReliableMessage rm = entry.getValue();

                try {
                    notification.sendMessage(rm);
                    store.remove(uuid);
                    forConfirm.put(uuid, rm);

                } catch (com.zeroc.Ice.Exception iceEx) {
                } catch (Exception ex) {
                }
            }

            try {
                Thread.sleep(1000); 
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }
}
