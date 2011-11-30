package net.jdigi.ui;

import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;

public class InputTextLine extends TextField implements ActionListener,
		TextListener {
	private static final long serialVersionUID = -8046939657796207609L;
	boolean hasNewText = false;

	public InputTextLine(int cols) {
		super(cols);
		addActionListener(this);
		addTextListener(this);
	}

	/**
	 * getText is expensive.
	 */
	private synchronized String getNewText() {
		if (hasNewText) {
			hasNewText = false;
			String text = getText();
			if (text != null && text.length() > 0) {
				setText("");
				return text;
			} else {
				return null;
			}
		}
		return null;
	}

	/**
	 * Notice when there has been typing.
	 */
	public synchronized void textValueChanged(TextEvent e) {
		hasNewText = true;
		// if (tx != null) {
		// String text = getNewText();
		// if (text != null)
		// tx.addText(text);
		// }
	}

	/** Handle the text field Return. */
	public synchronized void actionPerformed(ActionEvent e) {
		String text = getNewText();
		// if (tx != null) {
		// if (text == null) text = "";
		// tx.addTextNewline(text);
		// }
	}
}
