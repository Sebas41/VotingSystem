package Autentication;

import java.util.Optional;

public class AutenticationVoter implements AutenticationVoterInterface{
    private VoterRepository voterRepo;

    public AutenticationVoter() {
        this.voterRepo = new VoterRepository();
    }

    @Override
    public boolean authenticate(String id, String password) {
        Optional<Voter> opt = voterRepo.findById(id);
        if (!opt.isPresent()) {
            return false;
        }
        Voter v = opt.get();
        return v.getPassword().equals(password);
    }

    @Override
    public boolean hasAlreadyVoted(String id) {
        Optional<Voter> opt = voterRepo.findById(id);
        if (!opt.isPresent()) {
            return false; 
        }
        return  opt.get().isAlreadyVote();
    }
    
    @Override
    public void markAsVoted(String id) {
        Optional<Voter> opt = voterRepo.findById(id);
        if (opt.isPresent()) {
            Voter v = opt.get();
            v.vote();
            voterRepo.update(v);
        }
    }
}
