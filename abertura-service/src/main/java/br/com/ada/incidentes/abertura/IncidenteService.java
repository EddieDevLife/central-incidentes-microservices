package br.com.ada.incidentes.abertura;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class IncidenteService {

    private final IncidenteRepository repository;
    private final PublicadorDeIncidentes publicador;

    public IncidenteService(IncidenteRepository repository, PublicadorDeIncidentes publicador) {
        this.repository = repository;
        this.publicador = publicador;
    }

    /**
     * Grava no banco e publica na fila.
     *
     * Se a publicação falhar (RabbitMQ fora do ar), a exceção desfaz a gravação:
     * não sobra incidente "mudo", que ninguém vai triar.
     * Limitação conhecida (ADR 0002): se o banco falhar bem no commit, DEPOIS
     * de a mensagem já ter saído, sobra uma mensagem sem incidente. A solução
     * completa seria o padrão Outbox, que ficou fora do escopo.
     */
    @Transactional
    public Incidente abrir(NovoIncidenteRequest request) {
        Incidente incidente = repository.save(new Incidente(
                request.titulo(), request.descricao(), request.impacto(), request.urgencia()));
        publicador.publicar(incidente);
        return incidente;
    }

    /**
     * Publica DE NOVO a mesma mensagem (mesmo eventoId).
     * Existe para demonstrar a idempotência: na vida real a repetição acontece
     * sozinha (reenvio após timeout, reentrega do broker); aqui a provocamos.
     */
    @Transactional(readOnly = true)
    public Incidente reenviar(UUID id) {
        Incidente incidente = buscar(id);
        publicador.publicar(incidente);
        return incidente;
    }

    @Transactional(readOnly = true)
    public Incidente buscar(UUID id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Incidente não encontrado: " + id));
    }

    @Transactional(readOnly = true)
    public List<Incidente> listar() {
        return repository.findAll();
    }
}
