package br.com.ada.incidentes.triagem;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TriagemRepository extends JpaRepository<Triagem, UUID> {

    Optional<Triagem> findByIncidenteId(UUID incidenteId);
}
