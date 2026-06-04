package FrancescoAlves.capstone.services;

import FrancescoAlves.capstone.entities.User;
import FrancescoAlves.capstone.exceptions.NotFoundException;
import FrancescoAlves.capstone.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;


    public User findById(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("User", id));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new NotFoundException("User con email: ", email + " non trovato!"));
    }


}
