package br.com.ada.incidentes.triagem;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/** Na primeira subida, cadastra as regras de SLA padrão. */
@Component
public class RegraSlaSeed implements CommandLineRunner {

    private final RegraSlaRepository repository;

    public RegraSlaSeed(RegraSlaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() == 0) {
            repository.saveAll(List.of(
                    new RegraSla("P1", 60),      // 1 hora
                    new RegraSla("P2", 240),     // 4 horas
                    new RegraSla("P3", 480),     // 8 horas
                    new RegraSla("P4", 1440)));  // 24 horas
        }
    }
}
