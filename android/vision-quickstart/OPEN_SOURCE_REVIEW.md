# 목 자세 측정 오픈소스 도입 검토

확인일: 2026-09-23

## 결론

`motlabs/dont-be-turtle-android`는 검토용으로 내려받았으며, 우리 앱에 코드나 모델을 포함하지 않았다.
Android 저장소의 파일별 라이선스와 모델의 배포 조건을 구분해야 한다.
연결된 학습 저장소의 Apache-2.0 라이선스를 별도 Android 저장소 전체에 자동 적용하지 않는다.
이 문서는 확인한 근거와 도입 판단을 기록하며, 법률적 보증이나 모델 정확도 검증 결과가 아니다.

## 확인한 버전

| 대상 | 고정 버전 / 식별값 |
| --- | --- |
| [Android 저장소](https://github.com/motlabs/dont-be-turtle-android) | `f36c6265e705d56a40926832ef548bb436104788` |
| [학습 저장소](https://github.com/motlabs/dont-be-turtle) | `bd754736ea6caa3ccd58866657d97112f4bcf1cf` |
| Android 모델 경로 | `app/src/main/assets/turtle-model.tflite` |
| 모델 SHA-256 | `f28d8044d19c5f36141da64ed94bc2ba2b94741b4612f739e1cc7c34250be37e` |

참고 저장소는 프로젝트 밖 `C:\dev\references`에 두었다. 기존 앱의 빌드 대상이나 Git 원격은 변경하지 않았다.

## 라이선스 확인

| 항목 | 실제 확인 내용 | 도입 판단 |
| --- | --- | --- |
| Android 저장소 전체 | 루트 및 추적 파일에서 LICENSE / NOTICE를 찾지 못함. README에 전체 라이선스 선언 없음 | 전체 일괄 복사하지 않음 |
| `DrawView.kt`, `ImageClassifier.kt`, `ImageClassifierFloatInception.kt`, `Camera2BasicFragment.kt` 등 | 파일 헤더에 Apache-2.0, Copyright 2018 Zihua Zeng 및 Lang Feng 표시 | 해당 파일의 고지 조건을 지키는 재사용 후보. 모델 이용 조건과는 별개 |
| `AutoFitTextureView.kt` | 파일 헤더에 Apache-2.0, Copyright 2017 The TensorFlow Authors 표시 | 같은 기능이 이미 있는지 먼저 검토 |
| `MainActivity.kt` | 해당 파일에 라이선스 헤더 없음 | 위 파일들의 헤더를 이 파일까지 확대 적용하지 않음 |
| `turtle-model.tflite` | Android 저장소에서 모델을 명시적으로 포괄하는 사용·수정·재배포 조건을 확인하지 못함 | 앱 assets에 복사하거나 APK에 포함하지 않음 |
| 학습 저장소 | 루트 Apache-2.0 LICENSE 확인. 확인한 체크아웃의 추적 파일에 `.tflite` / `.pb` 없음 | Android 바이너리가 이 저장소에서 어떤 버전·조건으로 만들어졌는지 별도 확인 필요 |
| OpenCV 3.4.3, 이미지, 네이티브 바이너리 | 별도 검토 없이 포함하면 안 되는 제3자 구성요소 | 이번 도입에서 제외 |

참고: [GitHub의 저장소 라이선스 안내](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/licensing-a-repository).
공개 저장소라는 사실만으로 모든 파일과 모델의 재배포가 허용됐다고 판단하지 않는다.

## 실제 구현과 우리 목표의 차이

검토한 Android 소스 경로의 공통 접두사는 `app/src/main/java/jeongari/com/turtleposeestimator/`이다.

- `ImageClassifierFloatInception.kt`: 코드의 기본 입력은 256 x 256 BGR float, 출력 버퍼는 64 x 64 x 4이다. 이는 소스의 설정이며 바이너리 텐서를 직접 실행해 확인한 결과는 아니다.
- 같은 파일: 각 채널의 히트맵에 OpenCV 가우시안 블러를 적용하고 최댓값 위치를 점으로 사용한다. 실제 어깨와의 위치 오차를 검증하는 코드는 아니다.
- `DrawView.kt`: 모델의 네 점을 화면 크기에 맞춰 표시한다. 이 파일에는 귀-어깨각 계산이나 측정 결과 저장이 없다.
- 학습 저장소 `tfmodules/gen_tflite_coreml.py`의 `export_shape_in_json()`은 특징점을 `Head`, `Nose`, `Rshoulder`, `Lshoulder`로 내보낸다. **귀가 포함된 모델이라고 볼 근거가 없다.**
- 학습 저장소의 다른 데이터 준비 코드에는 머리·목·어깨 표기가 있어 버전별 정의가 일관적인지도 확인해야 한다. 위 목록이 Android 바이너리의 실제 채널 순서라고 단정하지 않는다.
- 학습 저장소 README의 정확도 항목은 `TBU`이다. Android 모델이 우리 측면 사진에서 기존 ML Kit보다 잘 작동한다는 근거는 아직 없다.
- Android 빌드는 support library 28, TensorFlow Lite 1.10.0, OpenCV 3.4.3에 의존한다. 이 구성을 현재 앱에 통째로 이식하지 않는다.

## 적용 여부

| 후보 | 우리 코드의 기존 대응 | 이번 결정 |
| --- | --- | --- |
| 이미지 좌표를 화면 좌표로 변환 | `GraphicOverlay`의 회전 후 이미지 크기, 확대·자르기·전면 반전 변환 | 기존 구현 유지. 다른 비율 변환을 겹쳐 붙이지 않음 |
| 추론과 화면 그리기 분리 | `NeckPostureProcessor` / `NeckPostureGraphic` | 이미 분리되어 있어 중복 클래스 추가하지 않음 |
| 네 점만 간단하게 표시 | 기존 귀·어깨 후보 표시 | 현재 표시 기능 유지 |
| 전용 모델의 어깨 위치 사용 | 우리 앱은 ML Kit Pose 어깨를 사용 | 바이너리 권한·채널 정의·실측 성능 확인 전 보류 |
| 히트맵 후처리 | 현재 ML Kit API는 랜드마크 좌표를 반환 | 현재 경로에 사용되지 않는 디코더나 OpenCV 의존성 추가하지 않음 |

이 검토만으로 측면 검출이 개선되었다고 보고하지 않는다. 이번 변경은 검토 문서와 README 링크뿐이며 기존 측정 소스의 미커밋 변경은 그대로 유지했다.

## 실제 도입 조건

1. 위 SHA-256 모델의 앱 포함, 수정, 재배포 및 서비스 사용 조건을 원저자에게 확인한다. 코드·모델·데이터 조건은 나누어 확인한다.
2. 해당 바이너리의 입력 전처리, 출력 텐서, 채널 이름 및 순서를 확인한다. 머리나 코를 귀로 대체하지 않는다.
3. 사용자가 검증용 사용에 동의한 동일한 측면 이미지로 기존 ML Kit과 비교한다. 외부로 업로드하지 않는다.
4. 좌우 측면 각각의 어깨 위치 오차, 검출 누락, 정지 시 흔들림, 처리 시간을 기록한다. 모델 점수만으로 정확성을 판정하지 않는다.
5. 개선이 확인된 경우에만 최소한의 Java 추론 코드를 연결하고 원본 저작권, 라이선스 전문, 수정 내역을 남긴다. 정확하지 않은 프레임에서는 각도를 생성하지 않는다.

## 원저자 문의 초안

아직 전송하지 않은 초안이다.

> 안녕하세요. `dont-be-turtle-android`를 참고하여 Android 목 자세 자가 측정 앱을 개발하고 있습니다.
> 일부 소스에 Apache-2.0 헤더가 있지만 저장소 전체 및 `app/src/main/assets/turtle-model.tflite`의 이용 조건은 찾지 못해 문의드립니다.
> 해당 모델(SHA-256: f28d8044d19c5f36141da64ed94bc2ba2b94741b4612f739e1cc7c34250be37e)을 앱에 포함하고 서비스에서 사용·수정·재배포할 수 있는지, 적용 라이선스와 필요한 고지를 알려주실 수 있을까요?
> 또한 모델의 네 출력 채널 정의, 전처리 규격, 학습 코드 버전 및 측면 평가 자료가 있다면 공유 부탁드립니다.

## 검증 범위

- 수행: 두 저장소의 고정 커밋, 파일별 라이선스 헤더, 입력·출력 처리 소스, 모델 해시 확인.
- 미수행: 외부 프로젝트 빌드·실행, 모델 성능 측정, 저작권자의 권한 확인 답변.
- 변경하지 않음: 모델 파일, 앱 의존성, 측정 알고리즘, 커밋·푸시.
