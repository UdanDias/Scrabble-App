package lk.kelaniya.uok.scrabble.scrabbleapp.service.secure;

import lk.kelaniya.uok.scrabble.scrabbleapp.dto.enums.RequestStatus;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.secure.AdminRequestDTO;

import java.util.List;

public interface AdminRequestService {

    List<AdminRequestDTO> getPendingRequests();
    void resolveRequest(String requestId, RequestStatus status);

    void createRequest(String email);
    String getRequestStatus(String email);
}