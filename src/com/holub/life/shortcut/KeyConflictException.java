package com.holub.life.shortcut;

import javax.swing.KeyStroke;

/**
 * 이미 다른 명령이 쓰고 있는 키를 지정하려 할 때 던진다.
 * 어떤 키를, 어느 명령이 쓰고 있어서, 어느 명령에 줄 수 없었는지를 담고 있다(거부 이유).
 *
 * checked 예외로 만든 이유: 키를 지정하는 코드가 충돌을 반드시 처리하게 하려고.
 */
public final class KeyConflictException extends Exception {

	private static final long serialVersionUID = 1L;

	private final KeyStroke key;
	private final String ownerId;
	private final String requestedId;

	public KeyConflictException(KeyStroke key, String ownerId, String requestedId) {
		super("Key [" + key + "] is already assigned to \"" + ownerId
				+ "\", so it cannot be assigned to \"" + requestedId + "\".");
		this.key = key;
		this.ownerId = ownerId;
		this.requestedId = requestedId;
	}

	/** 충돌이 난 키 */
	public KeyStroke key() {
		return key;
	}

	/** 그 키를 이미 쓰고 있는 명령의 id */
	public String ownerId() {
		return ownerId;
	}

	/** 그 키를 새로 받으려던 명령의 id */
	public String requestedId() {
		return requestedId;
	}
}
