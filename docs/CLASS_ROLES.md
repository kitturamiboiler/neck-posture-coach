# Android 클래스별 역할

작성일: 2026-10-03 · 대상: `android/vision-quickstart/app`의 현재 구현

한눈에 보는 구조: [아키텍처 이미지](ARCHITECTURE.png) · [확대 가능한 SVG](ARCHITECTURE.svg)

이 문서는 **목 자세 코치에서 직접 사용하는 클래스**를 중심으로 정리합니다. 기존 ML Kit Quickstart의 다른 검출 예제 전체는 제품 코드가 아니므로 나열하지 않습니다. 새 앱의 사진 측정은 **귀-사전 확인한 C7의 CVA**이고, 예전 실시간 실험은 **귀-어깨각**입니다. 두 각도와 판정 기준을 섞지 않습니다.

## 앱이 동작하는 흐름

```text
PostureCoachActivity (목표·오늘·운동·기록)
  ├─ PostureStore (목표 설정, 실천일, 측정값 저장)
  └─ PostureCaptureActivity (카메라, 3장 촬영, 기준점 확인)
       ├─ PostureSession (촬영 중 사진·점·화면 상태만 메모리에 보관)
       ├─ PosturePhotoView (사진 위 기준점과 선 표시, 터치 좌표 변환)
       ├─ ML Kit Face Detection (귀 후보 제안)
       ├─ CvaMath (각도·중앙값·반복성 계산)
       └─ PostureStore (확정한 측정값·촬영 조건 저장)
```

## 제품용 클래스

제품 클래스의 단일 원본은 `android/vision-quickstart/app/src/main/java/com/google/mlkit/vision/demo/java/neckposture/`입니다. 출시 모듈은 필요한 클래스만 공유합니다.

| 클래스 | 하는 일 | 알아둘 경계 |
| --- | --- | --- |
| [`PostureCoachActivity`](../android/vision-quickstart/app/src/main/java/com/google/mlkit/vision/demo/java/neckposture/PostureCoachActivity.java) | 앱 시작 화면. 목표 설정, 오늘의 상태, 운동 안내·타이머·완료 확인, 측정 기록, 설정을 화면으로 연결합니다. | 이미지를 분석하지 않습니다. 운동 완료는 사용자가 확인한 뒤 `PostureStore`에 기록합니다. |
| [`PostureCaptureActivity`](../android/vision-quickstart/app/src/main/java/com/google/mlkit/vision/demo/java/neckposture/PostureCaptureActivity.java) | 카메라 권한과 촬영 조건을 확인하고 사진 3장을 찍습니다. ML Kit의 귀 후보를 보여주며 사용자가 귀·C7을 확인하게 한 뒤 각도를 계산·저장합니다. | C7을 자동 검출하지 않습니다. 촬영 품질 조건과 세 사진 간 5도 차이 제한은 임시 기준이며 진단 기준이 아닙니다. |
| [`PosturePhotoView`](../android/vision-quickstart/app/src/main/java/com/google/mlkit/vision/demo/java/neckposture/PosturePhotoView.java) | 촬영 사진 위에 귀·C7 점, 선, 각도를 그립니다. 화면을 누른 위치를 원본 사진 좌표로 변환합니다. | 화면 표시와 점 지정만 맡습니다. 사진 분석·영구 저장은 하지 않습니다. |
| [`PostureSession`](../android/vision-quickstart/app/src/main/java/com/google/mlkit/vision/demo/java/neckposture/PostureSession.java) | `ViewModel`로 사진, 세 장의 기준점·각도·품질 상태와 현재 화면/타이머 상태를 메모리에 둡니다. 화면 회전 동안 상태를 유지합니다. | 사진은 DB에 저장하지 않습니다. 프로세스가 종료되면 미완료 촬영은 복원되지 않습니다. |
| [`PostureStore`](../android/vision-quickstart/app/src/main/java/com/google/mlkit/vision/demo/java/neckposture/PostureStore.java) | 목표 설정은 `SharedPreferences`, 실천일·측정값은 SQLite에 저장합니다. 하루 중복 실천을 막고 기록 조회·삭제를 제공합니다. | 사진 파일은 저장하지 않습니다. 측정 행에는 각 사진의 각도 3개, 중앙값, 촬영 조건이 들어갑니다. 내부 `Reading`은 조회 결과를 담습니다. |
| [`CvaMath`](../android/vision-quickstart/app/src/main/java/com/google/mlkit/vision/demo/java/neckposture/CvaMath.java) | 두 점의 수평선 기준 각도, 세 값의 중앙값, 값의 유효성·반복성을 계산합니다. | 점을 검출하지 않는 순수 계산 코드입니다. 5도 이내 반복성 검사는 촬영 일관성 확인용이지 정상/비정상 판정이 아닙니다. |
| [`CoachUi`](../android/vision-quickstart/app/src/main/java/com/google/mlkit/vision/demo/java/neckposture/CoachUi.java) | 제품 화면에서 쓰는 페이지, 글자, 버튼, 선택지, 구분선의 공통 생성을 맡습니다. | UI 모양을 돕는 클래스이며 측정·저장 로직은 없습니다. |

## 남아 있는 실시간 귀-어깨각 실험

다음 3개도 같은 `neckposture` 폴더에 있지만, **현재 사진 점검의 측정 결과를 만드는 경로는 아닙니다.** 설정에서 여는 예전 ML Kit 실험 화면이 사용합니다.

| 클래스 | 하는 일 | 제품과의 관계 |
| --- | --- | --- |
| [`NeckPostureProcessor`](../android/vision-quickstart/app/src/main/java/com/google/mlkit/vision/demo/java/neckposture/NeckPostureProcessor.java) | 한 실시간 프레임에서 Face Detection의 귀 후보와 Pose Detection의 어깨 후보를 결합·검사합니다. `DetectionResult`는 검출 결과, `Measurement`는 선택된 점·각도를 담습니다. | 출시 앱에서 제외했습니다. 사진 흐름은 별도 FaceLandmarkMapping을 사용하며 실시간 귀-어깨각을 사진 CVA로 저장하지 않습니다. |
| [`NeckPostureGraphic`](../android/vision-quickstart/app/src/main/java/com/google/mlkit/vision/demo/java/neckposture/NeckPostureGraphic.java) | 실시간 영상 위에 후보 점, 귀-어깨 연결선, 기준선, 각도와 상태 문구를 그립니다. | 사진 점검의 선·점은 `PosturePhotoView`가 따로 그립니다. |
| [`NeckPostureAngle`](../android/vision-quickstart/app/src/main/java/com/google/mlkit/vision/demo/java/neckposture/NeckPostureAngle.java) | 실시간 점의 이미지 내부 여부와 귀-어깨각 계산·50도 참고선 처리를 돕습니다. | 사진 흐름은 이미지 경계 확인 함수를 재사용합니다. 50도 실험 참고값을 사진 결과의 진단 경계로 쓰지 않습니다. |

## 기존 ML Kit 샘플과 연결되는 클래스

샘플의 검출기 예제 전체를 제품 클래스와 혼동하지 않도록, 실제 연결부만 적었습니다.

| 클래스 | 현재 역할 |
| --- | --- |
| [`EntryChoiceActivity`](../android/vision-quickstart/app/src/main/java/com/google/mlkit/vision/demo/EntryChoiceActivity.kt) | 기존 개발 모듈의 Java/Kotlin 샘플 선택 화면. 목log 출시 앱의 시작·설정 흐름에서는 제외했습니다. |
| [`ChooserActivity`](../android/vision-quickstart/app/src/main/java/com/google/mlkit/vision/demo/java/ChooserActivity.java) | Java 샘플에서 실행할 검출기와 카메라 예제를 고르는 화면입니다. |
| [`LivePreviewActivity`](../android/vision-quickstart/app/src/main/java/com/google/mlkit/vision/demo/java/LivePreviewActivity.java) | 실시간 샘플 카메라 화면. 'Neck Posture Measurement' 선택 시 `NeckPostureProcessor`를 사용합니다. |
| [`CameraSource`](../android/vision-quickstart/app/src/main/java/com/google/mlkit/vision/demo/CameraSource.java) · [`CameraSourcePreview`](../android/vision-quickstart/app/src/main/java/com/google/mlkit/vision/demo/CameraSourcePreview.java) | 기존 실시간 프리뷰의 카메라 프레임 공급·화면 배치입니다. 제품의 3장 촬영은 CameraX를 사용합니다. |
| [`GraphicOverlay`](../android/vision-quickstart/app/src/main/java/com/google/mlkit/vision/demo/GraphicOverlay.java) | 기존 실시간 검출 결과를 카메라 화면에 겹쳐 그리는 바탕입니다. |
| [`VisionProcessorBase`](../android/vision-quickstart/app/src/main/java/com/google/mlkit/vision/demo/java/VisionProcessorBase.java) | 기존 Java 검출기들이 프레임을 처리할 때 공유하는 기반 클래스입니다. |
| [`CameraXLivePreviewActivity`](../android/vision-quickstart/app/src/main/java/com/google/mlkit/vision/demo/java/CameraXLivePreviewActivity.java) | 별도의 CameraX 실시간 **샘플** 화면입니다. 제품의 사진 점검 화면과는 다릅니다. |

## 검사 클래스

| 클래스 | 확인하는 것 |
| --- | --- |
| [`CvaMathCheck`](../android/vision-quickstart/app/src/test/java/com/google/mlkit/vision/demo/java/neckposture/CvaMathCheck.java) | Android 화면 없이 각도·중앙값·유효성 계산을 확인합니다. |
| [`PostureFlowTest`](../android/vision-quickstart/app/src/androidTest/java/com/google/mlkit/vision/demo/java/neckposture/PostureFlowTest.java) | 제품 흐름의 저장소 격리, 측정 저장, 사진 터치 좌표 등을 기기에서 확인합니다. |
| [`NeckPostureAngleTest`](../android/vision-quickstart/app/src/androidTest/java/com/google/mlkit/vision/demo/java/neckposture/NeckPostureAngleTest.java) | 실시간 각도와 좌우 선택 등 계산·좌표 조건을 확인합니다. |
| [`EarReplayTest`](../android/vision-quickstart/app/src/androidTest/java/com/google/mlkit/vision/demo/java/neckposture/EarReplayTest.java) | 원본/반전 옆모습 자료로 귀 선택을 재현합니다. 필요한 외부 사진 자료가 없으면 해당 재현은 수행되지 않습니다. |
| [`ShoulderAdjustmentUiTest`](../android/vision-quickstart/app/src/androidTest/java/com/google/mlkit/vision/demo/java/ShoulderAdjustmentUiTest.java) | 실시간 실험 화면의 어깨점 수동 보정과 화면 전환 시 초기화를 확인합니다. |
| [`CameraFrameOwnershipTest`](../android/vision-quickstart/app/src/androidTest/java/com/google/mlkit/vision/demo/CameraFrameOwnershipTest.java) | 카메라가 다음 프레임 버퍼를 재사용해도 분석 입력이 바뀌지 않는지 확인합니다. |

## 수정할 때 찾는 곳

| 바꾸려는 내용 | 먼저 볼 클래스 |
| --- | --- |
| 목표·오늘·운동·기록 화면 | `PostureCoachActivity`, `CoachUi` |
| 촬영 순서·권한·귀 후보·C7 확인 | `PostureCaptureActivity`, `PostureSession` |
| 사진 위 점·선·터치 위치 | `PosturePhotoView` |
| 각도·세 사진 대표값 | `CvaMath` |
| 자동 촬영 연속 유지·프레임 신선도 조건 | `CaptureReadiness` |
| 제품 얼굴 방향·귀 후보 대응 (실험 SDK와 분리) | `FaceLandmarkMapping` |
| 일반 운동의 이름·단계·안전 문구 | `ExerciseLibrary` |
| 자체 운동 그림과 순서 재생 | `ExerciseArtworkView` |
| 평균·측정 범위에 따른 비진단 참고 안내 | `AngleInterpretation` |
| 오프라인 개인정보·이용·출처·건강 한계 안내 | `PrivacyActivity` |
| 카메라 영상 영역의 구도 가이드 | `CaptureGuideView` |
| 목표·실천 약속·실천 시간/메모·측정 기록 | `PostureStore` (DB v3, 내부 Practice/Reading) |
| 주간 날짜·완료일 계산 | `HabitProgress` |
| 예전 실시간 귀·어깨 후보 | `LivePreviewActivity`, `NeckPostureProcessor`, `NeckPostureGraphic` |

현재 구현·검증 범위와 남은 실기기 확인 사항은 [APP_STATUS.md](../android/vision-quickstart/APP_STATUS.md), 프로젝트 규칙은 [AGENTS.md](../AGENTS.md)를 참고합니다.

2026-10-28: 목log 출시 모듈은 제품 Java 패키지를 공유한다. PostureCoachActivity의 운동 일지와 사진 기록 구성을 분리하고, CoachUi에 공통 입력·주간 달력을 추가했다. HabitProgress는 순수 날짜/집계 계산이며 PostureStore DB v3은 메모만 추가하고 기존 각도·실천 기록을 보존한다. 자세한 화면별 변경은 [습관·일지 보완](HABIT_REFINEMENT_2026-10-28.md)을 참고한다.

2026-10-23 추가: 제품 화면의 대표값은 `CvaMath.mean`으로 계산한다. `PostureStore`는 DB v2에 평균을 저장하고 기존 중앙값과 사진별 값은 보존한다. `PostureCaptureActivity`는 별도 경량 FaceDetector로 구도를 확인하고 `CaptureReadiness`의 연속 유지 조건으로 자동 촬영을 시작한다. 구도 확인은 C7 자동 검출이 아니다. 알림은 기기 소리·햅틱 설정을 따른다.
