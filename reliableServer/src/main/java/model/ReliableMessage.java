package model;

import java.io.Serializable;

public class ReliableMessage implements Serializable{
    
    private String uuid;
    private long numberMessage;
    private String state;

    private Vote message;

    public ReliableMessage() {
    }
    public ReliableMessage(String uuid, long numberMessage, String state, Vote message) {
        this.uuid = uuid;
        this.numberMessage = numberMessage;
        this.state = state;
        this.message = message;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public long getNumberMessage() {
        return numberMessage;
    }

    public void setNumberMessage(long numberMessage) {
        this.numberMessage = numberMessage;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Vote getMessage() {
        return message;
    }

    public void setMessage(Vote message) {
        this.message = message;
    }
    
    

}
