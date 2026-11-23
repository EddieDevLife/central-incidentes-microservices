package br.com.ada.incidentes.triagem;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/regras-sla")
public class RegraSlaController {

    private final RegraSlaService service;

    public RegraSlaController(RegraSlaService service) {
        this.service = service;
    }

    /** Passa pelo cache: a 1ª chamada vai ao banco, as seguintes vêm do Redis. */
    @GetMapping("/{prioridade}")
    public RegraSlaDto buscar(@PathVariable Prioridade prioridade) {
        return service.buscar(prioridade);
    }

    /** Altera a regra no banco e apaga a entrada correspondente do cache. */
    @PutMapping("/{prioridade}")
    public RegraSlaDto atualizar(@PathVariable Prioridade prioridade,
                                 @Valid @RequestBody AtualizarRegraRequest request) {
        return service.atualizar(prioridade, request.prazoMinutos());
    }
}
