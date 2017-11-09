package chip;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Random;

public class Chip {

	private char[] memory;
	private char[] V;
	private char I;
	private char pc;

	private char stack[];
	private int stackPointer;

	private int delay_timer;
	private int sound_timer;

	private byte[] keys;
	private byte[] display;

	private boolean needRedraw;

	public void init() {
		memory = new char[4096];
		V = new char[16];
		I = 0x0;
		pc = 0x200;

		stack = new char[16];
		stackPointer = 0;

		delay_timer = 0;
		sound_timer = 0;

		keys = new byte[16];

		display = new byte[64 * 32];

		needRedraw = false;
		loadFont();
	}

	public void run() {
		char opcode = (char) ((memory[pc] << 8) | memory[pc + 1]);
		System.out.println(Integer.toHexString(opcode).toUpperCase());
		switch (opcode & 0xF000) {
		// http://devernay.free.fr/hacks/chip8/C8TECH10.HTM#3.1
		case 0x0000: {
			switch (opcode & 0x00FF) {
			case 0x00EE: { // 00EE - RET - Return from a subroutine
				stackPointer--;
				pc = (char) (stack[stackPointer] + 2);
				break;
			}
			case 0x00E0: { // 00E0 - CLS - Clear the display
				for (int i = 0; i < display.length; i++) {
					display[i] = 0;
				}
				pc += 2;
				needRedraw = true;
				break;
			}
			default: {
				System.err.println("Unspported opcode");
				System.exit(0);
				break;
			}
			}
		}
		case 0x1000: { // 1NNN - JP addr - Jump to location NNN
			int nnn = opcode & 0x0FFF;
			pc = (char) nnn;
			break;
		}

		case 0x2000: { // 2NNN - CALL addr - Call subroutine at NNN
			stack[stackPointer] = pc;
			stackPointer++;
			pc = (char) (opcode & 0x0FFF);
			break;
		}

		case 0x3000: { // 3xkk - SE Vx, byte - Skip next instruction if Vx = kk
			int x = (opcode & 0x0F00) >> 8;
			int kk = opcode & 0x00FF;
			if (V[x] == kk) {
				pc += 4;
			} else {
				pc += 2;
			}
			break;
		}
		case 0x4000: { // 4xkk - SNE Vx, byte - Skip next instruction if Vx !=
						// kk
			int x = (opcode & 0x0F00) >> 8;
			int kk = opcode & 0x00FF;
			if (V[x] != kk) {
				pc += 4;
			} else {
				pc += 2;
			}
			break;
		}
			 case 0x5000: { // 5xy0 - SE Vx, Vy - Skip next instruction if Vx
//			 = Vy
			 int x = (opcode & 0x0F00) >> 8;
			 int y = opcode & 0x00F0;
			 if (V[x] == V[y]) {
			 pc += 4;
			 } else {
			 pc += 2;
			 }
			 break;
			 }

		case 0x6000: { // 6xkk - LD Vx, byte - Set Vx = kk
			int x = (opcode & 0x0F00) >> 8;
			int kk = opcode & 0x00FF;
			V[x] = (char) kk;
			pc += 2;
			break;
		}

		case 0x7000: { // 7xkk - ADD Vx, byte - Set Vx = Vx + kk
			int x = (opcode & 0x0F00) >> 8;
			int kk = opcode & 0x00FF;
			V[x] = (char) ((V[x] + kk) & 0xFF);
			pc += 2;
			break;
		}

		case 0x8000: {
			switch (opcode & 0x000F) {
			case 0x0000: { // 8xy0 - LD Vx, Vy - Set Vx = Vy
				int x = (opcode & 0x0F00) >> 8;
				int y = (opcode & 0x00F0) >> 4;
				V[x] = V[y];
				pc += 2;
				break;
			}
				 case 0x0001: { // 8xy1 - OR Vx, Vy - Set Vx = Vx OR Vy
				 int x = (opcode & 0x0F00) >> 8;
				 int y = (opcode & 0x00F0) >> 4;
				 V[x] = (char) (V[x] | V[y]);
				 pc += 2;
				 break;
				 }
			case 0x0002: { // 8xy2 - AND Vx, Vy - Set Vx = Vx AND Vy
				int x = (opcode & 0x0F00) >> 8;
				int y = (opcode & 0x00F0) >> 4;
				V[x] = (char) (V[x] & V[y]);
				pc += 2;
				break;
			}
				 case 0x0003: { // 8xy3 - XOR Vx, Vy - Set Vx = Vx XOR Vy
				 int x = (opcode & 0x0F00) >> 8;
				 int y = (opcode & 0x00F0) >> 4;
				 V[x] = (char) (V[x] ^ V[y]);
				 }
			case 0x0004: { // 8xy4 - ADD Vx, Vy - Set Vx = Vx + Vy, set VF =
							// carry
				int x = (opcode & 0x0F00) >> 8;
				int y = (opcode & 0x00F0) >> 4;
				if (V[y] > 0xFF - V[x]) {
					V[0xF] = 1;
				} else {
					V[0xF] = 0;
				}
				V[x] = (char) ((V[x] + V[y]) & 0xFF);
				pc += 2;
				break;
			}
			case 0x0005: { // 8xy5 - SUB Vx, Vy - Set Vx = Vx - Vy, set VF = NOT
							// borrow
				int x = (opcode & 0x0F00) >> 8;
				int y = (opcode & 0x00F0) >> 4;
				if (V[x] > V[y]) {
					V[0xF] = 1;
				} else {
					V[0xF] = 0;
				}
				V[x] = (char) ((V[x] - V[y]) & 0xFF);
				pc += 2;
				break;
			}
			case 0x0006: { // 8xy6 - SHR Vx {, Vy} - Set Vx = Vx SHR 1
				int x = (opcode & 0x0F00) >> 8;
				V[x] = (char) (V[x] << 1);
				pc += 2;
				break;
			}
			case 0x0007: { // 8xy7 - SUBN Vx, Vy - Set Vx = Vy - Vx, set
				// VF =
				// NOT borrow
				int x = (opcode & 0x0F00) >> 8;
				int y = (opcode & 0x00F0) >> 4;
				if (V[x] > V[y]) {
					V[0xF] = 1;
				} else {
					V[0xF] = 0;
				}
				V[x] = (char) ((V[y] - V[x]) & 0xFF);
				pc += 2;
				break;
			}
			case 0x000E: { // 8xyE - SHL Vx, {, Vy} - Set Vx = Vx SHL 1
				int x = (opcode & 0x0F00) >> 8;
				V[x] = (char) (V[x] >> 1);
				pc += 2;
				break;
			}
			default: {
				System.err.println("Unspported opcode");
				System.exit(0);
				break;
			}
			}
		}
		case 0x9000: { // 9xy0 - SNE Vx, Vy - Skip next instruction if Vx
			// != Vy
			int x = (opcode & 0x0F00) >> 8;
			int y = (opcode & 0x00F0) >> 4;
			if (V[x] != V[y]) {
				pc += 4;
			} else {
				pc += 2;
			}
			break;
		}

		case 0xA000: { // ANNN - LD I, addr - Set I = NNN
			int nnn = opcode & 0x0FFF;
			I = (char) nnn;
			pc += 2;
			break;
		}

		case 0xB000: { // BNNN - JP V0, addr - Jump to location NNN + V0
			int nnn = opcode & 0x0FFF;
			pc = (char) (nnn + I);
			break;
		}
		case 0xC000: { // Cxkk - RND Vx, byte - Set Vx = random byte AND kk
			int x = (opcode & 0x0F00) >> 8;
			int kk = opcode & 0x00FF;
			int randy = new Random().nextInt(255);
			V[x] = (char) (randy & kk);
			pc += 2;
			break;
		}

		case 0xD000: { // DXYN - Draw sprite (Vx,Vy) size (8, n) - located at I
			int x = V[(opcode & 0x0F00) >> 8];
			int y = V[(opcode & 0x00F0) >> 4];
			int height = opcode & 0x000F;
			V[0xF] = 0;
			for (int _y = 0; _y < height; _y++) {
				int line = memory[I + _y];
				for (int _x = 0; _x < 8; _x++) {
					int px = line & (0x80 >> _x);
					if (px != 0) {
						int totalX = x + _x;
						int totalY = y + _y;
						totalX = totalX % 64;
						totalY = totalY % 32;
						int index = totalY * 64 + totalX;
						if (display[index] == 1) {
							V[0xF] = 1;
						}
						display[index] ^= 1;
					}
				}
			}
			pc += 2;
			needRedraw = true;
			break;
		}

		case 0xE000: {
			switch (opcode & 0x00FF) {
			case 0x009E: { // Ex9E - SKP Vx - Skip next instruction if key with
							// the value of Vx is pressed
				int x = (opcode & 0x0F00) >> 8;
				int key = V[x];
				if (keys[key] == 1) {
					pc += 4;
				} else {
					pc += 2;
				}
				break;
			}
			case 0x00A1: { // Ex9E - SKP Vx - Skip next instruction if key with
							// the value of Vx is pressed
				int x = (opcode & 0x0F00) >> 8;
				int key = V[x];
				if (keys[key] == 0) {
					pc += 4;
				} else {
					pc += 2;
				}
				break;
			}
			}
		}
			// case 0xF000:

			switch (opcode & 0x00FF) {

			case 0x0007: { // FX07: Set VX to the value of delay_timer
				int x = (opcode & 0x0F00) >> 8;
				V[x] = (char) delay_timer;
				pc += 2;
				System.out.println("V[" + x + "] has been set to " + delay_timer);
				break;
			}

			case 0x0015: { // FX15: Set delay timer to V[x]
				int x = (opcode & 0x0F00) >> 8;
				delay_timer = V[x];
				pc += 2;
				System.out.println("Set delay_timer to V[" + x + "] = " + (int) V[x]);
				break;
			}

			case 0x0018: { // FX18: Set the sound timer to V[x]
				int x = (opcode & 0x0F00) >> 8;
				sound_timer = V[x];
				pc += 2;
				break;
			}

			case 0x001E: { // FX1E: Adds VX to I
				int x = (opcode & 0x0F00) >> 8;
				I = (char) (I + V[x]);
				System.out.println("Adding V[" + x + "] = " + (int) V[x] + " to I");
				pc += 2;
				break;
			}

			case 0x0029: { // FX29: Sets I to the location of the sprite for the
							// character VX (Fontset)
				int x = (opcode & 0x0F00) >> 8;
				int character = V[x];
				I = (char) (0x050 + (character * 5));
				System.out.println("Setting I to Character V[" + x + "] = " + (int) V[x] + " Offset to 0x"
						+ Integer.toHexString(I).toUpperCase());
				pc += 2;
				break;
			}

			case 0x0033: { // FX33 Store a binary-coded decimal value VX in I, I
							// + 1 and I + 2
				int x = (opcode & 0x0F00) >> 8;
				int value = V[x];
				int hundreds = (value - (value % 100)) / 100;
				value -= hundreds * 100;
				int tens = (value - (value % 10)) / 10;
				value -= tens * 10;
				memory[I] = (char) hundreds;
				memory[I + 1] = (char) tens;
				memory[I + 2] = (char) value;
				System.out.println("Storing Binary-Coded Decimal V[" + x + "] = " + (int) (V[(opcode & 0x0F00) >> 8])
						+ " as { " + hundreds + ", " + tens + ", " + value + "}");
				pc += 2;
				break;
			}

			case 0x0065: { // FX65 Filss V0 to VX with values from I
				int x = (opcode & 0x0F00) >> 8;
				for (int i = 0; i <= x; i++) {
					V[i] = memory[I + i];
				}
				System.out.println("Setting V[0] to V[" + x + "] to the values of memory[0x"
						+ Integer.toHexString(I & 0xFFFF).toUpperCase() + "]");
				I = (char) (I + x + 1);
				pc += 2;
				break;
			}

			default:
				System.err.println("Unsupported Opcode!");
				System.exit(0);
			}
			break;

		default:
			System.err.println("Unsupported Opcode!");
			System.exit(0);
		}
	}

	public byte[] getDisplay() {
		return display;
	}

	public boolean needsRedraw() {
		return needRedraw;
	}

	public void removeDrawFlag() {
		needRedraw = false;
	}

	public void loadProgram(String file) {
		try {
			DataInputStream input = new DataInputStream(new FileInputStream(new File(file)));

			int index = 0;
			while (input.available() > 0) {
				memory[0x200 + index] = (char) (input.readByte() & 0xFF);
				index++;
			}
			input.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(0);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	public void loadFont() {
		for (int i = 0; i < ChipData.fontSet.length; i++) {
			memory[0x50 + i] = (char) (ChipData.fontSet[i] & 0xFF);
		}
	}

}
