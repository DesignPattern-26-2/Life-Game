# Life-Game
CAU CSE DP project

## 사용자 패턴 라이브러리

메인 보드에서 셀을 배치한 후 상단 **Patterns**를 누르면 시뮬레이션이 정지되고 패턴 라이브러리 창이 열립니다. 현재 보드의 모양은 임시 항목 **Current**로 표시되며, 저장된 패턴은 이름과 썸네일로 확인할 수 있습니다. 최초 목록에는 Glider, Blinker, Block, Toad가 제공됩니다.

1. **Current**를 선택하고 원하는 이름을 입력한 뒤 **Save**를 눌러 현재 보드의 모양을 저장합니다. 빈 보드는 저장할 수 없고, 저장 후 임시 항목은 목록에서 사라집니다.
2. 저장된 패턴을 선택한 뒤 **Apply**를 누르면 기존 보드가 지워지고 선택한 패턴이 중앙에 배치됩니다. 아직 저장하지 않은 Current에 살아 있는 셀이 있으면 **Back / Apply** 확인 창이 표시됩니다.
3. 저장된 패턴의 이름을 바꾼 후 **Save**를 누르면 해당 항목이 갱신됩니다. **Delete**는 선택한 저장 항목을 확인 후 삭제합니다.
4. 새로운 모양을 만들려면 라이브러리 창을 닫고 메인 보드에서 셀을 편집한 후 다시 Patterns를 엽니다. 창을 닫기만 하면 메인 보드는 변경되지 않으며, Save하지 않은 편집은 영구 저장되지 않습니다.

변경사항이 있고 살아 있는 셀이 있는 경우 Save가 활성화됩니다. Apply와 Delete는 저장된 항목에 수정사항이 없을 때 활성화됩니다.

패턴은 사용자 홈의 `.holub-life/patterns.xml`에 저장되어 재실행 후에도 유지됩니다. 기존 Grid 메뉴의 `.life` 보드 파일과 별도 형식입니다. 최대 64×64 모양과 한글 이름을 지원합니다. 빈 이름, 중복 이름, 예약 이름 Current 및 살아 있는 셀이 없는 패턴은 새로 저장할 수 없습니다.

## 실행

Java 8 이상에서 실행할 수 있습니다. 저장소 루트에서 소스를 컴파일한 뒤 실행하세요.

```sh
javac -encoding UTF-8 -d bin -sourcepath src src/com/holub/life/Life.java
java -cp bin com.holub.life.Life
```

Windows에서는 `run.bat`을 실행할 수 있습니다. JAVA_HOME이 있으면 해당 JDK를 사용하고, 없으면 PATH의 javac/java를 사용합니다.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-22'
.\run.bat
```

Eclipse에서는 Java 프로젝트에 src를 소스 폴더로 등록한 뒤 Life.java를 **Run As → Java Application**으로 실행합니다. 테스트도 실행하려면 test를 소스 폴더로 등록하고 JUnit 5 라이브러리를 추가합니다.

## 테스트

JUnit 5 테스트로 기본 프리셋, 세대 변화, 편집 상태, 현재 보드 스냅샷, 파일 저장·불러오기와 실패 시 편집 보존을 검증할 수 있습니다. 이전 편집 창에서 통과한 39개 테스트 기록은 이번 UI 변경 이전의 결과이며, 최신 버전은 다시 실행해야 합니다.

설치된 Eclipse의 JUnit 플러그인 JAR와 JDK 9 이상을 이용한 명령줄 실행:

```powershell
.\tools\test.ps1 -JdkDirectory 'C:\Program Files\Java\jdk-22' -EclipsePlugins 'C:\path\to\eclipse\plugins'
```

이 스크립트는 운영 소스를 Java 8 대상으로 컴파일합니다. 별도 데이터로 GUI를 시험하려면 테스트 스크립트 실행 후 다음 명령을 사용합니다.

```powershell
& "$env:JAVA_HOME\bin\java.exe" '-Dholub.life.patterns.file=build/ui-patterns.xml' -cp build/classes com.holub.life.Life
```

## 설계와 통합

Observer로 모델 변경을 화면에 반영하고, Repository로 파일 저장을 분리합니다. 현재 보드 읽기와 패턴 배치는 기존 Composite/Memento 구조를 활용하며, 배치 요청은 Command로 전달합니다. 썸네일 렌더링은 PatternListCellRenderer에 분리합니다.

### 주요 변경 파일

| 경로 | 역할 |
| --- | --- |
| `src/com/holub/life/patterns/` | 기본 프리셋, 현재 보드 스냅샷, 썸네일 목록, XML 저장 및 배치 명령 (Java 소스 17개) |
| `src/com/holub/life/Universe.java` | 기존 게임 화면에 Patterns 메뉴 연결 |
| `src/com/holub/life/Neighborhood.java` | 패턴 불러오기 후 블록 경계의 활성 상태 전파 보완 |
| `test/com/holub/life/patterns/` | 프리셋, 편집 모델 및 저장소 테스트 3개 클래스 |
| `run.bat`, `tools/` | Windows 실행 및 테스트 도구 |

패턴 패키지만 복사하면 기존 게임 화면에 메뉴가 연결되지 않습니다. 다른 기능과 통합할 때는 `Universe.java`의 메뉴 설치 호출과 `Neighborhood.transfer()`의 경계 상태 보완도 함께 유지하고 전체 테스트를 실행하세요. Conway의 생존·사망 규칙과 기존 Grid/Go 기능은 유지합니다.

초기 구현 및 이전 편집 창의 설계 근거와 UML은 [패턴 라이브러리 구현 문서](docs/pattern-library.md)를 참고하세요. 해당 문서의 직접 격자 편집 및 new.../load UI 설명은 현재 버전과 다릅니다. 현재 버전의 사용법은 이 README를 기준으로 합니다.
