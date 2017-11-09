package emu;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JFrame;

import chip.Chip;

public class ChipFrame extends JFrame {
	
	private static final long serialVersionUID = 9043845148893755180L;
	private ChipPanel panel;
	
	public ChipFrame(Chip c) {
		setPreferredSize(new Dimension(640, 320));
		pack();
		setPreferredSize(new Dimension(640+getInsets().left + getInsets().right, 320 + getInsets().bottom));
		panel = new ChipPanel(c);
		setLayout(new BorderLayout());
		add(panel, BorderLayout.CENTER);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setTitle("AJ Behncke's Chip8");
		pack();
		setVisible(true);
	}

}
