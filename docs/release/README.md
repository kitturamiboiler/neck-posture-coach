# 목log 출시 준비

기준일: 2026-10-28. 이 문서는 심사 승인서가 아니다.

관련 일정: [11월 9일 중간 점검 준비](../MIDTERM_PROGRESS_2026-11-09.md) · [12월 7일까지의 작업·제출 계획](../ROADMAP_2026-11-09_TO_12-07.md).

## 현재 배포 구성

- 출시 프로젝트: android/moklog. 기존 vision-quickstart는 개발·실험용으로 보존.
- 앱 이름: 목log. applicationId: com.moklog.app. 최초 Play 등록 후 식별자는 바꾸지 않는다.
- Java 제품 소스는 vision-quickstart의 neckposture 패키지가 단일 원본이다. Gradle Sync 작업이 선택한 소스만 build/generated로 복사한다. 생성 파일은 수정하지 않는다.
- 출시 모듈은 API 36, AGP 8.13.0, Gradle 8.14.3, CameraX 1.4.2를 사용한다.
- 바코드·OCR·포즈·샘플 선택 화면과 저장소/미디어 접근 권한은 출시 모듈에서 제외했다.
- 얼굴 검출은 bundled face-detection 16.1.7. 사진은 저장·업로드하지 않지만 SDK 진단 정보 처리까지 없다고 주장하지 않는다.
- 개인정보 화면에 오픈소스·SDK 고지를 넣고 APK에 Apache 2.0/MIT 라이선스 본문을 동봉했다. ML Kit은 별도 SDK 약관을 따른다.
- 개발 앱과 출시 앱은 별도 설치된다. 자동 데이터 이전은 구현하지 않았다.

## 빌드

WSL에서 실행한다.

~~~bash
cd ~/projects/neck-posture-coach/android/moklog
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export ANDROID_HOME=/home/mjm32/Android/Sdk
bash gradlew assembleDebug assembleDebugAndroidTest assembleRelease bundleRelease lintRelease
python3 scripts/verify_release.py build/outputs/bundle/release/Moklog-release.aab \
  --manifest build/intermediates/merged_manifests/release/processReleaseManifest/AndroidManifest.xml \
  --output build/release-audit.json
~~~

기본 release AAB는 소유자 서명이 없는 후보 파일이다. build 성공, API 36, ELF 정렬 검사만으로 제출 완료라고 표시하지 않는다. 서명 키는 저장소·채팅·발표 자료에 넣지 않는다.

휴대폰에 설치한 Release QA 앱은 테스트용 debug 인증서로 서명한 검증본이다. 실제 Play 앱 서명과 다르므로 이후 공개 버전으로 곧바로 덮어쓸 수 있다고 가정하지 않는다. 실사용 데이터를 쌓기 전에 최종 서명·설치 경로를 확정하고, 서명 변경을 위해 데이터를 무단 삭제하지 않는다.

## 소유자 서명 및 실제 제출 빌드

1. Play Console 계정을 개설하고 본인·조직 정보 및 필요한 기기 확인을 완료한다.
2. 운영자 이름, 공개 문의 이메일, 로그인 없이 접근 가능한 HTTPS 개인정보처리방침 페이지를 확정한다. 아래 HTML 초안을 실제 운영 정보로 완성하고 호스팅한다.
3. Android Studio의 Generate Signed Bundle 또는 keytool로 본인 소유 업로드 키를 생성하고 별도 안전한 장소에 백업한다. Play App Signing을 설정한다.
4. 터미널 환경에 MOKLOG_KEYSTORE, MOKLOG_STORE_PASSWORD, MOKLOG_KEY_ALIAS, MOKLOG_KEY_PASSWORD를 설정한다. 비밀번호를 명령 이력에 직접 적거나 채팅에 보내지 않는다.
5. 다음 명령을 실행한다. 운영자·이메일·URL 또는 키 환경이 없으면 submissionBundle은 실패하도록 구성했다.

~~~bash
bash gradlew submissionBundle \
  -PmoklogOperator='실제 운영자명' \
  -PmoklogEmail='실제 공개 문의 이메일' \
  -PmoklogPrivacyUrl='https://실제-공개-개인정보-주소'
~~~

키의 실제 인증서가 Play Console에 등록된 업로드 인증서와 같은지 확인한다. 키 분실에 대비한 백업은 개발자 책임이다.

## Play Console 입력 순서

1. 앱 생성: 목log, 한국어 기본 언어, 앱/게임 중 앱, 공개 지역·대상 연령·무료/유료 결정.
2. 스토어 등록정보: STORE_LISTING.md, 앱 아이콘, 실제 앱 스크린샷, 그래픽 이미지, 공개 문의 이메일.
3. 앱 콘텐츠: 광고 없음, 로그인 없이 접근 가능, 콘텐츠 등급 설문, 대상 사용자, Health apps declaration 작성.
4. 건강 기능은 일반 운동·실천 기록·자가 참고용 사진 각도다. 의료기기, 거북목 확정 진단, 치료 효과 보장으로 등록하지 않는다. 실제 기능을 기준으로 범주를 선택한다.
5. Data safety는 DATA_SAFETY_DRAFT.md와 최종 SDK를 대조해 본인이 확정한다. SDK 정보를 무시하고 '수집 없음'을 일괄 선택하지 않는다.
6. 개인정보처리방침은 공개 HTTPS HTML URL을 콘솔과 앱 설정에 반영한다. 초안 파일·비공개 GitHub 링크·PDF만 올리면 완료가 아니다.
7. 내부 테스트에 서명된 AAB 업로드, 사전 출시 보고서·지원 기기·충돌·권한·접근성 확인.
8. 2023-11-13 이후 생성한 개인 계정에 해당하면 최소 12명, 14일 연속 비공개 테스트 후 프로덕션 접근을 신청한다. 계정별 콘솔에 표시되는 실제 요건을 다시 확인한다.
9. 테스트 피드백을 반영하고 프로덕션 접근 승인 후 출시 심사를 제출한다. 승인 일정은 보장할 수 없다.

## 기술·실사용 검증 구분

- 정적 검사: 대상 API, 디버그 여부, 권한, 64비트 native ELF의 16KB LOAD 정렬.
- APK zipalign과 실제 AAB 메타데이터·서명 확인.
- S20 Ultra의 실행·첫 안내·목표·운동 안내·타이머·기록·설정·회전·카메라 권한 확인.
- 실제 사람의 귀·C7 위치, 자동 촬영, 평균 정확도는 별도다. 자동 검출 성공을 의료적 정확도 입증이라고 표현하지 않는다.
- 물리적 16KB 기기/에뮬레이터 런타임과 최신 Android 16 제스처·edge-to-edge 검증은 별도로 수행한다. Android 13 휴대폰 한 대가 이를 대체하지 않는다.
- 외부 운동 영상은 제공받은 후 저작권·초상권·앱 배포 허용을 확인한다. 현재 제공되는 자체 그림은 전문가 실제 시연 영상이 아니다.

## 최종 소유자 작업

- [ ] Play 계정 개설·본인 확인
- [ ] 운영자·공개 문의 이메일·공개 개인정보 URL
- [ ] 업로드 키 생성·백업·Play App Signing
- [ ] 스토어 설문·Data safety·건강 기능 선언 확정
- [ ] 필요한 비공개 테스트·프로덕션 접근 신청
- [ ] 사람 대상 촬영·운동 안내 최종 검토와 기기별 실사용 결과
- [ ] 외부 운동 영상 사용 권한 확인 후 연결 또는 자체 그림 안내로 출시 범위 확정

## 확인한 공식 문서

- https://support.google.com/googleplay/android-developer/answer/11926878
- https://support.google.com/googleplay/android-developer/answer/14151465
- https://support.google.com/googleplay/android-developer/answer/14738291
- https://support.google.com/googleplay/android-developer/answer/16679511
- https://developers.google.com/ml-kit/android-data-disclosure
- https://developer.android.com/guide/practices/page-sizes
