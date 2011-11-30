package net.jdigi.ui;

import java.awt.TextArea;

public class InputTextArea extends TextArea {
	private static final long serialVersionUID = 1940874404715535730L;

	public InputTextArea(int rows, int cols) {
		super("", rows, cols, TextArea.SCROLLBARS_BOTH);
		setBackground(java.awt.Color.white);
		setEditable(false);
	}

	int lastSent = 0;

	public synchronized void clear() {
		lastSent = getCaretPosition();
		setText("");
		lastSent = 0;
	}

	/**
	 * Don't let the user modify text that's already been sent.
	 * 
	 * @param str
	 *            the non-<code>null</code> text to insert
	 * @param pos
	 *            the position at which to insert
	 */
	public void insert(String str, int pos) {
		System.err.println("insert " + str + " " + pos);
		if (pos >= lastSent) {
			super.insert(str, pos);
		} else {
			System.err.println("beep");
		}
	}

	/**
	 * Don't let the user modify text that's already been sent.
	 * 
	 * @param str
	 *            the non-<code>null</code> text to use as the replacement
	 * @param start
	 *            the start position
	 * @param end
	 *            the end position
	 * @see java.awt.TextArea#insert
	 */
	public void replaceRange(String str, int start, int end) {
		if (end >= lastSent) {
			super.replaceRange(str, start, end);
		} else {
			System.err.println("beep");
		}
	}

	/**
	 * Don't let the user position back to text that's already been sent. This
	 * restriction is not optimal since it keeps you from selecting old text,
	 * but until we can change the display of the text to show it has been sent,
	 * it seems valuable.
	 * 
	 * @param position
	 *            the position of the text insertion caret
	 * @exception IllegalArgumentException
	 *                if the value supplied for <code>position</code> is less
	 *                than zero
	 */
	public synchronized void setCaretPosition(int position) {
		System.err.println("setCaretPosition " + position);
		if (position >= lastSent) {
			super.setCaretPosition(position);
		} else {
			System.err.println("beep");
		}
	}
}
