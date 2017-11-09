package emu;

import chip.Chip;

public class Main extends Thread {
	
	private Chip chip8;
	private ChipFrame chipFrame;
	
	public Main() {
		chip8 = new Chip();
		chip8.init();
		chip8.loadProgram("PONG2");
		chipFrame = new ChipFrame(chip8);
	}
	
	public void run() {
		while (true) {
			chip8.run();
			if(chip8.needsRedraw()) {
				chipFrame.repaint();
				chip8.removeDrawFlag();
			}
			try {
				Thread.sleep(16);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {
		Main main = new Main();
		main.start();
	}

}