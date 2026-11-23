package br.com.gestaocontratos.infraestrutura;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class ConfiguracaoGeral {

    /** Relógio no fuso do órgão: "hoje" é a data de Brasília, não a do servidor. */
    @Bean
    Clock relogioDoSistema(@Value("${gestao-contratos.fuso-horario}") String fuso) {
        return Clock.system(ZoneId.of(fuso));
    }

    @Bean
    OpenAPI documentacao() {
        return new OpenAPI()
                .info(new Info()
                        .title("Gestão de Contratos")
                        .version("1.0.0")
                        .description("Gestão e fiscalização de contratos de TI: vigência, aditivos, ordens de serviço, "
                                + "glosas por indicadores, empenhos e relatórios. Entre em POST /api/login e use o token "
                                + "no botão Authorize."))
                .components(new Components().addSecuritySchemes("token",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer")))
                .addSecurityItem(new SecurityRequirement().addList("token"));
    }
}
