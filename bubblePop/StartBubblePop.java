package bubblePop;

import org.mt4j.MTApplication;

public class StartBubblePop extends MTApplication{
	private static final long serialVersionUID = 1L;

public static void main(String[] args) {
	initialize();
}

@Override
public void startUp() {
	addScene(new BubblePopScene(this, "Bubble Pop"));
}

}
