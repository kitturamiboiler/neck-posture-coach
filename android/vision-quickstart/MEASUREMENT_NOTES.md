# 자세 측정 실험 노트

## 현재 방향

이 프로젝트는 실시간 운동 코칭보다 첫 측정, 운동 수행, 재측정, 변화 확인 흐름에 맞춘다.

현재는 새 목 검출 모델을 만들지 않고, ML Kit가 이미 제공하는 기준점을 먼저 사용한다.

## 현재 검출 결과

- Face Detection은 귀 위치를 비교적 잘 잡는다.
- Pose Detection은 어깨 위치를 잡는다.
- Face Detection의 귀 좌표와 Pose Detection의 귀 좌표는 서로 다를 수 있다. 두 모델이 같은 이름의 랜드마크를 쓰더라도 실제로 추정하는 기준점이 같다고 보장할 수 없다.
- Face Detection의 얼굴 박스는 귀나 목 후보 영역을 완전히 포함하지 않을 수 있다. 박스는 측정 영역이 아니라 ML Kit가 판단한 얼굴 영역이다.
- 전면 카메라에서는 화면 기준 좌우와 ML Kit의 사람 기준 좌우가 다르게 보일 수 있다.

## 2026-09-14 실험 결과

| 항목 | 결과 | 판단 |
| --- | --- | --- |
| Face Detection 귀 | 실제 귀구멍 근처를 비교적 잘 잡음 | 측정 기준점으로 사용 |
| Pose Detection 귀 | 볼/광대 쪽으로 앞에 찍히는 경우가 있음 | 측정에서는 제외 |
| Pose Detection 어깨 | 어깨 기준점을 잘 잡음 | 측정 기준점으로 사용 |
| Face Mesh | 얼굴 기준점 보강은 가능하지만 목/어깨는 직접 제공하지 않음 | 지금은 보류 |
| 커스텀 Neck Detection | 데이터셋/학습/검증 부담이 큼 | 지금은 하지 않음 |

결론:

```text
Face Detection 귀 + Pose Detection 어깨 = 자세 측정 기준
```

현재 Pose Detection만으로 계산된 예시:

```text
Pose ear + Pose shoulder = neck 48 deg
```

이 값은 Pose 귀가 실제 귀구멍보다 앞쪽으로 찍힌 상태라 최종 측정값으로 쓰지 않는다.

## 임시 디버그 표시

- Face Detection 화면
  - 귀와 코 기준점을 흰 점으로 표시한다.
  - 얼굴 박스보다 넓은 흰 후보 박스를 표시한다.
  - 귀 기준점에서 아래로 기준선을 표시한다.

- Pose Detection 화면
  - 귀와 어깨 기준점을 흰 점으로 표시한다.
  - 같은 쪽 귀와 어깨를 흰 선으로 연결한다.
  - 귀-어깨 선의 각도를 표시한다.

## 각도 계산

귀와 어깨 좌표를 사용한다.

```text
dx = abs(earX - shoulderX)
dy = shoulderY - earY
angle = atan2(dy, dx)
```

`angle`은 어깨에서 귀로 향하는 선과 수평선 사이의 각도다. 90도에 가까울수록 귀가 어깨 위에 가깝다.

현재 측정 프로토타입은 ML Kit 입력 이미지 좌표에서 계산하고 도 단위로 표시한다. 화면 표시에는 `GraphicOverlay`의 확대·이동·반전 변환을 적용한다.

```java
double angle =
    Math.toDegrees(Math.atan2(shoulderY - earY, Math.abs(earX - shoulderX)));
```

현재 `PoseGraphic`의 각도 표시는 수직선 기준 편차를 확인하는 임시 디버그 값이다. 최종 측정 로직에는 그대로 사용하지 않는다.

## 다음 확인

내일 작업 순서와 검증 항목은 아래 인수인계에 기록했다. 같은 프레임의 Face/Pose 결합 코드는 구현되어 있으므로 새로 만들지 않는다.

## 다음 개발 범위

| 순서 | 작업 | 완료 기준 |
| --- | --- | --- |
| 1 | 측정 전용 Processor 만들기 | 구현 완료, 실기기 확인 대기 |
| 2 | Face 귀 + Pose 어깨 표시 | 구현 완료, 실기기 확인 대기 |
| 3 | 귀-어깨 선 표시 | 구현 완료, 실기기 확인 대기 |
| 4 | 각도 계산 표시 | 구현 완료, 실기기 확인 대기 |
| 5 | 측정값 임시 저장 | 첫 측정과 재측정을 비교할 수 있음 |

## 2026-09-21 구현

- `LivePreviewActivity`의 기본 검출기로 `Neck Posture Measurement`를 추가했다.
- 같은 카메라 프레임을 Face Detection과 Pose Detection에 전달한다.
- 옆모습에서 보이는 Face 귀와 같은 쪽 Pose 어깨를 연결한다.
- 어깨 신뢰도 0.70 이상일 때 수평선 기준 귀-어깨각을 화면과 Logcat에 표시한다.
- 정면처럼 양쪽 귀가 모두 잡히거나 귀/어깨가 없으면 각도 대신 위치 안내를 표시한다.
- Debug APK와 Android 계측 테스트 코드 컴파일을 확인했다.
- 최초 빌드 당시에는 ADB 연결 기기가 없었다. 이후 태블릿 설치까지 진행했으며 결과는 아래에 기록했다.

## 2026-09-21 실기기 재확인

- 최초 실행 입력 크기가 `288x352`로 표시됐고, Face Detection 결과가 없어 선과 각도가 그려지지 않았다.
- 기본 크기 선택이 목표값 `480x360`보다 작은 `352x288`을 고르는 문제를 확인했다.
- 지원 크기 중 목표값 이상을 우선하도록 수정했다. 확인한 기기 지원 목록상 예상 선택값은 `640x480`이며, 수정 후 실제 입력 크기는 다시 확인해야 한다.
- 측정용 Face Detection은 윤곽선 검출을 끄고 `ACCURATE + LANDMARK_MODE_ALL`로 변경했다.
- 귀와 어깨가 모두 유효할 때만 그리던 방식을 바꿔, 검출된 귀 후보와 어깨 후보를 독립적으로 표시하도록 수정했다. 이후 안정화 버전에서는 유효한 측정에 안정화된 점을, 실패 상태에 원시 후보점을 표시한다.
- 수정 APK 빌드와 태블릿 설치는 성공했다.

## 2026-09-21 마감 및 내일 인수인계

현재 단계는 실서비스를 위한 측정 프로토타입이다. 검출 정확도와 흔들림 개선 효과는 아직 검증되지 않았다.

### 오늘 구현한 것

| 파일 | 역할 |
| --- | --- |
| `app/src/main/java/com/google/mlkit/vision/demo/CameraSource.java` | 자동 프리뷰 크기 선택 시 목표 해상도 이상을 우선 |
| `app/src/main/java/com/google/mlkit/vision/demo/java/LivePreviewActivity.java` | `Neck Posture Measurement` 메뉴와 기본 선택 연결 |
| `app/src/main/java/com/google/mlkit/vision/demo/java/neckposture/NeckPostureProcessor.java` | 같은 InputImage로 Face/Pose 실행, 귀·어깨 결합, 유효성 검사, 좌표 안정화 및 로그 |
| `app/src/main/java/com/google/mlkit/vision/demo/java/neckposture/NeckPostureGraphic.java` | 귀·어깨 점, 연결선, 수평 기준선, 각도 및 실패 안내 표시 |
| `app/src/main/java/com/google/mlkit/vision/demo/java/neckposture/NeckPostureAngle.java` | 수평선 기준 각도 계산 및 잘못된 좌표 거부 |
| `app/src/androidTest/java/com/google/mlkit/vision/demo/java/neckposture/NeckPostureAngleTest.java` | 90도·45도 및 귀가 어깨보다 아래인 경우 테스트 |

- 기존 Face/Pose 화면의 결과를 따로 가져오지 않고 동일한 ML Kit API를 같은 프레임에서 호출한다.
- Face는 `ACCURATE`, 랜드마크 사용, 윤곽선·표정 분류 미사용이다. Pose는 기존 샘플 설정을 따른다.
- 어깨 신뢰도 기준은 임시 `0.70`이다. Face 귀와 같은 이름의 좌우 Pose 어깨를 연결하며 실제 대응은 검증해야 한다.
- 좌표에 지수 이동평균(EMA)을 적용했다: `이전값 + 0.20 * (현재값 - 이전값)`.
- 표시 각도와 현재 로그 모두 안정화된 좌표로 계산한다. 안정화가 측정 정확도를 보장하지는 않는다.

### 확인 결과

| 항목 | 상태 |
| --- | --- |
| JDK 17 Debug APK 빌드 | 성공 |
| Android 계측 테스트 코드 컴파일 | 성공 |
| 최신 안정화 APK 설치 | 갤럭시 탭 SM-T735N에 성공 |
| `connectedDebugAndroidTest` 실행 | 결과 없이 장시간 대기하여 중단, 통과로 기록하지 않음 |
| 실제 측정 화면 | 사용자에게 점·각도 흔들림 보고를 받음 |
| 첨부 영상 분석 | 로컬 디코딩·재생을 완료하지 못함, 영상 기반 원인 및 변동폭 확인 안 됨 |
| EMA 적용 후 흔들림 개선 | 사용자 재확인 및 정량 검증 대기 |

### 내일 먼저 할 것

1. 태블릿 USB 디버깅 연결 후 `Java → LivePreviewActivity → Neck Posture Measurement`를 연다. 실제 입력 해상도와 전면 카메라 상태를 확인한다.
2. 기존 Face/Pose 화면과 통합 화면을 같은 촬영 조건에서 비교한다. 귀 위치, 같은 쪽 어깨 연결, 회전·좌우 반전 좌표가 맞는지 먼저 확인한다.
3. 몸을 고정했을 때 원시 좌표와 안정화 좌표를 함께 비교할 수 있도록 로그를 보완한다. 검출 누락, 측정 쪽 변경, 실제 이동을 구분한 뒤 필터 계수를 조정한다.
4. 현재 EMA는 검출 실패 시 이전 상태를 초기화하지 않는다. 재검출·인물 변경·카메라 전환 후 오래된 좌표가 섞이는 경우를 확인하고 초기화 조건을 보완한다. 실패 상태에서 원시 후보점으로 바뀌며 생기는 시각적 튐도 확인한다.
5. 양쪽 귀 검출만으로 정면을 판단하는 임시 규칙, 어깨 화면 이탈, 여러 사람 검출, 처리 실패 후 프레임 재개를 점검한다. 머리만 카메라로 돌리면 측정 조건도 달라지므로 검출 성공과 측정 적합성을 구분한다.
6. 계측 테스트 대기 원인을 확인하고 계산·안정화 동작을 검증한다. 좌표 대응과 반복성이 확인되면 촬영 품질 검사 → 3회 측정 중앙값 → 결과 → 로컬 기록 → 운동 안내 순서로 진행한다.

### Git 및 재개 위치

- 브랜치: `feat/pose-landmark-log`.
- 마지막 커밋 및 원격 반영: `bde925d docs: align agent rules with measurement plan`.
- 위 측정 기능·해상도 수정·EMA·계측 테스트·이 노트는 아직 커밋하지 않은 로컬 변경이다. `neckposture/`와 `androidTest/`는 새 파일이므로 누락하지 않는다.
- APK: `app/build/outputs/apk/debug/vision-quickstart-debug.apk`.
- 오늘은 문서 정리까지만 진행한다. 다음 작업 시작 전 `git status`로 변경을 확인하고 보존한다.

## 아직 하지 않는 것

- 커스텀 Neck Detection 모델 개발
- Face Mesh 도입
- 운동 코칭 로직 세분화
- 측정값 저장/그래프 화면
