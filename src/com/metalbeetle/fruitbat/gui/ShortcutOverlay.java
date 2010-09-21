package com.metalbeetle.fruitbat.gui;

import com.metalbeetle.fruitbat.util.Pair;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.DisplayMode;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JWindow;
import javax.swing.KeyStroke;
import static com.metalbeetle.fruitbat.util.Collections.*;

public class ShortcutOverlay implements KeyListener, WindowListener {
	static final long TICK_LENGTH = 50;
	static final int TICKS_UNTIL_OVERLAY = 9;
	static final int KEY_NOT_PRESSED = -1;
	final int keyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
	final Timer t;
	final OverlayTT tt;
	int ticksKeyPressed = KEY_NOT_PRESSED;
	HelpWindow helpWindow;
	Component currentComponent;

	public ShortcutOverlay() {
		t = new Timer("ShortcutOverlay", /*isDaemon*/ true);
		t.schedule(tt = new OverlayTT(), TICK_LENGTH, TICK_LENGTH);
	}

	public void shutdown() {
		t.cancel();
		if (helpWindow != null) {
			helpWindow.setVisible(false);
		}
	}

	public void attachTo(Component c) {
		c.addKeyListener(this);
		if (c instanceof JFrame) {
			((JFrame) c).addWindowListener(this);
		}
		if (c instanceof Container) {
			Container c2 = (Container) c;
			for (int i = 0; i < c2.getComponentCount(); i++) {
				attachTo(c2.getComponent(i));
			}
		}
	}

	public void detachFrom(Component c) {
		c.removeKeyListener(this);
		if (c instanceof JFrame) {
			((JFrame) c).removeWindowListener(this);
		}
		if (c instanceof Container) {
			Container c2 = (Container) c;
			for (int i = 0; i < c2.getComponentCount(); i++) {
				detachFrom(c2.getComponent(i));
			}
		}
	}

	public void keyTyped(KeyEvent e) {}

	public void keyPressed(KeyEvent e) {
		if (e.getKeyChar() == KeyEvent.CHAR_UNDEFINED && e.getModifiers() == keyMask) {
			currentComponent = e.getComponent();
			ticksKeyPressed = 0;
		}
	}

	public void keyReleased(KeyEvent e) {
		ticksKeyPressed = KEY_NOT_PRESSED;
	}

	public void windowOpened(WindowEvent e) {}

	public void windowClosing(WindowEvent e) {
		ticksKeyPressed = KEY_NOT_PRESSED;
	}

	public void windowClosed(WindowEvent e) {
		ticksKeyPressed = KEY_NOT_PRESSED;
	}

	public void windowIconified(WindowEvent e) {
		ticksKeyPressed = KEY_NOT_PRESSED;
	}

	public void windowDeiconified(WindowEvent e) {}

	public void windowActivated(WindowEvent e) {}

	public void windowDeactivated(WindowEvent e) {
		ticksKeyPressed = KEY_NOT_PRESSED;
	}

	class OverlayTT extends TimerTask {
		@Override
		public void run() {
			if (ticksKeyPressed != KEY_NOT_PRESSED && ticksKeyPressed++ == TICKS_UNTIL_OVERLAY) {
				try {
					Component c = currentComponent;
					while (c != null && !(c instanceof JFrame)) {
						c = c.getParent();
					}
					if (c instanceof JFrame && ((JFrame) c).getJMenuBar() != null) {
						DisplayMode dm = GraphicsEnvironment.getLocalGraphicsEnvironment().
								getDefaultScreenDevice().getDisplayMode();
						Robot r = new Robot();
						BufferedImage bg = r.createScreenCapture(
								new Rectangle((dm.getWidth() - 800) / 2, (dm.getHeight() - 600) / 2, 800, 600));
						helpWindow = new HelpWindow(bg, ((JFrame) c).getJMenuBar(),
								(dm.getWidth() - 800) / 2, (dm.getHeight() - 600) / 2, 800, 600);
						helpWindow.setVisible(true);
					}
				} catch (Exception e) {
					// Do nothing! Whee!
				}
			}
			if (ticksKeyPressed == KEY_NOT_PRESSED && helpWindow != null) {
				helpWindow.dispose();
				helpWindow = null;
			}
		}
	}

	static class HelpWindow extends JWindow {
		static final Color OVERLAY = new Color(160, 240, 140, 220); //new Color(31, 31, 31, 220);
		static final Color TEXT_C = Color.BLACK; //new Color(255, 255, 255);
		static final int LINE_H = 40;
		static final int SPACING = 40;
		static final int TEXT_Y_OFFSET = 16;
		static final int HEIGHT_FUDGE = 21;
		static final int SHORTCUT_TO_TEXT = 20;
		static final int ROWS = 12;
		static final float FONT_SIZE = 16f;
		final BufferedImage bg;
		final JMenuBar mb;
		final int width;
		final int height;
		

		HelpWindow(BufferedImage bg, JMenuBar mb, int x, int y, int width, int height) {
			this.bg = bg;
			this.mb = mb;
			this.width = width;
			this.height = height;
			setBounds(x, y, width, height);
			setAlwaysOnTop(true);
			setFocusable(false);
			// Prevents window shadow on OS X
			getRootPane().putClientProperty("Window.shadow", Boolean.FALSE);
		}

		@Override
		public void paint(Graphics g) {
			g.drawImage(bg, 0, 0, this);
			g.setFont(new JLabel().getFont().deriveFont(FONT_SIZE));
			ArrayList<Pair<String, String>> items = new ArrayList<Pair<String, String>>();
			int maxShortcutWidth = 0;
			int maxTextWidth = 0;
			FontMetrics fm = g.getFontMetrics();
			for (int i = 0; i < mb.getMenuCount(); i++) {
				JMenu menu = mb.getMenu(i);
				for (int j = 0; j < menu.getItemCount(); j++) {
					JMenuItem mi = menu.getItem(j);
					if (mi == null) { continue; }
					KeyStroke ks = mi.getAccelerator();
					if (ks != null && mi.isEnabled()) {
						String shortcut = "";
						int mods = ks.getModifiers();
						if ((mods & InputEvent.CTRL_MASK) != 0) {
							shortcut += "ctrl-";
						}
						if ((mods & InputEvent.ALT_MASK) != 0) {
							shortcut += "alt-";
						}
						if ((mods & InputEvent.SHIFT_MASK) != 0) {
							shortcut += "shift-";
						}
						if ((mods & InputEvent.META_MASK) != 0) {
							shortcut += "cmd-";
						}
						shortcut += keyCodeName(ks.getKeyCode());
						maxShortcutWidth = Math.max(maxShortcutWidth, fm.stringWidth(shortcut));
						maxTextWidth = Math.max(maxTextWidth, fm.stringWidth(mi.getText()));
						items.add(p(shortcut, mi.getText()));
					}
				}
			}
			final int cols = items.size() % ROWS == 0 ? items.size() / ROWS : items.size() / ROWS + 1;
			final int rows = items.size() < ROWS ? items.size() : ROWS;
			final int colWidth = maxShortcutWidth + SHORTCUT_TO_TEXT + maxTextWidth;
			final int totalWidth = cols * colWidth + (cols - 1) * SPACING;
			final int fromLeft = (width - totalWidth) / 2;
			final int fromTop = (height - rows * LINE_H) / 2;

			g.setColor(OVERLAY);
			g.fillRoundRect(
					fromLeft - SPACING,
					fromTop - SPACING,
					totalWidth + SPACING * 2,
					rows * LINE_H + SPACING * 2 - HEIGHT_FUDGE,
					SPACING,
					SPACING);
			g.setColor(TEXT_C);

			int itemIndex = 0;
			for (int column = 0; column < cols; column++) {
				for (int row = 0; row < ROWS && itemIndex < items.size(); row++) {
					Pair<String, String> item = items.get(itemIndex++);
					g.drawString(
							/*shortcut*/ item.a,
							/*x*/        fromLeft + column * (colWidth + SPACING),
							/*y*/        fromTop + row * LINE_H + TEXT_Y_OFFSET
					);
					g.drawString(
							/*text*/ item.b,
							/*x*/    fromLeft + column * (colWidth + SPACING) +
											colWidth - fm.stringWidth(item.b),
							/*y*/    fromTop + row * LINE_H + TEXT_Y_OFFSET
					);
				}
			}

			g.setColor(Color.RED);
		}

		static String keyCodeName(int code) {
			return KEY_CODES.containsKey(code) ? KEY_CODES.get(code) : "[?]";
		}

		static final Map<Integer, String> KEY_CODES;
		static {
			HashMap<Integer, String> kc = new HashMap<Integer, String>();
			kc.put(KeyEvent.VK_ENTER, "Enter");
			kc.put(KeyEvent.VK_BACK_SPACE, "Backspace");
			kc.put(KeyEvent.VK_TAB, "Tab");
			kc.put(KeyEvent.VK_CANCEL, "Cancel");
			kc.put(KeyEvent.VK_CLEAR, "Clear");
			kc.put(KeyEvent.VK_SHIFT, "Shift");
			kc.put(KeyEvent.VK_CONTROL, "Control");
			kc.put(KeyEvent.VK_ALT, "Alt");
			kc.put(KeyEvent.VK_PAUSE, "Pause");
			kc.put(KeyEvent.VK_CAPS_LOCK, "Caps Lock");
			kc.put(KeyEvent.VK_ESCAPE, "Esc");
			kc.put(KeyEvent.VK_SPACE, "Space");
			kc.put(KeyEvent.VK_PAGE_UP, "Page Up");
			kc.put(KeyEvent.VK_PAGE_DOWN, "Page Down");
			kc.put(KeyEvent.VK_END, "End");
			kc.put(KeyEvent.VK_HOME, "Home");
			kc.put(KeyEvent.VK_LEFT, "Left Arrow");
			kc.put(KeyEvent.VK_UP, "Up Arrow");
			kc.put(KeyEvent.VK_RIGHT, "Right Arrow");
			kc.put(KeyEvent.VK_DOWN, "Down Arrow");
			kc.put(KeyEvent.VK_COMMA, ",");
			kc.put(KeyEvent.VK_MINUS, "-");
			kc.put(KeyEvent.VK_PERIOD, ".");
			kc.put(KeyEvent.VK_SLASH, "/");
			kc.put(KeyEvent.VK_0, "0");
			kc.put(KeyEvent.VK_1, "1");
			kc.put(KeyEvent.VK_2, "2");
			kc.put(KeyEvent.VK_3, "3");
			kc.put(KeyEvent.VK_4, "4");
			kc.put(KeyEvent.VK_5, "5");
			kc.put(KeyEvent.VK_6, "6");
			kc.put(KeyEvent.VK_7, "7");
			kc.put(KeyEvent.VK_8, "8");
			kc.put(KeyEvent.VK_9, "9");
			kc.put(KeyEvent.VK_SEMICOLON, ";");
			kc.put(KeyEvent.VK_EQUALS, "=");
			kc.put(KeyEvent.VK_A, "A");
			kc.put(KeyEvent.VK_B, "B");
			kc.put(KeyEvent.VK_C, "C");
			kc.put(KeyEvent.VK_D, "D");
			kc.put(KeyEvent.VK_E, "E");
			kc.put(KeyEvent.VK_F, "F");
			kc.put(KeyEvent.VK_G, "G");
			kc.put(KeyEvent.VK_H, "H");
			kc.put(KeyEvent.VK_I, "I");
			kc.put(KeyEvent.VK_J, "J");
			kc.put(KeyEvent.VK_K, "K");
			kc.put(KeyEvent.VK_L, "L");
			kc.put(KeyEvent.VK_M, "M");
			kc.put(KeyEvent.VK_N, "N");
			kc.put(KeyEvent.VK_O, "O");
			kc.put(KeyEvent.VK_P, "P");
			kc.put(KeyEvent.VK_Q, "Q");
			kc.put(KeyEvent.VK_R, "R");
			kc.put(KeyEvent.VK_S, "S");
			kc.put(KeyEvent.VK_T, "T");
			kc.put(KeyEvent.VK_U, "U");
			kc.put(KeyEvent.VK_V, "V");
			kc.put(KeyEvent.VK_W, "W");
			kc.put(KeyEvent.VK_X, "X");
			kc.put(KeyEvent.VK_Y, "Y");
			kc.put(KeyEvent.VK_Z, "Z");
			kc.put(KeyEvent.VK_OPEN_BRACKET, "(");
			kc.put(KeyEvent.VK_BACK_SLASH, "\\");
			kc.put(KeyEvent.VK_CLOSE_BRACKET, ")");
			kc.put(KeyEvent.VK_NUMPAD0, "Numpad 0");
			kc.put(KeyEvent.VK_NUMPAD1, "Numpad 1");
			kc.put(KeyEvent.VK_NUMPAD2, "Numpad 2");
			kc.put(KeyEvent.VK_NUMPAD3, "Numpad 3");
			kc.put(KeyEvent.VK_NUMPAD4, "Numpad 4");
			kc.put(KeyEvent.VK_NUMPAD5, "Numpad 5");
			kc.put(KeyEvent.VK_NUMPAD6, "Numpad 6");
			kc.put(KeyEvent.VK_NUMPAD7, "Numpad 7");
			kc.put(KeyEvent.VK_NUMPAD8, "Numpad 8");
			kc.put(KeyEvent.VK_NUMPAD9, "Numpad 9");
			kc.put(KeyEvent.VK_MULTIPLY, "Numpad *");
			kc.put(KeyEvent.VK_ADD, "Numpad +");
			kc.put(KeyEvent.VK_SEPARATER, "Separator");
			kc.put(KeyEvent.VK_SEPARATOR, "Separator");
			kc.put(KeyEvent.VK_SUBTRACT, "Subtract");
			kc.put(KeyEvent.VK_DECIMAL, "Decimal");
			kc.put(KeyEvent.VK_DIVIDE, "Divide");
			kc.put(KeyEvent.VK_DELETE, "Delete");
			kc.put(KeyEvent.VK_NUM_LOCK, "Num Lock");
			kc.put(KeyEvent.VK_SCROLL_LOCK, "Scroll Lock");
			kc.put(KeyEvent.VK_F1, "F1");
			kc.put(KeyEvent.VK_F2, "F2");
			kc.put(KeyEvent.VK_F3, "F3");
			kc.put(KeyEvent.VK_F4, "F4");
			kc.put(KeyEvent.VK_F5, "F5");
			kc.put(KeyEvent.VK_F6, "F6");
			kc.put(KeyEvent.VK_F7, "F7");
			kc.put(KeyEvent.VK_F8, "F8");
			kc.put(KeyEvent.VK_F9, "F9");
			kc.put(KeyEvent.VK_F10, "F10");
			kc.put(KeyEvent.VK_F11, "F11");
			kc.put(KeyEvent.VK_F12, "F12");
			kc.put(KeyEvent.VK_F13, "F13");
			kc.put(KeyEvent.VK_F14, "F14");
			kc.put(KeyEvent.VK_F15, "F15");
			kc.put(KeyEvent.VK_F16, "F16");
			kc.put(KeyEvent.VK_F17, "F17");
			kc.put(KeyEvent.VK_F18, "F18");
			kc.put(KeyEvent.VK_F19, "F19");
			kc.put(KeyEvent.VK_F20, "F20");
			kc.put(KeyEvent.VK_F21, "F21");
			kc.put(KeyEvent.VK_F22, "F22");
			kc.put(KeyEvent.VK_F23, "F23");
			kc.put(KeyEvent.VK_F24, "F24");
			kc.put(KeyEvent.VK_PRINTSCREEN, "Print Screen");
			kc.put(KeyEvent.VK_INSERT, "Insert");
			kc.put(KeyEvent.VK_HELP, "Help");
			kc.put(KeyEvent.VK_META, "Meta");
			kc.put(KeyEvent.VK_BACK_QUOTE, "\"");
			kc.put(KeyEvent.VK_QUOTE, "\"");
			kc.put(KeyEvent.VK_KP_UP, "Keypad Up");
			kc.put(KeyEvent.VK_KP_DOWN, "Keypad Down");
			kc.put(KeyEvent.VK_KP_LEFT, "Keypad Left");
			kc.put(KeyEvent.VK_KP_RIGHT, "Keypad Right");
			kc.put(KeyEvent.VK_DEAD_GRAVE, "`");
			kc.put(KeyEvent.VK_DEAD_ACUTE, "´");
			kc.put(KeyEvent.VK_DEAD_CIRCUMFLEX, "^");
			kc.put(KeyEvent.VK_DEAD_TILDE, "~");
			/*
			kc.put(
			kc.put(KeyEvent.VK_DEAD_MACRON, "");
			kc.put(
			kc.put(KeyEvent.VK_DEAD_BREVE, "");
			kc.put(
			kc.put(KeyEvent.VK_DEAD_ABOVEDOT, "");
			kc.put(
			kc.put(KeyEvent.VK_DEAD_DIAERESIS, "");
			kc.put(
			kc.put(KeyEvent.VK_DEAD_ABOVERING, "");
			kc.put(
			kc.put(KeyEvent.VK_DEAD_DOUBLEACUTE, "");
			kc.put(
			kc.put(KeyEvent.VK_DEAD_CARON, "");
			kc.put(
			kc.put(KeyEvent.VK_DEAD_CEDILLA, "");
			kc.put(
			kc.put(KeyEvent.VK_DEAD_OGONEK, "");
			kc.put(
			kc.put(KeyEvent.VK_DEAD_IOTA, "");
			kc.put(
			kc.put(KeyEvent.VK_DEAD_VOICED_SOUND, "");
			kc.put(
			kc.put(KeyEvent.VK_DEAD_SEMIVOICED_SOUND, "");
			*/
			kc.put(KeyEvent.VK_AMPERSAND, "'");
			kc.put(KeyEvent.VK_ASTERISK, "*");
			kc.put(KeyEvent.VK_QUOTEDBL, "\"");
			kc.put(KeyEvent.VK_LESS, "<");
			kc.put(KeyEvent.VK_GREATER, ">");
			kc.put(KeyEvent.VK_BRACELEFT, "{");
			kc.put(KeyEvent.VK_BRACERIGHT, "}");
			kc.put(KeyEvent.VK_AT, "@");
			kc.put(KeyEvent.VK_COLON, ":");
			kc.put(KeyEvent.VK_CIRCUMFLEX, "^");
			kc.put(KeyEvent.VK_DOLLAR, "$");
			kc.put(KeyEvent.VK_EURO_SIGN, "€");
			kc.put(KeyEvent.VK_EXCLAMATION_MARK, "!");
			kc.put(KeyEvent.VK_INVERTED_EXCLAMATION_MARK, "¡");
			kc.put(KeyEvent.VK_LEFT_PARENTHESIS, ")");
			kc.put(KeyEvent.VK_NUMBER_SIGN, "#");
			kc.put(KeyEvent.VK_PLUS, "+");
			kc.put(KeyEvent.VK_RIGHT_PARENTHESIS, ")");
			kc.put(KeyEvent.VK_UNDERSCORE, "_");
			kc.put(KeyEvent.VK_WINDOWS, "Windows");
			/*
			kc.put(KeyEvent.VK_CONTEXT_MENU, "");
			kc.put(KeyEvent.VK_FINAL, "");
			kc.put(KeyEvent.VK_CONVERT, "");
			kc.put(KeyEvent.VK_NONCONVERT, "");
			kc.put(KeyEvent.VK_ACCEPT, "");
			kc.put(KeyEvent.VK_MODECHANGE, "");
			kc.put(KeyEvent.VK_KANA, "");
			kc.put(KeyEvent.VK_KANJI, "");
			kc.put(KeyEvent.VK_ALPHANUMERIC, "");
			kc.put(KeyEvent.VK_KATAKANA, "");
			kc.put(KeyEvent.VK_HIRAGANA, "");
			kc.put(KeyEvent.VK_FULL_WIDTH, "");
			kc.put(KeyEvent.VK_HALF_WIDTH, "");
			kc.put(KeyEvent.VK_ROMAN_CHARACTERS, "");
			kc.put(KeyEvent.VK_ALL_CANDIDATES, "");
			kc.put(KeyEvent.VK_PREVIOUS_CANDIDATE, "");
			kc.put(KeyEvent.VK_CODE_INPUT, "");
			kc.put(KeyEvent.VK_JAPANESE_KATAKANA, "");
			kc.put(KeyEvent.VK_JAPANESE_HIRAGANA, "");
			kc.put(KeyEvent.VK_JAPANESE_ROMAN, "");
			kc.put(KeyEvent.VK_KANA_LOCK, "");
			kc.put(KeyEvent.VK_INPUT_METHOD_ON_OFF, "");
			kc.put(KeyEvent.VK_CUT, "");
			kc.put(KeyEvent.VK_COPY, "");
			kc.put(KeyEvent.VK_PASTE, "");
			kc.put(KeyEvent.VK_UNDO, "");
			kc.put(KeyEvent.VK_AGAIN, "");
			kc.put(KeyEvent.VK_FIND, "");
			kc.put(KeyEvent.VK_PROPS, "");
			kc.put(KeyEvent.VK_STOP, "");
			kc.put(KeyEvent.VK_COMPOSE, "");
			kc.put(KeyEvent.VK_ALT_GRAPH, "");
			kc.put(KeyEvent.VK_BEGIN, "");*/
			KEY_CODES = Collections.unmodifiableMap(kc);
		}
	}
}
