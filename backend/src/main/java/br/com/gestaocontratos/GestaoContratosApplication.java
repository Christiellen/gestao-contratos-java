package br.com.gestaocontratos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

// Sem usuário em memória do Spring Security: a autenticação é por token próprio (seguranca.FiltroTokenBearer).
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class GestaoContratosApplication {

    public static void main(String[] args) {
        SpringApplication.run(GestaoContratosApplication.class, args);
    }
}
