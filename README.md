# qodana-github-app Project

Test project is based on tutorial for the github bot creation.

Tutorial URL: [https://quarkiverse.github.io/quarkiverse-docs/quarkus-github-app/dev/register-github-app.html](https://quarkiverse.github.io/quarkiverse-docs/quarkus-github-app/dev/register-github-app.html)
[https://quarkiverse.github.io/quarkiverse-docs/quarkus-github-app/dev/create-github-app.html](https://quarkiverse.github.io/quarkiverse-docs/quarkus-github-app/dev/create-github-app.html)

Uses:
* JGit to checkout the source code
* Jetbrains Qodana to run checks
* Testcontainers to run Qodana
* Java-sarif library to analyze Qodana result log

Requires `application.properties` file that contain the following properties:

```properties
quarkus.github-app.app-id= #1
quarkus.github-app.app-name= #qodana-bot
quarkus.github-app.webhook-proxy-url= #https://smee.io/12344566
quarkus.github-app.webhook-secret= #abababbcea0fa82e67e2db485fd320d31284ab41
quarkus.github-app.private-key= #-----BEGIN RSA PRIVATE KEY-----\
# MIIEpQIBAAKCAQEAsdGFaf5SQr1oFAJWi4mGcCGtOJYDkRVnyu4evQhA3E2O2FBZ\
# -----END RSA PRIVATE KEY-----

qodana.plugins.dir= #/Users/testuser/Library/Application Support/JetBrains/Toolbox/apps/IDEA-U/ch-0/213.5744.223/IntelliJ IDEA.app.plugins/jpa-buddy
```