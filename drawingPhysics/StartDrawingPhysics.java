package drawingPhysics;

import org.mt4j.MTApplication;

public class StartDrawingPhysics extends MTApplication{
	/**
	 * 
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public static void main(String[] args){
		initialize();
	}
	
	@Override
	public void startUp() {
		this.addScene(new MainDrawingPhysicsScene(this, "Drawing Physics"));		
	}
}
