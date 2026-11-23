package br.com.ada.incidentes.triagem;

/** É isto que fica guardado no cache (em JSON) e que a API devolve. */
public record RegraSlaDto(String prioridade, int prazoMinutos) {
}
