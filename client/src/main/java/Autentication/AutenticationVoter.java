package Autentication;

import java.util.Optional;

public class AutenticationVoter {
    private VoterRepository voterRepo;

    public AutenticationVoter(VoterRepository voterRepo) {
        this.voterRepo = voterRepo;
    }

    
    public boolean authenticate(String id, String password) {
        Optional<Voter> opt = voterRepo.findById(id);
        if (!opt.isPresent()) {
            return false;
        }
        Voter v = opt.get();
        return v.getPassword().equals(password);
    }


    public boolean hasAlreadyVoted(String id) {
        Optional<Voter> opt = voterRepo.findById(id);
        System.out.println("Se encontro el vontante: "+ (opt !=null));
        if (!opt.isPresent()) {
            return false; 
        }
        return  opt.get().isAlreadyVote();
    }


    

    public void markAsVoted(String id) {
        Optional<Voter> opt = voterRepo.findById(id);
        if (opt.isPresent()) {
            Voter v = opt.get();
            v.vote();
            voterRepo.update(v);
        }
    }
}
