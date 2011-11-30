package net.jdigi.ui;

import java.awt.TextArea;
import java.text.BreakIterator;

public class OutputTextArea extends TextArea {
	private static final long serialVersionUID = -1443114166911768829L;
	BreakIterator boundary = BreakIterator.getWordInstance();
	int col = 0;
	final static int MARGIN = 8;

	public OutputTextArea(int rows, int cols) {
		super("", rows, cols, TextArea.SCROLLBARS_BOTH);
		setBackground(java.awt.Color.white);
		setEditable(false);
	}

	// You must call init
	public void init() {
		setVisible(true);
	}

	void rubout() {
		try {
			setEditable(true);
			replaceRange("", getCaretPosition() - 1, getCaretPosition());
			setEditable(false);
		} catch (IllegalArgumentException iex) {
			System.err.println("[Unable to rubout]");
		}
	}

	// Single character at a time append.
	// Unfortunately this never does the wrapping.
	public void appendWrapped(char c) {
		// System.err.print(c);
		switch (c) {
		case '\n':
		case '\r':
			newline();
			break;
		case '':
			if (--col < 0) {
				col = 0; // uh oh
			}
			rubout();
			break;
		case ' ':
			if (col > getColumns() - MARGIN) {
				newline();
			} else {
				append(" ");
				c++;
			}
			break;
		default:
			append("" + c);
			col++;
		}
	}

	// unfortunately this never gets called because we add one at a time.
	public void appendWrapped(String s) {
		//System.err.println("appendWrapped " + s);
		boundary.setText(s);
		int start = boundary.first();
		for (int end = boundary.next(); end != BreakIterator.DONE; start = end, end = boundary
				.next()) {
			int len = end - start;
			if (col + len >= getColumns())
				newline();
			col += len;
			append(s.substring(start, end));
		}
	}

	void newline() {
		col = 0;
		append("\n");
	}
}
