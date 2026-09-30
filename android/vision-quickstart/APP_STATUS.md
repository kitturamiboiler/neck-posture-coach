# 목 자세 코치 개발 현황

기준일: 2026-09-30. WSL `neck-posture-coach` 작업본.

## 오늘 마감과 다음 작업

사용자 결정: 오늘은 구현·갤탭 검증 결과를 문서화하고 현재 기능 브랜치에 커밋·푸시하는 것으로 마감한다. S20 Ultra 검증과 디자인 개선은 다음에 진행한다.

- 앱 표시 이름은 `목 자세 코치`다. PC 화면을 미러링하는 프로그램이 아니라 Android에 설치되는 앱이며, 설치 후 PC·USB 연결 없이 실행된다. USB 분리 상태의 재실행 검증은 다음 기기 테스트에 포함한다.
- 현재는 개발용 Debug 앱이다. 스토어 등록, 릴리스 서명, 공개 배포는 아직 진행하지 않았다.
- 앱 UI 시안은 사용자가 추후 제공한다. 그 전에는 임의의 최종 디자인을 확정하지 않는다.
- 저장소는 `https://github.com/kitturamiboiler/neck-posture-coach.git`, 작업 브랜치는 `feat/pose-landmark-log`다. 오늘은 `dev` 병합을 하지 않는다.
- 원본 사진, 기기 스크린샷, APK, 서명키, 로컬 SDK 설정은 커밋 대상이 아니다. Windows 이전 작업본과 OlThing은 별개로 유지한다.

| 다음 순서 | 내용 | 완료 기준 |
| --- | --- | --- |
| 1. S20 Ultra | 모바일 화면 폭·큰 글꼴·스크롤·터치 기준점 조정, 전후면·좌우 촬영, 권한 거부·재허용, 회전·백그라운드 복귀, USB 없이 재실행 | 기기별 확인표와 실패 사례 기록. 오늘 갤탭 결과를 스마트폰 검증으로 대체하지 않음 |
| 2. 시안 반영 | 사용자가 제공하는 모바일 시안 기준으로 화면 구성·문구·운동 안내 개선 | 시안과 구현 비교, 작은 화면에서 겹침·잘림·조작 불편 재검증 |
| 3. 측정·습관 검증 | 귀·C7 기준점 대조, 다른 날·조건에서 반복 측정, 루틴 타이머와 실제 완료 확인·날짜별 기록 | 실제 정확도와 단순 저장·계산 성공을 구분하고 한계 문서화 |
| 4. 출시 준비 | 앱 식별자·버전·아이콘, 샘플 기능·불필요한 권한·의존성 정리, 릴리스 서명·AAB·테스트 배포, 개인정보 안내·스토어 자료, 제출 당시 정책 확인 | 별도 출시 체크리스트 검토 후 Google Play 제출. 심사 통과를 미리 보장하지 않음 |

현재 `applicationId`는 `com.google.mlkit.vision.demo`이고 ML Kit 아이콘과 실험 기능이 남아 있다. 배포용 식별자 변경 시 기존 앱의 로컬 기록 이전 여부도 함께 결정해야 한다. 최신 target SDK·테스트·개인정보/건강 관련 정책은 출시 준비 때 공식 문서로 다시 확인한다.

## 제품 흐름

운동 습관 목표 → 오늘의 루틴 → 실천 기록 → 시작·중간·마지막 사진 점검 → 동일 촬영 조건의 각도 변화 확인.

| 기능 | 구현 내용 | 한계 |
| --- | --- | --- |
| 목표 | 2주/4주, 하루 3분/5분 선택. 기본 제안 4주·3분 | 습관 목표이며 AI의 회복 기간 예측이 아님 |
| 오늘 | 경과 일수, 실천 일수, 다음 사진 점검 시점 | 앱 안에서 안내하며 백그라운드 알림은 없음 |
| 운동 | 가벼운 턱 당기기와 편안한 목 돌리기, 휴식 포함 타이머 | 각도별 맞춤 처방·실시간 수행 판정은 하지 않음 |
| 실천 | 타이머 후 실제 수행을 확인하면 당일 기록 | 자기 보고이며 같은 날짜는 중복하지 않음 |
| 촬영 | 전면/후면, 좌우 선택. CameraX 사진 3장 | 갤탭 전면·왼쪽 실촬영 확인. 다른 조합은 추가 검증 필요 |
| 자세 보정 억제 | 미리보기 가림, 촬영 시점 분산, 결과는 3장 확인 후 표시 | 처음부터 의도적으로 유지한 자세는 검출할 수 없음 |
| 기준점 | ML Kit 귀 후보와 수동 귀·C7 조정, 미세 이동 버튼 | C7은 먼저 위치를 확인한 표식이 필요. 자동 검출 아님 |
| 품질 확인 | 기기 수평·정면 얼굴·여러 사람 검사, 3회 차이 검사 | 얼굴 미검출/실패는 수동 확인. 센서 미지원 기기는 수평 직접 확인 |
| 결과 | 귀·C7 연결선, C7 수평·수직선, 각도 호, 3회 값과 중앙값 | 정상·경도·중증 분류 및 치료 효과 판정 없음 |
| 기록 | SQLite에 측정값·조건·실천 날짜 저장, 과거 목표 조회·측정 삭제·전체 삭제 | 사진은 보존하지 않아 추후 사진 재검토는 불가 |

## 측정 기준

CVA는 귀의 이주와 C7을 잇는 선과 C7을 지나는 수평선 사이의 각도다. 기존 Pose 어깨점을 낮춘 값은 C7이 아니다.

```text
dx = abs(ear.x - c7.x)
dy = c7.y - ear.y
angle = degrees(atan2(dy, dx))
result = median(angle1, angle2, angle3)
```

비유한 좌표, 1픽셀 미만의 두 점, C7 이하 높이의 귀는 계산하지 않는다. 화면 터치를 원본 이미지 좌표로 역변환하고 이미지 밀도에 따른 좌표 차이를 제거한다.

임시 촬영 품질 기준은 기기 좌우 기울기 5도, 앞뒤 기울기 15도, 세 값의 최댓값과 최솟값 차이 5도다. 이 수치는 임상 판정 기준이 아니며 실제 기기와 반복 측정으로 조정해야 한다. 측정 편차가 큰 경우 결과를 이력에 저장하지 않는다.

현재 얼굴 방향 확인은 기존 실험의 yaw 기준을 재사용한다. 얼굴 방향만으로 몸 전체가 정확한 측면이라고 보장할 수 없어 촬영 조건의 수동 확인을 함께 요구한다.

사진만 보고 일부러 턱을 당겼는지 단정할 수 없다. 현재 장치는 촬영 중 수치를 보고 자세를 맞추는 행동을 줄이고 불일치한 결과를 보류하는 수준이다. 같은 조건에서 반복해도 기준점의 실제 위치가 틀렸다면 값이 정확해지지 않는다.

## 구현 위치

- `PostureCoachActivity`: 목표·오늘·루틴·기록·설정.
- `PostureCaptureActivity`: 권한·CameraX 사진·센서·ML Kit 귀 후보·사진 확인·결과.
- `PosturePhotoView`: 사진과 좌표의 같은 변환, 기준점 조정·오버레이.
- `PostureSession`: 화면 회전 중 메모리에만 보존하는 사진과 진행 상태.
- `PostureStore`: 목표 설정과 SQLite 기록. 새 목표 후에도 이전 기록 보존.
- `CvaMath`: 각도·중앙값·반복성 계산. 기존 귀-어깨 코드도 같은 기하 공식을 호출.
- `CoachUi`: 두 화면에서 쓰는 기본 Android 위젯 배치.

새 의존성, 외부 AI API, 서버는 추가하지 않았다. 현재 AI 역할은 온디바이스 귀 후보 검출이다. 향후 자동 루틴 생성이나 회복 기간 예측이 필요하면 별도 근거·콘텐츠 검토와 구현이 필요하다.

## 검증

| 검사 | 상태 |
| --- | --- |
| `scripts/check-cva.sh` | 18개 계산 검사 통과 |
| Debug 앱 APK | 최종 수정 포함 빌드 통과 |
| AndroidTest APK | 최종 수정 포함 빌드 통과 |
| Android lint | 테마 호환 오류 수정 후 통과. 기존 Kotlin/lint 버전 메타데이터 메시지와 샘플 경고 남음 |
| `PostureFlowTest` | 갤탭 SM-T735N에서 2개 통과. 저장소 격리·기록 보존·이미지 좌표 확인 |
| `NeckPostureAngleTest` | 같은 기기에서 8개 통과 |
| `ShoulderAdjustmentUiTest` | 같은 기기에서 1개 통과. 이전 실시간 실험의 회귀 확인 |
| 설치·실행 | 기존 데이터 삭제 없이 업데이트 성공. 새 시작 화면 실행 및 가로 화면 캡처 확인 |
| 실제 귀 후보·C7 지정·저장 | 전면·왼쪽 사진 3장 확인 및 저장 완료. 귀 위치와 C7 지정은 사용자가 직접 확인 |
| 앱 재실행·화면 회전 | 프로세스 강제 종료 후 측정 3개·중앙값·조건 보존. 홈 세로 화면 캡처 및 가로 복귀 확인 |
| 임상적 정확도·운동 효과 | 검증하지 않음 |

```bash
cd /home/mjm32/projects/neck-posture-coach/android/vision-quickstart
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 bash scripts/check-cva.sh
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 bash gradlew :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
```

APK: `app/build/outputs/apk/debug/vision-quickstart-debug.apk`.

`PostureFlowTest`는 앱 권한 안에서 `posture_test_` 접두사의 별도 DB·환경설정을 사용해 실제 기록을 건드리지 않는다. 테스트 APK의 컨텍스트로 DB를 열면 앱 UID와 달라 실기기에서 권한 오류가 발생했으며, 이름으로 분리하는 방식으로 수정 후 통과했다. 같은 날짜 중복, DB 재열기, 새 목표 이후 과거 기록, 불안정 측정 저장 차단, 이미지 여백·밀도와 터치 좌표를 검사한다.

실기기에서 추가로 확인할 것은 첫 실행 권한 거부·재허용, 후면·오른쪽 촬영, 촬영 중 다른 앱으로 이동·화면 회전, 촬영 중 미리보기 가림 시점, 루틴 완료 후 날짜별 기록이다. 홈 화면의 회전 검증을 촬영 중 회전 검증으로 간주하지 않는다. 자동 검사와 사람이 직접 촬영해 확인한 항목은 구분한다.

### 2026-09-30 실촬영 검증

갤탭 SM-T735N이 ADB `device` 상태임을 확인했다. 카메라 화면에서 `기기 수평 확인됨`과 활성화된 촬영 버튼을 확인했다. 원격 촬영 버튼을 누르기 전에 사용자가 촬영을 진행해 기준점 확인 1/3 화면으로 넘어갔다. 따라서 이번에는 원격 촬영 시작 및 미리보기 가림 장면을 직접 관찰하지 못했다.

귀 후보가 생성된 뒤 사용자가 실제 이주 위치와 C7 지정을 확인했다. 첫 사진에서 기준점 확인 전 `다음 사진` 비활성화, 지정·확인 후 활성화를 확인했다. 마지막 사진도 별도 확인을 요구했고 사용자가 결과를 저장했다. 사진 보호 설정은 해제하지 않았으며 원본 사진을 추출하지 않았다.

| 항목 | 확인값 / 결과 |
| --- | --- |
| 기록 시각 | 2026-09-30 21:26, 기기 표시 기준 |
| 촬영 조건 | 서서 촬영 · 왼쪽 · 전면 |
| 첫째 / 둘째 / 셋째 | 52.5° / 55.4° / 55.7°, 화면 반올림값 |
| 중앙값 | 55.4° |
| 최댓값 − 최솟값 | 표시값 기준 3.2°. 앱의 임시 5° 편차 기준 통과 |
| 기록 보존 | `am force-stop` 후 재실행, 같은 세 값·조건 확인 |
| 화면 | 홈 가로·세로 전환 후 최근 측정값 유지. 자동 회전 설정 복원 |
| 운동 기록 | 0일 유지. 검증을 위해 실제 수행하지 않은 운동을 기록하지 않음 |

이는 한 사용자의 앱 동작 확인이며 임상적 정확도·정상 여부·개선 효과의 증거가 아니다. C7 위치는 독립적으로 검증하지 않았고, 전문가의 기준 측정과 대조하지 않았다. 연속 사진 3장의 편차가 작아도 같은 위치 지정 오류가 반복될 수 있다.

## 갤탭 업데이트 재현

빌드는 WSL, USB 연결·설치는 Windows ADB를 사용한다. Windows에서 기기가 보이지 않으면 `adb server-status`로 서버 경로를 확인한다. `/home/mjm32/Android/Sdk/platform-tools/adb`가 나오면 WSL ADB 서버를 종료한 뒤 Windows 서버를 시작한다.

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
wsl -d Ubuntu -- /home/mjm32/Android/Sdk/platform-tools/adb kill-server
& $adb start-server
& $adb devices -l
```

기존 Windows debug.keystore로 서명해야 기존 앱을 삭제하지 않고 업데이트할 수 있다. 아래 비밀번호는 Android 표준 디버그 키 기본값이며 배포용 키 설정이 아니다. 키 파일은 저장소에 추가하지 않는다. PowerShell의 `-P` 인수는 따옴표로 감싼다.

```powershell
wsl -d Ubuntu -- env JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ANDROID_HOME=/home/mjm32/Android/Sdk bash /home/mjm32/projects/neck-posture-coach/android/vision-quickstart/gradlew -p /home/mjm32/projects/neck-posture-coach/android/vision-quickstart :app:assembleDebug :app:assembleDebugAndroidTest '-Pandroid.injected.signing.store.file=/mnt/c/Users/mjm32/.android/debug.keystore' '-Pandroid.injected.signing.store.password=android' '-Pandroid.injected.signing.key.alias=androiddebugkey' '-Pandroid.injected.signing.key.password=android'
```

2026-09-30 설치한 APK 사본은 Windows `C:\dev\artifacts\neck-posture-coach-20260930`에 있다. 다시 빌드한 경우 사본도 최신 결과물로 교체해야 한다.

```powershell
$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb install -r C:\dev\artifacts\neck-posture-coach-20260930\neck-posture-coach-debug.apk
& $adb install -r -t C:\dev\artifacts\neck-posture-coach-20260930\neck-posture-coach-tests.apk
& $adb shell am instrument -w -r -e class 'com.google.mlkit.vision.demo.java.neckposture.PostureFlowTest,com.google.mlkit.vision.demo.java.neckposture.NeckPostureAngleTest,com.google.mlkit.vision.demo.java.ShoulderAdjustmentUiTest' com.google.mlkit.vision.demo.test/androidx.test.runner.AndroidJUnitRunner
```

설치 검증 결과: `OK (11 tests)`. 데이터 초기화나 앱 삭제 없이 업데이트했다. Git 커밋·푸시는 사용자 요청에 따른 오늘 마감 작업으로 별도 수행한다. OlThing과 Windows 이전 작업본은 수정하지 않았다.

## 참고 자료

- [ML Kit Pose Detection 랜드마크](https://developers.google.com/ml-kit/vision/pose-detection): C7은 제공되지 않음.
- [스마트폰 CVA 측정 타당도·신뢰도 연구](https://pmc.ncbi.nlm.nih.gov/articles/PMC7559098/): 귀 이주와 C7 표식을 이용한 측정. 이 앱의 정확도를 검증한 연구는 아님.
- [비방사선 자세 측정 방법 검토](https://pmc.ncbi.nlm.nih.gov/articles/PMC9354067/): 기준점 위치 및 표준화 문제.
- [Cambridge University Hospitals 운동 안내](https://www.cuh.nhs.uk/patient-information/neck-exercises-and-advice/): 일반 동작 설명을 한국어로 요약. 기관의 이미지·브랜딩은 복제하지 않음. 앱의 목표 시간은 치료 용량이 아니라 휴식을 포함한 실천 시간.

사용자가 제공한 안내 이미지의 등급표·운동 시간은 임상 규칙으로 그대로 도입하지 않았다.
