package main;

import java.awt.EventQueue;
import lavesdk.sandbox.Sandbox;

public class PluginTest extends Sandbox {
	private static final long serialVersionUID = 1L;

	public PluginTest() throws IllegalArgumentException {
		super(new MyAlgorithmPlugin(), "en");
	}

	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				new PluginTest().setVisible(true);
			}
		});
	}
}