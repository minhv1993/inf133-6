package paperboat;

import java.awt.event.KeyEvent;

import org.jbox2d.collision.AABB;
import org.jbox2d.collision.shapes.PolygonDef;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.World;
import org.mt4j.MTApplication;
import org.mt4j.components.MTComponent;
import org.mt4j.components.visibleComponents.font.FontManager;
import org.mt4j.components.visibleComponents.font.IFont;
import org.mt4j.components.visibleComponents.widgets.MTBackgroundImage;
import org.mt4j.components.visibleComponents.widgets.MTTextArea;
import org.mt4j.input.IMTInputEventListener;
import org.mt4j.input.inputData.AbstractCursorInputEvt;
import org.mt4j.input.inputData.InputCursor;
import org.mt4j.input.inputData.MTInputEvent;
import org.mt4j.input.inputProcessors.globalProcessors.CursorTracer;
import org.mt4j.sceneManagement.AbstractScene;
import org.mt4j.util.MTColor;
import org.mt4j.util.camera.MTCamera;
import org.mt4j.util.math.ToolsMath;
import org.mt4j.util.math.Vector3D;

import processing.core.PApplet;
import processing.core.PImage;
import advanced.physics.physicsShapes.IPhysicsComponent;
import advanced.physics.physicsShapes.PhysicsRectangle;
import advanced.physics.util.UpdatePhysicsAction;

public class PaperboatScene extends AbstractScene {
	private float timeStep = 1.0f/60.0f;
	private int constraintIterations = 10;
	
	/* The Canvas scale */
	private float scale = 20;
	private MTApplication app;
	private World world;
	
	private PaperBoat boat;

	private String imagesPath = "paperboat"+MTApplication.separator+"data"+MTApplication.separator;
	
	private MTComponent physicsContainer;
	
	private float invWidth, invHeight;
	// Ripple Effect
	PImage img;
	Ripple ripple;


	public PaperboatScene(MTApplication mtApplication, String name) {
		super(mtApplication, name);
		// The window background
		ripple = new Ripple(mtApplication.width, mtApplication.height);
		img = mtApplication.loadImage(imagesPath + "pebbleBG.jpg");
		this.app = mtApplication;
		this.setClearColor(new MTColor(0,0,0,255));
		this.registerGlobalInputProcessor(new CursorTracer(app, this));
		this.getCanvas().addChild(new MTBackgroundImage(app, app.loadImage(imagesPath + "pebbleBG.jpg"), true));
		
		invWidth = 1.0f/mtApplication.width;
        invHeight = 1.0f/mtApplication.height;
		
		float worldOffset = 10; //Make Physics world slightly bigger than screen border
		// Physics world dimensions
		AABB worldAABB = new AABB(new Vec2(-worldOffset, -worldOffset), new Vec2((app.width)/scale + worldOffset, (app.height)/scale + worldOffset));
		Vec2 gravity = new Vec2(0,0);
		boolean sleep = true;
		// Create the physics world
		this.world = new World(worldAABB, gravity, sleep);
		
		this.registerPreDrawAction(new UpdatePhysicsAction(world, timeStep, constraintIterations,scale));
		
		physicsContainer = new MTComponent(app);
		physicsContainer.scale(scale, scale, 1, Vector3D.ZERO_VECTOR);
		this.getCanvas().addChild(physicsContainer);
		
		// Create borders around the screen
		this.createScreenBorders(physicsContainer);
		
		PImage boatTex = mtApplication.loadImage(imagesPath + "paperboat.png");
		boat = new PaperBoat(new Vector3D(mtApplication.width-60, 60), 70, 32, app, world, 0.5f, 0.005f, 0.70f, scale);
		boat.setTexture(boatTex);
		boat.setFillColor(new MTColor(255,255,255,255));
		boat.setNoStroke(true);
		boat.setName("Boat");
		physicsContainer.addChild(boat);
		boat.getBody().applyImpulse(new Vec2(ToolsMath.getRandom(-4, 4), ToolsMath.getRandom(-4, 4)), boat.getBody().getWorldCenter());
		
		PaperBoatGoal goal = new PaperBoatGoal(new Vector3D(60, 60),100, 100, mtApplication, world, 0.0f, 0.1f, 0.0f,scale);
		goal.setName("Goal");
		goal.setFillColor(new MTColor(0,0,0,255));
		goal.setStrokeColor(new MTColor(0,0,255));
		physicsContainer.addChild(goal);
		
		// UI Layer on top of the physics layer 
		MTComponent uiLayer = new MTComponent(mtApplication, new MTCamera(mtApplication));
		uiLayer.setDepthBufferDisabled(true);
		getCanvas().addChild(uiLayer);
		IFont font = FontManager.getInstance().createFont(mtApplication, "arial", 25,new MTColor(255,255,255), new MTColor(0,0,0));
		
		MTTextArea finishText = new MTTextArea(mtApplication, font);
		finishText.setPickable(false);
		finishText.setNoFill(true);
		finishText.setNoStroke(true);
		finishText.setPositionGlobal(new Vector3D(25,60,0));
		finishText.setText("Finish");
		uiLayer.addChild(finishText);
		
		// Set up touch event listener
		this.getCanvas().addInputListener(new IMTInputEventListener(){
			public boolean processInputEvent(MTInputEvent ie){
				if(ie instanceof AbstractCursorInputEvt){
					AbstractCursorInputEvt posEvt = (AbstractCursorInputEvt)ie;
					if(posEvt.hasTarget() && posEvt.getTargetComponent().equals(getCanvas())){
						InputCursor m = posEvt.getCursor();
						AbstractCursorInputEvt prev = m.getPreviousEventOf(posEvt);
						if(prev == null)
							prev = posEvt;
						
						Vector3D pos = new Vector3D(posEvt.getPosX(), posEvt.getPosY(),0);
						Vector3D prevPos = new Vector3D(prev.getPosX(), prev.getPosY(),0);
						
						ripple.newframe();
					}
				}
				return false;
			}
		});
	}
	
	private class PaperBoat extends PhysicsRectangle {

		public PaperBoat(Vector3D centerPosition, float width,
				float height, PApplet applet, World world, float density,
				float friction, float restitution, float scale) {
			super(centerPosition, width, height, applet, world, density, friction,
					restitution, scale);
		}
		
		@Override
		protected void bodyDefB4CreationCallback(BodyDef def) {
			super.bodyDefB4CreationCallback(def);
			def.isBullet = true;
			def.linearDamping = 0.9f;
			def.angularDamping = 0.9f;
		}
		
		@Override
		protected void polyDefB4CreationCallback(PolygonDef def){
			super.polyDefB4CreationCallback(def);
			def.isSensor = false;
		}
	}
	
	private class PaperBoatGoal extends PhysicsRectangle {
		public PaperBoatGoal(Vector3D centerPosition, float width,
				float height, PApplet applet, World world, float density,
				float friction, float restitution, float scale) {
			super(centerPosition, width, height, applet, world, density, friction,
					restitution, scale);
		}
		
		@Override
		protected void bodyDefB4CreationCallback(BodyDef def) {
			def.isBullet = true;
			super.bodyDefB4CreationCallback(def);
		}
		
		@Override
		protected void polyDefB4CreationCallback(PolygonDef def){
			super.polyDefB4CreationCallback(def);
			def.isSensor = true;
		}
	}
	
	private void reset(){
		if(boat.getUserData("resetted") ==  null){
			boat.setUserData("resetted", true);
			app.invokeLater(new Runnable() {
				public void run(){
					IPhysicsComponent a = (IPhysicsComponent) boat;
					a.getBody().setXForm(new Vec2(getMTApplication().width/2f/scale, getMTApplication().height/2f/scale),a.getBody().getAngle());
					a.getBody().setLinearVelocity(new Vec2(ToolsMath.getRandom(-8, 8), ToolsMath.getRandom(-8, 8)));
					a.getBody().setAngularVelocity(0);
					boat.setUserData("resetted", null);
				}
			});
		}
	}
	
	private void createScreenBorders(MTComponent parent){
		//Left
		float borderWidth = 50f;
		float borderHeight = app.height;
		Vector3D pos = new Vector3D(-(borderWidth/2f),app.height/2f);
		PhysicsRectangle borderLeft = new PhysicsRectangle(pos, borderWidth, borderHeight,app, world,0,0,0,scale);
		borderLeft.setName("BorderLeft");
		parent.addChild(borderLeft);
		//Right
		pos = new Vector3D(app.width + (borderWidth/2), app.height/2);
		PhysicsRectangle borderRight = new PhysicsRectangle(pos, borderWidth, borderHeight, app, world, 0,0,0, scale);
		borderRight.setName("BorderRight");
		parent.addChild(borderRight);
		//Top
		borderWidth = app.width;
		borderHeight = 50f;
		pos = new Vector3D(app.width/2, -(borderHeight/2));
		PhysicsRectangle borderTop = new PhysicsRectangle(pos, borderWidth, borderHeight, app, world, 0,0,0, scale);
		borderTop.setName("BorderTop");
		parent.addChild(borderTop);
		//Bottom
		pos = new Vector3D(app.width/2 , app.height + (borderHeight/2));
		PhysicsRectangle borderBottom = new PhysicsRectangle(pos, borderWidth, borderHeight, app, world, 0,0,0, scale);
		borderBottom.setName("BorderBottom");
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
		if(evtID != KeyEvent.KEY_PRESSED)
			return;
		switch(e.getKeyCode()){
		case KeyEvent.VK_SPACE:
			this.reset();
			break;
			default:
				break;
		}
	}
	
	private class Ripple {
		int i, a, b;
		int oldind, newind, mapind;
		short ripplemap[]; // the height map
		int col[]; // the actual pixels
		int riprad;
		int rwidth, rheight, width, height;
		int ttexture[];
		int ssize;

		// constructor
		Ripple(int frameWidth, int frameHeight) {
			this.width = frameWidth;
			this.height = frameHeight;
			riprad = 3;
			rwidth = width >> 1;
			rheight = height >> 1;
			ssize = width * (height + 2) * 2;
			ripplemap = new short[ssize];
			col = new int[width * height];
			ttexture = new int[width * height];
			oldind = width;
			newind = width * (height + 3);
		}
		
		void newframe() {
			// update the height map and the image
			i = oldind;
			oldind = newind;
			newind = i;
			
			i = 0;
			mapind = oldind;
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					short data = (short)((ripplemap[mapind - width] + ripplemap[mapind + width] + 
							ripplemap[mapind - 1] + ripplemap[mapind + 1]) >> 1);
					data -= ripplemap[newind + i];
					data -= data >> 5;
					if (x == 0 || y == 0) // avoid the wraparound effect
						ripplemap[newind + i] = 0;
					else
						ripplemap[newind + i] = data;
 
					// where data = 0 then still, where data > 0 then wave
					data = (short)(1024 - data);
 
					// offsets
					a = ((x - rwidth) * data / 1024) + rwidth;
					b = ((y - rheight) * data / 1024) + rheight;
 
					//bounds check
					if (a >= width)
						a = width - 1;
					if (a < 0)
						a = 0;
					if (b >= height)
						b = height-1;
					if (b < 0)
						b=0;
 
					col[i] = img.pixels[a + (b * width)];
					mapind++;
					i++;
				}
			}
		}
	}
}
