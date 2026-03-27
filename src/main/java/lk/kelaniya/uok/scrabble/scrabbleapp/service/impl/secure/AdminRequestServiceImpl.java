package lk.kelaniya.uok.scrabble.scrabbleapp.service.impl.secure;

import jakarta.transaction.Transactional;
import lk.kelaniya.uok.scrabble.scrabbleapp.dao.secure.AdminRequestDao;
import lk.kelaniya.uok.scrabble.scrabbleapp.dao.secure.UserDao;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.enums.RequestStatus;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.enums.Role;
import lk.kelaniya.uok.scrabble.scrabbleapp.dto.secure.AdminRequestDTO;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.security.AdminRequestEntity;
import lk.kelaniya.uok.scrabble.scrabbleapp.entity.security.UserEntity;
import lk.kelaniya.uok.scrabble.scrabbleapp.service.secure.AdminRequestService;
import lk.kelaniya.uok.scrabble.scrabbleapp.util.UtilData;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class AdminRequestServiceImpl implements AdminRequestService {

    private final AdminRequestDao adminRequestDao;
    private final UserDao userDao;

    @Override
    public void createRequest(String email) {
        UserEntity user = userDao.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        adminRequestDao.findByUser_UserIdAndStatus(user.getUserId(), RequestStatus.PENDING)
                .ifPresent(r -> { throw new RuntimeException("A pending request already exists"); });

        AdminRequestEntity req = new AdminRequestEntity();
        req.setRequestId(UtilData.generateRequestId());
        req.setUser(user);
        req.setStatus(RequestStatus.PENDING);
        req.setRequestedDate(LocalDate.now());
        adminRequestDao.save(req);
    }

    @Override
    public List<AdminRequestDTO> getPendingRequests() {
        return adminRequestDao.findByStatus(RequestStatus.PENDING)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void resolveRequest(String requestId, RequestStatus status) {
        AdminRequestEntity req = adminRequestDao.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request not found"));
        req.setStatus(status);
        req.setResolvedDate(LocalDate.now());
        adminRequestDao.save(req);

        if (status == RequestStatus.APPROVED) {
            UserEntity user = req.getUser();
            user.setUserRole(Role.ADMIN);
            userDao.save(user);
        }
    }

    @Override
    public String getRequestStatus(String email) {
        UserEntity user = userDao.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return adminRequestDao
                .findTopByUser_UserIdOrderByRequestedDateDesc(user.getUserId())
                .map(r -> r.getStatus().name())
                .orElse("NONE");
    }

    private AdminRequestDTO toDTO(AdminRequestEntity entity) {
        AdminRequestDTO dto = new AdminRequestDTO();
        dto.setRequestId(entity.getRequestId());
        dto.setUserId(entity.getUser().getUserId());
        dto.setFirstName(entity.getUser().getFirstName());
        dto.setLastName(entity.getUser().getLastName());
        dto.setEmail(entity.getUser().getEmail());
        dto.setStatus(entity.getStatus());
        dto.setRequestedDate(entity.getRequestedDate());
        dto.setResolvedDate(entity.getResolvedDate());
        return dto;
    }
}