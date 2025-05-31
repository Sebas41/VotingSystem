package Autentication;

import java.util.Optional;

import model.Voter;
import repository.VoterRepository;

/**
 * Lógica de autenticación de votantes.
 * - Comprueba que exista el votante y que la contraseña coincida.
 * - También permite chequear si ya votó o no.
 */
public class AutenticationVoter {
    private VoterRepository voterRepo;

    public AutenticationVoter(VoterRepository voterRepo) {
        this.voterRepo = voterRepo;
    }

    /**
     * Valida credenciales: devuelve 'true' si existe el votante con esa id
     * y la contraseña coincide. Devuelve 'false' si no existe o la clave es errónea.
     */
    public boolean authenticate(String id, String password) {
        Optional<Voter> opt = voterRepo.findById(id);
        if (!opt.isPresent()) {
            return false;
        }
        Voter v = opt.get();
        return v.getPassword().equals(password);
    }

    /**
     * Indica si ese votante ya votó.
     */
    public boolean hasAlreadyVoted(String id) {
        Optional<Voter> opt = voterRepo.findById(id);
        System.out.println("Se encontro el vontante: "+ (opt !=null));
        return  opt.get().isAlreadyVote();
    }


    
    /**
     * Marca al votante como que ya votó (cambia su campo alreadyVote a YES).
     */
    public void markAsVoted(String id) {
        Optional<Voter> opt = voterRepo.findById(id);
        if (opt.isPresent()) {
            Voter v = opt.get();
            v.vote();
            voterRepo.update(v);
        }
    }
}
