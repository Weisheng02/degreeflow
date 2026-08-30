package com.portfolio.campusbooking.config;

import com.portfolio.campusbooking.resource.CampusResource;
import com.portfolio.campusbooking.resource.CampusResourceRepository;
import com.portfolio.campusbooking.resource.ResourceType;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SeedData {

    @Bean
    CommandLineRunner seedCampusResources(CampusResourceRepository repository) {
        return args -> {
            if (repository.count() > 0) {
                return;
            }
            repository.save(new CampusResource("Innovation Lab", ResourceType.ROOM, "Block A · Level 3", 24));
            repository.save(new CampusResource("Discussion Pod 04", ResourceType.ROOM, "Library · Level 2", 8));
            repository.save(new CampusResource("DSLR Camera Kit", ResourceType.EQUIPMENT, "Media Counter", 1));
            repository.save(new CampusResource("Portable Projector", ResourceType.EQUIPMENT, "ICT Helpdesk", 1));
        };
    }
}
