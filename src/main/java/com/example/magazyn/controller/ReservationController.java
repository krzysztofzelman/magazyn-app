package com.example.magazyn.controller;

import com.example.magazyn.dto.CreateReservationRequest;
import com.example.magazyn.dto.ReservationResponse;
import com.example.magazyn.entity.ReservationStatus;
import com.example.magazyn.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
@Tag(name = "Rezerwacje stanów", description = "Zarządzanie rezerwacjami stanów magazynowych")
@SecurityRequirement(name = "bearerAuth")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    @Operation(summary = "Utwórz rezerwację stanu", description = "Tworzy rezerwację dla produktu. Wymaga roli ADMIN.")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReservationResponse> createReservation(
            @Valid @RequestBody CreateReservationRequest request,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "anonymous";
        ReservationResponse response = reservationService.reserve(request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Lista rezerwacji", description = "Zwraca listę rezerwacji z opcjonalnym filtrowaniem po produkcie i statusie.")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ReservationResponse>> getReservations(
            @RequestParam(required = false) @Parameter(description = "ID produktu") Long productId,
            @RequestParam(required = false) @Parameter(description = "Status rezerwacji") ReservationStatus status) {
        return ResponseEntity.ok(reservationService.getReservations(productId, status));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Zwolnij rezerwację", description = "Zwalnia (anuluje) aktywną rezerwację. Wymaga roli ADMIN.")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReservationResponse> releaseReservation(
            @PathVariable @Parameter(description = "ID rezerwacji") Long id,
            Authentication authentication) {
        String username = authentication != null ? authentication.getName() : "anonymous";
        ReservationResponse response = reservationService.release(id, username);
        return ResponseEntity.ok(response);
    }
}
