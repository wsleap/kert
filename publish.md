## Publish
Publish to Maven Central

### Register project / package (one time setup)
Create JIRA ticket at https://issues.sonatype.org/
  * Project: Community Support - Open Source Project Repository Hosting (OSSRH)
  * Issue Type: New Project
  * Group Id: ws.leap.kert
  * Project URL: https://github.com/wsleap/kert
  * SCM url: https://github.com/wsleap/kert.git

Ticket: https://issues.sonatype.org/browse/OSSRH-62510

### Publish to Sonatype
1. Update version number (remove SNAPSHOT)
1. Publish to staging area
    ```shell
    gradle clean :kert-compiler:publish -PtargetOs=linux -PtargetArch=x86_64
    gradle clean :kert-compiler:publish -PtargetOs=windows -PtargetArch=x86_64
    gradle clean :kert-compiler:publish -PtargetOs=osx -PtargetArch=x86_64
    gradle :kert-http:publish
    gradle :kert-grpc:publish
    ```
1. Bump version number (add SNAPSHOT back)

Use this if want to check file format for the compiler
```shell
file kert-compiler/build/exe/protoc-gen-grpc-kert
```

### Promote to Maven Central
  * Go to https://oss.sonatype.org/#stagingRepositories
  * Close the staging repository if there is no problem.
  * Release the repository if close succeeded.
