package com.jobtracker.backendJobTracker.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jobtracker.backendJobTracker.application.ApplicationRepository;
import com.jobtracker.backendJobTracker.application.enums.ApplicationStatus;
import com.jobtracker.backendJobTracker.exception.BusinessRuleException;
import com.jobtracker.backendJobTracker.exception.ConflictException;
import com.jobtracker.backendJobTracker.exception.ResourceNotFoundException;
import com.jobtracker.backendJobTracker.status.dto.CreateStatusRequest;
import com.jobtracker.backendJobTracker.status.dto.StatusResponse;
import com.jobtracker.backendJobTracker.status.dto.UpdateStatusCategoryRequest;
import com.jobtracker.backendJobTracker.user.User;
import com.jobtracker.backendJobTracker.user.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatusService {

    private final StatusCategoryRepository repository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    public List<StatusResponse> list(UUID userId) {
        return repository.findByUserIdOrderByPositionAsc(userId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public StatusResponse create(UUID userId, CreateStatusRequest req) {
        String name = req.getName().trim();
        if (repository.existsByUserIdAndNameIgnoreCase(userId, name)) {
            throw new ConflictException("Status '" + name + "' already exists");
        }
        StatusCategory s = new StatusCategory();
        s.setUser(userRepository.getReferenceById(userId));
        s.setName(name);
        s.setColor(req.getColor());
        s.setSystemType(null); // user-created statuses carry no semantics
        s.setTerminal(req.isTerminal());
        s.setPosition((int) repository.countByUserId(userId)); // append to the end
        return toResponse(repository.save(s));
    }

    @Transactional
    public StatusResponse update(UUID userId, UUID id, UpdateStatusCategoryRequest req) {
        StatusCategory s = fetchOwned(userId, id);
        if (req.getName() != null) {
            String name = req.getName().trim();
            if (!name.equalsIgnoreCase(s.getName())
                    && repository.existsByUserIdAndNameIgnoreCase(userId, name)) {
                throw new ConflictException("Status '" + name + "' already exists");
            }
            s.setName(name);
        }
        if (req.getColor() != null) {
            s.setColor(req.getColor());
        }
        if (req.getTerminal() != null) {
            s.setTerminal(req.getTerminal());
        }
        return toResponse(repository.save(s));
    }

    /** Delete a status, optionally moving its applications to another status first. */
    @Transactional
    public void delete(UUID userId, UUID id, UUID reassignToId) {
        StatusCategory s = fetchOwned(userId, id);
        if (repository.countByUserId(userId) <= 1) {
            throw new BusinessRuleException("Cannot delete the last status");
        }
        long inUse = applicationRepository.countByStatusId(id);
        if (inUse > 0) {
            if (reassignToId == null) {
                throw new BusinessRuleException(
                        "This status has " + inUse + " application(s). Choose a status to move them to.");
            }
            StatusCategory target = fetchOwned(userId, reassignToId);
            applicationRepository.reassignStatus(id, target.getId());
        }
        repository.delete(s);
    }

    @Transactional
    public List<StatusResponse> reorder(UUID userId, List<UUID> orderedIds) {
        List<StatusCategory> owned = repository.findByUserIdOrderByPositionAsc(userId);
        Map<UUID, StatusCategory> byId = owned.stream()
                .collect(Collectors.toMap(StatusCategory::getId, s -> s));
        int pos = 0;
        for (UUID sid : orderedIds) {
            StatusCategory s = byId.get(sid);
            if (s == null) {
                throw new BusinessRuleException("Unknown status: " + sid);
            }
            s.setPosition(pos++);
        }
        repository.saveAll(owned);
        return list(userId);
    }

    /** Seed the 9 default statuses for a brand-new user (called from AuthService.register). */
    @Transactional
    public void seedDefaults(User user) {
        List<Object[]> defs = List.of(
                new Object[]{"Saved", "#64748b", ApplicationStatus.SAVED, false},
                new Object[]{"Applied", "#3b82f6", ApplicationStatus.APPLIED, false},
                new Object[]{"Screening", "#6366f1", ApplicationStatus.SCREENING, false},
                new Object[]{"Interview", "#a855f7", ApplicationStatus.INTERVIEW, false},
                new Object[]{"Final", "#f59e0b", ApplicationStatus.FINAL, false},
                new Object[]{"Offer", "#22c55e", ApplicationStatus.OFFER, true},
                new Object[]{"Rejected", "#ef4444", ApplicationStatus.REJECTED, true},
                new Object[]{"Withdrawn", "#9ca3af", ApplicationStatus.WITHDRAWN, true},
                new Object[]{"Ghosted", "#9ca3af", ApplicationStatus.GHOSTED, true});
        int pos = 0;
        for (Object[] d : defs) {
            StatusCategory s = new StatusCategory();
            s.setUser(user);
            s.setName((String) d[0]);
            s.setColor((String) d[1]);
            s.setSystemType((ApplicationStatus) d[2]);
            s.setTerminal((Boolean) d[3]);
            s.setPosition(pos++);
            repository.save(s);
        }
    }

    private StatusCategory fetchOwned(UUID userId, UUID id) {
        return repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Status not found: " + id));
    }

    private StatusResponse toResponse(StatusCategory s) {
        StatusResponse r = new StatusResponse();
        r.setId(s.getId());
        r.setName(s.getName());
        r.setColor(s.getColor());
        r.setPosition(s.getPosition());
        r.setSystemType(s.getSystemType());
        r.setTerminal(s.isTerminal());
        return r;
    }
}
