package com.holub.life.shortcut;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;
import java.util.logging.Logger;

import javax.swing.KeyStroke;

/**
 * 키맵을 properties 형식으로 저장하고 불러온다.
 *
 * 한 줄이 "명령 id = 키" 이다. 예:
 *   Grid/Load=ctrl pressed L
 *   Go/Tick\ (Single\ Step)=pressed SPACE
 *
 * 키를 글자로 바꾸는 방법은 Swing의 것을 그대로 쓴다. KeyStroke.toString()으로 쓰고
 * KeyStroke.getKeyStroke(String)으로 읽는다. 원본 MenuSite.mapNames()가 쓰는 표기와 같다.
 * 띄어쓰기 같은 특수 문자의 이스케이프는 java.util.Properties가 처리한다.
 */
public final class KeymapStore {

	private static final Logger log = Logger.getLogger("com.holub.life.shortcut");

	private KeymapStore() {
		// 객체를 만들 필요가 없는 클래스다. static 메서드만 있다.
	}

	/** 키맵을 스트림에 쓴다. 스트림은 닫지 않는다. */
	public static void write(Keymap keymap, OutputStream out) throws IOException {
		Properties properties = new Properties();
		for (Map.Entry<KeyStroke, String> binding : keymap.bindings().entrySet())
			properties.setProperty(binding.getValue(), binding.getKey().toString());

		properties.store(out, "Life Game shortcuts: command id = key");
	}

	/**
	 * 스트림에서 키맵을 읽는다. 스트림은 닫지 않는다.
	 * 읽을 수 없는 줄(키 표기가 틀렸거나, 앞줄과 같은 키를 쓰는 줄)은 경고를 남기고 건너뛴다.
	 * 한 줄이 틀렸다고 나머지 단축키까지 버리지 않기 위해서다.
	 */
	public static Keymap read(InputStream in) throws IOException {
		Properties properties = new Properties();
		properties.load(in);

		Keymap keymap = new Keymap();
		// Properties는 순서가 없다. 같은 키를 쓰는 줄이 둘일 때 어느 쪽이 남을지 매번 같도록 id 순으로 읽는다.
		for (String commandId : new TreeSet<String>(properties.stringPropertyNames())) {
			String keyText = properties.getProperty(commandId);
			KeyStroke key = KeyStroke.getKeyStroke(keyText);
			if (key == null) {
				log.warning("Ignored keymap entry \"" + commandId + "\": cannot read key \"" + keyText + "\"");
				continue;
			}
			try {
				keymap.bind(key, commandId);
			}
			catch (KeyConflictException conflict) {
				log.warning("Ignored keymap entry \"" + commandId + "\": " + conflict.getMessage());
			}
		}
		return keymap;
	}

	/** 이 키를 파일에 썼다가 그대로 다시 읽을 수 있는지. (이름이 없는 특수 키는 읽지 못한다) */
	public static boolean canStore(KeyStroke key) {
		return key.equals(KeyStroke.getKeyStroke(key.toString()));
	}

	/** 키맵을 파일에 저장한다. 폴더가 없으면 만든다. */
	public static void save(Keymap keymap, File file) throws IOException {
		File folder = file.getAbsoluteFile().getParentFile();
		if (folder != null)
			folder.mkdirs();

		OutputStream out = new FileOutputStream(file);
		try {
			write(keymap, out);
		}
		finally {
			out.close();
		}
	}

	/** 파일에서 키맵을 불러온다. 파일이 없으면 defaults를 그대로 돌려준다. */
	public static Keymap load(File file, Keymap defaults) throws IOException {
		if (!file.isFile())
			return defaults;

		InputStream in = new FileInputStream(file);
		try {
			return read(in);
		}
		finally {
			in.close();
		}
	}
}
