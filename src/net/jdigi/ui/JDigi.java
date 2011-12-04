package net.jdigi.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.io.IOException;
import java.text.DecimalFormat;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import net.jdigi.receiver.Controller;
import net.jdigi.receiver.Receiver;
import net.jdigi.transmitter.Transmitter;

public class JDigi extends JFrame implements Controller {
	private static final long serialVersionUID = 2087100291097214476L;

	private static final double DEFAULT_TRANSMIT_FREQUENCY = 1000; // Hz
	private static final double DEFAULT_RECEIVE_FREQUENCY = 1000; // Hz

	private Waterfall waterfall;

	private static String FREQUENCY_SUFFIX = " Hz";

	private JLabel receiveFrequencyLabel;
	private JLabel receiveFrequencyDisplay;

	private JLabel transmitFrequencyLabel;
	private JLabel transmitFrequencyDisplay;

	private int frequency;
	private boolean lsb = true;

	private final static int OUTPUT_TEXT_ROWS = 8;
	private final static int OUTPUT_TEXT_COLUMNS = 80;
	private final static int INPUT_TEXT_ROWS = 8;
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

	private Transmitter transmitter = null;

	public JDigi() {
		super("JDigi");

		this.receiver = new Receiver(this);
		this.transmitter = new Transmitter();

		setBackground(java.awt.Color.white);
		setLayout(new BorderLayout());
		setResizable(true);
		setVisible(true);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		init();
		pack();
		setReceiverFrequency(DEFAULT_RECEIVE_FREQUENCY);
		setTransmitterFrequency(DEFAULT_TRANSMIT_FREQUENCY);
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
		outputScrollPane.setBorder(BorderFactory.createTitledBorder("Received text"));
		outputScrollPane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
			public void adjustmentValueChanged(AdjustmentEvent e) {
				e.getAdjustable().setValue(e.getAdjustable().getMaximum());
			}
		});
		topPanel.add(outputScrollPane, BorderLayout.NORTH);

		inputTextArea = new JTextArea(INPUT_TEXT_ROWS, INPUT_TEXT_COLUMNS);
		inputTextArea.setEditable(false);
		inputTextArea.setVisible(true);
		inputScrollPane = new JScrollPane(inputTextArea);
		inputScrollPane.setBorder(BorderFactory.createTitledBorder("Transmitted text"));
		inputScrollPane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
			public void adjustmentValueChanged(AdjustmentEvent e) {
				e.getAdjustable().setValue(e.getAdjustable().getMaximum());
			}
		});
		topPanel.add(inputScrollPane, BorderLayout.CENTER);

		inputTextField = new JTextField(INPUT_TEXT_COLUMNS);
		inputTextField.setVisible(true);
		inputTextField.setBorder(BorderFactory.createTitledBorder("Input text"));
		inputTextField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String text = inputTextField.getText();
				inputTextArea.append(text + "\n");
				inputTextField.setText("");

				// Make sure the new text is visible, even if there
				// was a selection in the text area.
				inputTextArea.setCaretPosition(inputTextArea.getDocument().getLength());

				// Transmit inputted text
				transmitter.transmit(text);
			}
		});
		topPanel.add(inputTextField, BorderLayout.SOUTH);

		add(topPanel, BorderLayout.NORTH);

		// Center Panel: Menus and Buttons
		centerPanel = new JPanel(new BorderLayout());
		add(centerPanel, BorderLayout.CENTER);

		buttonRowPanel = new JPanel(new BorderLayout());

		JPanel topButtonRowPanel = new JPanel(new FlowLayout());
		buttonRowPanel.add(topButtonRowPanel, BorderLayout.NORTH);
		JLabel helpLabel1 = new JLabel("Shift-Click on waterfall to set the Transmit Frequency");
		topButtonRowPanel.add(helpLabel1);
		JButton clearTransmitButton = new JButton("Clear Transmit");
		clearTransmitButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				inputTextArea.setText("");
			}
		});
		topButtonRowPanel.add(clearTransmitButton);

		transmitFrequencyLabel = new JLabel("Transmit Frequency:");
		topButtonRowPanel.add(transmitFrequencyLabel);
		transmitFrequencyDisplay = new JLabel("          ");
		topButtonRowPanel.add(transmitFrequencyDisplay);

		JPanel bottomButtonRowPanel = new JPanel(new FlowLayout());
		buttonRowPanel.add(bottomButtonRowPanel, BorderLayout.SOUTH);
		JLabel helpLabel2 = new JLabel("Click on waterfall to set the Receive Frequency");
		bottomButtonRowPanel.add(helpLabel2);
		JButton clearReceiveButton = new JButton("Clear Receive");
		clearReceiveButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				outputTextArea.setText("");
			}
		});
		bottomButtonRowPanel.add(clearReceiveButton);

		receiveFrequencyLabel = new JLabel("Receive Frequency:");
		bottomButtonRowPanel.add(receiveFrequencyLabel);
		receiveFrequencyDisplay = new JLabel("          ");
		bottomButtonRowPanel.add(receiveFrequencyDisplay);

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

	public void setReceiverFrequency(double frequency) {
		if (receiver.getModemThread() != null) {
			receiver.getModemThread().setFrequency(frequency);
		}
		receiveFrequencyDisplay.setText(frequencyFormat.format(frequency) + FREQUENCY_SUFFIX);
		waterfall.setReceiveFrequency((int) frequency);
		if (outputTextArea != null) {
			outputTextArea.append("\nNow listening to " + frequency + " Hz.\n");
		}
	}

	public void setTransmitterFrequency(double frequency) {
		if (transmitter != null) {
			transmitter.setFrequency(frequency);
		}
		transmitFrequencyDisplay.setText(frequencyFormat.format(frequency) + FREQUENCY_SUFFIX);
		if (inputTextArea != null) {
			inputTextArea.append("\nNow transmitting on " + frequency + " Hz.\n");
		}
	}

	public void setSampleRate(int f) {
	}

	public void handleText(int frame, String text) {
		if (outputTextArea != null) {
			outputTextArea.append(text);
		}
	}

	public void handleSpectrum(int frame, double[] data, int length) {
		// System.out.println("JDigi/handleSpectrum - saw frame = " + frame +
		// ", length = " + length);
		try {
			if (waterfall != null) {
				waterfall.handleSpectrum(frame, data, length);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
