package threads;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import communication.Notification;
import model.Vote;
import model.ReliableMessage;
import repository.PendingMessageRepository;
public class RMJob extends Thread{

    public static final String PENDING = "Pending";
    public static final String SENDED = "Sended";
    private  PendingMessageRepository repository;

    private Map<String,ReliableMessage> messagesPendig = new ConcurrentHashMap<>();
    private Map<String,ReliableMessage> forConfirm = new ConcurrentHashMap<>();


    private Long sequenceNumber = 0l;
    private Object lock = new Object();
    private boolean enable = true;
    private Notification notification;

    public RMJob(Notification notification) {
        this.notification = notification;
        this.repository = new PendingMessageRepository();
        if(repository.findAll().size()>0 ) {
            long maxSeq = repository.findAll().values().stream()
                         .mapToLong(ReliableMessage::getNumberMessage)
                         .max()
                         .orElse(-1L);
            this.sequenceNumber = maxSeq + 1;
            this.messagesPendig= new ConcurrentHashMap<>(repository.findAll());

        }
    }

    public void add(Vote message){
        synchronized (lock) {
            ReliableMessage mes = new ReliableMessage(UUID.randomUUID().toString(), sequenceNumber++, PENDING, message);
            messagesPendig.put(mes.getUuid(),mes);
            repository.add(mes);
        }
    }

    public void confirmMessage(String uid){
        forConfirm.remove(uid);
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    @Override
    public void run(){
        while (enable) {
            
            for(Map.Entry<String,ReliableMessage> rm: messagesPendig.entrySet()){
                try {
                    notification.sendMessage(rm.getValue());
                    messagesPendig.remove(rm.getKey());
                    forConfirm.put(rm.getKey(), rm.getValue());
                    repository.remove(rm.getKey());
                } catch (com.zeroc.Ice.Exception e) {
                    //e.printStackTrace();
                } catch (Exception e) {
                    // System.err.println("Error sending message: " + e.getMessage());
                }
            }
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
