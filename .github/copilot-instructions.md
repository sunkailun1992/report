# GitHub Copilot Instructions

This repository is the `report` Java/Spring Boot report service. Before suggesting or changing code, read `AGENTS.md` and `docs/ai-coding/README.md`.

Follow these project rules:

- Follow `docs/ai-coding/AI_DIRECTORY_STRUCTURE_GUIDE.md` before adding, moving, or deleting directories.
- Keep Java code under `src/main/java/com/kellen`; tests belong under `src/test/java/com/kellen`.
- Do not nest sibling repositories such as `utils`, `user`, `gateway`, `admin-web`, or `ai` inside this repository.
- Keep Dubbo RPC interfaces and DTOs in sibling `rpc-api`; this service only implements provider code or calls published contracts.
- Do not change existing secrets, RabbitMQ addresses, Nacos addresses, database URLs, or production configuration values. Report file paths and line numbers only.
- Report ownership, tenant isolation, file access, export permissions, async generation, and MQ retry semantics must be enforced by backend services.
