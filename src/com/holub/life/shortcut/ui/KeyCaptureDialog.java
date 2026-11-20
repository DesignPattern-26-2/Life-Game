package com.holub.life.shortcut.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;

import com.holub.life.shortcut.KeymapStore;

/**
 * "새 키를 누르세요" 창. 사용자가 누른 키 조합 하나를 받아서 돌려준다.
 *
 * 여기서만 KeyListener를 쓴다. 단축키를 "실행"하는 쪽은 InputMap/ActionMap(KeyBinder)이지만,
 * 이 창은 미리 정해 둔 키가 아니라 "어떤 키든 다음에 눌리는 키"를 알아내야 하므로
 * 키 이벤트를 직접 받아야 한다.
 */
final class KeyCaptureDialog extends JDialog {

	private static final long serialVersionUID = 1L;

	private final JLabel hint = new JLabel("Hold Ctrl / Alt / Shift if you want, then press a key.", SwingConstants.CENTER);
	private final JPanel keyCatcher = new JPanel(new BorderLayout(0, 8));

	private KeyStroke captured; // 취소하면 null로 남는다

	/**
	 * 창을 띄우고 키가 눌릴 때까지 기다린다.
	 *
	 * @return 사용자가 누른 키. Cancel을 누르거나 창을 닫으면 null.
	 */
	static KeyStroke ask(Window owner, String commandName) {
		KeyCaptureDialog dialog = new KeyCaptureDialog(owner, commandName);
		dialog.setVisible(true); // modal이라 창이 닫힐 때까지 여기서 멈춘다
		return dialog.captured;
	}

	private KeyCaptureDialog(Window owner, String commandName) {
		super(owner, "Assign Key", ModalityType.APPLICATION_MODAL);

		keyCatcher.setBorder(BorderFactory.createEmptyBorder(16, 24, 8, 24));
		keyCatcher.add(new JLabel("Press the new key for:  " + commandName, SwingConstants.CENTER), BorderLayout.NORTH);
		keyCatcher.add(hint, BorderLayout.CENTER);

		// 키 입력은 이 패널이 받는다. Tab도 받을 수 있도록 포커스 이동 키 기능을 끈다.
		keyCatcher.setFocusable(true);
		keyCatcher.setFocusTraversalKeysEnabled(false);
		keyCatcher.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				keyPressedInDialog(e);
			}
		});

		// Cancel 버튼이 포커스를 가져가면 Space/Enter가 버튼을 눌러 버리므로 포커스를 받지 않게 한다.
		JButton cancel = new JButton("Cancel");
		cancel.setFocusable(false);
		cancel.addActionListener(e -> dispose());
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
		buttons.add(cancel);

		getContentPane().add(keyCatcher, BorderLayout.CENTER);
		getContentPane().add(buttons, BorderLayout.SOUTH);

		addWindowListener(new WindowAdapter() {
			public void windowOpened(WindowEvent e) {
				keyCatcher.requestFocusInWindow();
			}
		});

		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		pack();
		setLocationRelativeTo(owner);
	}

	private void keyPressedInDialog(KeyEvent e) {
		if (isModifierOnly(e.getKeyCode()))
			return; // Ctrl이나 Shift만 누른 상태다. 진짜 키가 눌릴 때까지 기다린다.

		KeyStroke key = KeyStroke.getKeyStrokeForEvent(e);
		if (!KeymapStore.canStore(key)) {
			hint.setText("That key cannot be saved to the keymap file. Try another key.");
			return;
		}
		captured = key;
		dispose();
	}

	private static boolean isModifierOnly(int keyCode) {
		return keyCode == KeyEvent.VK_SHIFT
				|| keyCode == KeyEvent.VK_CONTROL
				|| keyCode == KeyEvent.VK_ALT
				|| keyCode == KeyEvent.VK_ALT_GRAPH
				|| keyCode == KeyEvent.VK_META
				|| keyCode == KeyEvent.VK_UNDEFINED;
	}
}
