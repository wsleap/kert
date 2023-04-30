## Publish
Publish to Maven Central

### You must have these properties in your '~/.gradle/gradle.properties'
```properties
ossrhUsername=<your sonatype username>
ossrhPassword=<your sonatype password>
signing.keyId=<sign key id>
signing.password=<sign key password>
signing.secretKeyRingFile=<sign key ring file> ex. "/home/<user>/.gnupg/secring.gpg"
```

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
# Linux 64bit  
gradle clean :kert-grpc-compiler:publish -PtargetOs=linux -PtargetArch=x86_64
# Windows 64bit
gradle clean :kert-grpc-compiler:publish -PtargetOs=windows -PtargetArch=x86_64
# Mac Intel
gradle clean :kert-grpc-compiler:publish -PtargetOs=osx -PtargetArch=x86_64
# Mac M1/M2
gradle clean :kert-grpc-compiler:publish -PtargetOs=osx -PtargetArch=aarch_64
gradle :kert-http:publish
gradle :kert-grpc:publish
gradle :kert-graphql:publish
```
1. Bump version number (add SNAPSHOT back)

Use this to check file format for the compiler
```shell
file kert-grpc-compiler/build/exe/protoc-gen-grpc-kert
```

### Promote to Maven Central
  * Go to https://oss.sonatype.org/#stagingRepositories
  * Close the staging repository if there is no problem.
  * Release the repository if close succeeded.
