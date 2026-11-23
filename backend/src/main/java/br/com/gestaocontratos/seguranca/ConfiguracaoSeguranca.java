package br.com.gestaocontratos.seguranca;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * API sem sessão: cada requisição traz o token no cabeçalho, então não há cookie
 * de sessão e a proteção CSRF não se aplica.
 */
@Configuration
public class ConfiguracaoSeguranca {

    @Bean
    SecurityFilterChain filtros(HttpSecurity http, TokenService tokens, ObjectMapper json) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(HttpMethod.POST, "/api/login").permitAll()
                        .requestMatchers("/actuator/health/**", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/error")
                        .permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, resp, ex) -> responder(resp, json, 401, "Não autenticado."))
                        .accessDeniedHandler((req, resp, ex) -> responder(resp, json, 403, "Você não tem permissão para esta ação.")))
                .addFilterBefore(new FiltroTokenBearer(tokens), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    PasswordEncoder codificadorDeSenhas() {
        return new BCryptPasswordEncoder();
    }

    private static void responder(HttpServletResponse resposta, ObjectMapper json, int status, String mensagem) throws IOException {
        resposta.setStatus(status);
        resposta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        resposta.setCharacterEncoding("UTF-8");
        json.writeValue(resposta.getOutputStream(), Map.of("message", mensagem));
    }
}
