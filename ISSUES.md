# SonarCloud Issues (branch `ea`)

Total open/confirmed issues: 512

## 1. AZo0umkyC45ILiv_v3Ug

- **Rule**: `java:S1121`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesMarshaller.java:347`
- **Effort**: 5min
- **Created**: 2025-10-20T10:54:35+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Extract the assignment out of this expression.

## 2. AZo0umkyC45ILiv_v3Uh

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesMarshaller.java:347`
- **Effort**: 30min
- **Created**: 2025-10-20T10:54:35+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  This accessibility bypass should be removed.

## 3. AZo0umipC45ILiv_v3Uf

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/RandomDataInput.java:555`
- **Effort**: 6min
- **Created**: 2025-10-20T10:54:35+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 16 to the 15 allowed.

## 4. AZo0umVtC45ILiv_v3UX

- **Rule**: `java:S1181`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/ByteBuffers.java:44`
- **Effort**: 20min
- **Created**: 2025-10-20T10:54:35+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Catch Exception instead of Throwable.

## 5. AZo0umXHC45ILiv_v3UY

- **Rule**: `java:S1181`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/NativeBytesStore.java:70`
- **Effort**: 20min
- **Created**: 2025-10-20T10:54:35+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Catch Exception instead of Throwable.

## 6. AZo0umGfC45ILiv_v3UW

- **Rule**: `java:S1643`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/NativeBytesStoreTest.java:79`
- **Effort**: 10min
- **Created**: 2025-10-20T10:54:35+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Use a StringBuilder instead.

## 7. AZo0umeaC45ILiv_v3Ue

- **Rule**: `java:S2637`
- **Severity**: MINOR
- **Type**: BUG
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/AbstractBytes.java:126`
- **Effort**: 15min
- **Created**: 2025-10-20T09:17:29+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  "bytesStore" is marked "@NotNull" but is not initialized in this constructor.

## 8. AZhCNlG8tQPKfEYiRuch

- **Rule**: `java:S1128`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/ChunkedMappedBytes.java:36`
- **Effort**: 1min
- **Created**: 2025-07-25T09:03:41+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove this unused import 'java.util.Objects'.

## 9. AZhCNlC1tQPKfEYiRucg

- **Rule**: `java:S1172`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/MappedBytesEdgeTest.java:43`
- **Effort**: 5min
- **Created**: 2025-07-25T09:03:41+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove this unused method parameter "name".

## 10. AZfUX_KZcw2_Zca4HSWF

- **Rule**: `java:S1452`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/Bytes.java:491`
- **Effort**: 20min
- **Created**: 2025-07-03T11:27:57+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove usage of generic wildcard type.

## 11. AZfUX_KZcw2_Zca4HSWG

- **Rule**: `java:S1452`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/Bytes.java:522`
- **Effort**: 20min
- **Created**: 2025-07-03T11:27:57+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove usage of generic wildcard type.

## 12. AZfUX_Ctcw2_Zca4HSWE

- **Rule**: `java:S5826`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/MappedFileTest.java:42`
- **Effort**: 2min
- **Created**: 2025-07-03T11:27:57+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Annotate this method with JUnit5 '@org.junit.jupiter.api.BeforeEach' instead of JUnit4 '@Before'.

## 13. AZSoQm4yJXfuX-9srB75

- **Rule**: `java:S1141`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/BytesInternal.java:1630`
- **Effort**: 20min
- **Created**: 2024-12-31T11:52:53+0000
- **Assignee**: Unassigned
- **Message**:
  Extract this nested try block into a separate method.

## 14. AZMw7LYCGAwb07yl4Tlu

- **Rule**: `java:S1133`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/StreamingDataOutput.java:1178`
- **Effort**: 10min
- **Created**: 2024-11-11T16:27:45+0000
- **Assignee**: Unassigned
- **Message**:
  Do not forget to remove this deprecated code someday.

## 15. AZMw7LYCGAwb07yl4Tlv

- **Rule**: `java:S1123`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/StreamingDataOutput.java:1178`
- **Effort**: 5min
- **Created**: 2024-11-11T16:27:45+0000
- **Assignee**: Unassigned
- **Message**:
  Add the missing @deprecated Javadoc tag.

## 16. AZDllLnD5bUw8_NMnKSh

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/AbstractBytesTest.java:74`
- **Effort**: 5min
- **Created**: 2024-07-01T12:20:26+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "mockBytesStore" which hides the field declared at line 35.

## 17. AZDllLnD5bUw8_NMnKSi

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/AbstractBytesTest.java:77`
- **Effort**: 5min
- **Created**: 2024-07-01T12:20:26+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "bytes" which hides the field declared at line 34.

## 18. AZDllLnD5bUw8_NMnKSj

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/AbstractBytesTest.java:83`
- **Effort**: 5min
- **Created**: 2024-07-01T12:20:26+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "mockBytesStore" which hides the field declared at line 35.

## 19. AZDllLnD5bUw8_NMnKSk

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/AbstractBytesTest.java:86`
- **Effort**: 5min
- **Created**: 2024-07-01T12:20:26+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "bytes" which hides the field declared at line 34.

## 20. AZDllLnD5bUw8_NMnKSl

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/AbstractBytesTest.java:94`
- **Effort**: 5min
- **Created**: 2024-07-01T12:20:26+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "mockBytesStore" which hides the field declared at line 35.

## 21. AZDllLnD5bUw8_NMnKSm

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/AbstractBytesTest.java:97`
- **Effort**: 5min
- **Created**: 2024-07-01T12:20:26+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "bytes" which hides the field declared at line 34.

## 22. AZDllLnD5bUw8_NMnKSn

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/AbstractBytesTest.java:105`
- **Effort**: 5min
- **Created**: 2024-07-01T12:20:26+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "mockBytesStore" which hides the field declared at line 35.

## 23. AZDllLnD5bUw8_NMnKSo

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/AbstractBytesTest.java:108`
- **Effort**: 5min
- **Created**: 2024-07-01T12:20:26+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "bytes" which hides the field declared at line 34.

## 24. AZDllLnD5bUw8_NMnKSp

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/AbstractBytesTest.java:118`
- **Effort**: 5min
- **Created**: 2024-07-01T12:20:26+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "mockBytesStore" which hides the field declared at line 35.

## 25. AZDllLnD5bUw8_NMnKSq

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/AbstractBytesTest.java:121`
- **Effort**: 5min
- **Created**: 2024-07-01T12:20:26+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "bytes" which hides the field declared at line 34.

## 26. AZDllLnD5bUw8_NMnKSg

- **Rule**: `java:S2699`
- **Severity**: BLOCKER
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/AbstractBytesTest.java:146`
- **Effort**: 10min
- **Created**: 2024-07-01T12:20:26+0000
- **Assignee**: Unassigned
- **Message**:
  Add at least one assertion to this test case.

## 27. AZDllLnD5bUw8_NMnKSr

- **Rule**: `java:S1854`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/AbstractBytesTest.java:147`
- **Effort**: 1min
- **Created**: 2024-07-01T12:20:26+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this useless assignment to local variable "newPosition".

## 28. AZDllLnD5bUw8_NMnKSs

- **Rule**: `java:S1481`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/AbstractBytesTest.java:147`
- **Effort**: 5min
- **Created**: 2024-07-01T12:20:26+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "newPosition" local variable.

## 29. AZDllLk75bUw8_NMnKSW

- **Rule**: `java:S4144`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/AppendableUtilTest.java:123`
- **Effort**: 15min
- **Created**: 2024-07-01T12:20:26+0000
- **Assignee**: Unassigned
- **Message**:
  Update this method so that its implementation is not identical to "testAppendDouble" on line 52.

## 30. AZDllLfg5bUw8_NMnKSN

- **Rule**: `java:S1068`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/ByteableTest.java:28`
- **Effort**: 5min
- **Created**: 2024-07-01T12:20:26+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove this unused "bytesStore" private field.

## 31. AZDllLX25bUw8_NMnKSD

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesMarshallerTest.java:68`
- **Effort**: 5min
- **Created**: 2024-07-01T12:20:26+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "field" which hides the field declared at line 40.

## 32. AZDllLX25bUw8_NMnKSA

- **Rule**: `java:S2699`
- **Severity**: BLOCKER
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesMarshallerTest.java:102`
- **Effort**: 10min
- **Created**: 2024-07-01T12:20:26+0000
- **Assignee**: Unassigned
- **Message**:
  Add at least one assertion to this test case.

## 33. AZDllLX25bUw8_NMnKSB

- **Rule**: `java:S2699`
- **Severity**: BLOCKER
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesMarshallerTest.java:110`
- **Effort**: 10min
- **Created**: 2024-07-01T12:20:26+0000
- **Assignee**: Unassigned
- **Message**:
  Add at least one assertion to this test case.

## 34. AZDllLX25bUw8_NMnKSC

- **Rule**: `java:S2699`
- **Severity**: BLOCKER
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesMarshallerTest.java:118`
- **Effort**: 10min
- **Created**: 2024-07-01T12:20:26+0000
- **Assignee**: Unassigned
- **Message**:
  Add at least one assertion to this test case.

## 35. AZDllLjM5bUw8_NMnKSQ

- **Rule**: `java:S1128`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesTest.java:33`
- **Effort**: 1min
- **Created**: 2024-07-01T12:20:26+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused import 'org.junit.Ignore'.

## 36. AZDllLq55bUw8_NMnKSy

- **Rule**: `java:S2699`
- **Severity**: BLOCKER
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/MethodReaderBuilderTest.java:49`
- **Effort**: 10min
- **Created**: 2024-07-01T12:20:26+0000
- **Assignee**: Unassigned
- **Message**:
  Add at least one assertion to this test case.

## 37. AZDllLmK5bUw8_NMnKSa

- **Rule**: `java:S1130`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/StreamingOutputStreamTest.java:59`
- **Effort**: 5min
- **Created**: 2024-07-01T12:20:26+0000
- **Assignee**: Unassigned
- **Message**:
  Remove the declaration of thrown exception 'java.io.IOException', as it cannot be thrown from
  method's body.

## 38. AZDllLmK5bUw8_NMnKSb

- **Rule**: `java:S1130`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/StreamingOutputStreamTest.java:65`
- **Effort**: 5min
- **Created**: 2024-07-01T12:20:26+0000
- **Assignee**: Unassigned
- **Message**:
  Remove the declaration of thrown exception 'java.io.IOException', as it cannot be thrown from
  method's body.

## 39. AZDllLmj5bUw8_NMnKSd

- **Rule**: `java:S1128`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/UncheckedBytesTest.java:18`
- **Effort**: 1min
- **Created**: 2024-07-01T12:20:26+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unnecessary import: same package classes are always implicitly imported.

## 40. AZDllLmj5bUw8_NMnKSe

- **Rule**: `java:S1128`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/UncheckedBytesTest.java:19`
- **Effort**: 1min
- **Created**: 2024-07-01T12:20:26+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unnecessary import: same package classes are always implicitly imported.

## 41. AZDllLmj5bUw8_NMnKSf

- **Rule**: `java:S1128`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/UncheckedBytesTest.java:20`
- **Effort**: 1min
- **Created**: 2024-07-01T12:20:26+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unnecessary import: same package classes are always implicitly imported.

## 42. AZDllLmj5bUw8_NMnKSc

- **Rule**: `java:S5786`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/UncheckedBytesTest.java:30`
- **Effort**: 2min
- **Created**: 2024-07-01T12:20:26+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this 'public' modifier.

## 43. AZDllLur5bUw8_NMnKTD

- **Rule**: `java:S1128`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/UncheckedNativeBytesTest.java:24`
- **Effort**: 1min
- **Created**: 2024-07-01T12:20:26+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused import 'java.nio.BufferUnderflowException'.

## 44. AZDllLur5bUw8_NMnKS3

- **Rule**: `java:S5778`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/UncheckedNativeBytesTest.java:70`
- **Effort**: 5min
- **Created**: 2024-07-01T12:20:26+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor the code of the lambda to have only one invocation possibly throwing a runtime exception.

## 45. AZDllLur5bUw8_NMnKS4

- **Rule**: `java:S6068`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/UncheckedNativeBytesTest.java:78`
- **Effort**: 2min
- **Created**: 2024-07-01T12:20:26+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this and every subsequent useless "eq(...)" invocation; pass the values directly.

## 46. AZDllLur5bUw8_NMnKS5

- **Rule**: `java:S6068`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/UncheckedNativeBytesTest.java:86`
- **Effort**: 2min
- **Created**: 2024-07-01T12:20:26+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this and every subsequent useless "eq(...)" invocation; pass the values directly.

## 47. AZDllLur5bUw8_NMnKS6

- **Rule**: `java:S6068`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/UncheckedNativeBytesTest.java:98`
- **Effort**: 2min
- **Created**: 2024-07-01T12:20:26+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this and every subsequent useless "eq(...)" invocation; pass the values directly.

## 48. AZDllLur5bUw8_NMnKS7

- **Rule**: `java:S5778`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/UncheckedNativeBytesTest.java:167`
- **Effort**: 5min
- **Created**: 2024-07-01T12:20:26+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor the code of the lambda to have only one invocation possibly throwing a runtime exception.

## 49. AZDllLur5bUw8_NMnKS8

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/UncheckedNativeBytesTest.java:179`
- **Effort**: 5min
- **Created**: 2024-07-01T12:20:26+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "uncheckedBytes" which hides the field declared at line 34.

## 50. AZDllLur5bUw8_NMnKS9

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/UncheckedNativeBytesTest.java:186`
- **Effort**: 5min
- **Created**: 2024-07-01T12:20:26+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "uncheckedBytes" which hides the field declared at line 34.

## 51. AZDllLur5bUw8_NMnKS-

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/UncheckedNativeBytesTest.java:193`
- **Effort**: 5min
- **Created**: 2024-07-01T12:20:26+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "uncheckedBytes" which hides the field declared at line 34.

## 52. AZDllLur5bUw8_NMnKS_

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/UncheckedNativeBytesTest.java:200`
- **Effort**: 5min
- **Created**: 2024-07-01T12:20:26+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "uncheckedBytes" which hides the field declared at line 34.

## 53. AZDllLur5bUw8_NMnKTA

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/UncheckedNativeBytesTest.java:209`
- **Effort**: 5min
- **Created**: 2024-07-01T12:20:26+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "uncheckedBytes" which hides the field declared at line 34.

## 54. AZDllLur5bUw8_NMnKTB

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/UncheckedNativeBytesTest.java:218`
- **Effort**: 5min
- **Created**: 2024-07-01T12:20:26+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "uncheckedBytes" which hides the field declared at line 34.

## 55. AZDllLur5bUw8_NMnKTC

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/UncheckedNativeBytesTest.java:226`
- **Effort**: 5min
- **Created**: 2024-07-01T12:20:26+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "underlyingBytes" which hides the field declared at line 32.

## 56. AZDllLlw5bUw8_NMnKSZ

- **Rule**: `java:S5845`
- **Severity**: CRITICAL
- **Type**: BUG
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/algo/XxHashTest.java:31`
- **Effort**: 5min
- **Created**: 2024-07-01T12:20:26+0000
- **Assignee**: Unassigned
- **Message**:
  Change the assertion arguments to not compare a primitive value with null.

## 57. AZDllLlw5bUw8_NMnKSY

- **Rule**: `java:S2699`
- **Severity**: BLOCKER
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/algo/XxHashTest.java:58`
- **Effort**: 10min
- **Created**: 2024-07-01T12:20:26+0000
- **Assignee**: Unassigned
- **Message**:
  Add at least one assertion to this test case.

## 58. AZDllLtC5bUw8_NMnKS0

- **Rule**: `java:S1128`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/pool/BytesPoolTest.java:20`
- **Effort**: 1min
- **Created**: 2024-07-01T12:20:26+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused import 'org.junit.jupiter.api.BeforeEach'.

## 59. AZDllLaA5bUw8_NMnKSE

- **Rule**: `java:S1128`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/util/AbstractInternerTest.java:20`
- **Effort**: 1min
- **Created**: 2024-07-01T12:20:26+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unnecessary import: same package classes are always implicitly imported.

## 60. AZDllLbb5bUw8_NMnKSH

- **Rule**: `java:S5778`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/util/BinaryLengthLengthTest.java:66`
- **Effort**: 5min
- **Created**: 2024-07-01T12:20:26+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor the code of the lambda to have only one invocation possibly throwing a runtime exception.

## 61. AZDllLbb5bUw8_NMnKSI

- **Rule**: `java:S5778`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/util/BinaryLengthLengthTest.java:75`
- **Effort**: 5min
- **Created**: 2024-07-01T12:20:26+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor the code of the lambda to have only one invocation possibly throwing a runtime exception.

## 62. AZDllLcD5bUw8_NMnKSJ

- **Rule**: `java:S1128`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/util/Bit8StringInternerTest.java:19`
- **Effort**: 1min
- **Created**: 2024-07-01T12:20:26+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused import 'net.openhft.chronicle.bytes.BytesStore'.

## 63. AZDllLaa5bUw8_NMnKSF

- **Rule**: `java:S2699`
- **Severity**: BLOCKER
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/util/CompressionTest.java:25`
- **Effort**: 10min
- **Created**: 2024-07-01T12:20:26+0000
- **Assignee**: Unassigned
- **Message**:
  Add at least one assertion to this test case.

## 64. AZDllLaz5bUw8_NMnKSG

- **Rule**: `java:S1128`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/util/EscapingStopCharsTesterTest.java:21`
- **Effort**: 1min
- **Created**: 2024-07-01T12:20:26+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused import 'org.mockito.Mockito'.

## 65. AY_FiNqeOd5e3hTloS28

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/ByteBuffers.java:69`
- **Effort**: 30min
- **Created**: 2024-05-28T16:43:15+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  This accessibility bypass should be removed.

## 66. AY_FiNqeOd5e3hTloS29

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/ByteBuffers.java:70`
- **Effort**: 30min
- **Created**: 2024-05-28T16:43:15+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  This accessibility bypass should be removed.

## 67. AY_FiNrhOd5e3hTloS2_

- **Rule**: `java:S1128`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/UnsafeText.java:21`
- **Effort**: 1min
- **Created**: 2024-05-28T16:43:15+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove this unused import 'net.openhft.chronicle.core.UnsafeMemory.UNSAFE'.

## 68. AZSoQmqKJXfuX-9srB73

- **Rule**: `java:S1643`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/internal/BytesInternalTest.java:242`
- **Effort**: 10min
- **Created**: 2024-05-28T16:43:15+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Use a StringBuilder instead.

## 69. AY_FiNgsOd5e3hTloS2t

- **Rule**: `java:S1128`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/internal/UnsafeTextTest.java:29`
- **Effort**: 1min
- **Created**: 2024-05-28T16:43:15+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove this unused import 'net.openhft.chronicle.core.UnsafeMemory.UNSAFE'.

## 70. AY_FiNgsOd5e3hTloS2p

- **Rule**: `java:S5961`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/internal/UnsafeTextTest.java:73`
- **Effort**: 20min
- **Created**: 2024-05-28T16:43:15+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Refactor this method to reduce the number of assertions from 62 to less than 25.

## 71. AY_FiNgsOd5e3hTloS2q

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/internal/UnsafeTextTest.java:74`
- **Effort**: 0min
- **Created**: 2024-05-28T16:43:15+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Complete the task associated to this TODO comment.

## 72. AY_FiNgsOd5e3hTloS2r

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/internal/UnsafeTextTest.java:98`
- **Effort**: 5min
- **Created**: 2024-05-28T16:43:15+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  This block of commented-out lines of code should be removed.

## 73. AY_FiNgsOd5e3hTloS2s

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/internal/UnsafeTextTest.java:113`
- **Effort**: 5min
- **Created**: 2024-05-28T16:43:15+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  This block of commented-out lines of code should be removed.

## 74. AZDllLcp5bUw8_NMnKSK

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/internal/UnsafeTextTest.java:167`
- **Effort**: 2min
- **Created**: 2024-05-28T16:43:15+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 75. AY_FiNiCOd5e3hTloS2x

- **Rule**: `java:S1854`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/internal/BytesInternalTest.java:76`
- **Effort**: 1min
- **Created**: 2024-05-20T16:22:23+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove this useless assignment to local variable "sb".

## 76. AY_FiNiCOd5e3hTloS2y

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/internal/BytesInternalTest.java:209`
- **Effort**: 5min
- **Created**: 2024-05-20T16:22:23+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  This block of commented-out lines of code should be removed.

## 77. AY_FiNiCOd5e3hTloS2z

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/internal/BytesInternalTest.java:240`
- **Effort**: 0min
- **Created**: 2024-05-20T16:22:23+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Complete the task associated to this TODO comment.

## 78. AY_FiNiCOd5e3hTloS20

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/internal/BytesInternalTest.java:368`
- **Effort**: 5min
- **Created**: 2024-05-20T16:22:23+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  This block of commented-out lines of code should be removed.

## 79. AY_FiNiCOd5e3hTloS21

- **Rule**: `java:S1854`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/internal/BytesInternalTest.java:397`
- **Effort**: 1min
- **Created**: 2024-05-20T16:22:23+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove this useless assignment to local variable "f".

## 80. AY_FiNiCOd5e3hTloS23

- **Rule**: `java:S1481`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/internal/BytesInternalTest.java:397`
- **Effort**: 5min
- **Created**: 2024-05-20T16:22:23+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove this unused "f" local variable.

## 81. AY_FiNiCOd5e3hTloS2w

- **Rule**: `java:S4144`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/internal/BytesInternalTest.java:531`
- **Effort**: 15min
- **Created**: 2024-05-20T16:22:23+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Update this method so that its implementation is not identical to "simpleWriteFully3" on line 518.

## 82. AY_FiNhtOd5e3hTloS2v

- **Rule**: `java:S1128`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/internal/EmptyBytesStoreTest.java:22`
- **Effort**: 1min
- **Created**: 2024-05-20T16:22:23+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove this unused import 'net.openhft.chronicle.core.Jvm'.

## 83. AY_FiNhtOd5e3hTloS2u

- **Rule**: `java:S1172`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/internal/EmptyBytesStoreTest.java:48`
- **Effort**: 5min
- **Created**: 2024-05-20T16:22:23+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove this unused method parameter "type".

## 84. AY_FiN28Od5e3hTloS3T

- **Rule**: `java:S1452`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/Byteable.java:54`
- **Effort**: 20min
- **Created**: 2024-04-29T11:29:04+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove usage of generic wildcard type.

## 85. AY_FiN28Od5e3hTloS3U

- **Rule**: `java:S1452`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/Byteable.java:54`
- **Effort**: 20min
- **Created**: 2024-04-29T11:29:04+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove usage of generic wildcard type.

## 86. AY_FiNxcOd5e3hTloS3J

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/Bytes.java:1084`
- **Effort**: 11min
- **Created**: 2024-04-29T11:29:04+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 21 to the 15 allowed.

## 87. AY_FiNzQOd5e3hTloS3N

- **Rule**: `java:S1452`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesRingBuffer.java:156`
- **Effort**: 20min
- **Created**: 2024-04-29T11:29:04+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove usage of generic wildcard type.

## 88. AY_FiNzQOd5e3hTloS3O

- **Rule**: `java:S1452`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesRingBuffer.java:156`
- **Effort**: 20min
- **Created**: 2024-04-29T11:29:04+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove usage of generic wildcard type.

## 89. AY_FiNwROd5e3hTloS3D

- **Rule**: `java:S1452`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesStore.java:62`
- **Effort**: 20min
- **Created**: 2024-04-29T11:29:04+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove usage of generic wildcard type.

## 90. AY_FiNwROd5e3hTloS3E

- **Rule**: `java:S1452`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesStore.java:62`
- **Effort**: 20min
- **Created**: 2024-04-29T11:29:04+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove usage of generic wildcard type.

## 91. AY_FiNwROd5e3hTloS3F

- **Rule**: `java:S1452`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesStore.java:89`
- **Effort**: 20min
- **Created**: 2024-04-29T11:29:04+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove usage of generic wildcard type.

## 92. AY_FiNwROd5e3hTloS3G

- **Rule**: `java:S1452`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesStore.java:576`
- **Effort**: 20min
- **Created**: 2024-04-29T11:29:04+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove usage of generic wildcard type.

## 93. AY_FiNwROd5e3hTloS3H

- **Rule**: `java:S1452`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesStore.java:576`
- **Effort**: 20min
- **Created**: 2024-04-29T11:29:04+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove usage of generic wildcard type.

## 94. AY_FiN0sOd5e3hTloS3P

- **Rule**: `java:S1452`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/RandomDataInput.java:458`
- **Effort**: 20min
- **Created**: 2024-04-29T11:29:04+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove usage of generic wildcard type.

## 95. AY_FiN0sOd5e3hTloS3Q

- **Rule**: `java:S1452`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/RandomDataInput.java:458`
- **Effort**: 20min
- **Created**: 2024-04-29T11:29:04+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove usage of generic wildcard type.

## 96. AY_FiN1SOd5e3hTloS3R

- **Rule**: `java:S1452`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/RingBufferReader.java:104`
- **Effort**: 20min
- **Created**: 2024-04-29T11:29:04+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove usage of generic wildcard type.

## 97. AY_FiN1SOd5e3hTloS3S

- **Rule**: `java:S1452`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/RingBufferReader.java:104`
- **Effort**: 20min
- **Created**: 2024-04-29T11:29:04+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove usage of generic wildcard type.

## 98. AY_FiNtBOd5e3hTloS3A

- **Rule**: `java:S1452`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/BytesInternal.java:3446`
- **Effort**: 20min
- **Created**: 2024-04-29T11:29:04+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove usage of generic wildcard type.

## 99. AY_FiNtBOd5e3hTloS3B

- **Rule**: `java:S1452`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/BytesInternal.java:3446`
- **Effort**: 20min
- **Created**: 2024-04-29T11:29:04+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove usage of generic wildcard type.

## 100. AY_FiNo0Od5e3hTloS26

- **Rule**: `java:S5777`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/LockingByteableTest.java:83`
- **Effort**: 5min
- **Created**: 2024-04-29T11:29:04+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Move assertions into separate method or use assertThrows or try-catch instead.

## 101. AY_FiNo0Od5e3hTloS27

- **Rule**: `java:S5777`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/LockingByteableTest.java:143`
- **Effort**: 5min
- **Created**: 2024-04-29T11:29:04+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Move assertions into separate method or use assertThrows or try-catch instead.

## 102. AY_FiNonOd5e3hTloS25

- **Rule**: `java:S2186`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/domestic/ReentrantFileLockTest.java:203`
- **Effort**: 30min
- **Created**: 2024-04-29T11:29:04+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove this assertion.

## 103. AY_FiNiCOd5e3hTloS22

- **Rule**: `java:S1607`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/internal/BytesInternalTest.java:390`
- **Effort**: 10min
- **Created**: 2024-04-29T11:29:04+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Either add an explanation about why this test is skipped or remove the "@Ignore" annotation.

## 104. AY6FUwknCXhPloZvUpve

- **Rule**: `java:S2699`
- **Severity**: BLOCKER
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/MappedBytesStoreTest.java:108`
- **Effort**: 10min
- **Created**: 2024-03-21T09:37:00+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Add at least one assertion to this test case.

## 105. AY4byRC0Cx5BP03LbK7G

- **Rule**: `java:S5976`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesEqualityTests.java:121`
- **Effort**: 10min
- **Created**: 2024-03-07T23:51:17+0000
- **Assignee**: Unassigned
- **Message**:
  Replace these 3 tests with a single Parameterized one.

## 106. AYw0tGVvUtrwJAUvk2_S

- **Rule**: `java:S5786`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/UnsafeRWObjectTest.java:110`
- **Effort**: 2min
- **Created**: 2023-11-23T04:48:17+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove this 'public' modifier.

## 107. AYw0tGVvUtrwJAUvk2_T

- **Rule**: `java:S5786`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/UnsafeRWObjectTest.java:136`
- **Effort**: 2min
- **Created**: 2023-11-23T04:48:17+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this 'public' modifier.

## 108. AYw0tGVvUtrwJAUvk2_U

- **Rule**: `java:S5786`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/UnsafeRWObjectTest.java:157`
- **Effort**: 2min
- **Created**: 2023-11-23T04:48:17+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this 'public' modifier.

## 109. AYw0tGUZUtrwJAUvk2_O

- **Rule**: `java:S2699`
- **Severity**: BLOCKER
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/MappedBytesTest.java:651`
- **Effort**: 10min
- **Created**: 2023-11-03T15:58:34+0000
- **Assignee**: Unassigned
- **Message**:
  Add at least one assertion to this test case.

## 110. AYuVCh8rZPjqRKQOypiG

- **Rule**: `java:S2386`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/Chars.java:27`
- **Effort**: 15min
- **Created**: 2023-10-30T09:09:53+0000
- **Assignee**: Unassigned
- **Message**:
  Make this member "protected".

## 111. AYtnLMZqEg1bT-UxuLcE

- **Rule**: `java:S1452`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/pool/BytesPool.java:43`
- **Effort**: 20min
- **Created**: 2023-10-19T05:05:31+0000
- **Assignee**: Unassigned
- **Message**:
  Remove usage of generic wildcard type.

## 112. AYtnLMZqEg1bT-UxuLcF

- **Rule**: `java:S1452`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/pool/BytesPool.java:53`
- **Effort**: 20min
- **Created**: 2023-10-19T05:05:31+0000
- **Assignee**: Unassigned
- **Message**:
  Remove usage of generic wildcard type.

## 113. AYtnLMRKEg1bT-UxuLb3

- **Rule**: `java:S1452`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/BytesInternal.java:2359`
- **Effort**: 20min
- **Created**: 2023-10-19T00:58:45+0000
- **Assignee**: Unassigned
- **Message**:
  Remove usage of generic wildcard type.

## 114. AYtnLMKZEg1bT-UxuLbt

- **Rule**: `java:S5786`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/issue/Issue462Test.java:29`
- **Effort**: 2min
- **Created**: 2023-10-18T12:41:14+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this 'public' modifier.

## 115. AYtnLMKZEg1bT-UxuLbs

- **Rule**: `java:S5786`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/issue/Issue462Test.java:47`
- **Effort**: 2min
- **Created**: 2023-10-18T12:41:14+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this 'public' modifier.

## 116. AYtnLMXaEg1bT-UxuLb4

- **Rule**: `java:S1118`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/PageUtil.java:38`
- **Effort**: 5min
- **Created**: 2023-10-18T08:32:43+0000
- **Assignee**: Unassigned
- **Message**:
  Add a private constructor to hide the implicit public one.

## 117. AY-YRTt1rjWdiV1GRjNB

- **Rule**: `java:S1130`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/PageUtilTest.java:88`
- **Effort**: 5min
- **Created**: 2023-10-18T08:32:43+0000
- **Assignee**: Unassigned
- **Message**:
  Remove the declaration of thrown exception 'java.lang.Exception', as it cannot be thrown from
  method's body.

## 118. AY-YRTt1rjWdiV1GRjNC

- **Rule**: `java:S1130`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/PageUtilTest.java:96`
- **Effort**: 5min
- **Created**: 2023-10-18T08:32:43+0000
- **Assignee**: Unassigned
- **Message**:
  Remove the declaration of thrown exception 'java.lang.Exception', as it cannot be thrown from
  method's body.

## 119. AZDllLpp5bUw8_NMnKSw

- **Rule**: `java:S117`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/PageUtilTest.java:105`
- **Effort**: 2min
- **Created**: 2023-10-18T08:32:43+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this local variable to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 120. AZMw7LZNGAwb07yl4Tlx

- **Rule**: `java:S1854`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesUtil.java:144`
- **Effort**: 1min
- **Created**: 2023-08-30T11:59:28+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove this useless assignment to local variable "fieldGroup".

## 121. AZMw7LZNGAwb07yl4Tly

- **Rule**: `java:S1481`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesUtil.java:144`
- **Effort**: 5min
- **Created**: 2023-08-30T11:59:28+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove this unused "fieldGroup" local variable.

## 122. AZo0umX7C45ILiv_v3Uc

- **Rule**: `java:S1128`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/SingleMappedBytes.java:24`
- **Effort**: 1min
- **Created**: 2023-08-30T11:59:28+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove this unused import 'net.openhft.chronicle.core.io.ClosedIllegalStateException'.

## 123. AZo0umX7C45ILiv_v3Ud

- **Rule**: `java:S1128`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/SingleMappedBytes.java:26`
- **Effort**: 1min
- **Created**: 2023-08-30T11:59:28+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove this unused import 'net.openhft.chronicle.core.io.ThreadingIllegalStateException'.

## 124. AYqsZ12Mzzxmyr6IR46W

- **Rule**: `java:S2184`
- **Severity**: MINOR
- **Type**: BUG
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/HeapBytesStore.java:207`
- **Effort**: 5min
- **Created**: 2023-08-30T10:57:37+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Cast one of the operands of this addition operation to a "long".

## 125. AYqsZ2IPzzxmyr6IR46e

- **Rule**: `java:S1128`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesTextMethodTester.java:20`
- **Effort**: 1min
- **Created**: 2023-08-25T16:34:25+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove this unused import 'net.openhft.chronicle.core.io.ThreadingIllegalStateException'.

## 126. AYqsZ2IPzzxmyr6IR46d

- **Rule**: `java:S1128`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesTextMethodTester.java:19`
- **Effort**: 1min
- **Created**: 2023-08-22T11:35:24+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove this unused import 'net.openhft.chronicle.core.io.ClosedIllegalStateException'.

## 127. AYqsZ2AGzzxmyr6IR46Y

- **Rule**: `java:S1141`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/VanillaBytes.java:531`
- **Effort**: 20min
- **Created**: 2023-08-22T11:35:24+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Extract this nested try block into a separate method.

## 128. AYne2-ND2QOAFIMh_kek

- **Rule**: `java:S1161`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesStore.java:892`
- **Effort**: 5min
- **Created**: 2023-08-10T09:51:13+0000
- **Assignee**: Unassigned
- **Message**:
  Add the "@Override" annotation above this method signature

## 129. AZDllLgo5bUw8_NMnKSO

- **Rule**: `java:S1161`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/Issue523Test.java:36`
- **Effort**: 5min
- **Created**: 2023-08-02T12:40:54+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Add the "@Override" annotation above this method signature

## 130. AZDllLof5bUw8_NMnKSv

- **Rule**: `java:S1161`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/MappedBytesTest.java:76`
- **Effort**: 5min
- **Created**: 2023-08-02T12:40:54+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Add the "@Override" annotation above this method signature

## 131. AZDllLeb5bUw8_NMnKSM

- **Rule**: `java:S1161`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/MappedFileMultiThreadTest.java:48`
- **Effort**: 5min
- **Created**: 2023-08-02T12:40:54+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Add the "@Override" annotation above this method signature

## 132. AZDllLlW5bUw8_NMnKSX

- **Rule**: `java:S1161`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/MappedUniqueTimeProviderTest.java:40`
- **Effort**: 5min
- **Created**: 2023-08-02T12:40:54+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Add the "@Override" annotation above this method signature

## 133. AZDllLsG5bUw8_NMnKSz

- **Rule**: `java:S1161`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/domestic/ReentrantFileLockTest.java:62`
- **Effort**: 5min
- **Created**: 2023-08-02T12:40:54+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Add the "@Override" annotation above this method signature

## 134. AYne2-E72QOAFIMh_kej

- **Rule**: `java:S3415`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/PointerBytesStoreTest.java:74`
- **Effort**: 2min
- **Created**: 2023-07-26T09:34:31+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Swap these 2 arguments so they are in the correct order: expected value, actual value.

## 135. AYl3XT95Wm0iSXXh63Nl

- **Rule**: `java:S1068`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/NoBytesStore.java:45`
- **Effort**: 5min
- **Created**: 2023-07-10T16:24:31+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove this unused "BYTE_BUFFER" private field.

## 136. AYkeGp8HJDSPJ7SqLeHS

- **Rule**: `java:S1133`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/AbstractBytes.java:79`
- **Effort**: 10min
- **Created**: 2023-07-03T11:31:40+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Do not forget to remove this deprecated code someday.

## 137. AYkeGp8HJDSPJ7SqLeHT

- **Rule**: `java:S1123`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/AbstractBytes.java:79`
- **Effort**: 5min
- **Created**: 2023-07-03T11:31:40+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Add the missing @deprecated Javadoc tag.

## 138. AYkeGp9wJDSPJ7SqLeHa

- **Rule**: `java:S1133`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/UncheckedNativeBytes.java:56`
- **Effort**: 10min
- **Created**: 2023-07-03T11:31:40+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Do not forget to remove this deprecated code someday.

## 139. AYkeGp9wJDSPJ7SqLeHb

- **Rule**: `java:S1123`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/UncheckedNativeBytes.java:56`
- **Effort**: 5min
- **Created**: 2023-07-03T11:31:40+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Add the missing @deprecated Javadoc tag.

## 140. AYkeGp9wJDSPJ7SqLeHc

- **Rule**: `java:S1874`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/UncheckedNativeBytes.java:79`
- **Effort**: 15min
- **Created**: 2023-07-03T11:31:40+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove this use of "APPEND_0"; it is deprecated.

## 141. AY_FiN3OOd5e3hTloS3V

- **Rule**: `java:S1133`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/ByteStringAppender.java:267`
- **Effort**: 10min
- **Created**: 2023-06-30T12:54:02+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Do not forget to remove this deprecated code someday.

## 142. AY_FiN3OOd5e3hTloS3X

- **Rule**: `java:S1133`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/ByteStringAppender.java:275`
- **Effort**: 10min
- **Created**: 2023-06-30T12:54:02+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Do not forget to remove this deprecated code someday.

## 143. AYkeGp7CJDSPJ7SqLeHO

- **Rule**: `java:S1940`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/render/MaximumPrecision.java:56`
- **Effort**: 2min
- **Created**: 2023-06-30T12:54:02+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Use the opposite operator (">") instead.

## 144. AYkeGp7CJDSPJ7SqLeHM

- **Rule**: `java:S127`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/render/MaximumPrecision.java:75`
- **Effort**: 10min
- **Created**: 2023-06-30T12:54:02+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Refactor the code in order to not assign to this loop counter from within the loop body.

## 145. AYkeGp7CJDSPJ7SqLeHP

- **Rule**: `java:S1940`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/render/MaximumPrecision.java:104`
- **Effort**: 2min
- **Created**: 2023-06-30T12:54:02+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Use the opposite operator (">=") instead.

## 146. AYkeGp7CJDSPJ7SqLeHN

- **Rule**: `java:S127`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/render/MaximumPrecision.java:123`
- **Effort**: 10min
- **Created**: 2023-06-30T12:54:02+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Refactor the code in order to not assign to this loop counter from within the loop body.

## 147. AY-YRTvbrjWdiV1GRjNS

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/ByteStringAppenderTest.java:61`
- **Effort**: 5min
- **Created**: 2023-06-30T12:54:02+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "bytes" which hides the field declared at line 36.

## 148. AY-YRTqXrjWdiV1GRjMa

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesTest.java:1098`
- **Effort**: 0min
- **Created**: 2023-06-30T12:54:02+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Complete the task associated to this TODO comment.

## 149. AY-YRTqXrjWdiV1GRjMb

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesTest.java:1371`
- **Effort**: 5min
- **Created**: 2023-06-30T12:54:02+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  This block of commented-out lines of code should be removed.

## 150. AYl3XUH-Wm0iSXXh63Nx

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/Bytes.java:1025`
- **Effort**: 5min
- **Created**: 2023-06-29T12:05:52+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  This block of commented-out lines of code should be removed.

## 151. AYl3XUH-Wm0iSXXh63Ny

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/Bytes.java:1025`
- **Effort**: 0min
- **Created**: 2023-06-29T12:05:52+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Complete the task associated to this TODO comment.

## 152. AYl3XUH-Wm0iSXXh63Nz

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/Bytes.java:1086`
- **Effort**: 0min
- **Created**: 2023-06-29T12:05:52+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Complete the task associated to this TODO comment.

## 153. AYl3XT84Wm0iSXXh63Nk

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/BytesInternal.java:3168`
- **Effort**: 31min
- **Created**: 2023-06-29T12:05:52+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 41 to the 15 allowed.

## 154. AYl3XT-uWm0iSXXh63Nm

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/util/PropertyReplacer.java:34`
- **Effort**: 5min
- **Created**: 2023-06-29T12:05:52+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  This block of commented-out lines of code should be removed.

## 155. AYkeGpxxJDSPJ7SqLeHD

- **Rule**: `java:S5961`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesTest.java:1109`
- **Effort**: 20min
- **Created**: 2023-06-29T11:04:40+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Refactor this method to reduce the number of assertions from 30 to less than 25.

## 156. AYkeGp8HJDSPJ7SqLeHU

- **Rule**: `java:S1874`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/AbstractBytes.java:105`
- **Effort**: 15min
- **Created**: 2023-06-29T08:17:44+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove this use of "APPEND_0"; it is deprecated.

## 157. AYl3XTvSWm0iSXXh63Nd

- **Rule**: `java:S5778`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesJavaDocComplianceTest.java:135`
- **Effort**: 5min
- **Created**: 2023-06-20T11:02:53+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Refactor the code of the lambda to have only one invocation possibly throwing a runtime exception.

## 158. AY_FiNwjOd5e3hTloS3I

- **Rule**: `java:S1068`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/DistributedUniqueTimeProvider.java:49`
- **Effort**: 5min
- **Created**: 2023-06-08T16:43:57+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove this unused "deduplicator" private field.

## 159. AYft82y57ytXykK8cCL7

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesMethodReaderBuilder.java:105`
- **Effort**: 5min
- **Created**: 2023-05-05T13:52:04+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "in" which hides the field declared at line 34.

## 160. AYdQv9viCUsZ_mbGu7ou

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/UncheckedNativeBytes.java:272`
- **Effort**: 0min
- **Created**: 2023-04-01T05:59:31+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 161. AYdQv9viCUsZ_mbGu7ot

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/UncheckedNativeBytes.java:273`
- **Effort**: 5min
- **Created**: 2023-04-01T05:59:31+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 162. AYdIpeKZzbhzvciO3jUa

- **Rule**: `java:S2184`
- **Severity**: MINOR
- **Type**: BUG
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/AbstractBytes.java:1321`
- **Effort**: 5min
- **Created**: 2023-03-30T06:59:21+0000
- **Assignee**: Unassigned
- **Message**:
  Cast one of the operands of this addition operation to a "long".

## 163. AYcuZ_x9YUhnT2iQ2RP9

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/CommonMappedBytes.java:270`
- **Effort**: 8min
- **Created**: 2023-03-29T08:36:11+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 18 to the 15 allowed.

## 164. AYcsjc0e8rTBhadrxKyS

- **Rule**: `java:S1161`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/AbstractBytes.java:899`
- **Effort**: 5min
- **Created**: 2023-03-22T08:57:37+0000
- **Assignee**: JerryShea@github
- **Message**:
  Add the "@Override" annotation above this method signature

## 165. AYcsjcvR8rTBhadrxKyQ

- **Rule**: `java:S1181`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BinaryBytesMethodWriterInvocationHandler.java:68`
- **Effort**: 20min
- **Created**: 2023-03-20T17:42:26+0000
- **Assignee**: Unassigned
- **Message**:
  Catch Exception instead of Throwable.

## 166. AY-YRTkvrjWdiV1GRjLZ

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/UnsafeTextBytesTest.java:74`
- **Effort**: 5min
- **Created**: 2023-03-17T09:05:17+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 167. AYbEpJsFjHpEkdHRglp4

- **Rule**: `java:S1452`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesStore.java:131`
- **Effort**: 20min
- **Created**: 2023-02-21T15:09:51+0000
- **Assignee**: Unassigned
- **Message**:
  Remove usage of generic wildcard type.

## 168. AYY1rquJ51MA_RKGGmIr

- **Rule**: `java:S5164`
- **Severity**: MAJOR
- **Type**: BUG
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/domestic/ReentrantFileLock.java:51`
- **Effort**: 10min
- **Created**: 2023-02-09T01:24:02+0000
- **Assignee**: Unassigned
- **Message**:
  Call "remove()" on "heldLocks".

## 169. AZUakVj8QdJEqNZOtPKr

- **Rule**: `java:S5786`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/domestic/ReentrantFileLockTest.java:55`
- **Effort**: 2min
- **Created**: 2023-02-09T01:24:02+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this 'public' modifier.

## 170. AZUakVj8QdJEqNZOtPKs

- **Rule**: `java:S5786`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/domestic/ReentrantFileLockTest.java:67`
- **Effort**: 2min
- **Created**: 2023-02-09T01:24:02+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this 'public' modifier.

## 171. AYY1rql351MA_RKGGmIk

- **Rule**: `java:S5786`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/domestic/ReentrantFileLockTest.java:73`
- **Effort**: 2min
- **Created**: 2023-02-09T01:24:02+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this 'public' modifier.

## 172. AYY1rql451MA_RKGGmIl

- **Rule**: `java:S5786`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/domestic/ReentrantFileLockTest.java:87`
- **Effort**: 2min
- **Created**: 2023-02-09T01:24:02+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this 'public' modifier.

## 173. AY-YRTvFrjWdiV1GRjNQ

- **Rule**: `java:S1481`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/domestic/ReentrantFileLockTest.java:89`
- **Effort**: 5min
- **Created**: 2023-02-09T01:24:02+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "lock" local variable.

## 174. AYY1rql451MA_RKGGmIm

- **Rule**: `java:S5786`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/domestic/ReentrantFileLockTest.java:97`
- **Effort**: 2min
- **Created**: 2023-02-09T01:24:02+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this 'public' modifier.

## 175. AYY1rql451MA_RKGGmIn

- **Rule**: `java:S5786`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/domestic/ReentrantFileLockTest.java:108`
- **Effort**: 2min
- **Created**: 2023-02-09T01:24:02+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this 'public' modifier.

## 176. AYY1rql451MA_RKGGmIo

- **Rule**: `java:S5786`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/domestic/ReentrantFileLockTest.java:119`
- **Effort**: 2min
- **Created**: 2023-02-09T01:24:02+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this 'public' modifier.

## 177. AYY1rql451MA_RKGGmIp

- **Rule**: `java:S5786`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/domestic/ReentrantFileLockTest.java:133`
- **Effort**: 2min
- **Created**: 2023-02-09T01:24:02+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this 'public' modifier.

## 178. AYY1rql451MA_RKGGmIq

- **Rule**: `java:S5786`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/domestic/ReentrantFileLockTest.java:154`
- **Effort**: 2min
- **Created**: 2023-02-09T01:24:02+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this 'public' modifier.

## 179. AYR__CPqtzFseMukM-MH

- **Rule**: `java:S4144`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/NoBytesStore.java:374`
- **Effort**: 15min
- **Created**: 2022-11-16T03:48:49+0000
- **Assignee**: Unassigned
- **Message**:
  Update this method so that its implementation is not identical to "addressForRead" on line 367.

## 180. AYR__CCJtzFseMukM-MF

- **Rule**: `java:S3415`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/internal/EmptyBytesStoreTest.java:262`
- **Effort**: 2min
- **Created**: 2022-11-16T03:48:49+0000
- **Assignee**: Unassigned
- **Message**:
  Swap these 2 arguments so they are in the correct order: expected value, actual value.

## 181. AZDllLdR5bUw8_NMnKSL

- **Rule**: `java:S108`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/internal/EmptyBytesStoreTest.java:445`
- **Effort**: 5min
- **Created**: 2022-11-16T03:48:49+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this block of code, fill it in, or add a comment explaining why it is empty.

## 182. AYN6cMwwvLfgt_47r6N0

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesUtil.java:671`
- **Effort**: 6min
- **Created**: 2022-09-26T15:35:20+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 16 to the 15 allowed.

## 183. AYMOsKP9n9aFsyBPTF4L

- **Rule**: `java:S3415`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/PointerBytesStoreTest.java:33`
- **Effort**: 2min
- **Created**: 2022-09-05T17:26:04+0000
- **Assignee**: Unassigned
- **Message**:
  Swap these 2 arguments so they are in the correct order: expected value, actual value.

## 184. AYMOfiYDUj1Lk7_J2Ilc

- **Rule**: `java:S3415`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesTest.java:89`
- **Effort**: 2min
- **Created**: 2022-09-05T16:31:08+0000
- **Assignee**: Unassigned
- **Message**:
  Swap these 2 arguments so they are in the correct order: expected value, actual value.

## 185. AYJm_3kicF1V_Tdb7f_a

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/ChunkedMappedBytes.java:334`
- **Effort**: 0min
- **Created**: 2022-08-04T03:56:12+0000
- **Assignee**: JerryShea@github
- **Message**:
  Complete the task associated to this TODO comment.

## 186. AYJjLGNXSu9fydlWGkas

- **Rule**: `java:S1452`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesStore.java:144`
- **Effort**: 20min
- **Created**: 2022-08-03T10:06:06+0000
- **Assignee**: Unassigned
- **Message**:
  Remove usage of generic wildcard type.

## 187. AYJjLGNXSu9fydlWGkat

- **Rule**: `java:S1452`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesStore.java:154`
- **Effort**: 20min
- **Created**: 2022-08-03T10:06:06+0000
- **Assignee**: Unassigned
- **Message**:
  Remove usage of generic wildcard type.

## 188. AYJjLGNXSu9fydlWGkau

- **Rule**: `java:S1452`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesStore.java:164`
- **Effort**: 20min
- **Created**: 2022-08-03T10:06:06+0000
- **Assignee**: Unassigned
- **Message**:
  Remove usage of generic wildcard type.

## 189. AYJjLGNXSu9fydlWGkav

- **Rule**: `java:S1452`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesStore.java:175`
- **Effort**: 20min
- **Created**: 2022-08-03T10:06:06+0000
- **Assignee**: Unassigned
- **Message**:
  Remove usage of generic wildcard type.

## 190. AYJjLGDHSu9fydlWGkar

- **Rule**: `java:S2187`
- **Severity**: BLOCKER
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/ContentEqualsJLBHTest.java:23`
- **Effort**: 5min
- **Created**: 2022-07-25T18:39:41+0000
- **Assignee**: Unassigned
- **Message**:
  Add some tests to this class.

## 191. AY-YRTmrrjWdiV1GRjLq

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/internal/EmptyBytesStoreTest.java:507`
- **Effort**: 5min
- **Created**: 2022-07-25T18:39:41+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 192. AYIb9URNeC6NhI4Xw64C

- **Rule**: `java:S4144`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/HeapBytesStore.java:624`
- **Effort**: 15min
- **Created**: 2022-07-20T14:13:19+0000
- **Assignee**: Unassigned
- **Message**:
  Update this method so that its implementation is not identical to "addressForRead" on line 614.

## 193. AYIa3RlhUVjp_sVVkpyY

- **Rule**: `java:S1452`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/Bytes.java:398`
- **Effort**: 20min
- **Created**: 2022-07-20T09:07:17+0000
- **Assignee**: Unassigned
- **Message**:
  Remove usage of generic wildcard type.

## 194. AYIHsHrFsJU0gXq1cJkU

- **Rule**: `java:S1301`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesUtil.java:675`
- **Effort**: 5min
- **Created**: 2022-07-16T15:46:02+0000
- **Assignee**: Unassigned
- **Message**:
  Replace this "switch" statement by "if" statements to increase readability.

## 195. AYIHsHrFsJU0gXq1cJkW

- **Rule**: `java:S131`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesUtil.java:675`
- **Effort**: 5min
- **Created**: 2022-07-16T15:46:02+0000
- **Assignee**: Unassigned
- **Message**:
  Add a default case to this switch.

## 196. AYFr33vA68nMo98aZGDj

- **Rule**: `java:S1161`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/CommonMappedBytes.java:565`
- **Effort**: 5min
- **Created**: 2022-06-16T09:36:35+0000
- **Assignee**: Unassigned
- **Message**:
  Add the "@Override" annotation above this method signature

## 197. AYFjLCoeBFEDfTWj496_

- **Rule**: `java:S2447`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/BytesInternal.java:215`
- **Effort**: 20min
- **Created**: 2022-06-14T17:03:47+0000
- **Assignee**: Unassigned
- **Message**:
  Null is returned but a "Boolean" is expected.

## 198. AYFjLCoeBFEDfTWj497A

- **Rule**: `java:S2447`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/BytesInternal.java:231`
- **Effort**: 20min
- **Created**: 2022-06-14T17:03:47+0000
- **Assignee**: Unassigned
- **Message**:
  Null is returned but a "Boolean" is expected.

## 199. AYFjLCoeBFEDfTWj497B

- **Rule**: `java:S2447`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/BytesInternal.java:261`
- **Effort**: 20min
- **Created**: 2022-06-14T17:03:47+0000
- **Assignee**: Unassigned
- **Message**:
  Null is returned but a "Boolean" is expected.

## 200. AYFZ-zI3ylFVsoN27X3X

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/BytesInternal.java:130`
- **Effort**: 30min
- **Created**: 2022-06-12T22:13:28+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility update should be removed.

## 201. AYEUByTtFWip2-KRL_1n

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/MappedUniqueTimeProvider.java:59`
- **Effort**: 0min
- **Created**: 2022-05-30T08:13:22+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 202. AYAq6JYvvZr84gBKkwBH

- **Rule**: `java:S1452`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/Bytes.java:661`
- **Effort**: 20min
- **Created**: 2022-04-13T23:29:25+0000
- **Assignee**: Unassigned
- **Message**:
  Remove usage of generic wildcard type.

## 203. AYAq6JbfvZr84gBKkwBJ

- **Rule**: `java:S1452`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesContext.java:29`
- **Effort**: 20min
- **Created**: 2022-04-13T23:29:25+0000
- **Assignee**: Unassigned
- **Message**:
  Remove usage of generic wildcard type.

## 204. AYAq6JaNvZr84gBKkwBI

- **Rule**: `java:S1452`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesUtil.java:233`
- **Effort**: 20min
- **Created**: 2022-04-13T23:29:25+0000
- **Assignee**: Unassigned
- **Message**:
  Remove usage of generic wildcard type.

## 205. AYAq6JefvZr84gBKkwBO

- **Rule**: `java:S1452`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/MappedFile.java:393`
- **Effort**: 20min
- **Created**: 2022-04-13T23:29:25+0000
- **Assignee**: Unassigned
- **Message**:
  Remove usage of generic wildcard type.

## 206. AYAq6JefvZr84gBKkwBP

- **Rule**: `java:S1452`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/MappedFile.java:439`
- **Effort**: 20min
- **Created**: 2022-04-13T23:29:25+0000
- **Assignee**: Unassigned
- **Message**:
  Remove usage of generic wildcard type.

## 207. AYAq6JdsvZr84gBKkwBM

- **Rule**: `java:S1452`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/RandomCommon.java:224`
- **Effort**: 20min
- **Created**: 2022-04-13T23:29:25+0000
- **Assignee**: Unassigned
- **Message**:
  Remove usage of generic wildcard type.

## 208. AYAq6JdsvZr84gBKkwBN

- **Rule**: `java:S1452`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/RandomCommon.java:235`
- **Effort**: 20min
- **Created**: 2022-04-13T23:29:25+0000
- **Assignee**: Unassigned
- **Message**:
  Remove usage of generic wildcard type.

## 209. AYAq6JSUvZr84gBKkwBE

- **Rule**: `java:S1452`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/BytesInternal.java:1190`
- **Effort**: 20min
- **Created**: 2022-04-13T23:29:25+0000
- **Assignee**: Unassigned
- **Message**:
  Remove usage of generic wildcard type.

## 210. AYAq6JSVvZr84gBKkwBG

- **Rule**: `java:S1452`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/BytesInternal.java:3470`
- **Effort**: 20min
- **Created**: 2022-04-13T23:29:25+0000
- **Assignee**: Unassigned
- **Message**:
  Remove usage of generic wildcard type.

## 211. AY-YRTwLrjWdiV1GRjNe

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesReleaseInvariantObjectTest.java:95`
- **Effort**: 5min
- **Created**: 2022-04-13T23:29:25+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  This block of commented-out lines of code should be removed.

## 212. AX_l64I3wkROPlSg-NCi

- **Rule**: `java:S2699`
- **Severity**: BLOCKER
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/ZeroCostAssertionStatusTest.java:24`
- **Effort**: 10min
- **Created**: 2022-04-01T14:25:26+0000
- **Assignee**: minborg@github
- **Message**:
  Add at least one assertion to this test case.

## 213. AX_Z80eA1sHrYf5Lo8UT

- **Rule**: `java:S100`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/AppendableUtil.java:271`
- **Effort**: 5min
- **Created**: 2022-03-30T07:52:28+0000
- **Assignee**: minborg@github
- **Message**:
  Rename this method name to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 214. AX_Z80Ux1sHrYf5Lo8UO

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/CommonMappedBytes.java:325`
- **Effort**: 10min
- **Created**: 2022-03-30T07:52:28+0000
- **Assignee**: minborg@github
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 20 to the 15 allowed.

## 215. AX_N_8Jp5EDiRQZeJH3Z

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesMarshaller.java:141`
- **Effort**: 12min
- **Created**: 2022-03-27T22:26:33+0000
- **Assignee**: minborg@github
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 22 to the 15 allowed.

## 216. AX_N_8Jp5EDiRQZeJH3a

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesMarshaller.java:345`
- **Effort**: 30min
- **Created**: 2022-03-27T22:26:33+0000
- **Assignee**: minborg@github
- **Message**:
  This accessibility bypass should be removed.

## 217. AX_N_8Gq5EDiRQZeJH3W

- **Rule**: `java:S135`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/ref/TextIntArrayReference.java:130`
- **Effort**: 20min
- **Created**: 2022-03-27T22:26:33+0000
- **Assignee**: minborg@github
- **Message**:
  Reduce the total number of break and continue statements in this loop to use at most one.

## 218. AX9WHb1eFfFFIRtcbrrH

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/Bytes.java:1023`
- **Effort**: 10min
- **Created**: 2022-02-24T09:50:17+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 20 to the 15 allowed.

## 219. AX9WHb1eFfFFIRtcbrrJ

- **Rule**: `java:S127`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/Bytes.java:1045`
- **Effort**: 10min
- **Created**: 2022-02-24T09:50:17+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor the code in order to not assign to this loop counter from within the loop body.

## 220. AX9WHb1eFfFFIRtcbrrK

- **Rule**: `java:S127`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/Bytes.java:1110`
- **Effort**: 10min
- **Created**: 2022-02-24T09:50:17+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor the code in order to not assign to this loop counter from within the loop body.

## 221. AYw0tGVvUtrwJAUvk2_P

- **Rule**: `java:S5786`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/UnsafeRWObjectTest.java:29`
- **Effort**: 2min
- **Created**: 2022-02-23T10:53:11+0000
- **Assignee**: glukos@github
- **Message**:
  Remove this 'public' modifier.

## 222. AY-YRT2YrjWdiV1GRjOB

- **Rule**: `java:S2093`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/DistributedUniqueTimeProvider.java:62`
- **Effort**: 15min
- **Created**: 2022-02-16T17:56:13+0000
- **Assignee**: Unassigned
- **Message**:
  Change this "try" to a try-with-resources.

## 223. AY-YRTo8rjWdiV1GRjML

- **Rule**: `java:S1172`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/ref/ByteableReferenceTest.java:39`
- **Effort**: 5min
- **Created**: 2022-02-15T09:27:57+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused method parameter "className".

## 224. AYw0tGVvUtrwJAUvk2_V

- **Rule**: `java:S5786`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/UnsafeRWObjectTest.java:27`
- **Effort**: 2min
- **Created**: 2022-02-14T12:55:05+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove this 'public' modifier.

## 225. AX50THGdx7xEUXIcomBP

- **Rule**: `java:S135`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/BytesInternal.java:2978`
- **Effort**: 40min
- **Created**: 2022-01-19T11:00:38+0000
- **Assignee**: Unassigned
- **Message**:
  Reduce the total number of break and continue statements in this loop to use at most one.

## 226. AY-YRTrsrjWdiV1GRjMp

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/MappedUniqueTimeProviderTest.java:161`
- **Effort**: 5min
- **Created**: 2021-12-28T12:17:58+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  This block of commented-out lines of code should be removed.

## 227. AX1XRguEe2eGZuiAJJNT

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/BytesInternal.java:3331`
- **Effort**: 6min
- **Created**: 2021-11-23T08:24:46+0000
- **Assignee**: minborg@github
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 16 to the 15 allowed.

## 228. AY-YRTnprjWdiV1GRjLs

- **Rule**: `java:S1066`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesPrimitiveParameterTest.java:59`
- **Effort**: 5min
- **Created**: 2021-11-16T07:58:16+0000
- **Assignee**: Unassigned
- **Message**:
  Merge this if statement with the enclosing one.

## 229. AY-YRTnprjWdiV1GRjLw

- **Rule**: `java:S1854`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesPrimitiveParameterTest.java:98`
- **Effort**: 1min
- **Created**: 2021-11-16T07:58:16+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this useless assignment to local variable "os".

## 230. AY-YRTnprjWdiV1GRjLx

- **Rule**: `java:S1481`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesPrimitiveParameterTest.java:98`
- **Effort**: 5min
- **Created**: 2021-11-16T07:58:16+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "os" local variable.

## 231. AY-YRTnprjWdiV1GRjLv

- **Rule**: `java:S1854`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesPrimitiveParameterTest.java:105`
- **Effort**: 1min
- **Created**: 2021-11-16T07:58:16+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this useless assignment to local variable "bytes".

## 232. AY-YRTnprjWdiV1GRjLy

- **Rule**: `java:S1481`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesPrimitiveParameterTest.java:105`
- **Effort**: 5min
- **Created**: 2021-11-16T07:58:16+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "bytes" local variable.

## 233. AY-YRTnprjWdiV1GRjLu

- **Rule**: `java:S1854`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesPrimitiveParameterTest.java:106`
- **Effort**: 1min
- **Created**: 2021-11-16T07:58:16+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this useless assignment to local variable "bb".

## 234. AY-YRTnprjWdiV1GRjLz

- **Rule**: `java:S1481`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesPrimitiveParameterTest.java:106`
- **Effort**: 5min
- **Created**: 2021-11-16T07:58:16+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "bb" local variable.

## 235. AY-YRTnprjWdiV1GRjLt

- **Rule**: `java:S1144`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesPrimitiveParameterTest.java:176`
- **Effort**: 2min
- **Created**: 2021-11-16T07:58:16+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused private "provideNegativeNonNegativeOperationsOtherException" method.

## 236. AY-YRTnprjWdiV1GRjL5

- **Rule**: `java:S1854`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesPrimitiveParameterTest.java:177`
- **Effort**: 1min
- **Created**: 2021-11-16T07:58:16+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this useless assignment to local variable "os".

## 237. AY-YRTnprjWdiV1GRjL6

- **Rule**: `java:S1481`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesPrimitiveParameterTest.java:177`
- **Effort**: 5min
- **Created**: 2021-11-16T07:58:16+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "os" local variable.

## 238. AY-YRTnprjWdiV1GRjL4

- **Rule**: `java:S1854`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesPrimitiveParameterTest.java:183`
- **Effort**: 1min
- **Created**: 2021-11-16T07:58:16+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this useless assignment to local variable "bs".

## 239. AY-YRTnprjWdiV1GRjL7

- **Rule**: `java:S1481`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesPrimitiveParameterTest.java:183`
- **Effort**: 5min
- **Created**: 2021-11-16T07:58:16+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "bs" local variable.

## 240. AY-YRTnprjWdiV1GRjL3

- **Rule**: `java:S1854`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesPrimitiveParameterTest.java:184`
- **Effort**: 1min
- **Created**: 2021-11-16T07:58:16+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this useless assignment to local variable "bytes".

## 241. AY-YRTnprjWdiV1GRjL8

- **Rule**: `java:S1481`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesPrimitiveParameterTest.java:184`
- **Effort**: 5min
- **Created**: 2021-11-16T07:58:16+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "bytes" local variable.

## 242. AY-YRTnprjWdiV1GRjL2

- **Rule**: `java:S1854`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesPrimitiveParameterTest.java:185`
- **Effort**: 1min
- **Created**: 2021-11-16T07:58:16+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this useless assignment to local variable "bb".

## 243. AY-YRTnprjWdiV1GRjL9

- **Rule**: `java:S1481`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesPrimitiveParameterTest.java:185`
- **Effort**: 5min
- **Created**: 2021-11-16T07:58:16+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "bb" local variable.

## 244. AX1ItTrbRXQbSDaUHowj

- **Rule**: `java:S2160`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/NativeBytes.java:45`
- **Effort**: 30min
- **Created**: 2021-11-10T13:50:14+0000
- **Assignee**: minborg@github
- **Message**:
  Override the "equals" method in this class.

## 245. AX1ItTnjRXQbSDaUHowa

- **Rule**: `java:S2160`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/UncheckedBytes.java:46`
- **Effort**: 30min
- **Created**: 2021-11-10T13:50:14+0000
- **Assignee**: minborg@github
- **Message**:
  Override the "equals" method in this class.

## 246. AY-YRTwLrjWdiV1GRjNf

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesReleaseInvariantObjectTest.java:97`
- **Effort**: 5min
- **Created**: 2021-11-10T12:15:24+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 247. AY-YRTwLrjWdiV1GRjNd

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesReleaseInvariantObjectTest.java:89`
- **Effort**: 5min
- **Created**: 2021-11-05T12:49:08+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 248. AY-YRTwLrjWdiV1GRjNc

- **Rule**: `java:S1854`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesReleaseInvariantObjectTest.java:100`
- **Effort**: 1min
- **Created**: 2021-11-05T12:49:08+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this useless assignment to local variable "hash".

## 249. AY-YRTwLrjWdiV1GRjNg

- **Rule**: `java:S1481`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesReleaseInvariantObjectTest.java:100`
- **Effort**: 5min
- **Created**: 2021-11-05T12:49:08+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "hash" local variable.

## 250. AY-YRTwLrjWdiV1GRjNb

- **Rule**: `java:S1854`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesReleaseInvariantObjectTest.java:101`
- **Effort**: 1min
- **Created**: 2021-11-05T12:49:08+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this useless assignment to local variable "expected".

## 251. AY-YRTwLrjWdiV1GRjNh

- **Rule**: `java:S1481`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesReleaseInvariantObjectTest.java:101`
- **Effort**: 5min
- **Created**: 2021-11-05T12:49:08+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "expected" local variable.

## 252. AX_QA0qDvXDaEYQSpO0z

- **Rule**: `xml:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:pom.xml:383`
- **Effort**: 5min
- **Created**: 2021-11-05T12:01:01+0000
- **Assignee**: minborg@github
- **Message**:
  Remove this commented out code.

## 253. AZMw7LdnGAwb07yl4Tl1

- **Rule**: `xml:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:pom.xml:385`
- **Effort**: 5min
- **Created**: 2021-11-05T12:01:01+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this commented out code.

## 254. AY-YRToNrjWdiV1GRjMA

- **Rule**: `java:S1066`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesJavaDocComplianceTest.java:147`
- **Effort**: 5min
- **Created**: 2021-11-01T19:12:09+0000
- **Assignee**: Unassigned
- **Message**:
  Merge this if statement with the enclosing one.

## 255. AY-YRToNrjWdiV1GRjMC

- **Rule**: `java:S1481`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesJavaDocComplianceTest.java:151`
- **Effort**: 5min
- **Created**: 2021-11-01T19:12:09+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "foo" local variable.

## 256. AY-YRToNrjWdiV1GRjMD

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesJavaDocComplianceTest.java:173`
- **Effort**: 5min
- **Created**: 2021-11-01T19:12:09+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 257. AY-YRToNrjWdiV1GRjMB

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesJavaDocComplianceTest.java:122`
- **Effort**: 0min
- **Created**: 2021-11-01T12:33:07+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 258. AY-YRToNrjWdiV1GRjMF

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesJavaDocComplianceTest.java:234`
- **Effort**: 0min
- **Created**: 2021-11-01T12:33:07+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 259. AY-YRToNrjWdiV1GRjMG

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesJavaDocComplianceTest.java:235`
- **Effort**: 0min
- **Created**: 2021-11-01T12:33:07+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 260. AY-YRToNrjWdiV1GRjMH

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesJavaDocComplianceTest.java:236`
- **Effort**: 0min
- **Created**: 2021-11-01T12:33:07+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 261. AY-YRTuyrjWdiV1GRjNN

- **Rule**: `java:S1144`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/NotNullHandlingTest.java:71`
- **Effort**: 2min
- **Created**: 2021-11-01T12:33:07+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused private "fooZeroCost" method.

## 262. AXzVm29NM87u1Qpdut13

- **Rule**: `java:S100`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BinaryWireCode.java:184`
- **Effort**: 5min
- **Created**: 2021-10-28T12:23:14+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this method name to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 263. AXzVm2-eM87u1Qpdut2C

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesMarshaller.java:254`
- **Effort**: 30min
- **Created**: 2021-10-28T12:23:14+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 264. AXzVm24CM87u1Qpdutyr

- **Rule**: `java:S1452`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesStore.java:111`
- **Effort**: 20min
- **Created**: 2021-10-28T12:23:14+0000
- **Assignee**: Unassigned
- **Message**:
  Remove usage of generic wildcard type.

## 265. AXzVm26BM87u1Qpdutzx

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/BytesFieldInfo.java:59`
- **Effort**: 9min
- **Created**: 2021-10-28T12:23:14+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 19 to the 15 allowed.

## 266. AXzVm26nM87u1Qpdut0b

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/BytesInternal.java:2705`
- **Effort**: 44min
- **Created**: 2021-10-28T12:23:14+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 54 to the 15 allowed.

## 267. AXzVm26nM87u1Qpdut0c

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/BytesInternal.java:2831`
- **Effort**: 16min
- **Created**: 2021-10-28T12:23:14+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 26 to the 15 allowed.

## 268. AXzVm22AM87u1QpdutyF

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/util/AbstractInterner.java:185`
- **Effort**: 0min
- **Created**: 2021-10-28T12:23:14+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 269. AXzVm26nM87u1Qpdut0r

- **Rule**: `java:S135`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/BytesInternal.java:2747`
- **Effort**: 40min
- **Created**: 2021-10-08T11:57:06+0000
- **Assignee**: Unassigned
- **Message**:
  Reduce the total number of break and continue statements in this loop to use at most one.

## 270. AXzVm28pM87u1Qpdut1h

- **Rule**: `java:S2093`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/MappedBytes.java:110`
- **Effort**: 15min
- **Created**: 2021-09-16T14:40:44+0000
- **Assignee**: Unassigned
- **Message**:
  Change this "try" to a try-with-resources.

## 271. AXzVm28pM87u1Qpdut1i

- **Rule**: `java:S2093`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/MappedBytes.java:240`
- **Effort**: 15min
- **Created**: 2021-09-16T14:40:44+0000
- **Assignee**: Unassigned
- **Message**:
  Change this "try" to a try-with-resources.

## 272. AXzVm25JM87u1QpdutzN

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/ChunkedMappedBytes.java:380`
- **Effort**: 0min
- **Created**: 2021-09-16T14:40:44+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 273. AXzVm25JM87u1QpdutzP

- **Rule**: `java:S1119`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/ChunkedMappedBytes.java:590`
- **Effort**: 30min
- **Created**: 2021-09-16T14:40:44+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor the code to remove this label and the need for it.

## 274. AXzVm26KM87u1Qpdutz3

- **Rule**: `java:S1113`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/ChunkedMappedFile.java:408`
- **Effort**: 20min
- **Created**: 2021-09-16T14:40:44+0000
- **Assignee**: Unassigned
- **Message**:
  Do not override the Object.finalize() method.

## 275. AYl3XT7VWm0iSXXh63Nh

- **Rule**: `java:S1874`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/ChunkedMappedFile.java:408`
- **Effort**: 15min
- **Created**: 2021-09-16T14:40:44+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Don't override a deprecated method or explicitly mark it as "@Deprecated".

## 276. AXzVm25kM87u1QpdutzW

- **Rule**: `java:S2160`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/CommonMappedBytes.java:43`
- **Effort**: 30min
- **Created**: 2021-09-16T14:40:44+0000
- **Assignee**: Unassigned
- **Message**:
  Override the "equals" method in this class.

## 277. AXzVm25kM87u1Qpdutzd

- **Rule**: `java:S1119`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/CommonMappedBytes.java:343`
- **Effort**: 30min
- **Created**: 2021-09-16T14:40:44+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor the code to remove this label and the need for it.

## 278. AXzVm25kM87u1Qpdutze

- **Rule**: `java:S1119`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/CommonMappedBytes.java:365`
- **Effort**: 30min
- **Created**: 2021-09-16T14:40:44+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor the code to remove this label and the need for it.

## 279. AXzVm25bM87u1QpdutzU

- **Rule**: `java:S1874`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/SingleMappedFile.java:347`
- **Effort**: 15min
- **Created**: 2021-09-16T14:40:44+0000
- **Assignee**: Unassigned
- **Message**:
  Don't override a deprecated method or explicitly mark it as "@Deprecated".

## 280. AXzVm25bM87u1QpdutzV

- **Rule**: `java:S1113`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/SingleMappedFile.java:347`
- **Effort**: 20min
- **Created**: 2021-09-16T14:40:44+0000
- **Assignee**: Unassigned
- **Message**:
  Do not override the Object.finalize() method.

## 281. AY-YRTsirjWdiV1GRjMz

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/MappedBytesTest.java:492`
- **Effort**: 5min
- **Created**: 2021-09-16T14:40:44+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  This block of commented-out lines of code should be removed.

## 282. AY-YRTsirjWdiV1GRjM0

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/MappedBytesTest.java:497`
- **Effort**: 5min
- **Created**: 2021-09-16T14:40:44+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  This block of commented-out lines of code should be removed.

## 283. AY-YRTsirjWdiV1GRjM1

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/MappedBytesTest.java:500`
- **Effort**: 5min
- **Created**: 2021-09-16T14:40:44+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  This block of commented-out lines of code should be removed.

## 284. AY_FiNulOd5e3hTloS3C

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/ref/AbstractReference.java:83`
- **Effort**: 5min
- **Created**: 2021-08-26T10:50:14+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "bytesStore" which hides the field declared at line 49.

## 285. AXzVm24CM87u1Qpdutyx

- **Rule**: `java:S1452`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesStore.java:187`
- **Effort**: 20min
- **Created**: 2021-08-24T09:04:17+0000
- **Assignee**: minborg@github
- **Message**:
  Remove usage of generic wildcard type.

## 286. AYqsZ1uqzzxmyr6IR46S

- **Rule**: `java:S1128`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/ByteStringWriter.java:19`
- **Effort**: 1min
- **Created**: 2021-07-13T12:07:18+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove this unused import 'net.openhft.chronicle.core.Jvm'.

## 287. AXzVm26nM87u1Qpdut0C

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/BytesInternal.java:550`
- **Effort**: 0min
- **Created**: 2021-07-13T12:07:18+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 288. AXzVm26nM87u1Qpdut0E

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/BytesInternal.java:646`
- **Effort**: 17min
- **Created**: 2021-07-13T12:07:18+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 27 to the 15 allowed.

## 289. AXzVm26nM87u1Qpdut0F

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/BytesInternal.java:703`
- **Effort**: 0min
- **Created**: 2021-07-13T12:07:18+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 290. AXzVm26nM87u1Qpdut0p

- **Rule**: `java:S135`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/BytesInternal.java:759`
- **Effort**: 20min
- **Created**: 2021-07-13T12:07:18+0000
- **Assignee**: Unassigned
- **Message**:
  Reduce the total number of break and continue statements in this loop to use at most one.

## 291. AXzVm26nM87u1Qpdut0L

- **Rule**: `java:S100`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/BytesInternal.java:824`
- **Effort**: 5min
- **Created**: 2021-07-13T12:07:18+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this method name to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 292. AXzVm26nM87u1Qpdut0O

- **Rule**: `java:S100`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/BytesInternal.java:882`
- **Effort**: 5min
- **Created**: 2021-07-13T12:07:18+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this method name to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 293. AXzVm26nM87u1Qpdut0P

- **Rule**: `java:S100`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/BytesInternal.java:932`
- **Effort**: 5min
- **Created**: 2021-07-13T12:07:18+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this method name to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 294. AXzVm26nM87u1Qpdut0Q

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/BytesInternal.java:968`
- **Effort**: 14min
- **Created**: 2021-07-13T12:07:18+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 24 to the 15 allowed.

## 295. AXzVm26nM87u1Qpdut0R

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/BytesInternal.java:1024`
- **Effort**: 0min
- **Created**: 2021-07-13T12:07:18+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 296. AXzVm26nM87u1Qpdut0S

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/BytesInternal.java:1032`
- **Effort**: 6min
- **Created**: 2021-07-13T12:07:18+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 16 to the 15 allowed.

## 297. AXzVm26nM87u1Qpdut0T

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/BytesInternal.java:1083`
- **Effort**: 0min
- **Created**: 2021-07-13T12:07:18+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 298. AXzVm26nM87u1Qpdut0y

- **Rule**: `java:S1141`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/BytesInternal.java:1579`
- **Effort**: 20min
- **Created**: 2021-07-13T12:07:18+0000
- **Assignee**: Unassigned
- **Message**:
  Extract this nested try block into a separate method.

## 299. AXzVm26nM87u1Qpdut0U

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/BytesInternal.java:2061`
- **Effort**: 8min
- **Created**: 2021-07-13T12:07:18+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 18 to the 15 allowed.

## 300. AXzVm26nM87u1Qpdut0V

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/BytesInternal.java:2138`
- **Effort**: 39min
- **Created**: 2021-07-13T12:07:18+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 49 to the 15 allowed.

## 301. AXzVm26nM87u1Qpdut0q

- **Rule**: `java:S135`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/BytesInternal.java:2214`
- **Effort**: 20min
- **Created**: 2021-07-13T12:07:18+0000
- **Assignee**: Unassigned
- **Message**:
  Reduce the total number of break and continue statements in this loop to use at most one.

## 302. AXzVm26nM87u1Qpdut0W

- **Rule**: `java:S100`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/BytesInternal.java:2420`
- **Effort**: 5min
- **Created**: 2021-07-13T12:07:18+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this method name to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 303. AXzVm26nM87u1Qpdut0X

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/BytesInternal.java:2420`
- **Effort**: 12min
- **Created**: 2021-07-13T12:07:18+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 22 to the 15 allowed.

## 304. AXzVm26nM87u1Qpdut0Y

- **Rule**: `java:S100`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/BytesInternal.java:2464`
- **Effort**: 5min
- **Created**: 2021-07-13T12:07:18+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this method name to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 305. AXzVm26nM87u1Qpdut0Z

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/BytesInternal.java:2464`
- **Effort**: 9min
- **Created**: 2021-07-13T12:07:18+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 19 to the 15 allowed.

## 306. AXzVm26nM87u1Qpdut0a

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/BytesInternal.java:2547`
- **Effort**: 9min
- **Created**: 2021-07-13T12:07:18+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 19 to the 15 allowed.

## 307. AXzVm26nM87u1Qpdut0u

- **Rule**: `java:S135`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/BytesInternal.java:2874`
- **Effort**: 20min
- **Created**: 2021-07-13T12:07:18+0000
- **Assignee**: Unassigned
- **Message**:
  Reduce the total number of break and continue statements in this loop to use at most one.

## 308. AXzVm26nM87u1Qpdut0d

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/BytesInternal.java:2933`
- **Effort**: 7min
- **Created**: 2021-07-13T12:07:18+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 17 to the 15 allowed.

## 309. AXzVm26nM87u1Qpdut0t

- **Rule**: `java:S135`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/BytesInternal.java:2952`
- **Effort**: 40min
- **Created**: 2021-07-13T12:07:18+0000
- **Assignee**: Unassigned
- **Message**:
  Reduce the total number of break and continue statements in this loop to use at most one.

## 310. AXzVm26nM87u1Qpdut0v

- **Rule**: `java:S135`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/BytesInternal.java:3019`
- **Effort**: 40min
- **Created**: 2021-07-13T12:07:18+0000
- **Assignee**: Unassigned
- **Message**:
  Reduce the total number of break and continue statements in this loop to use at most one.

## 311. AXzVm26nM87u1Qpdut0w

- **Rule**: `java:S135`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/BytesInternal.java:3053`
- **Effort**: 20min
- **Created**: 2021-07-13T12:07:18+0000
- **Assignee**: Unassigned
- **Message**:
  Reduce the total number of break and continue statements in this loop to use at most one.

## 312. AXzVm26nM87u1Qpdut0j

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/BytesInternal.java:3415`
- **Effort**: 8min
- **Created**: 2021-07-13T12:07:18+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 18 to the 15 allowed.

## 313. AXzVm26nM87u1Qpdut0i

- **Rule**: `java:S2447`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/BytesInternal.java:3421`
- **Effort**: 20min
- **Created**: 2021-07-13T12:07:18+0000
- **Assignee**: Unassigned
- **Message**:
  Null is returned but a "Boolean" is expected.

## 314. AXzVm26nM87u1Qpdut0h

- **Rule**: `java:S2447`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/BytesInternal.java:3440`
- **Effort**: 20min
- **Created**: 2021-07-13T12:07:18+0000
- **Assignee**: Unassigned
- **Message**:
  Null is returned but a "Boolean" is expected.

## 315. AXzVm26nM87u1Qpdut0k

- **Rule**: `java:S1119`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/BytesInternal.java:3474`
- **Effort**: 30min
- **Created**: 2021-07-13T12:07:18+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor the code to remove this label and the need for it.

## 316. AXzVm252M87u1Qpdutzt

- **Rule**: `java:S1068`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/NativeBytesStore.java:79`
- **Effort**: 5min
- **Created**: 2021-07-13T12:07:18+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "finalizer" private field.

## 317. AXzVm252M87u1Qpdutzu

- **Rule**: `java:S1104`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/NativeBytesStore.java:81`
- **Effort**: 10min
- **Created**: 2021-07-13T12:07:18+0000
- **Assignee**: Unassigned
- **Message**:
  Make address a static final constant or non-public and provide accessors if needed.

## 318. AXzVm252M87u1Qpdutzv

- **Rule**: `java:S1104`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/NativeBytesStore.java:83`
- **Effort**: 10min
- **Created**: 2021-07-13T12:07:18+0000
- **Assignee**: Unassigned
- **Message**:
  Make memory a static final constant or non-public and provide accessors if needed.

## 319. AXzVm252M87u1Qpdutzw

- **Rule**: `java:S1104`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/NativeBytesStore.java:85`
- **Effort**: 10min
- **Created**: 2021-07-13T12:07:18+0000
- **Assignee**: Unassigned
- **Message**:
  Make maximumLimit a static final constant or non-public and provide accessors if needed.

## 320. AZo0umXHC45ILiv_v3UZ

- **Rule**: `java:S1874`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/NativeBytesStore.java:102`
- **Effort**: 15min
- **Created**: 2021-07-13T12:07:18+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove this use of "<init>"; it is deprecated.

## 321. AZo0umXHC45ILiv_v3Ua

- **Rule**: `java:S1133`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/NativeBytesStore.java:106`
- **Effort**: 10min
- **Created**: 2021-07-13T12:07:18+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Do not forget to remove this deprecated code someday.

## 322. AZo0umXHC45ILiv_v3Ub

- **Rule**: `java:S1123`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/NativeBytesStore.java:106`
- **Effort**: 5min
- **Created**: 2021-07-13T12:07:18+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Add the missing @deprecated Javadoc tag.

## 323. AXzVm252M87u1Qpdutzm

- **Rule**: `java:S1119`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/NativeBytesStore.java:734`
- **Effort**: 30min
- **Created**: 2021-07-13T12:07:18+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor the code to remove this label and the need for it.

## 324. AXzVm252M87u1Qpdutzn

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/NativeBytesStore.java:805`
- **Effort**: 30min
- **Created**: 2021-07-13T12:07:18+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 325. AXzVm252M87u1Qpdutzo

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/NativeBytesStore.java:806`
- **Effort**: 30min
- **Created**: 2021-07-13T12:07:18+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 326. AXzVm252M87u1Qpdutzp

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/NativeBytesStore.java:807`
- **Effort**: 30min
- **Created**: 2021-07-13T12:07:18+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 327. AXzVm252M87u1Qpdutzr

- **Rule**: `java:S1113`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/internal/NativeBytesStore.java:998`
- **Effort**: 20min
- **Created**: 2021-07-13T12:07:18+0000
- **Assignee**: Unassigned
- **Message**:
  Do not override the Object.finalize() method.

## 328. AZDllLv95bUw8_NMnKTF

- **Rule**: `java:S1124`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/StructTest.java:40`
- **Effort**: 2min
- **Created**: 2021-07-05T14:41:51+0000
- **Assignee**: Unassigned
- **Message**:
  Reorder the modifiers to comply with the Java Language Specification.

## 329. AY-YRTxMrjWdiV1GRjN-

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/StructTest.java:163`
- **Effort**: 5min
- **Created**: 2021-07-05T14:41:51+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 330. AY-YRTl5rjWdiV1GRjLf

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/ByteStoreTest.java:277`
- **Effort**: 5min
- **Created**: 2021-06-28T11:49:39+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  This block of commented-out lines of code should be removed.

## 331. AY-YRTl5rjWdiV1GRjLg

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/ByteStoreTest.java:378`
- **Effort**: 5min
- **Created**: 2021-06-28T11:49:39+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  This block of commented-out lines of code should be removed.

## 332. AXzVm24CM87u1Qpdutyq

- **Rule**: `java:S1452`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesStore.java:101`
- **Effort**: 20min
- **Created**: 2021-06-25T12:22:51+0000
- **Assignee**: Unassigned
- **Message**:
  Remove usage of generic wildcard type.

## 333. AY-YRTxMrjWdiV1GRjN_

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/StructTest.java:309`
- **Effort**: 5min
- **Created**: 2021-06-22T11:29:47+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  This block of commented-out lines of code should be removed.

## 334. AY-YRTxMrjWdiV1GRjOA

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/StructTest.java:409`
- **Effort**: 5min
- **Created**: 2021-06-22T11:29:47+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  This block of commented-out lines of code should be removed.

## 335. AXzVm2-eM87u1Qpdut2J

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesMarshaller.java:421`
- **Effort**: 30min
- **Created**: 2021-05-18T21:52:58+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 336. AXzVm2-eM87u1Qpdut2g

- **Rule**: `java:S1121`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesMarshaller.java:421`
- **Effort**: 5min
- **Created**: 2021-05-18T21:52:58+0000
- **Assignee**: Unassigned
- **Message**:
  Extract the assignment out of this expression.

## 337. AXzVm2-eM87u1Qpdut2L

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesMarshaller.java:488`
- **Effort**: 30min
- **Created**: 2021-05-18T21:52:58+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 338. AXzVm2-eM87u1Qpdut2h

- **Rule**: `java:S1121`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesMarshaller.java:488`
- **Effort**: 5min
- **Created**: 2021-05-18T21:52:58+0000
- **Assignee**: Unassigned
- **Message**:
  Extract the assignment out of this expression.

## 339. AY-YRTsBrjWdiV1GRjMv

- **Rule**: `java:S1066`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/StringRWPerfTest.java:92`
- **Effort**: 5min
- **Created**: 2021-05-13T10:40:34+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Merge this if statement with the enclosing one.

## 340. AY-YRTsirjWdiV1GRjM5

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/MappedBytesTest.java:616`
- **Effort**: 5min
- **Created**: 2021-04-21T12:15:55+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  This block of commented-out lines of code should be removed.

## 341. AY-YRTqBrjWdiV1GRjMS

- **Rule**: `java:S1172`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/internal/BytesInternalGuardedTest.java:43`
- **Effort**: 5min
- **Created**: 2021-04-19T15:15:56+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove this unused method parameter "name".

## 342. AY-YRTqBrjWdiV1GRjMT

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/internal/BytesInternalGuardedTest.java:57`
- **Effort**: 5min
- **Created**: 2021-04-19T15:15:56+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  This block of commented-out lines of code should be removed.

## 343. AY-YRTqBrjWdiV1GRjMV

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/internal/BytesInternalGuardedTest.java:132`
- **Effort**: 5min
- **Created**: 2021-04-19T15:15:56+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  This block of commented-out lines of code should be removed.

## 344. AY-YRTqBrjWdiV1GRjMU

- **Rule**: `java:S1854`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/internal/BytesInternalGuardedTest.java:137`
- **Effort**: 1min
- **Created**: 2021-04-19T15:15:56+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove this useless assignment to local variable "s2".

## 345. AY-YRTqBrjWdiV1GRjMW

- **Rule**: `java:S1481`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/internal/BytesInternalGuardedTest.java:137`
- **Effort**: 5min
- **Created**: 2021-04-19T15:15:56+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove this unused "s2" local variable.

## 346. AY-YRTqBrjWdiV1GRjMX

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/internal/BytesInternalGuardedTest.java:138`
- **Effort**: 5min
- **Created**: 2021-04-19T15:15:56+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  This block of commented-out lines of code should be removed.

## 347. AY-YRTqBrjWdiV1GRjMY

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/internal/BytesInternalGuardedTest.java:183`
- **Effort**: 5min
- **Created**: 2021-04-19T15:15:56+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  This block of commented-out lines of code should be removed.

## 348. AY-YRTsBrjWdiV1GRjMw

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/StringRWPerfTest.java:37`
- **Effort**: 5min
- **Created**: 2021-04-16T14:37:45+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "bytes" which hides the field declared at line 26.

## 349. AY-YRTsBrjWdiV1GRjMx

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/StringRWPerfTest.java:47`
- **Effort**: 5min
- **Created**: 2021-04-16T14:37:45+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Rename "bytes" which hides the field declared at line 26.

## 350. AY-YRTwCrjWdiV1GRjNa

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesMarshallableTest.java:367`
- **Effort**: 5min
- **Created**: 2021-04-16T08:54:02+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  This block of commented-out lines of code should be removed.

## 351. AY-YRTrhrjWdiV1GRjMj

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/Bytes4Test.java:35`
- **Effort**: 5min
- **Created**: 2021-04-15T08:56:46+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 352. AY-YRTrhrjWdiV1GRjMk

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/Bytes4Test.java:80`
- **Effort**: 5min
- **Created**: 2021-04-15T08:56:46+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 353. AY-YRTrhrjWdiV1GRjMl

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/Bytes4Test.java:90`
- **Effort**: 5min
- **Created**: 2021-04-15T08:56:46+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 354. AY-YRTrhrjWdiV1GRjMm

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/Bytes4Test.java:98`
- **Effort**: 5min
- **Created**: 2021-04-15T08:56:46+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 355. AY-YRTrhrjWdiV1GRjMn

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/Bytes4Test.java:103`
- **Effort**: 5min
- **Created**: 2021-04-15T08:56:46+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 356. AY-YRTrhrjWdiV1GRjMo

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/Bytes4Test.java:106`
- **Effort**: 5min
- **Created**: 2021-04-15T08:56:46+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 357. AZMw7LZ-GAwb07yl4Tlz

- **Rule**: `java:S1905`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/StreamingDataInput.java:661`
- **Effort**: 5min
- **Created**: 2021-04-12T15:21:44+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove this unnecessary cast to "StreamingDataInput".

## 358. AZfUX--8cw2_Zca4HSWD

- **Rule**: `java:S1128`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/StopBitTest.java:22`
- **Effort**: 1min
- **Created**: 2021-04-01T11:24:58+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused import 'java.nio.ByteBuffer'.

## 359. AY-YRTn5rjWdiV1GRjL-

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/StopBitTest.java:53`
- **Effort**: 5min
- **Created**: 2021-04-01T11:24:58+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 360. AZDllLth5bUw8_NMnKS1

- **Rule**: `java:S1068`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesMarshallableTest.java:45`
- **Effort**: 5min
- **Created**: 2021-03-09T10:43:47+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove this unused "name" private field.

## 361. AZUakVlRQdJEqNZOtPKu

- **Rule**: `java:S5786`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesTestCommon.java:75`
- **Effort**: 2min
- **Created**: 2021-02-21T18:55:22+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove this 'public' modifier.

## 362. AZUakVlRQdJEqNZOtPKv

- **Rule**: `java:S5786`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesTestCommon.java:123`
- **Effort**: 2min
- **Created**: 2021-02-21T18:55:22+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove this 'public' modifier.

## 363. AXzVm24_M87u1QpdutzG

- **Rule**: `java:S1181`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesMarshallable.java:72`
- **Effort**: 20min
- **Created**: 2021-02-08T14:06:01+0000
- **Assignee**: Unassigned
- **Message**:
  Catch Exception instead of Throwable.

## 364. AYqsZ2IPzzxmyr6IR46f

- **Rule**: `java:S1128`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesTextMethodTester.java:24`
- **Effort**: 1min
- **Created**: 2021-02-08T14:06:01+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove this unused import 'java.nio.BufferUnderflowException'.

## 365. AY6FUw4uCXhPloZvUpvh

- **Rule**: `java:S1181`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/HexDumpBytes.java:183`
- **Effort**: 20min
- **Created**: 2021-02-08T14:06:01+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Catch Exception instead of Throwable.

## 366. AXzVm2-nM87u1Qpdut2j

- **Rule**: `java:S1113`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/MappedFile.java:553`
- **Effort**: 20min
- **Created**: 2021-02-08T14:06:01+0000
- **Assignee**: Unassigned
- **Message**:
  Do not override the Object.finalize() method.

## 367. AY6FUwvOCXhPloZvUpvf

- **Rule**: `java:S1181`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/ref/BinaryIntReference.java:104`
- **Effort**: 20min
- **Created**: 2021-02-08T14:06:01+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Catch Exception instead of Throwable.

## 368. AXzVm22qM87u1QpdutyP

- **Rule**: `java:S1181`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/ref/BinaryLongArrayReference.java:426`
- **Effort**: 20min
- **Created**: 2021-02-08T14:06:01+0000
- **Assignee**: Unassigned
- **Message**:
  Catch Exception instead of Throwable.

## 369. AXzVm23cM87u1Qpdutyf

- **Rule**: `java:S1181`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/ref/BinaryLongReference.java:98`
- **Effort**: 20min
- **Created**: 2021-02-08T14:06:01+0000
- **Assignee**: Unassigned
- **Message**:
  Catch Exception instead of Throwable.

## 370. AXzVm23CM87u1QpdutyU

- **Rule**: `java:S1181`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/ref/UncheckedLongReference.java:121`
- **Effort**: 20min
- **Created**: 2021-02-08T14:06:01+0000
- **Assignee**: Unassigned
- **Message**:
  Catch Exception instead of Throwable.

## 371. AY-YRTwurjWdiV1GRjNl

- **Rule**: `java:S1172`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/ConcurrentRafAccessTest.java:62`
- **Effort**: 5min
- **Created**: 2021-02-08T14:06:01+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove this unused method parameter "file".

## 372. AY-YRTr2rjWdiV1GRjMs

- **Rule**: `java:S1130`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/algo/OptimisedBytesStoreHashTest.java:120`
- **Effort**: 5min
- **Created**: 2021-02-08T14:06:01+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove the declaration of thrown exception 'java.io.IOException', as it cannot be thrown from
  method's body.

## 373. AY-YRTwurjWdiV1GRjNo

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/ConcurrentRafAccessTest.java:102`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:58+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 374. AY-YRTwurjWdiV1GRjNr

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/ConcurrentRafAccessTest.java:112`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:58+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 375. AY-YRTwurjWdiV1GRjNu

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/ConcurrentRafAccessTest.java:123`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:58+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 376. AY-YRTwurjWdiV1GRjNx

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/ConcurrentRafAccessTest.java:133`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:58+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 377. AY-YRTwurjWdiV1GRjNz

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/ConcurrentRafAccessTest.java:149`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:58+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 378. AY-YRTwurjWdiV1GRjN2

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/ConcurrentRafAccessTest.java:182`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:58+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 379. AY-YRTwcrjWdiV1GRjNj

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/Issue85Test.java:111`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:58+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 380. AY-YRTwcrjWdiV1GRjNk

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/Issue85Test.java:118`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:58+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 381. AY-YRTsirjWdiV1GRjM2

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/MappedBytesTest.java:525`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:58+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 382. AY-YRTsirjWdiV1GRjM3

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/MappedBytesTest.java:530`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:58+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 383. AY-YRTsirjWdiV1GRjM4

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/MappedBytesTest.java:533`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:58+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 384. AY-YRTmWrjWdiV1GRjLl

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/NativeBytesStoreTest.java:114`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:58+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 385. AY-YRTmWrjWdiV1GRjLm

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/NativeBytesStoreTest.java:147`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:58+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 386. AY-YRTmWrjWdiV1GRjLn

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/NativeBytesStoreTest.java:149`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:58+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 387. AY-YRTmWrjWdiV1GRjLo

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/NativeBytesStoreTest.java:152`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:58+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 388. AY-YRTr2rjWdiV1GRjMr

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/algo/OptimisedBytesStoreHashTest.java:110`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:58+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 389. AY-YRTovrjWdiV1GRjMK

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/ref/TextIntArrayReferenceTest.java:151`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:58+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 390. AY-YRTpPrjWdiV1GRjMM

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/ref/TextLongArrayReferenceTest.java:44`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:58+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 391. AY-YRTs5rjWdiV1GRjM6

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/util/GzipTest.java:53`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:58+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 392. AY-YRTtOrjWdiV1GRjM9

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/util/LZWTest.java:50`
- **Effort**: 5min
- **Created**: 2020-12-10T11:37:58+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 393. AXzVm29NM87u1Qpdut1y

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BinaryWireCode.java:115`
- **Effort**: 2min
- **Created**: 2020-11-02T10:30:35+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 394. AXzVm29NM87u1Qpdut1z

- **Rule**: `java:S2386`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BinaryWireCode.java:179`
- **Effort**: 15min
- **Created**: 2020-11-02T10:30:35+0000
- **Assignee**: Unassigned
- **Message**:
  Move "STRING_FOR_CODE" to a class and lower its visibility

## 395. AXzVm2-zM87u1Qpdut2t

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/HexDumpBytes.java:1903`
- **Effort**: 0min
- **Created**: 2020-11-02T10:30:35+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 396. AYw0tGVvUtrwJAUvk2_R

- **Rule**: `java:S5786`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/UnsafeRWObjectTest.java:84`
- **Effort**: 2min
- **Created**: 2020-10-13T16:15:36+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this 'public' modifier.

## 397. AY-YRTpwrjWdiV1GRjMP

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesUtilTest.java:105`
- **Effort**: 0min
- **Created**: 2020-10-13T15:54:41+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 398. AYw0tGVvUtrwJAUvk2_Q

- **Rule**: `java:S5786`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/UnsafeRWObjectTest.java:59`
- **Effort**: 2min
- **Created**: 2020-10-13T15:54:41+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this 'public' modifier.

## 399. AXzVm23UM87u1Qpdutyc

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/ref/TextIntArrayReference.java:100`
- **Effort**: 0min
- **Created**: 2020-09-28T15:50:36+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 400. AY-YRTpZrjWdiV1GRjMN

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/ref/BinaryIntArrayReferenceTest.java:71`
- **Effort**: 5min
- **Created**: 2020-09-28T15:50:36+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 401. AY-YRTolrjWdiV1GRjMJ

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/ref/BinaryLongArrayReferenceTest.java:77`
- **Effort**: 5min
- **Created**: 2020-09-14T18:56:22+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 402. AZSoQmsTJXfuX-9srB74

- **Rule**: `java:S2093`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesMethodWriterBuilderTest.java:39`
- **Effort**: 15min
- **Created**: 2020-09-14T18:48:15+0000
- **Assignee**: Unassigned
- **Message**:
  Change this "try" to a try-with-resources.

## 403. AY-YRToCrjWdiV1GRjL_

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesMethodWriterBuilderTest.java:140`
- **Effort**: 5min
- **Created**: 2020-09-14T18:48:15+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 404. AY-YRTl5rjWdiV1GRjLa

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/ByteStoreTest.java:81`
- **Effort**: 5min
- **Created**: 2020-09-14T18:38:22+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "bytes" which hides the field declared at line 47.

## 405. AY-YRTl5rjWdiV1GRjLc

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/ByteStoreTest.java:141`
- **Effort**: 5min
- **Created**: 2020-09-14T18:38:22+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "bytes" which hides the field declared at line 47.

## 406. AY-YRTl5rjWdiV1GRjLe

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/ByteStoreTest.java:226`
- **Effort**: 5min
- **Created**: 2020-09-14T18:38:22+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "bytes" which hides the field declared at line 47.

## 407. AY-YRTl5rjWdiV1GRjLh

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/ByteStoreTest.java:421`
- **Effort**: 5min
- **Created**: 2020-09-14T18:38:22+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 408. AZSoQmnsJXfuX-9srB71

- **Rule**: `java:S2093`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/ByteStoreTest.java:434`
- **Effort**: 15min
- **Created**: 2020-09-14T18:38:22+0000
- **Assignee**: Unassigned
- **Message**:
  Change this "try" to a try-with-resources.

## 409. AY-YRTl5rjWdiV1GRjLi

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/ByteStoreTest.java:440`
- **Effort**: 5min
- **Created**: 2020-09-14T18:38:22+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "bytes" which hides the field declared at line 47.

## 410. AY-YRTl5rjWdiV1GRjLj

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/ByteStoreTest.java:573`
- **Effort**: 5min
- **Created**: 2020-09-14T18:38:22+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "bytes" which hides the field declared at line 47.

## 411. AY-YRTwCrjWdiV1GRjNZ

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesMarshallableTest.java:313`
- **Effort**: 5min
- **Created**: 2020-09-14T18:18:56+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 412. AY-YRTq8rjWdiV1GRjMc

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/readme/CASTest.java:53`
- **Effort**: 5min
- **Created**: 2020-09-14T17:57:30+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 413. AY-YRTq8rjWdiV1GRjMd

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/readme/CASTest.java:58`
- **Effort**: 5min
- **Created**: 2020-09-14T17:57:30+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 414. AY-YRTrPrjWdiV1GRjMh

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/readme/PrimitiveTest.java:135`
- **Effort**: 5min
- **Created**: 2020-09-14T17:51:51+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 415. AY-YRTrYrjWdiV1GRjMi

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/readme/StopBitTest.java:85`
- **Effort**: 5min
- **Created**: 2020-09-14T17:34:55+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 416. AY-YRTrFrjWdiV1GRjMe

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/readme/StringsTest.java:51`
- **Effort**: 5min
- **Created**: 2020-09-14T17:30:52+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 417. AY-YRTrFrjWdiV1GRjMf

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/readme/StringsTest.java:62`
- **Effort**: 5min
- **Created**: 2020-09-14T17:30:52+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 418. AY-YRTrFrjWdiV1GRjMg

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/readme/StringsTest.java:87`
- **Effort**: 5min
- **Created**: 2020-09-14T17:30:52+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 419. AZDllLqh5bUw8_NMnKSx

- **Rule**: `java:S108`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/MoreBytesTest.java:119`
- **Effort**: 5min
- **Created**: 2020-09-14T17:24:35+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this block of code, fill it in, or add a comment explaining why it is empty.

## 420. AXzVm2-eM87u1Qpdut2F

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesMarshaller.java:338`
- **Effort**: 30min
- **Created**: 2020-09-10T17:49:41+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 421. AXzVm260M87u1Qpdut07

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/VanillaBytes.java:636`
- **Effort**: 0min
- **Created**: 2020-07-13T08:48:29+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 422. AY-YRTqWrjWdiV1GRjMZ

- **Rule**: `java:S1172`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesTest.java:62`
- **Effort**: 5min
- **Created**: 2020-07-13T08:48:29+0000
- **Assignee**: JerryShea@github
- **Message**:
  Remove this unused method parameter "ignored".

## 423. AZDllLnu5bUw8_NMnKSt

- **Rule**: `java:S1488`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/jitter/MemoryMessager.java:72`
- **Effort**: 2min
- **Created**: 2020-07-03T14:53:35+0000
- **Assignee**: Unassigned
- **Message**:
  Immediately return this expression instead of assigning it to the temporary variable "length".

## 424. AXzVm2-8M87u1Qpdut2w

- **Rule**: `java:S2160`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/OnHeapBytes.java:32`
- **Effort**: 30min
- **Created**: 2020-06-17T18:04:55+0000
- **Assignee**: Unassigned
- **Message**:
  Override the "equals" method in this class.

## 425. AY-YRTuarjWdiV1GRjNL

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/MoreBytesTest.java:131`
- **Effort**: 0min
- **Created**: 2020-06-17T18:04:55+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 426. AZUakVlRQdJEqNZOtPKt

- **Rule**: `java:S5786`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesTestCommon.java:60`
- **Effort**: 2min
- **Created**: 2020-06-11T18:51:47+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this 'public' modifier.

## 427. AZUakVlRQdJEqNZOtPKw

- **Rule**: `java:S5786`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesTestCommon.java:40`
- **Effort**: 2min
- **Created**: 2020-06-01T06:48:37+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this 'public' modifier.

## 428. AZUakVdcQdJEqNZOtPKq

- **Rule**: `java:S5786`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesUtilTest.java:34`
- **Effort**: 2min
- **Created**: 2020-06-01T06:48:37+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this 'public' modifier.

## 429. AX1XRgmEe2eGZuiAJJNM

- **Rule**: `java:S5786`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/MappedFileTest.java:36`
- **Effort**: 2min
- **Created**: 2020-06-01T06:48:37+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this 'public' modifier.

## 430. AY-YRTwurjWdiV1GRjNm

- **Rule**: `java:S1854`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/ConcurrentRafAccessTest.java:97`
- **Effort**: 1min
- **Created**: 2020-05-27T13:35:08+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this useless assignment to local variable "summaryStatistics".

## 431. AY-YRTwurjWdiV1GRjNs

- **Rule**: `java:S1854`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/ConcurrentRafAccessTest.java:118`
- **Effort**: 1min
- **Created**: 2020-05-27T13:35:08+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this useless assignment to local variable "summaryStatistics".

## 432. AY-YRTwurjWdiV1GRjNv

- **Rule**: `java:S1854`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/ConcurrentRafAccessTest.java:128`
- **Effort**: 1min
- **Created**: 2020-05-27T13:35:08+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this useless assignment to local variable "summaryStatistics".

## 433. AY-YRTwurjWdiV1GRjNn

- **Rule**: `java:S1481`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/ConcurrentRafAccessTest.java:97`
- **Effort**: 5min
- **Created**: 2020-05-27T13:14:40+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "summaryStatistics" local variable.

## 434. AY-YRTwurjWdiV1GRjNp

- **Rule**: `java:S1854`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/ConcurrentRafAccessTest.java:107`
- **Effort**: 1min
- **Created**: 2020-05-27T13:14:40+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this useless assignment to local variable "summaryStatistics".

## 435. AY-YRTwurjWdiV1GRjNq

- **Rule**: `java:S1481`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/ConcurrentRafAccessTest.java:107`
- **Effort**: 5min
- **Created**: 2020-05-27T13:14:40+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "summaryStatistics" local variable.

## 436. AY-YRTwurjWdiV1GRjNt

- **Rule**: `java:S1481`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/ConcurrentRafAccessTest.java:118`
- **Effort**: 5min
- **Created**: 2020-05-27T13:14:40+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "summaryStatistics" local variable.

## 437. AY-YRTwurjWdiV1GRjNw

- **Rule**: `java:S1481`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/ConcurrentRafAccessTest.java:128`
- **Effort**: 5min
- **Created**: 2020-05-27T13:14:40+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "summaryStatistics" local variable.

## 438. AY-YRTwurjWdiV1GRjNy

- **Rule**: `java:S1172`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/ConcurrentRafAccessTest.java:137`
- **Effort**: 5min
- **Created**: 2020-05-27T13:14:40+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused method parameter "name".

## 439. AZDllLvd5bUw8_NMnKTE

- **Rule**: `java:S1488`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/ConcurrentRafAccessTest.java:148`
- **Effort**: 2min
- **Created**: 2020-05-27T13:14:40+0000
- **Assignee**: Unassigned
- **Message**:
  Immediately return this expression instead of assigning it to the temporary variable "elapsedNs".

## 440. AY-YRTwurjWdiV1GRjN0

- **Rule**: `java:S1854`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/ConcurrentRafAccessTest.java:181`
- **Effort**: 1min
- **Created**: 2020-05-27T13:14:40+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this useless assignment to local variable "elapsedNs".

## 441. AY-YRTwurjWdiV1GRjN1

- **Rule**: `java:S1481`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/ConcurrentRafAccessTest.java:181`
- **Effort**: 5min
- **Created**: 2020-05-27T13:14:40+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused "elapsedNs" local variable.

## 442. AXzVm27pM87u1Qpdut1O

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/AppendableUtil.java:169`
- **Effort**: 24min
- **Created**: 2020-04-07T11:12:20+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 34 to the 15 allowed.

## 443. AXzVm27pM87u1Qpdut1R

- **Rule**: `java:S135`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/AppendableUtil.java:173`
- **Effort**: 20min
- **Created**: 2020-04-07T11:12:20+0000
- **Assignee**: Unassigned
- **Message**:
  Reduce the total number of break and continue statements in this loop to use at most one.

## 444. AXzVm2-eM87u1Qpdut2E

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesMarshaller.java:297`
- **Effort**: 30min
- **Created**: 2019-11-22T17:55:52+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 445. AXzVm2-eM87u1Qpdut2O

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesMarshaller.java:557`
- **Effort**: 30min
- **Created**: 2019-11-19T11:23:06+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 446. AXzVm2-eM87u1Qpdut2P

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesMarshaller.java:564`
- **Effort**: 30min
- **Created**: 2019-11-19T11:23:06+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 447. AXzVm2-eM87u1Qpdut2T

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesMarshaller.java:652`
- **Effort**: 30min
- **Created**: 2019-11-19T11:23:06+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 448. AXzVm2-eM87u1Qpdut2U

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesMarshaller.java:659`
- **Effort**: 30min
- **Created**: 2019-11-19T11:23:06+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 449. AXzVm2-eM87u1Qpdut2W

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesMarshaller.java:708`
- **Effort**: 30min
- **Created**: 2019-11-19T11:23:06+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 450. AXzVm2-eM87u1Qpdut2X

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesMarshaller.java:715`
- **Effort**: 30min
- **Created**: 2019-11-19T11:23:06+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 451. AXzVm2-eM87u1Qpdut2Z

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesMarshaller.java:764`
- **Effort**: 30min
- **Created**: 2019-11-19T11:23:06+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 452. AXzVm2-eM87u1Qpdut2a

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesMarshaller.java:771`
- **Effort**: 30min
- **Created**: 2019-11-19T11:23:06+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 453. AXzVm2-eM87u1Qpdut2c

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesMarshaller.java:820`
- **Effort**: 30min
- **Created**: 2019-11-19T11:23:06+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 454. AXzVm2-eM87u1Qpdut2d

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesMarshaller.java:827`
- **Effort**: 30min
- **Created**: 2019-11-19T11:23:06+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 455. AY-YRTsVrjWdiV1GRjMy

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/ReadWriteMarshallableTest.java:31`
- **Effort**: 0min
- **Created**: 2019-04-24T11:33:34+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 456. AZDllLie5bUw8_NMnKSP

- **Rule**: `java:S117`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesUtilTest.java:82`
- **Effort**: 2min
- **Created**: 2019-03-21T08:26:57+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this local variable to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 457. AZDllLoJ5bUw8_NMnKSu

- **Rule**: `java:S117`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/ReadWriteMarshallableTest.java:35`
- **Effort**: 2min
- **Created**: 2019-03-21T08:26:57+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this local variable to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 458. AY-YRTvbrjWdiV1GRjNV

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/ByteStringAppenderTest.java:143`
- **Effort**: 5min
- **Created**: 2019-03-15T10:13:46+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 459. AXzVm27pM87u1Qpdut1Q

- **Rule**: `java:S3776`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/AppendableUtil.java:353`
- **Effort**: 6min
- **Created**: 2019-02-16T00:25:47+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor this method to reduce its Cognitive Complexity from 16 to the 15 allowed.

## 460. AY-YRTs5rjWdiV1GRjM7

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/util/GzipTest.java:63`
- **Effort**: 5min
- **Created**: 2019-02-11T22:24:09+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 461. AY-YRTs5rjWdiV1GRjM8

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/util/GzipTest.java:73`
- **Effort**: 5min
- **Created**: 2019-02-11T22:24:09+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 462. AY-YRTwcrjWdiV1GRjNi

- **Rule**: `java:S1172`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/Issue85Test.java:106`
- **Effort**: 5min
- **Created**: 2019-01-10T14:24:36+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused method parameter "i".

## 463. AZDllLuQ5bUw8_NMnKS2

- **Rule**: `java:S1488`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/Issue85Test.java:77`
- **Effort**: 2min
- **Created**: 2019-01-10T13:06:24+0000
- **Assignee**: Unassigned
- **Message**:
  Immediately return this expression instead of assigning it to the temporary variable "scalb".

## 464. AXzVm233M87u1Qpdutym

- **Rule**: `java:S1119`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/UncheckedBytes.java:319`
- **Effort**: 30min
- **Created**: 2018-10-23T11:25:21+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor the code to remove this label and the need for it.

## 465. AXzVm294M87u1Qpdut16

- **Rule**: `java:S2160`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/SubBytes.java:33`
- **Effort**: 30min
- **Created**: 2018-09-29T18:05:27+0000
- **Assignee**: Unassigned
- **Message**:
  Override the "equals" method in this class.

## 466. AXzVm24_M87u1QpdutzF

- **Rule**: `java:S100`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesMarshallable.java:64`
- **Effort**: 5min
- **Created**: 2018-07-16T09:07:18+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this method name to match the regular expression '^[a-z][a-zA-Z0-9]*$'.

## 467. AXzVm2-eM87u1Qpdut2Q

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesMarshaller.java:588`
- **Effort**: 30min
- **Created**: 2018-06-29T11:01:17+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 468. AXzVm2-eM87u1Qpdut2I

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesMarshaller.java:415`
- **Effort**: 30min
- **Created**: 2018-06-05T13:09:08+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 469. AXzVm2-eM87u1Qpdut2K

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesMarshaller.java:483`
- **Effort**: 30min
- **Created**: 2018-06-05T13:09:08+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 470. AY-YRTvbrjWdiV1GRjNR

- **Rule**: `java:S1172`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/ByteStringAppenderTest.java:38`
- **Effort**: 5min
- **Created**: 2018-05-15T21:10:00+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused method parameter "name".

## 471. AY-YRTvbrjWdiV1GRjNT

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/ByteStringAppenderTest.java:93`
- **Effort**: 5min
- **Created**: 2018-05-15T21:10:00+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 472. AXzVm260M87u1Qpdut01

- **Rule**: `java:S1210`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/VanillaBytes.java:703`
- **Effort**: 15min
- **Created**: 2018-04-25T20:43:01+0000
- **Assignee**: Unassigned
- **Message**:
  Override "equals(Object obj)" to comply with the contract of the "compareTo(T o)" method.

## 473. AXzVm2-eM87u1Qpdut2D

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesMarshaller.java:290`
- **Effort**: 30min
- **Created**: 2018-03-15T00:49:55+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 474. AX1XRgmEe2eGZuiAJJNL

- **Rule**: `java:S5826`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/MappedFileTest.java:255`
- **Effort**: 2min
- **Created**: 2017-11-20T17:59:52+0000
- **Assignee**: Unassigned
- **Message**:
  Annotate this method with JUnit5 '@org.junit.jupiter.api.AfterEach' instead of JUnit4 '@After'.

## 475. AY-YRTwCrjWdiV1GRjNY

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/BytesMarshallableTest.java:155`
- **Effort**: 5min
- **Created**: 2017-11-06T12:46:21+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 476. AXzVm27pM87u1Qpdut1S

- **Rule**: `java:S127`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/AppendableUtil.java:405`
- **Effort**: 10min
- **Created**: 2017-09-07T05:26:57+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor the code in order to not assign to this loop counter from within the loop body.

## 477. AXzVm27pM87u1Qpdut1T

- **Rule**: `java:S127`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/AppendableUtil.java:408`
- **Effort**: 10min
- **Created**: 2017-09-07T05:26:57+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor the code in order to not assign to this loop counter from within the loop body.

## 478. AXzVm27pM87u1Qpdut1U

- **Rule**: `java:S127`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/AppendableUtil.java:411`
- **Effort**: 10min
- **Created**: 2017-09-07T05:26:57+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor the code in order to not assign to this loop counter from within the loop body.

## 479. AXzVm27MM87u1Qpdut1F

- **Rule**: `java:S127`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/StreamingDataOutput.java:1059`
- **Effort**: 10min
- **Created**: 2017-09-07T05:26:57+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor the code in order to not assign to this loop counter from within the loop body.

## 480. AXzVm27MM87u1Qpdut1G

- **Rule**: `java:S127`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/StreamingDataOutput.java:1067`
- **Effort**: 10min
- **Created**: 2017-09-07T05:26:57+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor the code in order to not assign to this loop counter from within the loop body.

## 481. AXzVm27MM87u1Qpdut1H

- **Rule**: `java:S127`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/StreamingDataOutput.java:1073`
- **Effort**: 10min
- **Created**: 2017-09-07T05:26:57+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor the code in order to not assign to this loop counter from within the loop body.

## 482. AXzVm260M87u1Qpdut08

- **Rule**: `java:S127`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/VanillaBytes.java:177`
- **Effort**: 10min
- **Created**: 2017-09-07T05:26:57+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor the code in order to not assign to this loop counter from within the loop body.

## 483. AY-YRTl5rjWdiV1GRjLb

- **Rule**: `java:S1117`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/ByteStoreTest.java:104`
- **Effort**: 5min
- **Created**: 2016-12-28T18:21:57+0000
- **Assignee**: Unassigned
- **Message**:
  Rename "bytes" which hides the field declared at line 47.

## 484. AY-YRTvbrjWdiV1GRjNU

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/ByteStringAppenderTest.java:125`
- **Effort**: 5min
- **Created**: 2016-12-28T18:21:57+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 485. AY-YRTl5rjWdiV1GRjLd

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/ByteStoreTest.java:164`
- **Effort**: 5min
- **Created**: 2016-06-27T13:10:05+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 486. AXzVm220M87u1QpdutyT

- **Rule**: `java:S3626`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/ref/TextLongArrayReference.java:149`
- **Effort**: 1min
- **Created**: 2016-05-22T20:59:10+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this redundant jump.

## 487. AXzVm2-eM87u1Qpdut2B

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesMarshaller.java:229`
- **Effort**: 30min
- **Created**: 2016-04-20T12:27:03+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 488. AXzVm2-eM87u1Qpdut2M

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesMarshaller.java:513`
- **Effort**: 30min
- **Created**: 2016-04-20T12:27:03+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 489. AXzVm2-eM87u1Qpdut2N

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesMarshaller.java:531`
- **Effort**: 30min
- **Created**: 2016-04-20T12:27:03+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 490. AXzVm2-eM87u1Qpdut2R

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesMarshaller.java:606`
- **Effort**: 30min
- **Created**: 2016-04-20T12:27:03+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 491. AXzVm2-eM87u1Qpdut2S

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesMarshaller.java:624`
- **Effort**: 30min
- **Created**: 2016-04-20T12:27:03+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 492. AXzVm2-eM87u1Qpdut2V

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesMarshaller.java:680`
- **Effort**: 30min
- **Created**: 2016-04-20T12:27:03+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 493. AXzVm2-eM87u1Qpdut2Y

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesMarshaller.java:736`
- **Effort**: 30min
- **Created**: 2016-04-20T12:27:03+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 494. AXzVm2-eM87u1Qpdut2b

- **Rule**: `java:S3011`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesMarshaller.java:792`
- **Effort**: 30min
- **Created**: 2016-04-20T12:27:03+0000
- **Assignee**: Unassigned
- **Message**:
  This accessibility bypass should be removed.

## 495. AY-YRTpmrjWdiV1GRjMO

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/MappedMemoryTest.java:91`
- **Effort**: 5min
- **Created**: 2016-03-09T12:00:25+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 496. AX_QA0lwvXDaEYQSpO0v

- **Rule**: `java:S1168`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/util/Compression.java:134`
- **Effort**: 30min
- **Created**: 2016-02-12T11:07:52+0000
- **Assignee**: Unassigned
- **Message**:
  Return an empty array instead of null.

## 497. AXzVm220M87u1QpdutyS

- **Rule**: `java:S1135`
- **Severity**: INFO
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/ref/TextLongArrayReference.java:105`
- **Effort**: 0min
- **Created**: 2016-01-20T11:47:54+0000
- **Assignee**: Unassigned
- **Message**:
  Complete the task associated to this TODO comment.

## 498. AX_QA0lwvXDaEYQSpO0w

- **Rule**: `java:S1168`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/util/Compression.java:136`
- **Effort**: 30min
- **Created**: 2015-12-10T14:10:06+0000
- **Assignee**: Unassigned
- **Message**:
  Return an empty array instead of null.

## 499. AXzVm22WM87u1QpdutyK

- **Rule**: `java:S115`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/util/Compressions.java:45`
- **Effort**: 2min
- **Created**: 2015-12-10T14:10:06+0000
- **Assignee**: Unassigned
- **Message**:
  Rename this constant name to match the regular expression '^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$'.

## 500. AY-YRTtOrjWdiV1GRjM-

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/util/LZWTest.java:60`
- **Effort**: 5min
- **Created**: 2015-12-10T14:10:06+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 501. AY-YRTtOrjWdiV1GRjM_

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/util/LZWTest.java:70`
- **Effort**: 5min
- **Created**: 2015-12-10T14:10:06+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 502. AXzVm2x2M87u1Qpdutx0

- **Rule**: `java:S5778`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/ByteStoreTest.java:611`
- **Effort**: 5min
- **Created**: 2015-11-06T12:21:54+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor the body of this try/catch to have only one invocation possibly throwing a runtime
  exception.

## 503. AXzVm24CM87u1Qpdutys

- **Rule**: `java:S1452`
- **Severity**: CRITICAL
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/BytesStore.java:120`
- **Effort**: 20min
- **Created**: 2015-10-10T04:47:01+0000
- **Assignee**: Unassigned
- **Message**:
  Remove usage of generic wildcard type.

## 504. AXzVm28aM87u1Qpdut1e

- **Rule**: `java:S2160`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/MappedBytesStore.java:46`
- **Effort**: 30min
- **Created**: 2015-09-21T18:39:46+0000
- **Assignee**: Unassigned
- **Message**:
  Override the "equals" method in this class.

## 505. AYg7_mkctyf7ubiXbZ6L

- **Rule**: `java:S1481`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/algo/OptimisedBytesStoreHash.java:134`
- **Effort**: 5min
- **Created**: 2015-08-21T20:23:46+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove this unused "l2" local variable.

## 506. AYg7_mkctyf7ubiXbZ6M

- **Rule**: `java:S1481`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/algo/OptimisedBytesStoreHash.java:286`
- **Effort**: 5min
- **Created**: 2015-08-21T20:23:46+0000
- **Assignee**: peter-lawrey@github
- **Message**:
  Remove this unused "l2" local variable.

## 507. AXzVm27MM87u1Qpdut1C

- **Rule**: `java:S1119`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/StreamingDataOutput.java:959`
- **Effort**: 30min
- **Created**: 2015-08-20T13:07:39+0000
- **Assignee**: Unassigned
- **Message**:
  Refactor the code to remove this label and the need for it.

## 508. AZMw7LYnGAwb07yl4Tlw

- **Rule**: `java:S1128`
- **Severity**: MINOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/main/java/net/openhft/chronicle/bytes/UncheckedNativeBytes.java:25`
- **Effort**: 1min
- **Created**: 2015-08-11T11:55:29+0000
- **Assignee**: Unassigned
- **Message**:
  Remove this unused import 'net.openhft.chronicle.core.OS'.

## 509. AY-YRToarjWdiV1GRjMI

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/PrintVdsoMain.java:37`
- **Effort**: 5min
- **Created**: 2015-07-12T23:20:07+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 510. AY-YRTr2rjWdiV1GRjMq

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/algo/OptimisedBytesStoreHashTest.java:59`
- **Effort**: 5min
- **Created**: 2015-06-28T16:13:49+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 511. AY-YRTr2rjWdiV1GRjMt

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/algo/OptimisedBytesStoreHashTest.java:123`
- **Effort**: 5min
- **Created**: 2015-06-28T16:13:49+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.

## 512. AY-YRTr2rjWdiV1GRjMu

- **Rule**: `java:S125`
- **Severity**: MAJOR
- **Type**: CODE_SMELL
- **Location**: `OpenHFT_Chronicle-Bytes:src/test/java/net/openhft/chronicle/bytes/algo/OptimisedBytesStoreHashTest.java:138`
- **Effort**: 5min
- **Created**: 2015-06-28T16:13:49+0000
- **Assignee**: Unassigned
- **Message**:
  This block of commented-out lines of code should be removed.
