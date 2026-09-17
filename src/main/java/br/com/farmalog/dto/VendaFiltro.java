package br.com.farmalog.dto;

import java.time.LocalDate;

public record VendaFiltro(LocalDate inicio, LocalDate fim) {
}
