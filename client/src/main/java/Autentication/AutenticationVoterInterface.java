package Autentication;

public interface AutenticationVoterInterface{

    boolean authenticate(String id, String password);
    boolean hasAlreadyVoted(String id);
    void markAsVoted(String id);





}
