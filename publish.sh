./gradlew clean :kert-compiler:publish -PtargetOs=linux -PtargetArch=x86_64
./gradlew clean :kert-compiler:publish -PtargetOs=windows -PtargetArch=x86_64
./gradlew clean :kert-compiler:publish -PtargetOs=osx -PtargetArch=x86_64
./gradlew :kert-http:publish
./gradlew :kert-grpc:publish
./gradlew :kert-graphql:publish

echo Artifacts are uploaded to Sonatype.
echo Go to https://oss.sonatype.org/#stagingRepositories to release the artifacts to Maven Central.
