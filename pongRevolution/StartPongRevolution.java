package pongRevolution;

import org.mt4j.MTApplication;

public class StartPongRevolution extends MTApplication{
	private static final long serialVersionUID = 1L;

	public static void main(String[] args) {
		initialize();
	}

	@Override
	public void startUp() {
		addScene(new PongRevolutionScene(this, "Air Hockey Scene"));
	}
	
}
