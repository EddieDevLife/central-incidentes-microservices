package br.com.ada.incidentes.triagem;

/**
 * A matriz impacto x urgência, comum em gestão de incidentes:
 *
 *                 urgência ALTA   MÉDIA   BAIXA
 *  impacto ALTO        P1          P2      P3
 *  impacto MÉDIO       P2          P3      P4
 *  impacto BAIXO       P3          P4      P4
 *
 * Analogia: é a triagem do pronto-socorro. Não importa quem chegou primeiro,
 * e sim a gravidade (impacto) e o quanto dá para esperar (urgência).
 */
public final class CalculadoraPrioridade {

    private CalculadoraPrioridade() {
    }

    public static Prioridade calcular(Nivel impacto, Nivel urgencia) {
        int pontos = peso(impacto) + peso(urgencia);
        if (pontos >= 6) return Prioridade.P1;
        if (pontos == 5) return Prioridade.P2;
        if (pontos == 4) return Prioridade.P3;
        return Prioridade.P4;
    }

    private static int peso(Nivel nivel) {
        return switch (nivel) {
            case ALTO -> 3;
            case MEDIO -> 2;
            case BAIXO -> 1;
        };
    }
}
