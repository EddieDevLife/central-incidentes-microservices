package br.com.ada.incidentes.triagem;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/triagens")
public class TriagemController {

    private final TriagemRepository repository;

    public TriagemController(TriagemRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<TriagemResponse> listar() {
        return repository.findAll().stream().map(TriagemResponse::de).toList();
    }

    /** 404 aqui pode significar só "ainda não foi triado": a fila é assíncrona. */
    @GetMapping("/{incidenteId}")
    public TriagemResponse buscar(@PathVariable UUID incidenteId) {
        return repository.findByIncidenteId(incidenteId)
                .map(TriagemResponse::de)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Incidente ainda não triado: " + incidenteId));
    }
}
