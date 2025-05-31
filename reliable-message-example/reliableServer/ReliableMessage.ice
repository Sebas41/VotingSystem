module reliableMessage{

    ["java:serializable:model.ReliableMessage"]
    sequence<byte> RMessage;
    ["java:serializable:model.Message"]
    sequence<byte> Message;

    struct Vote {
        string id;
        string vote;
    };
    
    interface ACKService{
        void ack(string messageId);
    }
    interface RMDestination{
        void reciveMessage(RMessage rmessage, ACKService* prx);
    }
    interface RMSource{
        void setServerProxy(RMDestination* destination);
        void sendMessage(Message msg);
    }

}