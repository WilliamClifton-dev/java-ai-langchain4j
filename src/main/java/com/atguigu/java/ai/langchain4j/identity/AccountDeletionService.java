package com.atguigu.java.ai.langchain4j.identity;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountDeletionService {
    public static final String CONFIRMATION = "DELETE_MY_ACCOUNT";

    private final AccountDataLifecycleMapper mapper;

    public AccountDeletionService(AccountDataLifecycleMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional
    public void delete(String userId, String confirmation) {
        if (!CONFIRMATION.equals(confirmation)) {
            throw new InvalidAccountDeletionConfirmationException();
        }
        if (mapper.findAccount(userId).isEmpty()) {
            throw new AccountDataNotFoundException();
        }

        // Remove restrictive dependants first; the remaining owned tables cascade.
        mapper.anonymizeAuditEvents(userId);
        mapper.deleteWeeklyReviews(userId);
        mapper.deletePlanVersions(userId);
        mapper.deletePlans(userId);
        if (mapper.deleteAccount(userId) != 1) {
            throw new AccountDataNotFoundException();
        }
    }
}
