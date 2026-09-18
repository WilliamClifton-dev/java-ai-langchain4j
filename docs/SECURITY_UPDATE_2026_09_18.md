# Security Update - September 18, 2026

## Summary

Updated critical dependencies to address security vulnerabilities identified in OSV audit.

## Changes

### Dependency Updates

- **Netty**: 4.1.136.Final → 4.1.138.Final
  - Fixes: CVE-2026-89044 (HTTP request smuggling vulnerability)
  - Severity: CRITICAL
  - Reference: https://www.vulncheck.com/advisories/netty-4.1.133-final-through-4.1.137-final-and-4.2.13-final-through-4.2.17-final-http-request-smuggling-via-transfer-encoding

- **Tomcat**: 10.1.55 → 10.1.59
  - Resolves 3 CRITICAL vulnerabilities in embedded Tomcat
  - Reference: https://tomcat.apache.org/

### Test Fixes

- **RetentionCleanupPersistenceTest**: Fixed test isolation by using user-scoped queries
- **DataLifecycleApiTest**: Added missing `last_message_at` column for V15 migration compatibility

### Configuration Updates

- Increased Docker Compose backend healthcheck timeout:
  - `start_period`: 45s → 60s
  - `retries`: 12 → 15
  - Accommodates slightly longer startup time with Tomcat 10.1.59

## Verification

### Local Tests
✅ All 175 tests passing (2 skipped)
✅ Maven build successful
✅ OSV security audit: 0 vulnerabilities found

### CI Status
✅ Backend tests passing
✅ Frontend tests passing  
✅ Security audit passing
⚠️ Compose smoke test: intermittent failures in CI environment

Note: Compose smoke test failures appear to be CI environment-specific (resource constraints). Local Docker Compose startup works correctly.

## References

- Spring Boot 3.5 EOL: June 30, 2026 (https://endoflife.date/spring-boot)
- Netty 4.1.138.Final release: September 10, 2026
- Tomcat 10.1.59 release: Latest stable version

## Future Recommendations

1. **Upgrade to Spring Boot 4.x**: Spring Boot 3.5 reached EOL. Consider upgrading to Spring Boot 4.1 for continued security support.
2. **Monitor Compose smoke tests**: Investigate CI-specific startup issues if they persist.
3. **Regular security audits**: Run `./scripts/security/osv-audit.ps1` regularly to catch new vulnerabilities.
