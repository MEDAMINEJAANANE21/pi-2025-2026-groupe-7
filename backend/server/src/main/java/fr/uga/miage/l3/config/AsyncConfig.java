package fr.uga.miage.l3.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Pool dédié aux optimisations, pour ne pas saturer le pool commun de la JVM.
 */
@Configuration
public class AsyncConfig {

    public static final String OPTIMISATION_EXECUTOR = "optimisationExecutor";

    @Bean(name = OPTIMISATION_EXECUTOR)
    public Executor optimisationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int processors = Runtime.getRuntime().availableProcessors();
        executor.setCorePoolSize(processors);
        executor.setMaxPoolSize(processors);
        executor.setThreadNamePrefix("optimisation-");
        executor.initialize();
        return executor;
    }
}
