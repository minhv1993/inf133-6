package pongRevolution;

import java.awt.event.KeyEvent;
import java.nio.FloatBuffer;

import javax.media.opengl.GL;

import msafluid.MSAFluidSolver2D;

import org.jbox2d.collision.AABB;
import org.jbox2d.collision.shapes.CircleDef;
import org.jbox2d.collision.shapes.PolygonDef;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.ContactListener;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.contacts.ContactPoint;
import org.jbox2d.dynamics.contacts.ContactResult;
import org.jbox2d.dynamics.joints.Joint;
import org.jbox2d.dynamics.joints.JointType;
import org.jbox2d.dynamics.joints.MouseJoint;
import org.mt4j.MTApplication;
import org.mt4j.components.MTComponent;
import org.mt4j.components.visibleComponents.font.FontManager;
import org.mt4j.components.visibleComponents.font.IFont;
import org.mt4j.components.visibleComponents.shapes.MTEllipse;
import org.mt4j.components.visibleComponents.shapes.MTLine;
import org.mt4j.components.visibleComponents.shapes.MTRectangle;
import org.mt4j.components.visibleComponents.widgets.MTTextArea;
import org.mt4j.input.inputData.AbstractCursorInputEvt;
import org.mt4j.input.inputData.InputCursor;
import org.mt4j.input.inputProcessors.IGestureEventListener;
import org.mt4j.input.inputProcessors.MTGestureEvent;
import org.mt4j.input.inputProcessors.componentProcessors.dragProcessor.DragEvent;
import org.mt4j.input.inputProcessors.componentProcessors.dragProcessor.DragProcessor;
import org.mt4j.input.inputProcessors.globalProcessors.CursorTracer;
import org.mt4j.sceneManagement.AbstractScene;
import org.mt4j.util.MT4jSettings;
import org.mt4j.util.MTColor;
import org.mt4j.util.camera.MTCamera;
import org.mt4j.util.math.ToolsMath;
import org.mt4j.util.math.Vector3D;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.opengl.PGraphicsOpenGL;
import advanced.physics.physicsShapes.IPhysicsComponent;
import advanced.physics.physicsShapes.PhysicsCircle;
import advanced.physics.physicsShapes.PhysicsRectangle;
import advanced.physics.util.PhysicsHelper;
import advanced.physics.util.UpdatePhysicsAction;

import com.sun.opengl.util.BufferUtil;

public class PongRevolutionScene extends AbstractScene {
	private float timeStep = 1.0f/60.0f;
	private int constraintIterations = 10;
	
	/** THE CANVAS SCALE **/
	private float scale = 20;
	private MTApplication app;
	private World world;
	
	/** FLUID ENGINE COMPONENTS **/
	private final float FLUID_WIDTH = 120;
	private float invWidth, invHeight;    // inverse of screen dimensions
	private float aspectRatio, aspectRatio2;
	private MSAFluidSolver2D fluidSolver;
	private PImage imgFluid;
	private boolean drawFluid = true;
	
	private ParticleSystem particleSystem;
	
	/** PHYSIC ENGINE COMPONENTS **/
	private MTComponent physicsContainer;
	
	private MTTextArea t1;
	private MTTextArea t2;
	private int scorePlayer1;
	private int scorePlayer2;

	private Paddle redCircle;
	private Paddle blueCircle;
	
	private PongBall ball;
	private boolean enableSound = true;
	
	private String imagePath = "pongRevolution" + MTApplication.separator + "data" + MTApplication.separator;
	
	public PongRevolutionScene(MTApplication mtApplication, String name) {
		super(mtApplication, name);
		this.app = mtApplication;
		this.setClearColor(new MTColor(0,0,0,255));
		this.registerGlobalInputProcessor(new CursorTracer(app, this));
		
		// FUILD BACKGROUND
		if (!MT4jSettings.getInstance().isOpenGlMode()){
			System.err.println("Scene only usable when using the OpenGL renderer! - See settings.txt");
        	return;
        }
		
        //pa.hint( PApplet.ENABLE_OPENGL_4X_SMOOTH );    // Turn on 4X antialiasing
        invWidth = 1.0f/mtApplication.width;
        invHeight = 1.0f/mtApplication.height;
        aspectRatio = mtApplication.width * invHeight;
        aspectRatio2 = aspectRatio * aspectRatio;
     
        // Create fluid and set options
        fluidSolver = new MSAFluidSolver2D((int)(FLUID_WIDTH), (int)(FLUID_WIDTH * mtApplication.height/mtApplication.width));
//        fluidSolver.enableRGB(true).setFadeSpeed(0.003f).setDeltaT(0.5f).setVisc(0.00005f);
        fluidSolver.enableRGB(true).setFadeSpeed(0.003f).setDeltaT(0.8f).setVisc(0.00004f);
     
        // Create image to hold fluid picture
        imgFluid = mtApplication.createImage(fluidSolver.getWidth(), fluidSolver.getHeight(), PApplet.RGB);
        
        // Create particle system
        particleSystem = new ParticleSystem(mtApplication, fluidSolver);
        
        //FIXME make componentInputProcessor?
        this.getCanvas().addChild(new FluidImage(mtApplication));
        this.getCanvas().setDepthBufferDisabled(true);
        
		
		this.scorePlayer1 = 0;
		this.scorePlayer2 = 0;
		
		float worldOffset = 10; // Make physics world slightly bigger than screen borders
		// Physics world dimensions
		AABB worldAABB = new AABB(new Vec2(-worldOffset, -worldOffset), new Vec2((app.width)/scale + worldOffset, (app.height)/scale + worldOffset));
		Vec2 gravity = new Vec2(0, 0);
		boolean sleep = true;
		//Create the physics world
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

		//Create gamefield marks
		MTLine line = new MTLine(mtApplication, mtApplication.width/2f/scale, 0, mtApplication.width/2f/scale, mtApplication.height/scale);
		line.setPickable(false);
//		line.setStrokeColor(new MTColor(0,0,0));
		line.setStrokeColor(new MTColor(150,150,150));
		line.setStrokeWeight(0.5f);
		physicsContainer.addChild(line);
		
		MTEllipse centerCircle = new MTEllipse(mtApplication, new Vector3D(mtApplication.width/2f/scale, mtApplication.height/2f/scale), 80/scale, 80/scale);
		centerCircle.setPickable(false);
		centerCircle.setNoFill(true);
//		centerCircle.setStrokeColor(new MTColor(0,0,0));
		centerCircle.setStrokeColor(new MTColor(150,150,150));
		centerCircle.setStrokeWeight(0.5f);
		physicsContainer.addChild(centerCircle);
		
		MTEllipse centerCircleInner = new MTEllipse(mtApplication, new Vector3D(mtApplication.width/2f/scale, mtApplication.height/2f/scale), 10/scale, 10/scale);
		centerCircleInner.setPickable(false);
		centerCircleInner.setFillColor(new MTColor(160,160,160));
//		centerCircleInner.setStrokeColor(new MTColor(150,150,150));
//		centerCircleInner.setStrokeColor(new MTColor(0,0,0));
		centerCircleInner.setStrokeColor(new MTColor(150,150,150));
		centerCircleInner.setStrokeWeight(0.5f);
		physicsContainer.addChild(centerCircleInner);
		
		// PHYSICS OBJECT
		// CREATE THE BALL
		ball = new PongBall(app, new Vector3D(mtApplication.width/2f, mtApplication.height/2f), 20, world, 1.0f, 0.00001f, 1.0f, scale);
		PImage ballTex = mtApplication.loadImage(imagePath + "meteor.png");
		ball.setTexture(ballTex);
		ball.setFillColor(new MTColor(255,255,255,255));
		ball.setNoStroke(true);
		ball.setName("ball");
		physicsContainer.addChild(ball);
		ball.getBody().applyImpulse(new Vec2(ToolsMath.getRandom(-8f, 8),ToolsMath.getRandom(-8, 8)), ball.getBody().getWorldCenter());
		
		//CREATE THE GOALS
		PongGoal goal1 = new PongGoal(new Vector3D(0, mtApplication.height/2f), 50, mtApplication.height/4f, mtApplication, world, 0.0f, 0.1f, 0.0f, scale);
		goal1.setName("goal1");
		goal1.setFillColor(new MTColor(255,50,50));
		goal1.setStrokeColor(new MTColor(255,50,50));
		physicsContainer.addChild(goal1);
		
		PongGoal goal2 = new PongGoal(new Vector3D(mtApplication.width, mtApplication.height/2f), 50, mtApplication.height/4f, mtApplication, world, 0.0f, 0.1f, 0.0f, scale);
		goal2.setName("goal2");
		goal2.setFillColor(new MTColor(50,50,255));
		goal2.setStrokeColor(new MTColor(50,50,255));
		physicsContainer.addChild(goal2);
		
		PImage paddleTex = mtApplication.loadImage(imagePath + "paddle.png");
		redCircle = new Paddle(app, new Vector3D(mtApplication.width - 60, mtApplication.height/2f), 50, world, 1.0f, 0.3f, 0.4f, scale);
		redCircle.setTexture(paddleTex);
		redCircle.setFillColor(new MTColor(255,50,50));
		redCircle.setNoStroke(true);
		redCircle.setName("red");
		redCircle.setPickable(false);
		physicsContainer.addChild(redCircle);
		
		blueCircle = new Paddle(app, new Vector3D(80, mtApplication.height/2f), 50, world, 1.0f, 0.3f, 0.4f, scale);
		blueCircle.setTexture(paddleTex);
		blueCircle.setFillColor(new MTColor(50,50,255));
		blueCircle.setNoStroke(true);
		blueCircle.setName("blue");
		blueCircle.setPickable(false);
		physicsContainer.addChild(blueCircle);
		
		//Make two components for both game field sides
		MTRectangle leftSide = new MTRectangle(
				PhysicsHelper.scaleDown(0, scale), PhysicsHelper.scaleDown(0, scale), 
				PhysicsHelper.scaleDown(app.width/2f, scale), PhysicsHelper.scaleDown(app.height, scale),app);
		leftSide.setName("left side");
		leftSide.setNoFill(true); //Make it invisible -> only used for dragging
		leftSide.setNoStroke(true);
		leftSide.unregisterAllInputProcessors();
		leftSide.removeAllGestureEventListeners(DragProcessor.class);
		leftSide.registerInputProcessor(new DragProcessor(app));
		leftSide.addGestureListener(DragProcessor.class, new GameFieldHalfDragListener(blueCircle));
		physicsContainer.addChild(0, leftSide);
		MTRectangle rightSide = new MTRectangle(
				PhysicsHelper.scaleDown(app.width/2f, scale), PhysicsHelper.scaleDown(0, scale), 
				PhysicsHelper.scaleDown(app.width, scale), PhysicsHelper.scaleDown(app.height, scale),app);
		rightSide.setName("right Side");
		rightSide.setNoFill(true); //Make it invisible -> only used for dragging
		rightSide.setNoStroke(true);
		rightSide.unregisterAllInputProcessors();
		rightSide.removeAllGestureEventListeners(DragProcessor.class);
		rightSide.registerInputProcessor(new DragProcessor(app));
		rightSide.addGestureListener(DragProcessor.class, new GameFieldHalfDragListener(redCircle));
		physicsContainer.addChild(0, rightSide);
		
		//Display Score UI
		MTComponent uiLayer = new MTComponent(mtApplication, new MTCamera(mtApplication));
		uiLayer.setDepthBufferDisabled(true);
		getCanvas().addChild(uiLayer);
		IFont font = FontManager.getInstance().createFont(mtApplication, "arial", 50, new MTColor(255,255,255),new MTColor(0,0,0));
		
		t1 = new MTTextArea(mtApplication, font);
		t1.setPickable(false);
		t1.setNoFill(true);
		t1.setNoStroke(true);
		t1.setPositionGlobal(new Vector3D(5,30,0));
		uiLayer.addChild(t1);
		
		t2 = new MTTextArea(mtApplication, font);
		t2.setPickable(false);
		t2.setNoFill(true);
		t2.setNoStroke(true);
		t2.setPositionGlobal(new Vector3D(mtApplication.width - 65 , 30,0));
		uiLayer.addChild(t2);
		this.updateScores();
		
		//Set up check for collisions between objects
		this.addWorldContactListener(world);
	}
	
	// FUILD ENGINE RELATED CLASSES AND METHODS
	private class FluidImage extends MTComponent{
		public FluidImage(PApplet applet) {
			super(applet);
		}
		//@Override
		public void drawComponent(PGraphics g) {
			super.drawComponent(g);
			drawFluidImage();
			
			g.noSmooth();
			g.fill(255,255,255,255);
			g.tint(255,255,255,255);
			
			//FIXME TEST
			PGraphicsOpenGL pgl = (PGraphicsOpenGL)g; 
			GL gl = pgl.gl;
			gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
			gl.glDisableClientState(GL.GL_COLOR_ARRAY);
			gl.glDisable(GL.GL_LINE_SMOOTH);
			gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
		}
	}
	
	private void addForce(float x, float y, float dx, float dy) {
	    float speed = dx * dx  + dy * dy * aspectRatio2;    // balance the x and y components of speed with the screen aspect ratio
	 
	    if(speed > 0) {
	        if(x < 0){ 
	        	x = 0; 
	        }else if(x > 1){
	        	x = 1;
	        }if(y < 0){ 
	        	y = 0; 
	        }else if(y > 1){ 
	        	y = 1;
	        }
	        
	        float colorMult = 5;
	        float velocityMult = 20.0f;
	 
	        int index = fluidSolver.getIndexForNormalizedPosition(x, y);
	 
//	        PApplet.color drawColor;
	        app.colorMode(PApplet.HSB, 360, 1, 1);
	        float hue = ((x + y) * 180 + app.frameCount) % 360;
	        int drawColor = app.color(hue, 1, 1);
	        app.colorMode(PApplet.RGB, 1);  
	 
	        fluidSolver.rOld[index]  += app.red(drawColor) 	* colorMult;
	        fluidSolver.gOld[index]  += app.green(drawColor) 	* colorMult;
	        fluidSolver.bOld[index]  += app.blue(drawColor) 	* colorMult;
	 
	        //Particles
	        particleSystem.addParticles(x * app.width, y * app.height, 10);
	        
	        fluidSolver.uOld[index] += dx * velocityMult;
	        fluidSolver.vOld[index] += dy * velocityMult;
	        
//	        mtApp.noSmooth();
//			mtApp.fill(255,255,255,255);
//			mtApp.tint(255,255,255,255);
			
			//FIXME TEST
			app.colorMode(PApplet.RGB, 255);  
	    }
	}
	
	private void drawFluidImage(){
		app.colorMode(PApplet.RGB, 1);  
		 
		fluidSolver.update();
	    if(drawFluid) {
	        for(int i=0; i<fluidSolver.getNumCells(); i++) {
	            int d = 2;
	            imgFluid.pixels[i] = app.color(fluidSolver.r[i] * d, fluidSolver.g[i] * d, fluidSolver.b[i] * d);
	        }  
	        imgFluid.updatePixels();//  fastblur(imgFluid, 2);
	        
//	        app.image(imgFluid, 0, 0, app.width, app.height); //FIXME this messes up blend transition!
	        
	        app.textureMode(PConstants.NORMAL);
//	        app.textureMode(app.IMAGE);
//	        app.beginShape(MTApplication.QUADS);
	        app.beginShape();
	        app.texture(imgFluid);
	        app.vertex(0, 0, 0, 0);
	        app.vertex(app.width, 0, 1, 0);
	        app.vertex(app.width, app.height, 1, 1);
	        app.vertex(0, app.height, 0, 1);
	        app.endShape();

	    } 
	    particleSystem.updateAndDraw();
	    
	    app.colorMode(PApplet.RGB, 255);  
	}
	
	private class Particle {
		private final static float MOMENTUM = 0.5f;
		private final static float FLUID_FORCE = 0.6f;

		private float x, y;
		private float vx, vy;
		//private float radius;       // particle's size
		protected float alpha;
		private float mass;
		private PApplet p;
		private float invWidth;
		private float invHeight;
		private MSAFluidSolver2D fluidSolver;
		
		
		public Particle(PApplet p, MSAFluidSolver2D fluidSolver, float invWidth, float invHeight){
			this.p = p;
			this.invWidth = invWidth;
			this.invHeight = invHeight;
			this.fluidSolver = fluidSolver;
		}

	   public void init(float x, float y) {
	       this.x = x;
	       this.y = y;
	       vx = 0;
	       vy = 0;
	       //radius = 5;
	       alpha = p.random(0.3f, 1);
	       mass = p.random(0.1f, 1);
	   }


	   public void update() {
	       // only update if particle is visible
	       if(alpha == 0) return;

	       // read fluid info and add to velocity
	       int fluidIndex = fluidSolver.getIndexForNormalizedPosition(x * invWidth, y * invHeight);
	       vx = fluidSolver.u[fluidIndex] * p.width * mass * FLUID_FORCE + vx * MOMENTUM;
	       vy = fluidSolver.v[fluidIndex] * p.height * mass * FLUID_FORCE + vy * MOMENTUM;

	       // update position
	       x += vx;
	       y += vy;

	       // bounce of edges
	       if(x<0) {
	           x = 0;
	           vx *= -1;
	       }else if(x > p.width) {
	           x = p.width;
	           vx *= -1;
	       }

	       if(y<0) {
	           y = 0;
	           vy *= -1;
	       }else if(y > p.height) {
	           y = p.height;
	           vy *= -1;
	       }

	       // hackish way to make particles glitter when the slow down a lot
	       if(vx * vx + vy * vy < 1) {
	           vx = p.random(-1, 1);
	           vy = p.random(-1, 1);
	       }

	       // fade out a bit (and kill if alpha == 0);
	       alpha *= 0.999;
	       if(alpha < 0.01) 
	    	   alpha = 0;

	   }


	   public void updateVertexArrays(int i, FloatBuffer posBuffer, FloatBuffer colBuffer) {
	       int vi = i * 4;
	       posBuffer.put(vi++, x - vx);
	       posBuffer.put(vi++, y - vy);
	       posBuffer.put(vi++, x);
	       posBuffer.put(vi++, y);

	       int ci = i * 6;
	       colBuffer.put(ci++, alpha);
	       colBuffer.put(ci++, alpha);
	       colBuffer.put(ci++, alpha);
	       colBuffer.put(ci++, alpha);
	       colBuffer.put(ci++, alpha);
	       colBuffer.put(ci++, alpha);
	   }


	   public void drawOldSchool(GL gl) {
	       gl.glColor3f(alpha, alpha, alpha);
	       gl.glVertex2f(x-vx, y-vy);
	       gl.glVertex2f(x, y);
	   }

	}
	
	public class ParticleSystem{
		private FloatBuffer posArray;
		private FloatBuffer colArray;
		private final static int maxParticles = 5000;
		private int curIndex;
		
		boolean renderUsingVA = true;
		
		private Particle[] particles;
		private PApplet p;
		private MSAFluidSolver2D fluidSolver;
		private float invWidth;
		private float invHeight;
		
		private boolean drawFluid;
		
		public ParticleSystem(PApplet p, MSAFluidSolver2D fluidSolver) {
			this.p = p;
			this.fluidSolver = fluidSolver;
			this.invWidth = 1.0f/p.width;
			this.invHeight = 1.0f/p.height;
			
			this.drawFluid = true;
			
			particles = new Particle[maxParticles];
			
			for(int i=0; i<maxParticles; i++) {
				particles[i] = new Particle(p, this.fluidSolver, invWidth, invHeight);
			}
			
			curIndex = 0;

			posArray = BufferUtil.newFloatBuffer(maxParticles * 2 * 2);// 2 coordinates per point, 2 points per particle (current and previous)
			colArray = BufferUtil.newFloatBuffer(maxParticles * 3 * 2);
		}


		public void updateAndDraw(){
			PGraphicsOpenGL pgl = (PGraphicsOpenGL)p.g;         // processings opengl graphics object
			GL gl = pgl.beginGL();                // JOGL's GL object

			gl.glEnable( GL.GL_BLEND );             // enable blending
			
			/*
			if(!drawFluid) 
				fadeToColor(p, gl, 0, 0, 0, 0.05f);
			*/

			gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE);  // additive blending (ignore alpha)
			gl.glEnable(GL.GL_LINE_SMOOTH);        // make points round
			gl.glLineWidth(1);


			if(renderUsingVA) {
				for(int i=0; i<maxParticles; i++) {
					if(particles[i].alpha > 0) {
						particles[i].update();
						particles[i].updateVertexArrays(i, posArray, colArray);
					}
				}    
				gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
				gl.glVertexPointer(2, GL.GL_FLOAT, 0, posArray);

				gl.glEnableClientState(GL.GL_COLOR_ARRAY);
				gl.glColorPointer(3, GL.GL_FLOAT, 0, colArray);

				gl.glDrawArrays(GL.GL_LINES, 0, maxParticles * 2);
			} 
			else {
				/*
				gl.glBegin(GL.GL_LINES);               // start drawing points
				for(int i=0; i<maxParticles; i++) {
					if(particles[i].alpha > 0) {
						particles[i].update();
						particles[i].drawOldSchool(gl);    // use oldschool renderng
					}
				}
				gl.glEnd();
				*/
			}

//			gl.glDisable(GL.GL_BLEND);
			//Reset blendfunction
			gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
//			pgl.endGL();
			pgl.endGL();
		}

		/*
		public void fadeToColor(PApplet p, GL10 gl, float r, float g, float b, float speed) {
//			gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
			gl.glColor4f(r, g, b, speed);
			gl.glBegin(GL.GL_QUADS);
				gl.glVertex2f(0, 0);
				gl.glVertex2f(p.width, 0);
				gl.glVertex2f(p.width, p.height);
				gl.glVertex2f(0, p.height);
			gl.glEnd();
		}
		*/
		

		public void addParticles(float x, float y, int count ){
			for(int i=0; i<count; i++) addParticle(x + p.random(-15, 15), y + p.random(-15, 15));
		}


		public void addParticle(float x, float y) {
			particles[curIndex].init(x, y);
			curIndex++;
			if(curIndex >= maxParticles) curIndex = 0;
		}



		public boolean isDrawFluid() {
			return drawFluid;
		}

		public void setDrawFluid(boolean drawFluid) {
			this.drawFluid = drawFluid;
		}

	}

	// PHYSICS ENGINE RELATED CLASSES AND METHODS
	private class PongBall extends PhysicsCircle {
		public PongBall(PApplet applet, Vector3D centerPoint, float radius,
			World world, float density, float friction, float restitution, float worldScale) {
		super(applet, centerPoint, radius, world, density, friction, restitution, worldScale);
		} 
	
		@Override
		protected void circleDefB4CreationCallback(CircleDef def) {
			super.circleDefB4CreationCallback(def);
			def.radius = def.radius -5/scale;
		}
		@Override
		protected void bodyDefB4CreationCallback(BodyDef def) {
			super.bodyDefB4CreationCallback(def);
	//		def.linearDamping = 0.15f;
			def.linearDamping = 0.25f;
			def.isBullet = true;
			def.angularDamping = 0.9f;
			
	//		def.fixedRotation = true;
		}
	}
	
	private class PongGoal extends PhysicsRectangle {
		public PongGoal(Vector3D centerPosition, float width, float height,
				PApplet applet, World world, float density, float friction,float restitution, float scale) {
			super(centerPosition, width, height, applet, world, density, friction,restitution, scale);
		}
		
		@Override
		protected void bodyDefB4CreationCallback(BodyDef def) {
			def.isBullet = true;
			super.bodyDefB4CreationCallback(def);
		}

		@Override
		protected void polyDefB4CreationCallback(PolygonDef def) {
			super.polyDefB4CreationCallback(def);
			def.isSensor = true; //THIS AS SENSOR!
		}
	}
	
	private class PongPad  extends PhysicsRectangle {
		
		public PongPad(Vector3D centerPosition, float width, float height,
				PApplet applet, World world, float density, float friction,float restitution, float scale) {
			super(centerPosition, width, height, applet, world, density, friction,restitution, scale);
		}
		
		@Override
		protected void bodyDefB4CreationCallback(BodyDef def) {
			def.isBullet = false;
			super.bodyDefB4CreationCallback(def);
		}

		@Override
		protected void polyDefB4CreationCallback(PolygonDef def) {
			super.polyDefB4CreationCallback(def);
			def.isSensor = false; //THIS AS SENSOR!
		}
	}
	
	private class GameFieldHalfDragListener implements IGestureEventListener{
		private MTComponent comp;
		
		public GameFieldHalfDragListener(MTComponent dragComp){
			this.comp = dragComp;
			if (comp.getUserData("box2d") == null){
				throw new RuntimeException("GameFieldHalfDragListener has to be given a physics object!");
			}
		}
		
		public boolean processGestureEvent(MTGestureEvent ge) {
			DragEvent de = (DragEvent)ge;
			try{
				Body body = (Body)comp.getUserData("box2d");
				MouseJoint mouseJoint;
				Vector3D to = new Vector3D(de.getTo());
				InputCursor m = de.getDragCursor();
				AbstractCursorInputEvt posEvt = de.getDragCursor().getCurrentEvent();
				AbstractCursorInputEvt prev = m.getPreviousEventOf(posEvt);
				if (prev == null)
					prev = posEvt;

				Vector3D pos = new Vector3D(posEvt.getPosX(), posEvt.getPosY(), 0);
				Vector3D prevPos = new Vector3D(prev.getPosX(), prev.getPosY(), 0);

				//System.out.println("Pos: " + pos);
				float mouseNormX = pos.x * invWidth;
				float mouseNormY = pos.y * invHeight;
				//System.out.println("MouseNormPosX: " + mouseNormX + "," + mouseNormY);
				float mouseVelX = (pos.x - prevPos.x) * invWidth;
				float mouseVelY = (pos.y - prevPos.y) * invHeight;
				/*
				System.out.println("Mouse vel X: " + mouseVelX + " mouseNormX:" + mouseNormX);
				System.out.println("Mouse vel Y: " + mouseVelY + " mouseNormY:" + mouseNormY);
				 */
				addForce(mouseNormX, mouseNormY, mouseVelX, mouseVelY);
				//Un-scale position from mt4j to box2d
				PhysicsHelper.scaleDown(to, scale);
				switch (de.getId()) {
				case DragEvent.GESTURE_DETECTED:
					comp.sendToFront();
					body.wakeUp();
					body.setXForm(new Vec2(to.x,  to.y), body.getAngle());
					mouseJoint = PhysicsHelper.createDragJoint(world, body, to.x, to.y);
					comp.setUserData(comp.getID(), mouseJoint);
					break;
				case DragEvent.GESTURE_UPDATED:
					mouseJoint = (MouseJoint) comp.getUserData(comp.getID());
					if (mouseJoint != null){
						boolean onCorrectGameSide = ((MTComponent)de.getTargetComponent()).containsPointGlobal(de.getTo());
						//System.out.println(((MTComponent)de.getTargetComponent()).getName()  + " Contains  " + to + " -> " + contains);
						if (onCorrectGameSide){
							mouseJoint.setTarget(new Vec2(to.x, to.y));	
						}
					}
					break;
				case DragEvent.GESTURE_ENDED:
					mouseJoint = (MouseJoint) comp.getUserData(comp.getID());
					if (mouseJoint != null){
						comp.setUserData(comp.getID(), null);
						//Only destroy the joint if it isnt already (go through joint list and check)
						for (Joint joint = world.getJointList(); joint != null; joint = joint.getNext()) {
							JointType type = joint.getType();
							switch (type) {
							case MOUSE_JOINT:
								MouseJoint mj = (MouseJoint)joint;
								if (body.equals(mj.getBody1()) || body.equals(mj.getBody2())){
									if (mj.equals(mouseJoint)) {
										world.destroyJoint(mj);
									}
								}
								break;
							default:
								break;
							}
						}
					}
					mouseJoint = null;
					break;
				default:
					break;
				}
			}catch (Exception e) {
				System.err.println(e.getMessage());
			}
			return false;
		}
	}
	
	private void reset(){
		if (ball.getUserData("resetted") == null){ //To make sure that we call destroy only once
			ball.setUserData("resetted", true); 
			app.invokeLater(new Runnable() {
				public void run() {
					IPhysicsComponent a = (IPhysicsComponent)ball;
					a.getBody().setXForm(new Vec2(getMTApplication().width/2f/scale, getMTApplication().height/2f/scale), a.getBody().getAngle());
//					a.getBody().setLinearVelocity(new Vec2(0,0));
					a.getBody().setLinearVelocity(new Vec2(ToolsMath.getRandom(-8, 8),ToolsMath.getRandom(-8, 8)));
					a.getBody().setAngularVelocity(0);
					ball.setUserData("resetted", null); 
				}
			});
		}
		this.scorePlayer1 = 0;
		this.scorePlayer2 = 0;
		this.updateScores();
	}

	private void addWorldContactListener(World world){
		world.setContactListener(new ContactListener() {
			public void result(ContactResult point) {
//				System.out.println("Result contact");
			}
			//@Override
			public void remove(ContactPoint point) {
//				System.out.println("remove contact");
			}
			//@Override
			public void persist(ContactPoint point) {
//				System.out.println("persist contact");
			}
			//@Override
			public void add(ContactPoint point) {
//				/*
				Shape shape1 = point.shape1;
				Shape shape2 = point.shape2;
				final Body body1 = shape1.getBody();
				final Body body2 = shape2.getBody();
				Object userData1 = body1.getUserData();
				Object userData2 = body2.getUserData();
				
				if (userData1 instanceof IPhysicsComponent  && userData2 instanceof IPhysicsComponent) { //Check for ball/star collision
					IPhysicsComponent physObj1 = (IPhysicsComponent) userData1;
					IPhysicsComponent physObj2 = (IPhysicsComponent) userData2;
//					System.out.println("Collided: " + mt4jObj1 + " with " + mt4jObj2);
					if (physObj1 instanceof MTComponent && physObj2 instanceof MTComponent) {
						MTComponent comp1 = (MTComponent) physObj1;
						MTComponent comp2 = (MTComponent) physObj2;

						//Check if one of the components is the BALL
						MTComponent ball = isHit("ball", comp1, comp2);
						final MTComponent theBall = ball;
						
						//Check if one of the components is the GOAL
						MTComponent goal1 = isHit("goal1", comp1, comp2);
						MTComponent goal2 = isHit("goal2", comp1, comp2);
						
						//Check if a puck was involved
						MTComponent bluePuck = isHit("blue", comp1, comp2);
						MTComponent redPuck = isHit("red", comp1, comp2);
						
						//Check if a border was hit
						MTComponent border = null;
						if (comp1.getName() != null && comp1.getName().startsWith("border")){
							border = comp1;
						}else if (comp2.getName() != null && comp2.getName().startsWith("border")){
							border = comp2;
						}
						
						if (ball != null){
							//CHECK IF BALL HIT A PADDLE
							if (enableSound && (bluePuck != null || redPuck != null)){
//								System.out.println("PUCK HIT BALL!");
								/*
								triggerSound(paddleHit);
								*/
							}
							
							
							//Check if BALL HIT A GOAL 
							if (goal1 != null || goal2 != null){
								//BALL HIT A GOAL
								if (goal1 != null){
									System.out.println("GOAL FOR PLAYER 2!");
									scorePlayer2++;
								}else if (goal2 != null){
									System.out.println("GOAL FOR PLAYER 1!");
									scorePlayer1++;
								}
								
								//Update scores
								updateScores();
								//Play goal sound
//								triggerSound(goalHit);
								
								if (scorePlayer1 >= 15 || scorePlayer2 >= 15){
									reset();
								}else{
								
								//Reset ball
								if (theBall.getUserData("resetted") == null){ //To make sure that we call destroy only once
									theBall.setUserData("resetted", true); 
									app.invokeLater(new Runnable() {
										public void run() {
											IPhysicsComponent a = (IPhysicsComponent)theBall;
											a.getBody().setXForm(new Vec2(getMTApplication().width/2f/scale, getMTApplication().height/2f/scale), a.getBody().getAngle());
//											a.getBody().setLinearVelocity(new Vec2(0,0));
											a.getBody().setLinearVelocity(new Vec2(ToolsMath.getRandom(-8, 8),ToolsMath.getRandom(-8, 8)));
											a.getBody().setAngularVelocity(0);
											theBall.setUserData("resetted", null); 
										}
									});
								}
								}
								
							}
							
							//If ball hit border Play sound
							if (enableSound && border != null){
								/*
								triggerSound(wallHit);
								*/
							}
						}
					}
				}else{ //if at lest one if the colliding bodies' userdata is not a physics shape
					
				}
//				*/
			}
		});
	}
	
	private MTComponent isHit(String componentName, MTComponent comp1, MTComponent comp2){
		MTComponent hitComp = null;
		if (comp1.getName() != null && comp1.getName().equalsIgnoreCase(componentName)){
			hitComp = comp1;
		}else if (comp2.getName() != null && comp2.getName().equalsIgnoreCase(componentName)){
			hitComp = comp2;
		}
		return hitComp;
	}
	
	private void updateScores(){
		t1.setText(Integer.toString(scorePlayer1));
		t2.setText(Integer.toString(scorePlayer2));
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

	private class Paddle extends PhysicsCircle{
		public Paddle(PApplet applet, Vector3D centerPoint, float radius,
				World world, float density, float friction, float restitution, float worldScale) {
			super(applet, centerPoint, radius, world, density, friction, restitution, worldScale);
		} 
		@Override
		protected void bodyDefB4CreationCallback(BodyDef def) {
			super.bodyDefB4CreationCallback(def);
			def.fixedRotation = true;
			def.linearDamping = 0.5f;
		}
	}
	
	public void keyEvent(KeyEvent e){
		int evtID = e.getID();
		if (evtID != KeyEvent.KEY_PRESSED)
			return;
		switch (e.getKeyCode()){
		case KeyEvent.VK_BACK_SPACE:
			app.popScene();
			break;
		case KeyEvent.VK_SPACE:
			this.reset();
			break;
		default:
			break;
		}
	}

	@Override
	public void init() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void shutDown() {
		// TODO Auto-generated method stub
		
	}
	
}
