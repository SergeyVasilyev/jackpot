package com.sportygroup.jackpot.init;

import com.sportygroup.jackpot.config.JackpotProperties;
import com.sportygroup.jackpot.dto.CreateJackpotCommand;
import com.sportygroup.jackpot.dto.JackpotDto;
import com.sportygroup.jackpot.service.JackpotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.List;

/**
 * On startup, reads the jackpots JSON file and calls {@link JackpotService#create} per entry (A1),
 * so initialization goes through the same creation logic a future REST endpoint would reuse. Logs
 * the resulting ids.
 */
@Component
@Slf4j
public class JackpotInitializer implements ApplicationRunner {

    private final JackpotService jackpotService;
    private final ObjectMapper objectMapper;
    private final Resource jackpotsResource;

    public JackpotInitializer(JackpotService jackpotService, ObjectMapper objectMapper,
                              ResourceLoader resourceLoader, JackpotProperties properties) {
        this.jackpotService = jackpotService;
        this.objectMapper = objectMapper;
        this.jackpotsResource = resourceLoader.getResource(properties.initializer().file());
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!jackpotsResource.exists()) {
            log.warn("Jackpot initializer file {} not found — no jackpots loaded", jackpotsResource);
            return;
        }
        try (InputStream in = jackpotsResource.getInputStream()) {
            List<CreateJackpotCommand> commands = objectMapper.readValue(in, new TypeReference<>() {
            });
            for (CreateJackpotCommand command : commands) {
                JackpotDto dto = jackpotService.create(command);
                log.info("Initialized jackpot id={}", dto.id());
            }
            log.info("Jackpot initialization complete: {} entries processed", commands.size());
        }
    }
}
