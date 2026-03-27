package lk.kelaniya.uok.scrabble.scrabbleapp.controller;

import lk.kelaniya.uok.scrabble.scrabbleapp.dto.enums.RequestStatus;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.secure.AdminRequestDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.service.secure.AdminRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin-request")
@RequiredArgsConstructor
public class AdminRequestController {

    private final AdminRequestService adminRequestService;

    @PostMapping
    public ResponseEntity<Void> requestAdmin(@RequestParam String email) {
        adminRequestService.createRequest(email);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/pending")
    public ResponseEntity<List<AdminRequestDTO>> getPending() {
        return ResponseEntity.ok(adminRequestService.getPendingRequests());
    }

    @PatchMapping("/resolve")
    public ResponseEntity<Void> resolve(
            @RequestParam String requestId,
            @RequestParam RequestStatus status) {
        adminRequestService.resolveRequest(requestId, status);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/status")
    public ResponseEntity<String> getStatus(@RequestParam String email) {
        return ResponseEntity.ok(adminRequestService.getRequestStatus(email));
    }
}
