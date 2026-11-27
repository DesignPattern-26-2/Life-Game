# 단축키 기능 설계 결정 기록

작성일: 2026-11-20, 브랜치 `feat/shortcut`

이 문서는 단축키 기능을 어떻게 만들었고 왜 그렇게 만들었는지를 적은 것이다.
확인하지 못한 것은 8장에 "확인하지 않음"으로 따로 적었다.

## 1. 무엇을 만들었나

원본 Life는 모든 동작을 메뉴로만 실행한다. 이 기능은 세 가지를 더한다.

1. 메뉴 항목을 명령(Command) 목록으로 자동 수집한다.
2. 키 입력을 한 곳에서 받아 명령을 실행한다.
3. 화면에서 키를 지정/해제하고, 명령 여러 개를 묶은 매크로를 만들고, 둘 다 파일에 저장한다.

기본 단축키는 두지 않았다. 처음 실행하면 아무 키도 지정되어 있지 않고,
`Shortcuts > Edit Shortcuts...` 창에서 직접 지정한다. (구조만 제공하기로 한 결정. 4.15 참고)

### 동작 흐름

```
[메뉴 등록]  Universe / Clock / 팀원 코드
                 │  MenuSite.addLine(...)            ← 호출하는 쪽은 고치지 않는다
                 ▼
             MenuSite ──알림──▶ MenuCommandCollector ──▶ CommandRegistry
                                (리스너를 ActionListenerCommand로 감싸 등록)

[키 입력]    키 ──▶ 루트 페인 InputMap ──▶ ActionMap ──▶ CommandAction
                 ──▶ Command.execute() ──▶ 원래의 메뉴 리스너 ──▶ Universe / Clock

[키 설정]    ShortcutDialog ──▶ Keymap.bind / unbind
                 ──알림──▶ KeyBinder (InputMap/ActionMap 다시 적기)
                 ──알림──▶ ShortcutFeature (keymap.properties 저장)
                 ──알림──▶ ShortcutTableModel (표 갱신)
```

### 클래스 목록

새 코드는 모두 `com.holub.life.shortcut` 패키지(창은 하위 `ui` 패키지)에 있다.

| 클래스 | 하는 일 |
|---|---|
| `Command` | 명령 인터페이스: `id()`, `displayName()`, `execute()` |
| `CommandRegistry` | 명령을 id로 등록, 조회, 해제, 전체 목록 |
| `ActionListenerCommand` | `ActionListener`를 `Command`로 감싼다 |
| `MenuCommandCollector` | `MenuSite.addLine` 알림을 받아 명령을 등록한다 |
| `Keymap` | `KeyStroke` → 명령 id. 조회, 지정, 해제, 충돌 검사 |
| `KeyConflictException` | 충돌로 거부될 때의 이유(키, 기존 명령, 요청 명령) |
| `CommandAction` | `Command`를 Swing `Action`으로 감싼다 |
| `KeyBinder` | `Keymap` 내용을 루트 페인의 InputMap/ActionMap에 옮겨 적는다 |
| `KeymapStore` | 키맵을 properties 파일로 저장/불러오기 |
| `MacroCommand` | 명령 여러 개를 순서대로 실행하는 명령 |
| `UserMacros` | 사용자가 만든 매크로의 생성, 삭제, 복원 |
| `MacroStore` | 매크로 정의를 properties 파일로 저장/불러오기 |
| `ChangeObserver` | "내용이 바뀌었다"는 알림을 받는 인터페이스 |
| `ShortcutFeature` | 위 부품을 만들어 연결한다. `Life.java`가 아는 유일한 클래스 |
| `ui.ShortcutDialog` | 명령 목록 창(키 지정/해제/실행, 매크로 생성/삭제) |
| `ui.ShortcutTableModel` | 명령 목록 표의 내용 |
| `ui.KeyCaptureDialog` | "새 키를 누르세요" 창 |
| `ui.MacroDialog` | 매크로 생성 창 |
| `ui.KeyText` | `KeyStroke`를 "Ctrl+L" 같은 화면용 글자로 바꾼다 |

## 2. 빌드와 테스트 실행

저장소에는 빌드 도구가 없어서, 컴파일과 테스트를 한 번에 하는 스크립트를 두었다.

```
bash scripts/test.sh                                          # Git Bash, macOS, Linux
powershell -ExecutionPolicy Bypass -File scripts\test.ps1     # Windows PowerShell
```

- JDK는 `JAVA_HOME`이 있으면 그것을, 없으면 PATH의 `java`/`javac`를 쓴다.
- JUnit 5 콘솔 러너 jar(`junit-platform-console-standalone-1.10.2.jar`)는 저장소에 올리지 않았다.
  `lib/`에 없으면 스크립트가 Maven Central에서 내려받는다. `lib/`와 `out/`은 `.gitignore`에 있다.
- 테스트는 `-Djava.awt.headless=true`로 실행한다. 창을 만들 수 없는 조건에서 76개가 모두 통과한다.
- 테스트를 돌린 뒤 프로그램 실행: `java -cp out/main com.holub.life.Life`
- 설정 파일은 사용자 홈의 `.life-game/keymap.properties`, `.life-game/macros.properties`다.
  키나 매크로를 처음 바꿀 때 만들어진다. 지우면 처음 상태(키 없음, 매크로 없음)로 돌아간다.

## 3. 적용한 패턴과 역할 대응표

### 3.1 Command

| 역할 | 클래스 |
|---|---|
| Command | `Command` |
| ConcreteCommand | `ActionListenerCommand`(메뉴 항목 하나), `MacroCommand`(명령 묶음) |
| Invoker | `CommandAction`(키가 눌리면 Swing이 호출한다), `ShortcutDialog`의 Run 버튼 |
| Receiver | 메뉴에 달려 있던 원래의 `ActionListener`와 그 뒤의 `Universe`, `Clock` |
| Client | `MenuCommandCollector`(메뉴 명령을 만든다), `UserMacros`(매크로를 만든다), `ShortcutFeature`(전체 연결) |

교과서 구조와 다른 점: 교과서의 ConcreteCommand는 Receiver를 직접 들고 그 메서드를 부른다.
여기서는 Receiver 자리에 원본의 `ActionListener`가 있다. `Universe`와 `Clock`의 실제 동작
(`outermostCell.clear()`, `startTicking()` 등)은 private이거나 익명 리스너 안에 있어서
원본을 고치지 않고는 직접 부를 수 없기 때문이다. 그래서 `ActionListenerCommand`는
ConcreteCommand이면서 동시에 Adapter다(3.2).

Invoker가 둘인 것이 Command 패턴을 쓴 효과다. 키 입력과 Run 버튼은 같은 `Command`를
실행할 뿐, 그것이 메뉴 동작인지 매크로인지 모른다.

### 3.2 Adapter (두 군데, 방향이 반대)

| 역할 | 메뉴 리스너 → 명령 | 명령 → Swing |
|---|---|---|
| Target | `Command` | `javax.swing.Action` |
| Adaptee | `java.awt.event.ActionListener` | `Command` |
| Adapter | `ActionListenerCommand` | `CommandAction` |
| Client | `CommandRegistry`를 쓰는 쪽(`KeyBinder`, `MacroCommand`, 창) | Swing의 키 바인딩(ActionMap) |

둘 다 객체 어댑터다(Adaptee를 필드로 갖고 호출을 넘긴다).
`CommandAction`이 `AbstractAction`을 상속하는 것은 `Action` 인터페이스의 나머지 메서드를
직접 구현하지 않으려는 것이고, Adaptee(`Command`)는 상속이 아니라 필드로 갖는다.

### 3.3 Observer (두 군데)

| 역할 | 메뉴 항목 추가 알림 | 내용 변경 알림 |
|---|---|---|
| Subject | `MenuSite` (`addLineListener`, `addLine` 안의 알림) | `CommandRegistry`, `Keymap`, `UserMacros` (`addObserver`) |
| Observer | `MenuSite.LineListener` | `ChangeObserver` |
| ConcreteObserver | `MenuCommandCollector` | `KeyBinder`, `ShortcutTableModel`, `ShortcutFeature`의 저장 메서드(`this::saveKeymap`, `this::saveMacros`) |
| 알림 방식 | push: 메뉴 지정자, 이름, JMenuItem, 리스너를 인자로 넘긴다 | pull: 인자 없이 "바뀌었다"만 알리고, 받는 쪽이 다시 읽는다 |

`ShortcutTableModel`과 `JTable` 사이의 갱신(`fireTableDataChanged`)은 Swing에 원래 있는
Observer이고, 새로 만든 것이 아니다.

### 3.4 Composite

| 역할 | 클래스 |
|---|---|
| Component | `Command` |
| Leaf | `ActionListenerCommand` |
| Composite | `MacroCommand` |
| Client | `CommandAction`, `KeyBinder`, 창. 매크로인지 아닌지 구분하지 않고 `execute()`만 부른다 |

교과서 구조와 다른 점: 자식을 넣고 빼는 메서드(`add`, `remove`)가 Component에도 Composite에도 없다.
단계 목록은 생성자에서 한 번 정해지고 바뀌지 않는다. 이유는 두 가지다.
(1) `Command` 인터페이스를 `id`, 표시 이름, `execute` 세 개로 작게 유지한다.
(2) 만든 뒤에 단계를 바꿀 수 없으므로, 매크로가 자기 자신을 단계로 갖는 순환을 만들 수 없다.

### 3.5 패턴이 아닌 것: Singleton을 쓰지 않았다

원본의 `MenuSite`, `Universe`, `Clock`은 Singleton이다. 새 코드의 `CommandRegistry`, `Keymap`,
`UserMacros`는 Singleton이 아니고, `ShortcutFeature`가 `new`로 만들어 생성자로 넘겨준다.
테스트마다 빈 객체를 새로 만들 수 있어야 하기 때문이다. 실제로 모든 테스트가 그렇게 한다.

## 4. 검토한 대안과 선택 이유

### 4.1 메뉴 자동 수집 vs 팀원이 Command를 직접 등록

자동 수집을 골랐다. 팀원이 `MenuSite.addLine`으로 메뉴를 추가하기만 하면 코드 수정 없이 명령 목록에 잡힌다.
직접 등록 방식은 팀원이 단축키 패키지를 알아야 하고 등록을 빠뜨릴 수 있다.

치른 값: 원본 `MenuSite`를 고쳐야 했다(5장). 그리고 메뉴를 거치지 않는 동작은 명령이 되지 않는다.
지금은 메뉴 없이 명령만 등록하는 공개 통로가 없다(7장).

### 4.2 `addLine` 안에서 알리기 vs 나중에 메뉴바를 훑기

`addLine` 안에서 알리는 쪽을 골랐다.

메뉴바를 훑는 방법(`JMenuBar`를 돌며 `JMenuItem.getActionListeners()`를 읽기)은 `MenuSite`를 고치지 않아도 된다.
그러나 훑는 시점 이후에 추가된 메뉴를 놓치고, `MenuSite`가 메뉴바를 통째로 다시 만드는 경우
(`regenerateMenuBar`)를 따라가야 하며, Swing이 내부적으로 만드는 팝업 메뉴 구조에 기대게 된다.

`MenuSite`를 바깥에서 감싸는 방법(Decorator/Proxy)은 쓸 수 없다. `MenuSite`는 전부 static이라
감싼 객체로 바꿔 끼울 수 없고, `Universe`와 `Clock`의 호출 코드를 모두 고쳐야 한다.

### 4.3 알림 인터페이스를 어디에 둘 것인가

`MenuSite` 안의 중첩 인터페이스(`MenuSite.LineListener`)로 두었다.

- 단축키 패키지에 두면 `com.holub.ui`가 `com.holub.life.shortcut`을 import하게 되어 의존 방향이 거꾸로 된다.
  `MenuSite`는 Life와 무관한 범용 UI 클래스다.
- `com.holub.ui`에 새 파일로 두면 원본 수정은 몇 줄 줄지만, 새 코드를 새 패키지에만 둔다는 원칙에서 벗어난다.
- 원본도 같은 방식을 쓴다(`Clock.Listener`, `Publisher.Distributor`가 중첩 인터페이스다).

### 4.4 Adapter가 리스너를 직접 호출 vs `JMenuItem.doClick()`

리스너를 직접 호출하되, 이벤트의 source로 원래의 `JMenuItem`을 넘긴다.

source가 중요한 이유: `Clock`은 메뉴 6개에 리스너 하나를 같이 달고,
`((JMenuItem)e.getSource()).getName()`의 첫 글자로 동작을 고른다(`Clock.java` 88–103행).
source가 `JMenuItem`이 아니면 `ClassCastException`이 난다.
이 경우는 `MenuCommandCollectorTest.sharedListenerSeesItsOwnMenuItemAsSource`가 확인한다.

`doClick()`을 쓰지 않은 이유: 인자 없는 `doClick()`은 버튼을 눌린 모습으로 잠깐 그렸다가 되돌리느라
호출한 스레드를 잠시 멈추고(JDK 17 소스에서 `doClick(68)`, 즉 68ms), 그 항목에 달린 리스너를 전부 부른다.
어댑터가 감싼 리스너 "하나"를 정확히 한 번 부른다는 것을 테스트로 확인하기 어렵다.

함께 정한 것: 메뉴에서 비활성화된 항목(`MenuSite.setEnable(requester, false)`)은 단축키로도 실행하지 않는다.
`ActionListenerCommand.execute()`가 source 컴포넌트의 `isEnabled()`를 확인한다.

### 4.5 InputMap/ActionMap vs KeyListener

루트 페인의 InputMap/ActionMap(`WHEN_IN_FOCUSED_WINDOW`)을 골랐다.

- `KeyListener`는 포커스를 가진 컴포넌트만 키를 받는다. 포커스가 옮겨 가면 단축키가 죽는다.
  `WHEN_IN_FOCUSED_WINDOW`는 창 안 어디에 포커스가 있어도 동작한다.
- Swing의 구조가 우리 구조와 그대로 맞는다.
  InputMap(키 → 이름표)은 `Keymap`(키 → 명령 id)에, ActionMap(이름표 → Action)은
  `CommandRegistry`(id → Command)에 대응한다. `KeyBinder`는 옮겨 적기만 한다.
- `KeyEventDispatcher`로 모든 키를 가로채는 방법도 있지만, 텍스트 입력 중인 키나 대화상자의 키까지
  가로채게 되어 직접 걸러야 한다.

예외로 `KeyCaptureDialog`에서만 `KeyListener`를 쓴다. 그 창은 미리 정한 키가 아니라
"다음에 눌리는 아무 키"를 알아내야 하는데, InputMap은 미리 등록한 키에만 반응하기 때문이다.

### 4.6 충돌 시 거부 vs 덮어쓰기

거부를 골랐다. `Keymap.bind()`는 다른 명령이 쓰는 키를 받으면 `KeyConflictException`을 던지고
키맵은 그대로 둔다. 예외에 키, 기존 명령, 요청 명령이 들어 있어서 창이 이유를 보여 줄 수 있다.

덮어쓰면 기존 명령의 단축키가 말없이 사라진다. 사용자는 나중에 그 키가 안 눌릴 때에야 알게 된다.
"물어보고 덮어쓰기"는 만들지 않았다. 지금은 기존 키를 해제한 뒤 다시 지정해야 한다.

`KeyConflictException`은 checked 예외다. 키를 지정하는 코드가 충돌 처리를 빠뜨리지 못하게 하려는 것이다.

### 4.7 Keymap에 Command 객체가 아니라 id를 저장

`Keymap`은 `KeyStroke → String(id)`다. 명령이 아직 등록되지 않았어도 키맵 파일을 먼저 읽을 수 있고,
파일에 그대로 쓸 수 있고, 명령 객체 없이 테스트할 수 있다.
치른 값: 키맵에 있는 id의 명령이 실제로 없을 수 있다. `KeyBinder`는 그런 키를 건너뛰고,
명령이 나중에 등록되면 알림을 받아 다시 건다(`KeyBinderTest.keyForCommandRegisteredLaterStartsWorking`).

### 4.8 없는 id를 찾았을 때

`CommandRegistry.find()`는 `Optional<Command>`를 돌려준다. null을 돌려주면 확인을 빠뜨리기 쉽고,
예외를 던지면 "키맵에는 있는데 명령은 없는" 정상적인 상황마다 예외 처리가 필요하다.
원본의 `Cell.DUMMY` 같은 Null Object(아무 일도 안 하는 명령)는 없는 명령을 실행해도 조용히 넘어가서
문제를 숨기므로 쓰지 않았다.

### 4.9 중복 id

- `CommandRegistry.register()`는 같은 id를 `IllegalArgumentException`으로 거부한다. 먼저 등록된 명령은 그대로 남는다.
- `MenuCommandCollector`는 거부하지 않고 `#2`, `#3`을 붙여 등록하고 경고 로그를 남긴다.
  수집기는 `MenuSite.addLine` 안에서 불리므로, 여기서 예외가 나면 메뉴를 추가하던 팀원 코드가 죽기 때문이다.

### 4.10 명령 하나에 키 하나

`Keymap.bind()`로 새 키를 지정하면 그 명령의 이전 키는 사라진다. 표의 한 줄이 명령 하나, 키 칸이 하나라서
화면과 파일 형식(`명령 id = 키`)이 단순해진다. 한 명령에 키 여러 개는 지원하지 않는다.

### 4.11 명령 id 규칙

메뉴 지정자의 `:`를 `/`로 바꾸고 `/항목이름`을 붙인다. 예: `Grid/Clear`, `Go/Tick (Single Step)`.
화면 라벨이 아니라 `MenuSite`의 name을 쓴다. `MenuSite`는 name(고정)과 label(바뀔 수 있음)을 구분하기 때문이다.
한계: 메뉴 이름이나 항목 이름에 `/`가 들어 있으면 서로 다른 항목이 같은 id가 될 수 있다. 이 경우는 처리하지 않았다
(같은 id가 되면 4.9에 따라 `#2`가 붙는다).

### 4.12 키를 글자로 적는 방법

파일에는 `KeyStroke.toString()`으로 쓰고 `KeyStroke.getKeyStroke(String)`으로 읽는다(예: `ctrl pressed L`).
원본 `MenuSite.mapNames()`가 단축키 표기에 쓰는 것과 같은 Swing 표기다.
직접 형식을 만들지 않았으므로 파싱 코드가 없다. 썼다가 다시 읽을 수 없는 키는
`KeymapStore.canStore()`로 걸러서 `KeyCaptureDialog`가 받지 않는다.

화면에는 `KeyText.describe()`로 `Ctrl+L`처럼 보여 준다. 표시용일 뿐 저장에는 쓰지 않는다.

### 4.13 매크로가 단계를 Command 객체로 보관 vs id로 보관

`MacroCommand`는 `List<Command>`를 갖는다(Composite 그대로). id로 보관하고 실행할 때 레지스트리에서 찾는
방법은 저장하기 쉽지만, 매크로 A가 B를, B가 A를 가리키는 순환이 가능해져 실행 중 검사가 필요하다.

저장은 따로 푼다. `UserMacros`가 매크로마다 "정의"(이름 → 단계 id 목록)를 기억하고, `MacroStore`가 그것을
파일에 쓴다. 다시 시작하면 `UserMacros.restore()`가 정의로부터 `MacroCommand`를 다시 만든다.
매크로가 다른 매크로를 단계로 쓸 수 있으므로, 단계가 모두 등록된 매크로부터 만들고 더 만들 수 있는 것이
없을 때까지 되풀이한다. 손으로 고친 파일에 순환이 있으면 둘 다 만들어지지 않을 뿐 무한히 돌지 않는다
(`UserMacrosTest.restoreDoesNotLoopOnMacrosThatReferToEachOther`).

### 4.14 매크로 정의 파일 형식

`매크로 이름 = 단계 id들`이고 단계 id는 줄바꿈 문자로 구분한다(파일에는 `\n`으로 적힌다).
쉼표나 `|`는 메뉴 이름에 들어갈 수 있지만 줄바꿈은 들어갈 수 없어서 구분자로 골랐다.

### 4.15 기본 단축키 없음

기본 키를 정하지 않기로 했다(작업 중 결정). 키맵 파일이 없으면 빈 키맵으로 시작한다.
"파일이 없으면 기본값"이라는 구조는 남아 있다. `ShortcutFeature.defaultKeymap()`이 지금은 빈 키맵을 돌려주고,
기본 키를 정하고 싶으면 그 메서드에서 `bind()`를 부르면 된다.

### 4.16 설정 파일 위치와 저장 시점

사용자 홈의 `.life-game` 폴더에 둔다. 작업 폴더에 두면 저장소에 실수로 커밋될 수 있다.
`ShortcutFeature`의 두 번째 생성자로 폴더를 바꿀 수 있다.

키나 매크로가 바뀌면 곧바로 저장한다(저장 버튼 없음). `ShortcutFeature`가 `Keymap`과 `UserMacros`를 관찰한다.

## 5. 원본에서 수정한 파일과 줄

수정한 원본 파일은 두 개다. 두 파일 모두 줄을 추가만 했고, 기존 줄을 바꾸거나 지우지 않았다.
저작권 주석과 `@include /etc/license.txt`도 그대로다. 추가한 곳에는 `[shortcut]`으로 시작하는 한 줄 주석이 있다.
(원본 파일이 ASCII라서 이 주석들은 영어로 썼다.)

### `src/com/holub/ui/MenuSite.java` (21줄 추가)

| 줄 | 내용 | 이유 |
|---|---|---|
| 346–349 | `addLine()` 끝에서 등록된 관찰자에게 (메뉴 지정자, 이름, `JMenuItem`, 리스너)를 알린다. 구분선(`"-"`)은 리스너가 없어 건너뛴다 | 리스너는 `addLine` 안에서 `JMenuItem`에만 저장되므로, 호출하는 쪽을 고치지 않고 모든 메뉴 항목을 수집하려면 여기서 알려야 한다 |
| 352–356 | 중첩 인터페이스 `LineListener` | 알림을 받을 쪽의 형태를 정한다(4.3) |
| 358–360 | 관찰자 목록 `lineListeners` | 등록된 관찰자를 기억한다 |
| 362–366 | `addLineListener()` | 관찰자를 등록한다 |

### `src/com/holub/life/Life.java` (7줄 추가)

| 줄 | 내용 | 이유 |
|---|---|---|
| 6 | `import com.holub.life.shortcut.ShortcutFeature;` | 아래 두 곳에서 쓴다 |
| 30–31 | `MenuSite.establish()` 직후에 `new ShortcutFeature(this)` | 메뉴 수집은 `Universe.instance()`가 메뉴를 등록하기 전에 시작해야 한다 |
| 37–38 | `Universe`를 추가한 뒤에 `shortcuts.install()` | 키맵을 불러오고 키 바인딩을 건다. Shortcuts 메뉴가 Grid, Go 오른쪽에 오려면 그 뒤에 추가해야 한다 |

### 수정하지 않은 것

`Universe.java`, `Clock.java`, `Cell.java`, `Neighborhood.java`, `Resident.java`를 포함해 위 두 파일 외의
원본 소스는 수정하지 않았다(`git diff main`으로 확인).

### 원본이 아닌 새 파일 (패키지 밖)

`.gitignore`, `.gitattributes`, `scripts/test.sh`, `scripts/test.ps1`, `test/` 아래의 테스트, 이 문서.

## 6. 테스트

### 6.1 JUnit (76개, headless)

| 테스트 클래스 | 개수 | 확인하는 것 |
|---|---|---|
| `CommandRegistryTest` | 8 | 등록, 조회, 등록 순서, 없는 id(빈 Optional), 중복 id 거부, null 거부, 해제, 알림 |
| `ActionListenerCommandTest` | 7 | 리스너가 정확히 한 번 호출됨, 이벤트 source, actionCommand, 비활성 항목은 실행 안 함 |
| `MenuCommandCollectorTest` | 5 | id 규칙, 자동 등록, 수집된 명령 실행 시 리스너 한 번 호출, Clock식 공유 리스너, 중복 항목 `#2` |
| `KeymapTest` | 11 | 지정, 조회(양방향), 해제, 충돌 거부와 이유, 거부 후 키맵 불변, 충돌 검사, 재지정, 알림, equals |
| `KeyBinderTest` | 8 | `CommandAction`이 명령을 한 번 실행, 지정된 키로 명령 실행, 변경/해제/재지정 반영, 나중에 등록된 명령 |
| `KeymapStoreTest` | 8 | 저장 후 불러온 키맵이 원래와 같음(스트림, 파일), 특수 문자 id, 파일 없으면 기본값, 잘못된 줄과 겹치는 줄 |
| `MacroCommandTest` | 7 | 실행 순서, 빈 매크로, 같은 단계 반복, 매크로 안의 매크로, 목록 복사, 레지스트리 등록 후 키로 실행 |
| `UserMacrosTest` | 11 | 생성과 등록, 이름/단계 검사, 삭제, 사용 중인 매크로 삭제 거부, 복원 순서, 없는 단계, 순환 |
| `MacroStoreTest` | 5 | 저장 후 불러온 정의가 원래와 같음, 빈 매크로, 파일 없음, 저장 → 재시작 흐름 |
| `ui.ShortcutTableModelTest` | 6 | 표의 줄과 칸, 키맵/레지스트리 변경 반영, 등록되지 않은 명령의 키, `KeyText` |

`KeyBinderTest`와 `MacroCommandTest`의 키 입력은 창 없이 `JPanel`에 바인딩을 설치하고,
Swing이 하는 순서(InputMap에서 이름표 → ActionMap에서 Action → `actionPerformed`)를 직접 따라 한다.

`MenuSite` 자체(훅이 실제로 `addLine`에서 불리는지)는 JUnit으로 확인하지 않는다.
`MenuSite.establish()`가 `JFrame`을 요구하는데 headless에서는 `JFrame`을 만들 수 없기 때문이다.
이 부분은 6.2에서 확인했다.

### 6.2 실제 창으로 확인한 것 (저장소에 넣지 않은 일회용 점검 프로그램)

Life 창을 실제로 띄우고 프로그램으로 조작해 확인했다. 설정 파일이 있는 실행에서 98개 항목,
설정 파일이 없는 첫 실행에서 18개 항목이고 모두 통과했다.
조작 방법: 마우스는 `Universe` 패널에 `MouseEvent`를 직접 전달, 메뉴는 `JMenuItem.doClick()`,
키는 `SwingUtilities.processKeyBindings()` 호출(Swing이 `WHEN_IN_FOCUSED_WINDOW` 바인딩을 처리하는 진입점),
칸의 상태는 `Universe.paint()`를 이미지에 그려 색으로 읽었다. 키맵과 매크로 파일은 임시 폴더를 썼다.

원본 기능:

- 메뉴바에 Grid(Clear, Load, Store, Exit)와 Go(Halt, Tick, Agonizing, Slow, Medium, Fast)가 있다.
- 칸 클릭으로 켜고 끄기.
- 메뉴 Clear, 메뉴 Tick(블링커가 세로로 바뀜), 속도 4종 각각 실행 후 Halt로 정지.
- 메뉴 Store로 파일 저장 → Clear → Load로 같은 무늬 복원(파일 선택 창은 프로그램으로 조작).
- 메뉴 Exit로 종료(종료 코드 0).

단축키 기능:

- 파일에서 읽은 키로 Tick, Clear, Fast, Halt, Load, Store, 매크로가 실행된다. 지정하지 않은 키는 아무 일도 하지 않는다.
- Shortcuts 창: 표 내용, 키 지정(Shift만 누르면 계속 기다림), 취소, 충돌 거부 메시지, 재지정, 해제, 해제한 키 재사용, Run.
- 매크로 창: 단계 추가/위로/아래로/삭제, 단계 없음 거부, 이름 중복 거부, 생성, 매크로에 키 지정, 매크로 삭제(키도 함께 해제).
- 바뀐 내용이 `keymap.properties`와 `macros.properties`에 저장된다.
- 설정 파일이 없는 첫 실행: 키가 하나도 지정되지 않고, 원본 기능이 동작하고, 설정 폴더가 만들어지지 않는다.

### 6.3 손으로 확인한 것

2026-11-20에 Life를 직접 실행해 `Shortcuts > Edit Shortcuts...`에서 키를 지정하고 눌러 보았고, 정상 동작했다.
눌러 본 키와 명령의 목록은 남기지 않았다.

## 7. 다른 팀원 코드와의 관계

2026-11-20 기준 `origin`에는 `main`뿐이고 팀원 브랜치가 없다. `main`에는 원본 소스만 있다.

- 키 입력을 받는 코드(`KeyListener`, `InputMap`, `ActionMap`, `KeyEventDispatcher` 등)는 `src` 전체에 없다.
  따라서 지금은 겹치는 키가 없다.
- 원본 `MenuSite`에는 메뉴 단축키 기능이 이미 있다(649행 `mapNames()`, 672행 `addMapping()`, 732–744행의 `setAccelerator`).
  Life에서는 아무도 부르지 않는다(`MenuSite.Test`에서만 쓴다). 누군가 이 기능을 쓰기 시작하면
  `Keymap`은 그 키를 모르므로 같은 키가 두 곳에 걸릴 수 있다. 이 경우의 동작은 확인하지 않았다.

팀원이 알아야 할 것:

- 메뉴를 `MenuSite.addLine()`으로 추가하면 자동으로 명령이 된다. id는 `메뉴/항목`이다. 추가로 할 일은 없다.
- 메뉴 없이 명령만 등록하는 공개 통로는 없다. `CommandRegistry`는 `ShortcutFeature` 안에 있다.
  필요해지면 `ShortcutFeature`에 등록 메서드를 여는 것이 가장 작은 변경이다.
- 기능이 `KeyListener` 등으로 직접 키를 받으면 `Keymap`의 충돌 검사에 잡히지 않는다.
  그 동작을 메뉴 항목으로도 내놓으면 단축키 창에서 한꺼번에 관리된다.
- `MenuSite.removeMyMenus()`로 메뉴를 지워도 명령은 레지스트리에 남는다(8장).

## 8. 확신이 없는 부분, 직접 확인해야 할 부분

### 확인하지 않음

- **손으로 누른 키 입력의 범위.** 직접 실행해 정상 동작하는 것은 확인했지만(6.3), 어떤 키와 명령까지 눌러 보았는지는 기록이 없다.
  아래 항목들은 그때 따로 확인하지 않은 것으로 보고 그대로 둔다.
- **한글 입력기(IME)가 켜진 상태**에서 글자 키 단축키가 동작하는지.
- **메뉴가 열려 있는 동안** 단축키를 눌렀을 때의 동작.
- **메인 창에 텍스트 입력 칸이 생겼을 때.** 지금은 없다. 팀원이 추가하면 글자 키 단축키와 입력이 어떻게 나뉘는지 확인해야 한다.
- **Java 17 외의 버전에서의 실행.** Temurin 17.0.18로만 실행했다. `javac --release 8`로 컴파일은 되지만 Java 8에서 실행해 보지는 않았다.
- **macOS, Linux.** Windows에서만 실행했다. `scripts/test.sh`도 Windows의 Git Bash에서만 돌려 보았다.
- **저장할 수 없는 키가 실제로 어떤 키인지.** `KeymapStore.canStore()`가 걸러 내지만, 걸러지는 실제 키를 찾아보지는 않았다.
- **Load나 Store가 들어간 매크로.** 파일 선택 창이 단계 순서대로 뜰 것으로 예상하지만 실행해 보지 않았다.
- **`MenuSite.mapNames()`로 메뉴 가속키를 건 경우**와 `Keymap`의 키가 겹칠 때의 동작(7장).

### 모름 (본인이나 팀이 정해야 함)

- 팀의 목표 Java 버전과, 수업에서 요구하는 JUnit 버전(4인지 5인지). 지금은 JUnit 5(콘솔 러너 1.10.2)다.
- 팀 공용 빌드 방식. 지금의 `scripts/test.*`는 이 브랜치에서 임시로 정한 것이다.
- 루트의 `com/**/*.class` 80개의 처리. 2005년 원본 jar의 산출물이 저장소에 그대로 추적되고 있고,
  `MenuSite`와 `Life`를 고쳤으므로 이제 소스와 맞지 않는다. 이 브랜치에서는 건드리지 않았다.

### 알고 있는 한계

- `MenuSite.removeMyMenus()`와 연동하지 않았다. 메뉴가 지워져도 명령은 남고 단축키로 실행된다.
  지금 Life에서 `removeMyMenus()`를 부르는 코드는 없다. 연동하려면 `MenuSite`에 삭제 알림을 더 넣어야 한다.
- 표시 이름은 메뉴 항목이 등록되는 순간의 라벨이다. 나중에 `setText()`로 라벨을 바꿔도 따라가지 않는다.
- 스레드 동기화를 하지 않았다. 레지스트리와 키맵은 창이 뜨기 전의 main 스레드(원본 `Life` 생성자가 도는 곳)와
  그 뒤의 이벤트 스레드에서만 쓴다고 가정한다. 다른 스레드에서 `addLine()`을 부르면 안전하지 않다.
- 단축키로 실행한 명령은 메뉴를 클릭했을 때와 같은 스레드(이벤트 스레드)에서 같은 리스너를 부른다.
  원본의 스레드 구조(Tick은 이벤트 스레드, 자동 진행은 Timer 스레드)를 바꾸지 않았지만,
  원본 구조 자체가 안전한지는 검토하지 않았다.
- 단계 명령이 없어서 만들어지지 않은 매크로는 정의가 파일에 보존되지만 창에는 보이지 않아, 창에서 지울 수 없다.
- 충돌 시 "덮어쓸까요?"를 묻는 기능이 없다. 기존 키를 해제하고 다시 지정해야 한다.
- 매크로를 고치는 기능이 없다. 지우고 다시 만들어야 한다.
- 매크로 실행 중 한 단계에서 예외가 나면 나머지 단계는 실행되지 않는다.
