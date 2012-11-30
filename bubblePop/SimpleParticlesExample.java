package bubblePop;

import org.mt4j.MTApplication;
import org.mt4j.input.IMTInputEventListener;
import org.mt4j.input.inputData.AbstractCursorInputEvt;
import org.mt4j.input.inputData.MTInputEvent;
import org.mt4j.sceneManagement.AbstractScene;
import org.mt4j.util.MT4jSettings;
import org.mt4j.util.camera.MTCamera;

import processing.core.PImage;
import processing.core.PVector;

public class SimpleParticlesExample extends MTApplication {
	private static final long serialVersionUID = 1L;

	public static void main(String[] args) {
		initialize();
	}

	@Override
	public void startUp() {
		if (MT4jSettings.getInstance().isOpenGlMode()){
			addScene(new ParticleTest(this, "Particles test"));			
		}else{
			System.err.println("Particle Scene can only be run in opengl mode!");
		}
	}
	
	private class ParticleTest extends AbstractScene{
		private String path = "bubblePop" + MTApplication.separator + "data" + MTApplication.separator;
		
		public ParticleTest(MTApplication mtApplication, String name) {
			super(mtApplication, name);
			
			final MTParticleSystem mtPs = new MTParticleSystem(getMTApplication(), 0,0, mtApplication.width, mtApplication.height);
			mtPs.attachCamera(new MTCamera(getMTApplication()));
			mtPs.setPickable(false);
			getCanvas().addChild(mtPs);
			final PImage texture = getMTApplication().loadImage(path + "particle.png");
			
			getCanvas().addInputListener(new IMTInputEventListener() {
				public boolean processInputEvent(MTInputEvent inEvt) {
					if (inEvt instanceof AbstractCursorInputEvt) {
						AbstractCursorInputEvt ce = (AbstractCursorInputEvt) inEvt;
						mtPs.getParticleSystem().addParticle(new ImageParticle(getMTApplication(), new PVector(ce.getPosX(), ce.getPosY()), texture));
					}
					return false;
				}
			});
		}

		@Override
		public void init() {
			getMTApplication().frameRate(60);
		}

		@Override
		public void shutDown() {
			// TODO Auto-generated method stub
			
		}
	}

}
