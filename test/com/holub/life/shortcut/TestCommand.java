package com.holub.life.shortcut;

import java.util.List;

/**
 * 테스트용 명령. 실행될 때마다 자기 id를 기록지(log)에 적는다.
 * 여러 명령이 같은 기록지를 쓰면 실행 순서를 확인할 수 있다.
 */
final class TestCommand implements Command {

	private final String id;
	private final List<String> log;

	TestCommand(String id, List<String> log) {
		this.id = id;
		this.log = log;
	}

	public String id() {
		return id;
	}

	public String displayName() {
		return "Test " + id;
	}

	public void execute() {
		log.add(id);
	}
}
