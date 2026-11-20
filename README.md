# Life-Game
CAU CSE DP project

## 셀 통계 실행

JDK 8 이상이 설치된 환경에서 저장소 루트를 기준으로 실행합니다. PowerShell 예시입니다.

```powershell
New-Item -ItemType Directory -Force out | Out-Null
$sources = (Get-ChildItem src -Recurse -Filter *.java).FullName
javac -encoding UTF-8 -d out $sources
java -cp out com.holub.life.Life
```

게임판을 클릭해 0세대를 배치하고 **Go → Tick (Single Step)** 또는 자동 속도를 선택합니다. 오른쪽 패널은 변화가 없는 세대도 기록합니다. 보드 편집, Clear, 파일 불러오기 성공 시 새 실험의 0세대가 시작됩니다.

| 지표 | 의미 |
|---|---|
| 생존율 | 직전 세대의 생존 셀 중 같은 위치에서 살아남은 비율 |
| 개체 밀도 | 보드 전체 칸 중 살아 있는 칸의 비율 |
| 최대 군집 비율 | 8방향으로 연결된 가장 큰 군집이 생존 셀에서 차지하는 비율 |
| 재등장 빈도 | 1세대부터 조회 세대까지, 이전에 관찰한 전체 보드 배치가 다시 나타난 세대의 비율. 주기는 같은 배치를 마지막으로 본 이후의 세대 수 |

슬라이더로 과거 세대를 선택하고 **최신 세대**로 실시간 추적을 재개할 수 있습니다. 추이 그래프는 선택한 세대까지 최근 120세대를 표시합니다. **군집 지도 · 셀 이력**에서 칸을 클릭하면 해당 좌표의 최근 64세대 이력을 볼 수 있습니다. 초록은 생존, 회색은 사망이며, 분모가 0인 지표는 대시로 표시합니다.

모델 회귀 테스트는 위 컴파일 후 실행할 수 있습니다.

```powershell
javac -encoding UTF-8 -cp out -d out test/com/holub/life/StatisticsTest.java
java -ea -cp out com.holub.life.StatisticsTest
```
