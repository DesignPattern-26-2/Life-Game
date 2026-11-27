package com.holub.life.shortcut;

import javax.swing.KeyStroke;

/**
 * 키맵 파일이 없을 때 쓰는 기본 단축키 표.
 *
 * 키를 고른 기준
 *   - 다른 프로그램에서 같은 일에 쓰는 키를 따른다(Ctrl+Z, Ctrl+S, Space로 재생·정지, 그림 도구의 글자 키).
 *   - 숫자만 누르는 키는 쓰지 않는다. 타임라인에 세대 번호를 치는 칸이 있다. 속도와 규칙은 Ctrl과 함께 누른다.
 *   - 보드를 새로 시작하게 하는 명령(Clear, 규칙·경계 변경)은 실수로 누르기 어렵게 Ctrl 조합으로 둔다.
 *
 * 명령 id는 "메뉴/항목"이다(MenuCommandCollector.commandId). 메뉴 항목의 이름이나 위치를 바꾸면 여기도 고쳐야 한다.
 * 표의 id가 실제 메뉴에 없으면 그 키는 조용히 무시된다(KeyBinder가 등록된 명령의 키만 건다).
 *
 * 이 표는 키맵 파일이 없을 때만 쓰인다. 사용자가 키를 하나라도 바꾸면 전체가 파일에 저장되고 그 뒤로는 파일이 쓰인다.
 */
public final class DefaultKeymap {

	// { 명령 id, 키 }. 키는 KeyStroke.getKeyStroke(String)의 표기다.
	private static final String[][] BINDINGS = {
		// Grid: 다른 프로그램의 File 메뉴에 해당한다
		{ "Grid/Clear",                               "control N" },       // New
		{ "Grid/Load",                                "control O" },       // Open
		{ "Grid/Store",                               "control S" },       // Save
		{ "Grid/Patterns...",                         "control L" },       // Library
		{ "Grid/Exit",                                "control Q" },       // Quit

		// Go: 재생기처럼 Space로 멈췄다 잇는다. 속도는 느린 것부터 1~4, 0은 정지
		{ "Go/Pause or Resume",                       "SPACE" },
		{ "Go/Tick (Single Step)",                    "control T" },
		{ "Go/Halt",                                  "control 0" },
		{ "Go/Agonizing",                             "control 1" },
		{ "Go/Slow",                                  "control 2" },
		{ "Go/Medium",                                "control 3" },
		{ "Go/Fast",                                  "control 4" },

		// Edit
		{ "Edit/Undo",                                "control Z" },
		{ "Edit/Redo",                                "control Y" },
		{ "Edit/Copy",                                "control C" },
		{ "Edit/Cut",                                 "control X" },
		{ "Edit/Paste",                               "control V" },
		{ "Edit/Delete",                              "DELETE" },
		{ "Edit/Deselect",                            "ESCAPE" },
		{ "Edit/Shortcuts...",                        "control K" },       // Keys

		// Tool: 그림 프로그램처럼 글자 하나. V 기본 도구, P 펜, E 지우개, M 영역 선택
		{ "Tool/Click (Toggle)",                      "V" },
		{ "Tool/Pen",                                 "P" },
		{ "Tool/Eraser",                              "E" },
		{ "Tool/Select",                              "M" },

		// Rule: 메뉴에 나오는 순서대로 Ctrl+Shift+1~6
		{ "Rule/Conway (B3/S23)",                     "control shift 1" },
		{ "Rule/HighLife (B36/S23)",                  "control shift 2" },
		{ "Rule/Seeds (B2/S)",                        "control shift 3" },
		{ "Rule/Day & Night (B3678/S34678)",          "control shift 4" },
		{ "Rule/Life without Death (B3/S012345678)",  "control shift 5" },
		{ "Rule/Maze (B3/S12345)",                    "control shift 6" },
		{ "Rule/Custom...",                           "control R" },

		// Boundary
		{ "Boundary/Wall",                            "control shift W" },
		{ "Boundary/Torus",                           "control shift T" },
	};

	private DefaultKeymap() {
		// 객체를 만들 필요가 없는 클래스다. static 메서드만 있다.
	}

	/** 표에 있는 지정 개수. */
	public static int size() {
		return BINDINGS.length;
	}

	/**
	 * 기본 단축키가 지정된 새 키맵. 부를 때마다 새로 만들므로 받은 쪽이 마음대로 고쳐도 된다.
	 *
	 * @throws IllegalStateException 표가 잘못됐을 때(읽을 수 없는 키 표기, 두 명령이 같은 키). 테스트가 잡는다.
	 */
	public static Keymap create() {
		Keymap keymap = new Keymap();
		for (String[] binding : BINDINGS) {
			String commandId = binding[0];
			KeyStroke key = KeyStroke.getKeyStroke(binding[1]);
			if (key == null)
				throw new IllegalStateException("Default shortcut for " + commandId + ": cannot read key \"" + binding[1] + "\"");
			try {
				keymap.bind(key, commandId);
			}
			catch (KeyConflictException conflict) {
				throw new IllegalStateException("Default shortcut for " + commandId + ": " + conflict.getMessage());
			}
		}
		return keymap;
	}
}
