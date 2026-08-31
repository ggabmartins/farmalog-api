package br.com.farmalog;

import org.springframework.boot.SpringApplication;

public class TestFarmalogApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(FarmalogApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
