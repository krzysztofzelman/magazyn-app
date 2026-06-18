package com.example.magazyn.ksef;

import com.example.magazyn.ksef.exception.KsefAuthenticationException;
import com.example.magazyn.ksef.exception.KsefCommunicationException;
import com.example.magazyn.ksef.exception.KsefValidationException;
import com.example.magazyn.ksef.model.dto.*;
import com.example.magazyn.ksef.model.enums.KSeFStatus;
import com.example.magazyn.ksef.service.KsefAuthService;
import com.example.magazyn.ksef.service.KsefInvoiceService;
import com.example.magazyn.ksef.service.KsefValidationService;
import com.example.magazyn.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for KSeF module with mocked service layer.
 * Tests controller endpoints, exception handling, and validation.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class KsefIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KsefInvoiceService ksefInvoiceService;

    @MockBean
    private KsefAuthService ksefAuthService;

    @Autowired
    private JwtUtil jwtUtil;

    private String adminToken;

    @BeforeEach
    void setUp() {
        adminToken = jwtUtil.generateToken("admin", "ROLE_ADMIN", 1L);
    }

    // ──────────────────────────────────────────────
    // Invoice sending tests
    // ──────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldSendInvoiceToKSeF() throws Exception {
        KSeFSendInvoiceResponse response = new KSeFSendInvoiceResponse(
                "KSEF-REF-123", "SENT", "Faktura wysłana pomyślnie", 1L);

        when(ksefInvoiceService.sendInvoice(eq(1L), anyString())).thenReturn(response);

        mockMvc.perform(post("/api/ksef/invoices/send/{invoiceId}", 1L)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ksefReferenceNumber").value("KSEF-REF-123"))
                .andExpect(jsonPath("$.status").value("SENT"))
                .andExpect(jsonPath("$.message").value("Faktura wysłana pomyślnie"))
                .andExpect(jsonPath("$.ksefInvoiceId").value(1));

        verify(ksefInvoiceService).sendInvoice(1L, "admin");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldHandleKSeFAuthenticationError() throws Exception {
        when(ksefInvoiceService.sendInvoice(eq(1L), anyString()))
                .thenThrow(new KsefAuthenticationException("Brak aktywnej sesji KSeF"));

        mockMvc.perform(post("/api/ksef/invoices/send/{invoiceId}", 1L)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("AUTH_ERROR"))
                .andExpect(jsonPath("$.message").value("Brak aktywnej sesji KSeF"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldHandleCommunicationError() throws Exception {
        when(ksefInvoiceService.sendInvoice(eq(1L), anyString()))
                .thenThrow(new KsefCommunicationException("KSeF API unavailable", 503, "SERVICE_UNAVAILABLE"));

        mockMvc.perform(post("/api/ksef/invoices/send/{invoiceId}", 1L)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value("API_ERROR"))
                .andExpect(jsonPath("$.httpStatus").value(503))
                .andExpect(jsonPath("$.errorCode").value("SERVICE_UNAVAILABLE"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldValidateInvoiceBeforeSend() throws Exception {
        when(ksefInvoiceService.sendInvoice(eq(1L), anyString()))
                .thenThrow(new KsefValidationException(List.of(
                        "NIP nabywcy musi zawierać 10 cyfr",
                        "Kwota netto musi być większa od zera")));

        mockMvc.perform(post("/api/ksef/invoices/send/{invoiceId}", 1L)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0]").value("NIP nabywcy musi zawierać 10 cyfr"))
                .andExpect(jsonPath("$.errors[1]").value("Kwota netto musi być większa od zera"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldRejectNonAdminSendingInvoice() throws Exception {
        mockMvc.perform(post("/api/ksef/invoices/send/{invoiceId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    // ──────────────────────────────────────────────
    // Invoice status tests
    // ──────────────────────────────────────────────

    @Test
    @WithMockUser
    void shouldGetInvoiceStatus() throws Exception {
        KSeFStatusResponse statusResponse = new KSeFStatusResponse(
                1L, "INV/2026/001", KSeFStatus.ACCEPTED,
                "KSEF-REF-123", "200", "Accepted");

        when(ksefInvoiceService.getInvoiceStatus(1L)).thenReturn(statusResponse);

        mockMvc.perform(get("/api/ksef/invoices/status/{id}", 1L)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.invoiceNumber").value("INV/2026/001"))
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.ksefReferenceNumber").value("KSEF-REF-123"));
    }

    // ──────────────────────────────────────────────
    // Invoice listing tests
    // ──────────────────────────────────────────────

    @Test
    @WithMockUser
    void shouldListKSeFInvoices() throws Exception {
        KSeFInvoiceResponse invoice = new KSeFInvoiceResponse(
                1L, 100L, "INV/2026/001", "KSEF-REF-123",
                KSeFStatus.SENT, "200", null, 1,
                LocalDateTime.now(), null, null,
                "admin", LocalDateTime.now(), LocalDateTime.now());

        Page<KSeFInvoiceResponse> page = new PageImpl<>(List.of(invoice));

        when(ksefInvoiceService.getAllKsefInvoices(any(PageRequest.class))).thenReturn(page);

        mockMvc.perform(get("/api/ksef/invoices")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].ksefReferenceNumber").value("KSEF-REF-123"))
                .andExpect(jsonPath("$.content[0].status").value("SENT"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser
    void shouldGetSingleKSeFInvoice() throws Exception {
        KSeFInvoiceResponse invoice = new KSeFInvoiceResponse(
                1L, 100L, "INV/2026/001", "KSEF-REF-123",
                KSeFStatus.SENT, "200", null, 2,
                LocalDateTime.now(), null, null,
                "admin", LocalDateTime.now(), LocalDateTime.now());

        when(ksefInvoiceService.getKsefInvoice(1L)).thenReturn(invoice);

        mockMvc.perform(get("/api/ksef/invoices/{id}", 1L)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.invoiceNumber").value("INV/2026/001"))
                .andExpect(jsonPath("$.status").value("SENT"));
    }

    // ──────────────────────────────────────────────
    // Session management tests
    // ──────────────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldInitiateSession() throws Exception {
        KSeFSessionResponse sessionResponse = new KSeFSessionResponse(
                1L, "REF-001", true,
                LocalDateTime.now(), LocalDateTime.now().plusHours(8),
                null, null, "1234567890", "1.6", null);

        when(ksefAuthService.getOrCreateSession("admin")).thenReturn(sessionResponse);

        mockMvc.perform(post("/api/ksef/admin/session/init")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referenceNumber").value("REF-001"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.nip").value("1234567890"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldCloseSession() throws Exception {
        doNothing().when(ksefAuthService).closeSession(1L, "admin");

        mockMvc.perform(post("/api/ksef/admin/session/{sessionId}/close", 1L)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("closed"));

        verify(ksefAuthService).closeSession(1L, "admin");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldListSessions() throws Exception {
        KSeFSessionResponse session = new KSeFSessionResponse(
                1L, "REF-001", true,
                LocalDateTime.now(), LocalDateTime.now().plusHours(8),
                null, null, "1234567890", "1.6", null);

        when(ksefAuthService.getSessions()).thenReturn(List.of(session));

        mockMvc.perform(get("/api/ksef/admin/sessions")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].referenceNumber").value("REF-001"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldGetDashboard() throws Exception {
        KSeFDashboardStats stats = new KSeFDashboardStats(
                100L, 50L, 5L, 20L, 10L, 15L,
                2L, true, "1234567890", "ACTIVE");

        when(ksefInvoiceService.getDashboardStats()).thenReturn(stats);

        mockMvc.perform(get("/api/ksef/admin/dashboard")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSent").value(100))
                .andExpect(jsonPath("$.accepted").value(50))
                .andExpect(jsonPath("$.activeSessions").value(2))
                .andExpect(jsonPath("$.sessionActive").value(true));
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldDenyNonAdminAccessToAdminEndpoints() throws Exception {
        mockMvc.perform(post("/api/ksef/admin/session/init")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/ksef/admin/dashboard"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/ksef/admin/sessions"))
                .andExpect(status().isForbidden());
    }

    // ──────────────────────────────────────────────
    // Validation service unit test
    // ──────────────────────────────────────────────

    @Test
    void validationServiceShouldRejectInvalidInvoice() {
        KsefValidationService validationService = new KsefValidationService();

        KSeFInvoiceRequest invalidRequest = new KSeFInvoiceRequest(
                "", // blank invoice number
                null, // no issue date
                null, // no sale date
                null, // no due date
                BigDecimal.ZERO, // net = 0
                BigDecimal.ZERO, // vat = 0
                BigDecimal.ZERO, // gross = 0
                "USD", // not PLN
                "123", // invalid NIP
                "",
                "",
                null, // no seller NIP
                null, null, null,
                null, null,
                List.of(), // no items
                null);

        List<String> errors = validationService.validate(invalidRequest);
        assert errors.size() >= 5 : "Expected at least 5 validation errors, got " + errors.size();
        assert errors.stream().anyMatch(e -> e.contains("NIP")) : "Expected NIP validation error";
        assert errors.stream().anyMatch(e -> e.contains("PLN")) : "Expected currency validation error";
    }

    @Test
    void validationServiceShouldAcceptValidInvoice() {
        KsefValidationService validationService = new KsefValidationService();

        KSeFInvoiceRequest validRequest = new KSeFInvoiceRequest(
                "INV/2026/001",
                java.time.LocalDate.now().minusDays(1),
                java.time.LocalDate.now().minusDays(1),
                java.time.LocalDate.now().plusDays(30),
                new BigDecimal("1000.00"),
                new BigDecimal("230.00"),
                new BigDecimal("1230.00"),
                "PLN",
                "1234567890",
                "Buyer Company",
                "Buyer Address",
                "0987654321",
                "Seller Company",
                "Seller Address",
                "TRANSFER",
                "PL1234567890",
                List.of(new KSeFInvoiceItemRequest(
                        "Product A", "szt.", new BigDecimal("10"),
                        new BigDecimal("100.00"), new BigDecimal("23"),
                        new BigDecimal("1000.00"), new BigDecimal("230.00"),
                        new BigDecimal("1230.00"))),
                "Payment by transfer");

        List<String> errors = validationService.validate(validRequest);
        assert errors.isEmpty() : "Expected no validation errors, got: " + errors;
    }
}
