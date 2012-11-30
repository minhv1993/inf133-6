package bubblePop;

import java.awt.event.KeyEvent;

import org.jbox2d.collision.AABB;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.World;
import org.mt4j.MTApplication;
import org.mt4j.components.MTComponent;
import org.mt4j.input.inputData.InputCursor;
import org.mt4j.input.inputProcessors.IGestureEventListener;
import org.mt4j.input.inputProcessors.MTGestureEvent;
import org.mt4j.input.inputProcessors.componentProcessors.dragProcessor.DragEvent;
import org.mt4j.input.inputProcessors.componentProcessors.dragProcessor.DragProcessor;
import org.mt4j.input.inputProcessors.componentProcessors.tapProcessor.TapEvent;
import org.mt4j.input.inputProcessors.componentProcessors.tapProcessor.TapProcessor;
import org.mt4j.input.inputProcessors.globalProcessors.CursorTracer;
import org.mt4j.sceneManagement.AbstractScene;
import org.mt4j.util.MTColor;
import org.mt4j.util.camera.MTCamera;
import org.mt4j.util.math.ToolsMath;
import org.mt4j.util.math.Vector3D;

import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;
import advanced.physics.physicsShapes.PhysicsCircle;
import advanced.physics.physicsShapes.PhysicsRectangle;
import advanced.physics.util.PhysicsHelper;
import advanced.physics.util.UpdatePhysicsAction;

public class BubblePopScene extends AbstractScene {
	private float timeStep = 1.0f/60.0f;
	private int constraintIterations = 10;
	float scale = 20;
	private MTApplication app;
	private World world;
	
	private MTComponent physicsContainer;
	
	private String imagesPath = "bubblePop"+MTApplication.separator+"data"+MTApplication.separator;
	PImage bubbleTex;
	PImage bubbleTex2;
	PImage texture;
	
	MTParticleSystem mtPs;
	
	public BubblePopScene(MTApplication mtApplication, String name){
		super(mtApplication, name);
		this.app = mtApplication;
		this.setClearColor(new MTColor(0,0,0,255));
		this.registerGlobalInputProcessor(new CursorTracer(app, this));

		mtPs = new MTParticleSystem(getMTApplication(), 0,0, mtApplication.width, mtApplication.height);
		mtPs.attachCamera(new MTCamera(getMTApplication()));
		mtPs.setPickable(false);
		getCanvas().addChild(mtPs);
		texture = getMTApplication().loadImage(imagesPath + "particle.png");
		
		float worldOffset = 10; //Make Physics world slightly bigger than screen borders
		//Physics world dimensions
		AABB worldAABB = new AABB(new Vec2(-worldOffset, -worldOffset), new Vec2((app.width)/scale + worldOffset, (app.height)/scale + worldOffset));
		Vec2 gravity = new Vec2(0, 0);
		boolean sleep = true;
		//Create the pyhsics world
		this.world = new World(worldAABB, gravity, sleep);
		
		//Update the positions of the components according the the physics simulation each frame
		this.registerPreDrawAction(new UpdatePhysicsAction(world, timeStep, constraintIterations, scale));

		physicsContainer = new MTComponent(app);
		//Scale the physics container. Physics calculations work best when the dimensions are small (about 0.1 - 10 units)
		//So we make the display of the container bigger and add in turn make our physics object smaller
		physicsContainer.scale(scale, scale, 1, Vector3D.ZERO_VECTOR);
		this.getCanvas().addChild(physicsContainer);

		//Create borders around the screen
		this.createScreenBorders(physicsContainer);
		
		//Create bubbles
		bubbleTex = mtApplication.loadImage(imagesPath + "bubble.png");
		bubbleTex2 = mtApplication.loadImage(imagesPath + "bubble2.png");
		
		MTColor color = new MTColor(255,255,255,255);
		for (int i = 0; i < 40; i++) {
			final BubblePop c = new BubblePop(app, new Vector3D(ToolsMath.getRandom(60, mtApplication.width-60), ToolsMath.getRandom(60, mtApplication.height-60)), 50, world, 1.0f, 0.3f, 0.4f, scale);
			PhysicsHelper.addDragJoint(world, c, c.getBody().isDynamic(), scale);
			c.unregisterAllInputProcessors();
			c.removeAllGestureEventListeners();
			
			if(i%2 == 0){
				c.setTexture(bubbleTex);
				c.setFillColor(color);
				c.setNoStroke(true);
				c.registerInputProcessor(new TapProcessor(app));
				c.addGestureListener(TapProcessor.class, new IGestureEventListener(){
					@Override
					public boolean processGestureEvent(MTGestureEvent  ge){
						TapEvent te = (TapEvent)ge; 
						InputCursor cursor = te.getCursor();
			            if(te.getId() == TapEvent.GESTURE_DETECTED){
							mtPs.getParticleSystem().addParticle(new ImageParticle(getMTApplication(), new PVector(cursor.getPosition().x, cursor.getPosition().y), texture)); 
			                c.destroy();
							createBubble("pop");
			            } 
						return false;
					}
				});
			}else{
				c.setStrokeColor(color);
				c.setNoFill(true);
				c.registerInputProcessor(new DragProcessor(app));
				c.addGestureListener(DragProcessor.class, new IGestureEventListener(){
					@Override
					public boolean processGestureEvent(MTGestureEvent  ge){
						DragEvent ze = (DragEvent)ge; 
						InputCursor cursor = ze.getDragCursor();
			            if(ze.getId() == DragEvent.GESTURE_ENDED){ 
			            	mtPs.getParticleSystem().addParticle(new ImageParticle(getMTApplication(), new PVector(cursor.getPosition().x, cursor.getPosition().y), texture)); 
			                c.destroy();
							createBubble("Drag");
			            } 
						return false;
					}
				});
			}
			
			
			physicsContainer.addChild(c);
			c.getBody().applyImpulse(new Vec2(ToolsMath.getRandom(-20f, 20),ToolsMath.getRandom(-20, 20)), c.getBody().getWorldCenter());
		}
	}
	
	private void createBubble(String type){
		final BubblePop c = new BubblePop(app, new Vector3D(ToolsMath.getRandom(60, app.width-60), ToolsMath.getRandom(60, app.height-60)), 50, world, 1.0f, 0.3f, 0.4f, scale);
		PhysicsHelper.addDragJoint(world, c, c.getBody().isDynamic(), scale);
		c.unregisterAllInputProcessors();
		c.removeAllGestureEventListeners();
		
		if(type.equals("pop")){
			c.setTexture(bubbleTex);
			c.setNoStroke(true);
			c.setStrokeColor(new MTColor(255,255,255,255));
			c.registerInputProcessor(new TapProcessor(app));
			c.addGestureListener(TapProcessor.class, new IGestureEventListener(){
				@Override
				public boolean processGestureEvent(MTGestureEvent  ge){
					TapEvent te = (TapEvent)ge; 
					InputCursor cursor = te.getCursor();
		            if(te.getId() == TapEvent.GESTURE_DETECTED){ 
		            	mtPs.getParticleSystem().addParticle(new ImageParticle(getMTApplication(), new PVector(cursor.getPosition().x, cursor.getPosition().y), texture)); 
		                c.destroy();
		                createBubble("pop");
		            } 
					return false;
				}
			});
		}else{
			//c.setTexture(bubbleTex2);
			c.setStrokeColor(new MTColor(255,255,255,255));
			c.setNoFill(true);
			c.registerInputProcessor(new DragProcessor(app));
			c.addGestureListener(DragProcessor.class, new IGestureEventListener(){
				@Override
				public boolean processGestureEvent(MTGestureEvent  ge){
					DragEvent ze = (DragEvent)ge; 
					InputCursor cursor = ze.getDragCursor();
		            if(ze.getId() == DragEvent.GESTURE_ENDED){ 
		            	mtPs.getParticleSystem().addParticle(new ImageParticle(getMTApplication(), new PVector(cursor.getPosition().x, cursor.getPosition().y), texture)); 
		                c.destroy();
		                createBubble("Drag");
		            } 
					return false;
				}
			});
		}
		
		physicsContainer.addChild(c);
		c.getBody().applyImpulse(new Vec2(ToolsMath.getRandom(-20f, 20),ToolsMath.getRandom(-20, 20)), c.getBody().getWorldCenter());
	}
	
	private class BubblePop extends PhysicsCircle {
		public BubblePop(PApplet applet, Vector3D centerPoint, float radius,
				World world, float density, float friction, float restitution, float worldScale) {
			super(applet, centerPoint, radius, world, density, friction, restitution, worldScale);
		} 
		@Override
		protected void bodyDefB4CreationCallback(BodyDef def) {
			super.bodyDefB4CreationCallback(def);
			def.fixedRotation = true;
		}
	}
	
	private void createScreenBorders(MTComponent parent){
		//Left border 
		float borderWidth = 50f;
		float borderHeight = app.height;
		Vector3D pos = new Vector3D(-(borderWidth/2f) , app.height/2f);
		PhysicsRectangle borderLeft = new PhysicsRectangle(pos, borderWidth, borderHeight, app, world, 0,0,0, scale);
		borderLeft.setName("borderLeft");
		parent.addChild(borderLeft);
		//Right border
		pos = new Vector3D(app.width + (borderWidth/2), app.height/2);
		PhysicsRectangle borderRight = new PhysicsRectangle(pos, borderWidth, borderHeight, app, world, 0,0,0, scale);
		borderRight.setName("borderRight");
		parent.addChild(borderRight);
		//Top border
		borderWidth = app.width;
		borderHeight = 50f;
		pos = new Vector3D(app.width/2, -(borderHeight/2));
		PhysicsRectangle borderTop = new PhysicsRectangle(pos, borderWidth, borderHeight, app, world, 0,0,0, scale);
		borderTop.setName("borderTop");
		parent.addChild(borderTop);
		//Bottom border
		pos = new Vector3D(app.width/2 , app.height + (borderHeight/2));
		PhysicsRectangle borderBottom = new PhysicsRectangle(pos, borderWidth, borderHeight, app, world, 0,0,0, scale);
		borderBottom.setName("borderBottom");
		parent.addChild(borderBottom);
	}
	
	@Override
	public void init() {
		this.getMTApplication().registerKeyEvent(this);
	}

	@Override
	public void shutDown() {
		this.getMTApplication().unregisterKeyEvent(this);
	}
	
	public void keyEvent(KeyEvent e){
		int evtID = e.getID();
		if (evtID != KeyEvent.KEY_PRESSED)
			return;
		switch (e.getKeyCode()){
		case KeyEvent.VK_SPACE:
			break;
			default:
				break;
		}
	}

}
