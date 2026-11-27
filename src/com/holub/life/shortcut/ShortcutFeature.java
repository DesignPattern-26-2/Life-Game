package com.holub.life.shortcut;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import com.holub.life.shortcut.ui.ShortcutDialog;
import com.holub.ui.MenuSite;

/**
 * 단축키 기능의 부품을 만들고 서로 연결하는 곳. Life.java는 이 클래스만 알면 된다.
 *
 * 두 단계로 나뉜다.
 *   1. 생성자  : 메뉴 수집을 시작한다. 다른 메뉴가 등록되기 "전"에 불러야 한다.
 *   2. install : 저장된 매크로와 키맵을 불러오고, 키 바인딩을 걸고, Edit 메뉴에 "Shortcuts..." 항목을 추가한다.
 *                다른 메뉴가 등록된 "뒤"에 부른다(그래야 이 항목이 Edit 메뉴의 맨 아래에 온다).
 *
 * 레지스트리, 키맵 같은 부품은 Singleton이 아니라 여기서 new로 만들어 필요한 곳에 넘겨준다.
 */
public final class ShortcutFeature {

	private static final Logger log = Logger.getLogger("com.holub.life.shortcut");

	/** 단축키 창을 여는 메뉴 항목의 위치. 명령 id는 "Edit/Shortcuts..."가 된다. */
	static final String MENU = "Edit";
	static final String ITEM = "Shortcuts...";

	private final JFrame frame;
	private final File keymapFile;
	private final File macroFile;
	private final Runnable pauseGame;

	private final CommandRegistry registry = new CommandRegistry();
	private final UserMacros macros = new UserMacros(registry);
	private Keymap keymap;

	/**
	 * 설정 파일을 사용자 홈 폴더의 ".life-game" 폴더에 둔다.
	 *
	 * @param pauseGame 단축키 창을 열기 전에 부를 동작. 게임의 진행을 멈춘다.
	 */
	public ShortcutFeature(JFrame frame, Runnable pauseGame) {
		this(frame, new File(System.getProperty("user.home"), ".life-game"), pauseGame);
	}

	/**
	 * @param configFolder keymap.properties와 macros.properties를 둘 폴더
	 * @param pauseGame    단축키 창을 열기 전에 부를 동작. 이 패키지는 시계를 모르므로
	 *                     멈추는 방법은 만드는 쪽(Life)이 넘겨준다.
	 */
	public ShortcutFeature(JFrame frame, File configFolder, Runnable pauseGame) {
		if (pauseGame == null)
			throw new IllegalArgumentException("pauseGame must not be null");

		this.frame = frame;
		this.pauseGame = pauseGame;
		this.keymapFile = new File(configFolder, "keymap.properties");
		this.macroFile = new File(configFolder, "macros.properties");

		// 지금부터 MenuSite.addLine()으로 추가되는 모든 메뉴 항목이 명령으로 수집된다.
		MenuSite.addLineListener(new MenuCommandCollector(registry));
	}

	/** 저장된 매크로와 키맵을 불러오고, 창의 루트 페인에 키 바인딩을 건다. */
	public void install() {
		restoreMacros();
		keymap = loadKeymap();

		new KeyBinder(frame.getRootPane(), registry, keymap).install();

		// 키맵이나 매크로가 바뀌면 곧바로 파일에 저장한다.
		keymap.addObserver(this::saveKeymap);
		macros.addObserver(this::saveMacros);

		// Edit 메뉴 맨 아래의 "Shortcuts...". 항목이 하나뿐인 메뉴를 따로 두지 않으려고 Edit에 넣었다.
		// 이 항목도 addLine()을 거치므로 명령으로 수집되어 단축키를 받을 수 있다.
		// 창은 한 번만 만들어 다시 쓴다(열 때마다 만들면 표의 관찰자가 계속 쌓인다).
		final ShortcutDialog dialog = new ShortcutDialog(frame, registry, keymap, macros);
		MenuSite.addLine(this, MENU, "-", null);
		MenuSite.addLine(this, MENU, ITEM, e -> {
			pauseGame.run(); // 팀 규칙: 새 창이 열리면 진행을 멈춘다
			dialog.setLocationRelativeTo(frame);
			dialog.setVisible(true);
		});
	}

	/**
	 * 키맵 파일이 없을 때 쓰는 기본 단축키.
	 * 지금은 일부러 비워 두었다(구조만 제공). 기본 키를 정하려면 여기서 bind()를 부르면 된다.
	 */
	private static Keymap defaultKeymap() {
		return new Keymap();
	}

	private Keymap loadKeymap() {
		try {
			return KeymapStore.load(keymapFile, defaultKeymap());
		}
		catch (IOException e) {
			log.warning("Could not read " + keymapFile + " (" + e.getMessage() + "); using default shortcuts");
			return defaultKeymap();
		}
	}

	private void restoreMacros() {
		try {
			List<String> notBuilt = macros.restore(MacroStore.load(macroFile));
			if (!notBuilt.isEmpty())
				log.warning("Macros skipped because a step command is missing: " + notBuilt);
		}
		catch (IOException e) {
			log.warning("Could not read " + macroFile + " (" + e.getMessage() + "); starting without macros");
		}
	}

	private void saveKeymap() {
		try {
			KeymapStore.save(keymap, keymapFile);
		}
		catch (IOException e) {
			warnNotSaved(keymapFile, e);
		}
	}

	private void saveMacros() {
		try {
			MacroStore.save(macros.definitions(), macroFile);
		}
		catch (IOException e) {
			warnNotSaved(macroFile, e);
		}
	}

	private void warnNotSaved(File file, IOException e) {
		JOptionPane.showMessageDialog(frame,
				"Could not save " + file + "\n" + e.getMessage(),
				"Shortcuts", JOptionPane.WARNING_MESSAGE);
	}
}
