# 목 자세 코치 (Neck Posture Coach)

Google ML Kit `vision-quickstart` 샘플을 기반으로 만드는 Android 거북목 자세 측정 프로젝트입니다.

하루 운동 습관을 기록하고 시작·중간·마지막에 옆모습 사진을 점검합니다. 온디바이스 ML Kit이 귀 후보를 제안하고, 사용자가 귀의 이주와 C7 표식을 확인해 참고용 CVA 각도를 계산합니다. 루틴은 2주/4주, 하루 3분/5분 목표로 설정합니다.

## 진행 현황

**마지막 갱신: 2026-09-30 (한국 시간)**

**현재 단계: 갤탭에서 핵심 흐름을 확인한 개발용 앱. Google Play 미등록.**

작업 브랜치: `feat/pose-landmark-log` · `dev` 미병합.

| 상태 | 항목 | 확인 내용 / 남은 작업 |
| --- | --- | --- |
| 완료 | 운동 습관 앱 흐름 구현 | 목표 설정·오늘·루틴·사진 점검·결과·기록 연결 |
| 완료 | 빌드·자동 검사 | Debug 앱/테스트 APK 빌드, lint, 계산 검사 18개, 갤탭 계측 테스트 11개 통과 |
| 완료 | 갤탭 사진 점검 | 전면·왼쪽 3장, 귀 후보와 사용자 C7 확인, 측정값 저장 |
| 완료 | 기록 보존·홈 화면 회전 | 앱 강제 종료·재실행 후 기록 유지, 가로·세로 홈 확인 |
| 구현 / 추가 검증 | 운동 실천 | 안내·타이머·일별 저장 구현. 실제 루틴 완료·날짜 변경 확인 남음 |
| 구현 / 추가 검증 | 촬영 안정성 | 후면·오른쪽, 권한 거부, 촬영 중 회전·중단 복귀 확인 남음 |
| 미검증 | 측정 정확도 | 실제 귀·C7 기준점 대조와 반복 측정 필요. 진단·치료 효과 판정 아님 |
| 예정 | S20 Ultra 모바일 검증 | 작은 화면·글꼴·터치·카메라·USB 분리 후 실행 |
| 예정 | UI 개선 | 사용자 제공 시안을 받은 뒤 반영 |
| 예정 | Google Play 출시 | 배포 구성·개인정보 안내·스토어 자료·제출 시점 정책 검토 후 진행 |

### 이번 주 작업록 · 2026-09-28 ~ 2026-10-04

| 날짜 | 작업 | 결과 |
| --- | --- | --- |
| 2026-09-30 | 실시간 실험에서 운동 습관·사진 점검 중심 앱으로 구성 | 한국어 `목 자세 코치` 시작 화면 연결 |
| 2026-09-30 | 사진 3장·귀/C7 수동 확인·CVA 중앙값·로컬 기록 구현 | 사진은 메모리에서만 처리, 측정값과 실천 날짜 저장 |
| 2026-09-30 | Windows/WSL ADB 충돌·기존 APK 서명·테스트 저장소 격리 정리 | 기존 데이터 삭제 없이 설치, 실기기 테스트 11개 통과 |
| 2026-09-30 | 갤탭 실촬영·저장·재실행 검증 | 흐름 확인 완료. 상세 수치와 한계는 [검증 기록](android/vision-quickstart/APP_STATUS.md#2026-09-30-실촬영-검증) 참고 |
| 2026-09-30 | 오늘 작업 마감·후속 범위 결정 | S20 Ultra 검증, 제공 시안 반영, Google Play 출시를 다음 단계로 정리 |

### 다음 작업

- [ ] Galaxy S20 Ultra 연결 후 모바일 실기기 검증
- [ ] 사용자 시안 수령 및 모바일 UI 반영
- [ ] 측정 정확도·촬영 예외·운동 완료 기록 추가 검증
- [ ] 릴리스 앱 정리 및 Google Play 제출 준비

진행표와 작업록은 작업을 마칠 때 확인한 결과만 갱신합니다. 구현 완료와 검증 완료를 구분하며, 새 주가 시작돼도 이전 날짜의 기록을 삭제하지 않습니다.

## 현재 상태

- 기반 프로젝트: Google ML Kit 샘플 저장소
- 작업 앱: `android/vision-quickstart`
- 대상 플랫폼: Android
- 현재 구현 언어: Java
- 처리 방식: 온디바이스
- 서버/백엔드: 없음
- 사진 저장: 하지 않음. 사진은 메모리에서 처리하고 측정값·촬영 조건·실천 기록은 SQLite에 저장
- 시작 화면: 한국어 `목 자세 코치`. 기존 ML Kit 샘플은 설정의 실험 화면에서 접근
- 실행 방식: 설치된 앱은 PC·USB 연결 없이 기기에서 실행. USB는 개발 중 설치·디버깅용
- 배포 상태: 개발용 Debug 앱. Google Play 미등록

## 프로젝트 규칙

코드를 수정하기 전 [`AGENTS.md`](AGENTS.md)를 먼저 확인합니다.

핵심 원칙:

- 서버, 클라우드 동기화, 분석, 광고, 계정 시스템을 추가하지 않습니다.
- 사진이나 영상을 저장하거나 업로드하지 않습니다.
- "진단", "치료" 같은 의료 표현을 사용하지 않습니다.
- 새 의존성은 검토 없이 추가하지 않습니다.
- 자세 각도 계산 로직은 ML Kit/카메라 코드와 분리해 테스트 가능하게 둡니다.

## 개발 흐름

브랜치 전략:

```text
master        안정 브랜치
dev           개발 통합 브랜치이자 기본 브랜치
feat/*        dev에서 분기하는 기능 브랜치
fix/*         dev에서 분기하는 수정 브랜치
docs/*        dev에서 분기하는 문서 브랜치
```

커밋 형식:

```text
type(scope): summary
```

예시:

```text
docs: 거북목 자세 측정 프로젝트 설명 추가
fix(android): 지원되지 않는 Gradle JVM 옵션 제거
feat(M-03): 자세 각도 계산기 추가
```

## 빌드

주 개발 환경은 WSL Ubuntu입니다. Android 프로젝트는 저장소의 android/vision-quickstart 폴더에 있습니다.

~~~bash
cd ~/projects/neck-posture-coach/android/vision-quickstart
bash ./gradlew :app:assembleDebug
~~~

- JDK: /usr/lib/jvm/java-17-openjdk-amd64
- Android SDK: ~/Android/Sdk (Android 34, Build Tools 30.0.3)
- APK: app/build/outputs/apk/debug/vision-quickstart-debug.apk

Linux Android Studio는 ~/apps/android-studio/bin/studio로 실행하고 ~/projects/neck-posture-coach/android/vision-quickstart를 엽니다. Gradle JDK는 위 JDK 17 경로를 선택합니다. USB 연결·설치는 Windows SDK의 ADB를 사용합니다. 기존 설치본과 같은 서명으로 업데이트하는 절차는 [앱 개발 현황](android/vision-quickstart/APP_STATUS.md)에 있습니다.

## 앱 흐름과 검증

목표 설정 → 오늘의 루틴 → 실천 기록 → 사진 점검 → 귀·C7 확인 → 결과 저장 → 다음 점검에서 비교.

구현 내용, 측정 한계, 실행 명령, 실기기 확인 항목은 [앱 개발 현황](android/vision-quickstart/APP_STATUS.md)에 정리했습니다.

사진에서 의도적인 턱 당김을 완전히 판별할 수 없으며, 각도만으로 질환이나 개선 여부를 판정하지 않습니다. 귀-어깨 실시간 실험과 새 CVA 사진 측정은 다른 측정 방법입니다.

## 출시 목표

최종 목표는 Google Play에 등록하는 독립 실행 Android 앱입니다. 먼저 Galaxy S20 Ultra에서 모바일 사용성을 검증하고, 사용자가 제공할 시안에 맞춰 화면을 다듬습니다. 오늘 추가 기기 테스트·대규모 UI 재설계·스토어 등록은 진행하지 않습니다.

현재 패키지·아이콘·실험 기능에는 ML Kit 샘플 구성이 남아 있습니다. 배포 식별자와 버전, 불필요한 샘플·권한·의존성, 릴리스 서명과 AAB, 개인정보 안내, 스토어 자료를 정리하고 제출 시점의 Google Play 정책을 확인해야 합니다. 지금 Debug APK가 출시 준비 완료 상태인 것은 아닙니다.

## 원본

외부 코드·모델 도입 전 검토 내용은 [오픈소스 도입 검토](android/vision-quickstart/OPEN_SOURCE_REVIEW.md)에 기록합니다.

이 프로젝트는 Google ML Kit 샘플 저장소에서 시작했습니다.

https://github.com/googlesamples/mlkit
