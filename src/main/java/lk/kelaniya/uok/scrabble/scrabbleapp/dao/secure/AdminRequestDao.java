package lk.kelaniya.uok.scrabble.scrabbleapp.dao.secure;

import lk.kelaniya.uok.scrabble.scrabbleapp.dto.enums.RequestStatus;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.security.AdminRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AdminRequestDao extends JpaRepository<AdminRequestEntity, String> {
    Optional<AdminRequestEntity> findByUser_UserIdAndStatus(String userId, RequestStatus status);
    List<AdminRequestEntity> findByStatus(RequestStatus status);
    Optional<AdminRequestEntity> findTopByUser_UserIdOrderByRequestedDateDesc(String userId);
}