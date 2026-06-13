package com.example.magazyn.controller;

import com.example.magazyn.dto.CreateInvoiceRequest;
import com.example.magazyn.dto.InvoiceResponse;
import com.example.magazyn.dto.PayInvoiceRequest;
import com.example.magazyn.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invoices")
@Tag(name = "Invoices", description = "FV invoice management")
@SecurityRequirement(name = "bearerAuth")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List invoices with optional status filter and year")
    public ResponseEntity<?> getAllInvoices(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "0") int size) {
        if (size > 0) {
            Page<InvoiceResponse> result = invoiceService.getAllInvoicesPaged(
                    PageRequest.of(page, size));
            return ResponseEntity.ok(result);
        }
        return ResponseEntity.ok(invoiceService.getAllInvoices(status, year));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get invoice details with items")
    public ResponseEntity<InvoiceResponse> getInvoice(@PathVariable Long id) {
        return ResponseEntity.ok(invoiceService.getInvoice(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Create a blank DRAFT invoice")
    public ResponseEntity<InvoiceResponse> createInvoice(
            @Valid @RequestBody CreateInvoiceRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        InvoiceResponse invoice = invoiceService.createBlankInvoice(request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(invoice);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Update a DRAFT invoice")
    public ResponseEntity<InvoiceResponse> updateInvoice(
            @PathVariable Long id,
            @Valid @RequestBody CreateInvoiceRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(invoiceService.updateInvoice(id, request, username));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Delete a DRAFT invoice")
    public ResponseEntity<Void> deleteInvoice(
            @PathVariable Long id,
            Authentication authentication) {
        String username = authentication.getName();
        invoiceService.deleteInvoice(id, username);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/issue")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Issue a DRAFT invoice (DRAFT → ISSUED)")
    public ResponseEntity<InvoiceResponse> issueInvoice(
            @PathVariable Long id,
            Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(invoiceService.issueInvoice(id, username));
    }

    @PostMapping("/from-document/{documentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Generate an invoice from a confirmed WZ document")
    public ResponseEntity<InvoiceResponse> generateFromDocument(
            @PathVariable Long documentId,
            Authentication authentication) {
        String username = authentication.getName();
        InvoiceResponse invoice = invoiceService.generateFromDocument(documentId, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(invoice);
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Mark invoice as paid")
    public ResponseEntity<InvoiceResponse> payInvoice(
            @PathVariable Long id,
            @RequestBody(required = false) PayInvoiceRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        String paymentMethod = request != null ? request.paymentMethod() : null;
        String paymentAccount = request != null ? request.paymentAccount() : null;
        return ResponseEntity.ok(invoiceService.payInvoice(id, paymentMethod, paymentAccount, username));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Cancel an invoice")
    public ResponseEntity<InvoiceResponse> cancelInvoice(
            @PathVariable Long id,
            Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(invoiceService.cancelInvoice(id, username));
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Export invoice to PDF")
    public ResponseEntity<byte[]> exportInvoicePdf(@PathVariable Long id) {
        byte[] pdfBytes = invoiceService.exportInvoicePdf(id);
        InvoiceResponse inv = invoiceService.getInvoice(id);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + inv.number() + ".pdf\"")
                .header("Content-Type", "application/pdf")
                .body(pdfBytes);
    }
}
