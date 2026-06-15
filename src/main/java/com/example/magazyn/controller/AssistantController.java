package com.example.magazyn.controller;

import com.example.magazyn.dto.AssistantRequest;
import com.example.magazyn.dto.AssistantResponse;
import com.example.magazyn.service.AssistantService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private final AssistantService assistantService;

    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping("/chat")
    public ResponseEntity<AssistantResponse> chat(@Valid @RequestBody AssistantRequest request) {
        AssistantResponse response = assistantService.chat(request);
        return ResponseEntity.ok(response);
    }
}
