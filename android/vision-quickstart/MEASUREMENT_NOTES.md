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
dy = abs(shoulderY - earY)
angle = atan2(dx, dy)
```

`angle`은 수직선에서 귀-어깨 선이 얼마나 앞으로 기울었는지를 보는 값이다.

현재 코드는 화면 좌표 기준으로 계산하고, 도 단위로 표시한다.

```java
double angle =
    Math.toDegrees(Math.atan2(Math.abs(earX - shoulderX), Math.abs(shoulderY - earY)));
```

## 다음 확인

- Logcat에서 `FaceDetectorProcessor`의 `Face landmarks`와 `PoseDetectorProcessor`의 `Posture landmarks`를 비교한다.
- 측정 전용 화면에서 Face Detection과 Pose Detection을 함께 실행한다.
- Face Detection의 귀 좌표와 Pose Detection의 어깨 좌표를 같은 화면에 표시한다.
- Face 귀와 Pose 어깨를 연결하고 각도를 계산한다.
- 어깨가 특정 자세에서 빠지면 사용자가 어깨점을 직접 탭하는 보정 흐름을 추가한다.

## 다음 개발 범위

| 순서 | 작업 | 완료 기준 |
| --- | --- | --- |
| 1 | 측정 전용 Processor 만들기 | Face와 Pose 결과를 한 화면에서 같이 받음 |
| 2 | Face 귀 + Pose 어깨 표시 | 실제 귀구멍과 어깨에 흰 점이 찍힘 |
| 3 | 귀-어깨 선 표시 | 측면 자세에서 기준선이 자연스럽게 보임 |
| 4 | 각도 계산 표시 | 화면 또는 Logcat에 각도가 표시됨 |
| 5 | 측정값 임시 저장 | 첫 측정과 재측정을 비교할 수 있음 |

## 아직 하지 않는 것

- 커스텀 Neck Detection 모델 개발
- Face Mesh 도입
- 운동 코칭 로직 세분화
- 측정값 저장/그래프 화면
