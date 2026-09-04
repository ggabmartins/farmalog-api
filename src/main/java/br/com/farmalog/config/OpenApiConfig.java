package br.com.farmalog.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI farmalogOpenAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("Farmalog API")
						.description("API REST de gestão de farmácia independente: controle de estoque por lote, "
								+ "validade e registro de vendas.")
						.version("1.0.0"));
	}
}
