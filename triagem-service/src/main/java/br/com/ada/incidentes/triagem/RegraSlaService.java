package br.com.ada.incidentes.triagem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RegraSlaService {

    private static final Logger log = LoggerFactory.getLogger(RegraSlaService.class);

    private final RegraSlaRepository repository;

    public RegraSlaService(RegraSlaRepository repository) {
        this.repository = repository;
    }

    /**
     * Analogia: o post-it no monitor com o ramal que você mais usa.
     * Em vez de abrir a lista telefônica (banco) toda vez, você olha o post-it (Redis).
     *
     * Com @Cacheable, o Spring olha o Redis ANTES de entrar neste método.
     * Se achar, devolve direto e o método nem roda. Por isso o log abaixo só
     * aparece quando o cache NÃO tinha a resposta ("cache miss").
     */
    @Cacheable(cacheNames = CacheConfig.CACHE_REGRAS_SLA, key = "#prioridade.name()")
    public RegraSlaDto buscar(Prioridade prioridade) {
        log.info("CACHE MISS: buscando regra de SLA de {} no banco", prioridade);
        RegraSla regra = buscarNoBanco(prioridade);
        return new RegraSlaDto(regra.getPrioridade(), regra.getPrazoMinutos());
    }

    /**
     * A regra mudou: o post-it está desatualizado, então é rasgado (@CacheEvict).
     * A próxima consulta vai ao banco e escreve um post-it novo.
     *
     * De propósito, este método NÃO é @Transactional: o save() grava e confirma
     * no banco antes de o método terminar, e só depois o cache é apagado. Assim
     * ninguém consegue recolocar o valor antigo no cache nesse intervalo.
     */
    @CacheEvict(cacheNames = CacheConfig.CACHE_REGRAS_SLA, key = "#prioridade.name()")
    public RegraSlaDto atualizar(Prioridade prioridade, int prazoMinutos) {
        RegraSla regra = buscarNoBanco(prioridade);
        regra.setPrazoMinutos(prazoMinutos);
        repository.save(regra);
        log.info("Regra de SLA de {} alterada para {} min; cache apagado", prioridade, prazoMinutos);
        return new RegraSlaDto(regra.getPrioridade(), regra.getPrazoMinutos());
    }

    private RegraSla buscarNoBanco(Prioridade prioridade) {
        return repository.findById(prioridade.name()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Regra de SLA não encontrada: " + prioridade));
    }
}
