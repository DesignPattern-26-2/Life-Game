# Life-Game
CAU CSE DP project

## 사용자 패턴 라이브러리

상단 **Patterns**를 누르면 별도 편집 창이 열립니다. 기본 Glider, Blinker, Block, Toad와 직접 만든 패턴을 관리할 수 있습니다.

1. **new...**를 선택하고 이름을 입력한 뒤, 격자를 클릭·드래그해 패턴을 그립니다.
2. **save**로 저장합니다. 기존 항목을 선택해 수정·저장하면 해당 항목이 갱신됩니다.
3. **load**는 선택한 저장 패턴을 메인 보드 중앙에 배치합니다. 기존 보드의 셀은 지워지고 편집 창은 닫힙니다.
4. **delete**로 선택한 저장 패턴을 삭제합니다.

변경이 없으면 save는 비활성, 변경이 있으면 활성입니다. 저장된 항목을 선택하고 수정사항이 없을 때 delete/load가 활성화됩니다. 다른 항목을 선택하거나 창을 닫을 때 미저장 내용이 있으면 **저장 / 저장하지 않기 / 취소**를 표시합니다.

패턴은 사용자 홈의 `.holub-life/patterns.xml`에 저장되어 재실행 후에도 유지됩니다. 기존 Grid 메뉴의 `.life` 보드 파일과 별도 형식입니다. 최대 64×64 모양과 한글 이름을 지원하며, 이름이 없거나 중복되거나 살아 있는 셀이 없는 패턴은 저장하지 않습니다.

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

JUnit 5 테스트 39개로 기본 프리셋, 세대 변화, 편집 상태, 파일 저장·불러오기와 실패 시 편집 보존을 검증합니다.

설치된 Eclipse의 JUnit 플러그인 JAR와 JDK 9 이상을 이용한 명령줄 실행:

```powershell
.\tools\test.ps1 -JdkDirectory 'C:\Program Files\Java\jdk-22' -EclipsePlugins 'C:\path\to\eclipse\plugins'
```

이 스크립트는 운영 소스를 Java 8 대상으로 컴파일합니다. 별도 데이터로 GUI를 시험하려면 테스트 스크립트 실행 후 다음 명령을 사용합니다.

```powershell
& "$env:JAVA_HOME\bin\java.exe" '-Dholub.life.patterns.file=build/ui-patterns.xml' -cp build/classes com.holub.life.Life
```

## 설계와 통합

Observer로 모델 변경을 화면에 반영하고, Repository로 파일 저장을 분리합니다. 패턴 배치는 Command와 기존 Composite/Memento 구조를 사용합니다.

구현 범위, 기존 소스 변경, 설계패턴 적용 근거와 UML은 [패턴 라이브러리 구현 문서](docs/pattern-library.md)에 정리했습니다. 다른 기능과 통합할 때는 Universe의 메뉴 연결 및 Neighborhood.transfer의 경계 상태 보완을 함께 유지하고 전체 테스트를 실행하세요.
