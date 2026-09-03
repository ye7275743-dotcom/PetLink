# M06 Selective Merge Manifest

This manifest exists because a full candidate ZIP must NOT be overlaid onto the current PetLink workspace.

## Never overwrite from this candidate

Do not copy these candidate files into the current workspace:

- `README-CODING.md`
- `server/README.md`
- any M01-M05 implementation/evidence document
- any M01-M05 business source or test file
- any Frozen API or SQL baseline file

The current workspace versions remain authoritative.

## Files to merge for M06

Copy/add only these M06 implementation paths:

```text
server/src/main/java/com/petlink/modules/followup/package-info.java
server/src/main/java/com/petlink/modules/followup/controller/FollowUpAdminController.java
server/src/main/java/com/petlink/modules/followup/controller/FollowUpAdoptionRecordController.java
server/src/main/java/com/petlink/modules/followup/controller/FollowUpController.java
server/src/main/java/com/petlink/modules/followup/controller/FollowUpMediaController.java
server/src/main/java/com/petlink/modules/followup/controller/FollowUpRescuerController.java
server/src/main/java/com/petlink/modules/followup/dto/SubmitFollowUpRequest.java
server/src/main/java/com/petlink/modules/followup/entity/FollowUpImage.java
server/src/main/java/com/petlink/modules/followup/entity/FollowUpRecord.java
server/src/main/java/com/petlink/modules/followup/mapper/FollowUpImageMapper.java
server/src/main/java/com/petlink/modules/followup/mapper/FollowUpRecordMapper.java
server/src/main/java/com/petlink/modules/followup/service/FollowUpAccessService.java
server/src/main/java/com/petlink/modules/followup/service/FollowUpFileBindingService.java
server/src/main/java/com/petlink/modules/followup/service/FollowUpMediaService.java
server/src/main/java/com/petlink/modules/followup/service/FollowUpQueryService.java
server/src/main/java/com/petlink/modules/followup/service/FollowUpRequestNormalizer.java
server/src/main/java/com/petlink/modules/followup/service/FollowUpResponseAssembler.java
server/src/main/java/com/petlink/modules/followup/service/FollowUpService.java
server/src/main/java/com/petlink/modules/followup/service/FollowUpTransactionalService.java
server/src/main/java/com/petlink/modules/followup/vo/FollowUpImageResponse.java
server/src/main/java/com/petlink/modules/followup/vo/FollowUpRecordResponse.java

server/src/test/java/com/petlink/modules/followup/FollowUpControllerSecurityWebTest.java
server/src/test/java/com/petlink/modules/followup/FollowUpFileBindingServiceTest.java
server/src/test/java/com/petlink/modules/followup/FollowUpQueryServiceTest.java
server/src/test/java/com/petlink/modules/followup/FollowUpRequestNormalizerTest.java
server/src/test/java/com/petlink/modules/followup/FollowUpServiceIdempotencyTest.java
server/src/test/java/com/petlink/modules/followup/FollowUpTransactionalServiceTest.java

server/scripts/verify_m06_static.py
server/scripts/smoke-m06.ps1
server/docs/M06-IMPLEMENTATION-CANDIDATE.md
server/docs/M06-SELECTIVE-MERGE-MANIFEST.md
```

## Shared prerequisites

The accepted M05 workspace already contains the Frozen `follow_up_record`, `follow_up_image`, `temporary_file` schema and the shared file-association support required by M06. Therefore this delta intentionally contains no SQL/API overwrite and no shared M01-M05 source overwrite.

## Required post-merge procedure

1. stop the old M05 process on port 8080;
2. merge only the paths above;
3. run the full Maven suite and M02-M06 static verifiers;
4. start the merged backend;
5. run `scripts/smoke-m06.ps1` against the real MySQL instance;
6. confirm the concurrent idempotency stage returns exactly one 201 and one 200 with the same `followUpId`;
7. only then update workspace evidence/status from M06 Candidate to M06 Frozen.
