# Runtime Dependency Security Audit

**Audit date:** 2026-08-16
**Scope:** Maven runtime dependency graph
**Advisory source:** OSV API
**Release target:** L1 public beta

## Result

The current runtime graph passes the Task 18 known-vulnerability gate:

```text
OSV runtime audit: dependencies=126, findings=0
```

No OSV advisory matched the resolved package versions at scan time. The default
gate fails on Critical, High, or unknown-severity findings. The full backend
suite also passed with 132 tests, 0 failures, 0 errors and one opt-in external
model test skipped.

This result replaces the earlier family-level assessment. That assessment used
the Spring Boot 3.3.5 graph and left individual advisories untriaged, so it was
not sufficient to complete Task 18.

## Reproducible Check

Run from the repository root:

```powershell
./scripts/security/osv-audit.ps1
```

The script resolves the Maven runtime graph, submits exact Maven package names
and versions to OSV, fetches advisory details, writes
`target/security/osv-report.json`, and exits non-zero when a finding meets the
configured threshold.

To fail on Moderate and above:

```powershell
./scripts/security/osv-audit.ps1 -FailOn MODERATE
```

## Before Remediation

A fresh query of the Spring Boot 3.3.5 graph on 2026-08-16 found 91 unique
matches across 25 packages:

| Severity | Count |
|---|---:|
| Critical | 7 |
| High | 34 |
| Moderate | 35 |
| Low | 15 |

Critical/High matches included Tomcat, Spring Security, Spring Framework,
Spring Boot, Spring Data, Netty, Jackson, Micrometer and OpenNLP. Continuing to
describe 3.3.5 as an acceptable clean baseline would have hidden known release
blockers.

## Remediation

| Component | Previous | Accepted version | Reason |
|---|---:|---:|---|
| Spring Boot | 3.3.5 | 3.5.16 | Supported Java 17 maintenance line and updated Spring ecosystem |
| Spring Framework | 6.1.14 | 6.2.19 | Clears current Critical/High framework matches |
| Spring Security | 6.3.4 | 6.5.11 | Clears header, password and authorization advisories |
| Spring Data Commons | 3.3.5 | 3.5.13 | Clears unbounded property-cache advisory |
| Tomcat Embed | 10.1.31 | 10.1.55 | Clears current request handling and authentication advisories |
| Micrometer | 1.13.6 | 1.15.12 | Clears HTTP instrumentation denial-of-service advisory |
| Jackson | 2.17.2 | 2.21.5 | Clears current databind/core matches |
| Netty | 4.1.114 | 4.1.136.Final | Clears codec and handler resource-exhaustion matches |
| Log4j API | 2.23.1 | 2.26.1 | Clears MapMessage JSON encoding match |
| Commons Lang | 3.14.0 | 3.18.0 | Clears uncontrolled recursion match |
| OpenNLP | 1.9.4 | 2.5.11 | Clears XXE, arbitrary class loading and allocation matches |
| MyBatis Spring Boot | 3.0.3 | 3.0.5 | Current Boot 3.2-3.5 compatible maintenance release |
| OpenAPI UI | Knife4j 4.3.0 | Springdoc 2.8.17 | Removes the adapter that blocked the supported Boot upgrade |

The project inherits Spring Boot dependency management from its parent; the
duplicate Boot BOM import was removed. Jackson, Netty and Log4j BOMs are
imported before the legacy LangChain4j BOM so that it cannot pull security-fixed
families back to older versions. OpenNLP is explicitly managed because it is a
LangChain4j transitive dependency.

## Compatibility Evidence

```text
mvn -q clean test
Tests: 132, failures: 0, errors: 0, skipped: 1

mvn -q -DskipTests package
PASS

docker compose config --quiet
PASS
```

OpenAPI path drift tests pass against Springdoc 2.8.17. Existing LangChain4j
provider adapters compile and all default provider-isolated tests pass. Default
tests still require no live MySQL, Redis or model provider.

## Limitations And Review Policy

- OSV reports known advisories; zero matches do not prove a dependency is
  trustworthy or that the application has no vulnerability.
- The scan is network-backed and intentionally separate from the external-free
  default test suite. Task 22 must run it as a release/security job.
- OWASP Dependency-Check was not accepted as evidence because NVD and Retire.js
  feeds did not complete without an NVD API key.
- This audit does not include DAST/IAST, license compliance, dependency
  provenance verification or penetration testing. Those remain separate
  release-hardening activities and must not be implied by this result.
- Re-run after every dependency change and before a release. Next scheduled
  review: 2026-09-16.

## Authoritative Sources

- OSV API: https://google.github.io/osv.dev/api/
- Spring Boot Maven dependency management: https://docs.spring.io/spring-boot/maven-plugin/using.html
- Springdoc: https://springdoc.org/
- MyBatis Spring Boot compatibility: https://github.com/mybatis/spring-boot-starter#requirements

**Task 18 dependency gate:** PASS as of 2026-08-16.
**L2/enterprise security readiness:** NOT CLAIMED.
