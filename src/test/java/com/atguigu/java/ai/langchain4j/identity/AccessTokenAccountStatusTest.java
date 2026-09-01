package com.atguigu.java.ai.langchain4j.identity;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccessTokenAccountStatusTest {

    private static final String USER_ID = "00000000-0000-4000-8000-000000000201";

    @Test
    void acceptsOnlyAnActiveAccount() {
        UserAccountMapper mapper = mock(UserAccountMapper.class);
        when(mapper.findById(USER_ID)).thenReturn(Optional.of(account(AccountStatus.ACTIVE)));

        assertThat(new AccountStatusTokenValidator(mapper).validate(token()).hasErrors())
                .isFalse();
    }

    @Test
    void rejectsDeletedLockedAndMissingAccountsWithoutDisclosingState() {
        for (AccountStatus status : new AccountStatus[]{AccountStatus.DELETED, AccountStatus.LOCKED}) {
            UserAccountMapper mapper = mock(UserAccountMapper.class);
            when(mapper.findById(USER_ID)).thenReturn(Optional.of(account(status)));

            var result = new AccountStatusTokenValidator(mapper).validate(token());

            assertThat(result.hasErrors()).isTrue();
            assertThat(result.getErrors().iterator().next().getDescription())
                    .isEqualTo("Account is not active");
        }

        UserAccountMapper missing = mock(UserAccountMapper.class);
        when(missing.findById(USER_ID)).thenReturn(Optional.empty());
        assertThat(new AccountStatusTokenValidator(missing).validate(token()).hasErrors())
                .isTrue();
    }

    @Test
    void failsClosedWhenAccountLookupFails() {
        UserAccountMapper mapper = mock(UserAccountMapper.class);
        when(mapper.findById(USER_ID)).thenThrow(new IllegalStateException("database detail"));

        var result = new AccountStatusTokenValidator(mapper).validate(token());

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors().iterator().next().getDescription())
                .isEqualTo("Account is not active");
    }

    private Jwt token() {
        return Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject(USER_ID)
                .build();
    }

    private UserAccount account(AccountStatus status) {
        return new UserAccount(USER_ID, "account@example.com", "hash", status,
                java.time.Instant.EPOCH, java.time.Instant.EPOCH);
    }
}
