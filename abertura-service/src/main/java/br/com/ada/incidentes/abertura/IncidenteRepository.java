package br.com.ada.incidentes.abertura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IncidenteRepository extends JpaRepository<Incidente, UUID> {
}
