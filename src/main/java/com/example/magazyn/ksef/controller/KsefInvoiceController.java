package com.example.magazyn.ksef.controller;

import com.example.magazyn.ksef.exception.KsefAuthenticationException;
import com.example.magazyn.ksef.exception.KsefCommunicationException;
import com.example.magazyn.ksef.exception.KsefValidationException;
import com.example.magazyn.ksef.model.dto.KSeFInvoiceResponse;
import com.example.magazyn.ksef.model.dto.KSeFSendInvoiceResponse;
import com.example.magazyn.ksef.model.dto.KSeFStatusResponse;
import com.example.magazyn.ksef.service.KsefInvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ksef/invoices")
@Tag(name = "KSeF Invoices", description = "KSeF invoice management — send, status, list")
@SecurityRequirement(name = "bearerAuth")
public class KsefInvoiceController {

    private final KsefInvoiceService ksefInvoiceService;

    public KsefInvoiceController(KsefInvoiceService ksefInvoiceService) {
        this.ksefInvoiceService = ksefInvoiceService;
    }

    @PostMapping("/send/{invoiceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Send an internal invoice to KSeF")
    public ResponseEntity<?> sendInvoice(
            @PathVariable Long invoiceId,
            Authentication auth) {
        try {
            KSeFSendInvoiceResponse response = ksefInvoiceService.sendInvoice(invoiceId, auth.getName());
            return ResponseEntity.ok(response);
        } catch (KsefValidationException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "VALIDATION_ERROR",
                    "message", e.getMessage(),
                    "errors", e.getErrors()
            ));
        } catch (KsefAuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "status", "AUTH_ERROR",
                    "message", e.getMessage()
            ));
        } catch (KsefCommunicationException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                    "status", "API_ERROR",
                    "message", e.getMessage(),
                    "httpStatus", e.getHttpStatus(),
                    "errorCode", e.getErrorCode()
            ));
        }
    }

    @GetMapping("/status/{ksefInvoiceId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get KSeF invoice status")
    public ResponseEntity<KSeFStatusResponse> getStatus(@PathVariable Long ksefInvoiceId) {
        return ResponseEntity.ok(ksefInvoiceService.getInvoiceStatus(ksefInvoiceId));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List KSeF invoices with pagination")
    public ResponseEntity<Page<KSeFInvoiceResponse>> listInvoices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ksefInvoiceService.getAllKsefInvoices(PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get KSeF invoice details")
    public ResponseEntity<KSeFInvoiceResponse> getInvoice(@PathVariable Long id) {
        return ResponseEntity.ok(ksefInvoiceService.getKsefInvoice(id));
    }
}
