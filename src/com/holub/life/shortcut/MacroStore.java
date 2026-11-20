package com.holub.life.shortcut;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;

/**
 * 매크로 정의(이름 → 단계 명령 id 목록)를 properties 형식으로 저장하고 불러온다.
 *
 * 한 줄이 "매크로 이름 = 단계 id들" 이고, 단계 id는 줄바꿈 문자로 구분한다. 예:
 *   reset=Go/Halt\nGrid/Clear
 *
 * 구분자로 줄바꿈을 쓰는 이유: 메뉴 이름에는 쉼표나 '|' 같은 글자가 들어갈 수 있지만
 * 줄바꿈은 들어갈 수 없어서, 명령 id와 헷갈릴 일이 없다. 파일에는 Properties가 "\n"으로 적어 준다.
 */
public final class MacroStore {

	private static final String STEP_SEPARATOR = "\n";

	private MacroStore() {
		// 객체를 만들 필요가 없는 클래스다. static 메서드만 있다.
	}

	/** 매크로 정의를 스트림에 쓴다. 스트림은 닫지 않는다. */
	public static void write(Map<String, List<String>> definitions, OutputStream out) throws IOException {
		Properties properties = new Properties();
		for (Map.Entry<String, List<String>> definition : definitions.entrySet())
			properties.setProperty(definition.getKey(), String.join(STEP_SEPARATOR, definition.getValue()));

		properties.store(out, "Life Game macros: macro name = command ids separated by \\n");
	}

	/** 스트림에서 매크로 정의를 읽는다. 스트림은 닫지 않는다. 결과는 이름 순이다. */
	public static Map<String, List<String>> read(InputStream in) throws IOException {
		Properties properties = new Properties();
		properties.load(in);

		Map<String, List<String>> definitions = new LinkedHashMap<String, List<String>>();
		for (String name : new TreeSet<String>(properties.stringPropertyNames())) {
			String steps = properties.getProperty(name);
			if (steps.isEmpty())
				definitions.put(name, new ArrayList<String>()); // 단계가 없는 매크로
			else
				definitions.put(name, new ArrayList<String>(Arrays.asList(steps.split(STEP_SEPARATOR))));
		}
		return definitions;
	}

	/** 매크로 정의를 파일에 저장한다. 폴더가 없으면 만든다. */
	public static void save(Map<String, List<String>> definitions, File file) throws IOException {
		File folder = file.getAbsoluteFile().getParentFile();
		if (folder != null)
			folder.mkdirs();

		OutputStream out = new FileOutputStream(file);
		try {
			write(definitions, out);
		}
		finally {
			out.close();
		}
	}

	/** 파일에서 매크로 정의를 불러온다. 파일이 없으면 빈 맵을 돌려준다(매크로 없음). */
	public static Map<String, List<String>> load(File file) throws IOException {
		if (!file.isFile())
			return new LinkedHashMap<String, List<String>>();

		InputStream in = new FileInputStream(file);
		try {
			return read(in);
		}
		finally {
			in.close();
		}
	}
}
