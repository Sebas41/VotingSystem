module reliableMessage{

    ["java:serializable:model.ReliableMessage"]
    sequence<byte> RMessage;
    ["java:serializable:model.Message"]
    sequence<byte> Message;

    ["java:serializable:model.Vote"]
    sequence<byte> Vote;

    interface ACKService{
        void ack(string messageId);
    }
    interface RMDestination{
        void reciveMessage(RMessage rmessage, ACKService* prx);
    }
    interface RMSource{
        void setServerProxy(RMDestination* destination);
        void sendMessage(Vote msg);
    }


    interface SendVote{
        void receiveVote(Vote vote);
    }

}