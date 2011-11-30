package net.jdigi.ui;

import java.awt.BorderLayout;
import java.awt.Choice;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.text.DecimalFormat;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import net.jdigi.receiver.Controller;
import net.jdigi.receiver.Receiver;

public class JDigi extends JFrame implements Controller {
	private static final long serialVersionUID = 2087100291097214476L;

	private Waterfall waterfall;
	
	private static String FREQUENCY_SUFFIX = " Hz";
	private JLabel frequencyDisplay;

	private int frequency;
	private boolean lsb = true;

	private final static int OUTPUT_TEXT_ROWS = 22;
	private final static int OUTPUT_TEXT_COLUMNS = 80;
	private final static int INPUT_TEXT_ROWS = 7;
	private final static int INPUT_TEXT_COLUMNS = 80;
	
	private JTextArea outputTextArea;
	private JScrollPane outputScrollPane;

	private JTextArea inputTextArea;
	private JScrollPane inputScrollPane;

	private JTextField inputTextField;
	private JPanel topPanel;
	private JPanel centerPanel;
	private JPanel bottomPanel;
	private JPanel buttonRowPanel;

	private DecimalFormat frequencyFormat = new DecimalFormat("#####.######");

	private Receiver receiver = null;

	public JDigi() {
		super("JDigi");

		this.receiver = new Receiver(this);
		
		setBackground(java.awt.Color.white);
		setLayout(new BorderLayout());
		setResizable(true);
		setVisible(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		init();
		pack();
	}

	public static void main(String[] args) {
		JDigi jdigi = new JDigi();
	}

	public void init() {
		// Top Panel: Output, Input, Typein
		topPanel = new JPanel(new BorderLayout());

		outputTextArea = new JTextArea(OUTPUT_TEXT_ROWS, OUTPUT_TEXT_COLUMNS);
		outputTextArea.setEditable(false);
		outputTextArea.setVisible(true);
		outputScrollPane = new JScrollPane(outputTextArea);

		topPanel.add(outputScrollPane, BorderLayout.NORTH);

		inputTextArea = new JTextArea(INPUT_TEXT_ROWS, INPUT_TEXT_COLUMNS);
		inputTextArea.setEditable(false);
		inputTextArea.setVisible(true);
		inputScrollPane = new JScrollPane(inputTextArea);

		topPanel.add(inputScrollPane, BorderLayout.CENTER);

		inputTextField = new JTextField(INPUT_TEXT_COLUMNS);
		inputTextField.setVisible(true);
		inputTextField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
		        String text = inputTextField.getText();
		        inputTextArea.append(text + "\n");
		        inputTextField.setText("");

		        //Make sure the new text is visible, even if there
		        //was a selection in the text area.
		        inputTextArea.setCaretPosition(inputTextArea.getDocument().getLength());
		        
		        // TODO also need to transmit text
			}
		});
		topPanel.add(inputTextField, BorderLayout.SOUTH);
		
		add(topPanel, BorderLayout.NORTH);

		// Center Panel: Menus and Buttons
		String qsoName = "QSO 1";
		centerPanel = new JPanel(new BorderLayout());
		add(centerPanel, BorderLayout.CENTER);

		buttonRowPanel = new JPanel(new FlowLayout());

		Choice bQso = new Choice();
		bQso.add(qsoName);
		bQso.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				Choice c = ((Choice) (e.getItemSelectable()));
				System.err.println("QSO " + c.getItem(c.getSelectedIndex()));
			}
		});
		buttonRowPanel.add(bQso);

		frequencyDisplay = new JLabel("          ");
		buttonRowPanel.add(frequencyDisplay, BorderLayout.EAST);
		frequencyDisplay.setVisible(true);

		JButton bTx = new JButton("TX");
		bTx.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.err.println("TX Button");
			}
		});
		buttonRowPanel.add(bTx, BorderLayout.EAST);

		JButton bRx = new JButton("RX");
		bRx.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.err.println("RX Button");
			}
		});
		buttonRowPanel.add(bRx, BorderLayout.EAST);

		JButton bAbortTx = new JButton("ABORT TX!");
		bAbortTx.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.err.println("ABORT TX!");
			}
		});
		buttonRowPanel.add(bAbortTx, BorderLayout.EAST);

		JButton bClearRx = new JButton("Clear RX");
		bClearRx.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				outputTextArea.setText("");
			}
		});
		buttonRowPanel.add(bClearRx, BorderLayout.EAST);

		JButton bClearTx = new JButton("Clear TX");
		bClearTx.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				inputTextArea.setText("");
			}
		});
		buttonRowPanel.add(bClearTx, BorderLayout.EAST);

		JCheckBox bAfc = new JCheckBox("AFC", true);
		bAfc.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				lsb = ((JCheckBox) (e.getItemSelectable())).isSelected();
				System.err.println("AFC=" + lsb);
			}
		});
		buttonRowPanel.add(bAfc, BorderLayout.EAST);

		JCheckBox bSql = new JCheckBox("SQL", true);
		bSql.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				lsb = ((JCheckBox) (e.getItemSelectable())).isSelected();
				System.err.println("SQL=" + lsb);
			}
		});
		buttonRowPanel.add(bSql, BorderLayout.EAST);

		JCheckBox bLsb = new JCheckBox("LSB", false);
		bLsb.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				lsb = ((JCheckBox) (e.getItemSelectable())).isSelected();
				System.err.println("LSB=" + lsb);
			}
		});
		buttonRowPanel.add(bLsb, BorderLayout.EAST);

		buttonRowPanel.setVisible(true);

		centerPanel.add(buttonRowPanel, BorderLayout.SOUTH);

		// Bottom panel: Waterfall
		bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		add(bottomPanel, BorderLayout.SOUTH);

		waterfall = new Waterfall(this);
		bottomPanel.add(waterfall);
		waterfall.init();
		waterfall.setVisible(true);

		bottomPanel.setVisible(true);

		inputTextField.requestFocus();
	}

	String formatMHz(double mhz) {
		return frequencyFormat.format(mhz);
	}

	boolean carrierFrequencyKnown() {
		return waterfall.carrierfreq != 0;
	}

	double carrierFrequencyMHz(int offsetTicks) {
		double mhz = waterfall.carrierfreq / 1e6;
		return (lsb ? (mhz - offsetAsHz(offsetTicks) / 1e6) : (mhz + offsetAsHz(offsetTicks) / 1e6));
	}

	double carrierFrequencyMHz() {
		return carrierFrequencyMHz(frequency);
	}

	int offsetAsHz(int offsetTicks) {
		return ((int) (offsetTicks * Waterfall.FREQUENCY_TO_PIXELS_SCALE_FACTOR + 0.5));
	}

	int offsetAsHz() {
		return offsetAsHz(frequency);
	}

	public void setFrequency(double f) {
		setFrequency(f, false);
	}

	public void setFrequency(double f, boolean userClick) {
		if (receiver.getModemThread() != null) {
			receiver.getModemThread().setFrequency(frequency);
		}
		frequencyDisplay.setText(frequencyFormat.format(f) + FREQUENCY_SUFFIX);
		waterfall.setFrequency((int)frequency);
		System.out.println("JDigi/setFrequency - setting frequency to " + f + " Hz.");
	}

	public void setSampleRate(int f) {
	}

	public void handleText(int frame, String s) {
		outputTextArea.append(s);
	}

	public void handleSpectrum(int frame, double[] data, int length) {
		// System.out.println("JDigi/handleSpectrum - saw frame = " + frame +
		// ", length = " + length);
		try {
			waterfall.handleSpectrum(frame, data, length);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
