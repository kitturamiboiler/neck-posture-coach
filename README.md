# Neck Posture Coach

Google ML Kit `vision-quickstart` 샘플을 기반으로 만드는 Android 거북목 자세 측정 프로젝트입니다.

목표는 스마트폰/태블릿 카메라로 옆모습을 촬영하고, 온디바이스 ML Kit으로 귀와 어깨 랜드마크를 검출한 뒤 목 기울기 각도를 계산하는 것입니다. 결과는 자가 참고용 자세 지표로 보여주고, 교정 운동과 변화 추적까지 이어가는 것을 목표로 합니다.

## 현재 상태

- 기반 프로젝트: Google ML Kit 샘플 저장소
- 작업 앱: `android/vision-quickstart`
- 대상 플랫폼: Android
- 처리 방식: 온디바이스
- 서버/백엔드: 없음
- 사진 저장: 하지 않음. 계산된 측정값만 저장 예정
- 확인 완료: JDK 17 기준 Gradle debug build 성공

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
master 또는 main  안정 브랜치
dev               개발 통합 브랜치
feat/*            dev에서 분기하는 기능 브랜치
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

이 샘플 프로젝트는 JDK 17로 빌드합니다.

PowerShell:

```powershell
$env:JAVA_HOME='C:\Users\mjm32\.jdks\jbr-17.0.14'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
cd C:\dev\mlkit\android\vision-quickstart
.\gradlew.bat assembleDebug
```

Android Studio:

1. `C:\dev\mlkit\android\vision-quickstart` 폴더를 엽니다.
2. Gradle JDK를 `C:\Users\mjm32\.jdks\jbr-17.0.14`로 설정합니다.
3. Gradle Sync를 실행합니다.
4. 실제 Android 기기에서 실행합니다.

## 다음 목표

1. 갤럭시 탭 또는 Android 실기기에서 ML Kit 샘플을 실행합니다.
2. Pose Detection에서 귀와 어깨 랜드마크가 잡히는지 확인합니다.
3. 최소한의 목 각도 계산 프로토타입을 추가합니다.
4. 촬영 품질 게이트를 추가합니다.
5. 위험도 판정과 로컬 측정값 저장을 연결합니다.

## 원본

이 프로젝트는 Google ML Kit 샘플 저장소에서 시작했습니다.

https://github.com/googlesamples/mlkit
