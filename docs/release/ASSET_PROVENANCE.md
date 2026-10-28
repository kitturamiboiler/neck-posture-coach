# 배포 자료 출처

- moklog_logo.png: 사용자가 2026-10-28 제공한 원본 PNG를 변경하지 않고 앱 리소스로 복사했다. 운영자는 상업 배포 권리를 최종 확인한다.
- exercise_guide.png: 이 작업에서 내장 이미지 생성 도구로 새로 만들고 방향을 수정한 자체 참고 그림. 의료 전문가 인증·실제 운동 영상으로 표시하지 않는다. 외부 기관의 사진·영상·로고는 복제하지 않았다.
- 운동 문구: Cambridge University Hospitals의 Neck exercises and advice를 참고해 일반 동작을 자체 한국어 표현으로 요약했다. 정해진 횟수·치료 용량·효과는 도입하지 않았다.
- https://www.cuh.nhs.uk/patient-information/neck-exercises-and-advice/
- 각도 기준 안내: 연구마다 문턱값이 달라 자가 참고라는 설명을 함께 표시한다.
- https://journals.plos.org/plosone/article?id=10.1371/journal.pone.0330665
- 스토어 512px 아이콘·1024×500 그래픽: Android Canvas에서 제공 로고와 자체 문구를 렌더링한 이미지. 개인 카메라 사진을 사용하지 않았다.
- 라이브러리 고지: android/moklog/THIRD_PARTY_NOTICES.txt와 기존 vision-quickstart/LICENSE를 빌드 시 합쳐 앱에 동봉한다. Checker Framework는 MIT인 annotation 라이브러리만 사용하며, ML Kit 약관을 오픈소스 라이선스로 바꾸지 않는다.

그림 생성 프롬프트 요약: 초록색 옷을 입은 성인의 준비/작은 턱 당김, 정면/편안한 목 회전 두 줄의 설명 그림. 흰 배경, 글자·기관 로고 없음. 첫 생성에서 턱 당김 방향 화살표가 반대여서 편집 도구로 왼쪽 방향 및 작은 수평 후퇴로 수정했다.

원본 생성 파일:
- exec-d12638ab-1ae5-402b-a2de-1bd925a24479.png: 초기안, 사용하지 않음.
- exec-47ab9f3c-3d67-4b5b-abd7-76321fc994f1.png: 수정안, 앱 assets/exercise_guide.png에 반영.

외부 운동 영상은 아직 제공되지 않았다. 향후 영상마다 원본 URL, 권리자, 앱 내 재생/복제 허용, 초상권, 사용 기간, 출처 표기를 기록한 뒤 반영한다.
