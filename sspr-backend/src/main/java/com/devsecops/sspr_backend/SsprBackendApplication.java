package com.devsecops.ssprbackend;

import com.devsecops.ssprbackend.model.User;
import com.devsecops.ssprbackend.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SsprBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SsprBackendApplication.class, args);
    }

    @Bean
    CommandLineRunner initDatabase(UserRepository userRepository) {
        return args -> {
            if (userRepository.count() == 0) {
                userRepository.save(new User(null, "devsecops@test.com", "JohnDoe", User.UserStatus.ACTIVE));
                System.out.println(">>> SEEDED SEED USER: devsecops@test.com");
            }
        };
    }
}