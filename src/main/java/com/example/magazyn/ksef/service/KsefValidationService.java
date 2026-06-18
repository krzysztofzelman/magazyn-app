package com.example.magazyn.ksef.service;

import com.example.magazyn.ksef.model.dto.KSeFInvoiceItemRequest;
import com.example.magazyn.ksef.model.dto.KSeFInvoiceRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Validates invoice data before sending to KSeF API.
 * Implements basic validation rules per MF specification.
 */
@Service
public class KsefValidationService {

    private static final Logger log = LoggerFactory.getLogger(KsefValidationService.class);
    private static final List<BigDecimal> VALID_VAT_RATES = List.of(
            new BigDecimal("0"),
            new BigDecimal("5"),
            new BigDecimal("8"),
            new BigDecimal("23"),
            new BigDecimal("ZW"),
            new BigDecimal("OO")
    );

    /**
     * Validate a KSeF invoice request before submission.
     * @return list of validation errors (empty = valid)
     */
    public List<String> validate(KSeFInvoiceRequest request) {
        List<String> errors = new ArrayList<>();

        // Required fields
        if (request.invoiceNumber() == null || request.invoiceNumber().isBlank()) {
            errors.add("Numer faktury jest wymagany");
        }

        if (request.issueDate() == null) {
            errors.add("Data wystawienia jest wymagana");
        } else if (request.issueDate().isAfter(LocalDate.now())) {
            errors.add("Data wystawienia nie może być przyszła");
        }

        if (request.saleDate() == null) {
            errors.add("Data sprzedaży jest wymagana");
        }

        // NIP validation (basic — 10 digits)
        if (request.buyerNip() == null || !request.buyerNip().matches("\\d{10}")) {
            errors.add("NIP nabywcy musi zawierać 10 cyfr");
        }
        if (request.sellerNip() == null || !request.sellerNip().matches("\\d{10}")) {
            errors.add("NIP sprzedawcy musi zawierać 10 cyfr");
        }

        // Buyer/Seller names
        if (request.buyerName() == null || request.buyerName().isBlank()) {
            errors.add("Nazwa nabywcy jest wymagana");
        }
        if (request.sellerName() == null || request.sellerName().isBlank()) {
            errors.add("Nazwa sprzedawcy jest wymagana");
        }

        // Amount validation
        if (request.totalNet() == null || request.totalNet().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Kwota netto musi być większa od zera");
        }
        if (request.totalGross() == null || request.totalGross().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Kwota brutto musi być większa od zera");
        }

        // VAT consistency: net + vat should approximately equal gross
        if (request.totalNet() != null && request.totalVat() != null && request.totalGross() != null) {
            BigDecimal calculatedGross = request.totalNet().add(request.totalVat());
            BigDecimal diff = calculatedGross.subtract(request.totalGross()).abs();
            if (diff.compareTo(new BigDecimal("0.02")) > 0) {
                errors.add("Niezgodność kwot: netto + vat = " + calculatedGross
                        + ", brutto = " + request.totalGross());
            }
        }

        // Currency
        if (request.currency() == null || request.currency().isBlank()) {
            errors.add("Waluta jest wymagana");
        } else if (!request.currency().equals("PLN")) {
            errors.add("Aktualnie obsługiwana jest tylko waluta PLN");
        }

        // Items validation
        if (request.items() == null || request.items().isEmpty()) {
            errors.add("Faktura musi zawierać co najmniej jedną pozycję");
        } else {
            for (int i = 0; i < request.items().size(); i++) {
                List<String> itemErrors = validateItem(request.items().get(i), i + 1);
                errors.addAll(itemErrors);
            }
        }

        return errors;
    }

    private List<String> validateItem(KSeFInvoiceItemRequest item, int index) {
        List<String> errors = new ArrayList<>();
        String prefix = "Pozycja " + index + ": ";

        if (item.name() == null || item.name().isBlank()) {
            errors.add(prefix + "nazwa towaru/usługi jest wymagana");
        }
        if (item.quantity() == null || item.quantity().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add(prefix + "ilość musi być większa od zera");
        }
        if (item.unitPriceNet() == null || item.unitPriceNet().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add(prefix + "cena jednostkowa netto musi być większa od zera");
        }
        if (item.totalNet() == null || item.totalNet().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add(prefix + "wartość netto musi być większa od zera");
        }

        // VAT rate validation
        if (item.vatRate() == null) {
            errors.add(prefix + "stawka VAT jest wymagana");
        }

        return errors;
    }
}
