package br.com.ada.incidentes.abertura;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/incidentes")
public class IncidenteController {

    private final IncidenteService service;

    public IncidenteController(IncidenteService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IncidenteResponse abrir(@Valid @RequestBody NovoIncidenteRequest request) {
        return IncidenteResponse.de(service.abrir(request));
    }

    @GetMapping
    public List<IncidenteResponse> listar() {
        return service.listar().stream().map(IncidenteResponse::de).toList();
    }

    @GetMapping("/{id}")
    public IncidenteResponse buscar(@PathVariable UUID id) {
        return IncidenteResponse.de(service.buscar(id));
    }

    /** Só para a demonstração: manda a mesma mensagem outra vez. */
    @PostMapping("/{id}/reenviar")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public IncidenteResponse reenviar(@PathVariable UUID id) {
        return IncidenteResponse.de(service.reenviar(id));
    }
}
