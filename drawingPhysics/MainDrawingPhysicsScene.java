package drawingPhysics;

import java.util.HashMap;

import org.jbox2d.collision.AABB;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.World;
import org.mt4j.MTApplication;
import org.mt4j.components.MTComponent;
import org.mt4j.components.TransformSpace;
import org.mt4j.components.visibleComponents.shapes.AbstractShape;
import org.mt4j.components.visibleComponents.shapes.MTRectangle;
import org.mt4j.components.visibleComponents.widgets.MTSceneTexture;
import org.mt4j.components.visibleComponents.widgets.buttons.MTImageButton;
import org.mt4j.input.IMTInputEventListener;
import org.mt4j.input.inputData.AbstractCursorInputEvt;
import org.mt4j.input.inputData.InputCursor;
import org.mt4j.input.inputData.MTInputEvent;
import org.mt4j.input.inputProcessors.IGestureEventListener;
import org.mt4j.input.inputProcessors.MTGestureEvent;
import org.mt4j.input.inputProcessors.componentProcessors.tapProcessor.TapEvent;
import org.mt4j.input.inputProcessors.componentProcessors.tapProcessor.TapProcessor;
import org.mt4j.input.inputProcessors.globalProcessors.CursorTracer;
import org.mt4j.sceneManagement.AbstractScene;
import org.mt4j.sceneManagement.IPreDrawAction;
import org.mt4j.util.MT4jSettings;
import org.mt4j.util.MTColor;
import org.mt4j.util.math.ToolsMath;
import org.mt4j.util.math.Vector3D;
import org.mt4j.util.opengl.GLFBO;

import processing.core.PApplet;
import processing.core.PImage;
import advanced.physics.physicsShapes.PhysicsRectangle;
import advanced.physics.util.UpdatePhysicsAction;

public class MainDrawingPhysicsScene extends AbstractScene {
	private float timeStep = 1.0f / 60.0f;
	private int constraintIterations = 10;
	private float scale = 20;
	private World world;
	private MTComponent physicsContainer;
	private MTApplication pa;
	private DrawingPhysicsScene drawingScene;
	private String imagesPath = "drawingPhysics" + MTApplication.separator + "data" + MTApplication.separator;
	
	public MainDrawingPhysicsScene(MTApplication mtApplication, String name) {
		super(mtApplication, name);
		this.pa = mtApplication;

		if (!(MT4jSettings.getInstance().isOpenGlMode() && GLFBO.isSupported(pa))){
			System.err.println("Drawing example can only be run in OpenGL mode on a gfx card supporting the GL_EXT_framebuffer_object extension!");
			return;
		}
		// THE DRAWING ASPECT OF THE APPLICATION
		//CREATE THE DRAWING BOARD
        MTRectangle frame = new MTRectangle(pa.width, pa.height, pa);
        this.getCanvas().addChild(frame);
        //Create the scene in which we actually draw
        drawingScene = new DrawingPhysicsScene(pa, "DrawPhysics Scene");
        drawingScene.setClear(false);
		
        //Create the frame/window that displays the drawing scene through a FBO
        //final MTSceneTexture sceneWindow = new MTSceneTexture(0,0, pa, drawingScene);
		//We have to create a fullscreen fbo in order to save the image uncompressed
		final MTSceneTexture sceneTexture = new MTSceneTexture(pa,0, 0, pa.width, pa.height, drawingScene);
        sceneTexture.getFbo().clear(true, 255, 255, 255, 0, true);
        sceneTexture.setStrokeColor(new MTColor(155,155,155));
        frame.addChild(sceneTexture);
        
        //Eraser button
        PImage eraser = pa.loadImage(imagesPath + "Kde_crystalsvg_eraser.png");
        MTImageButton b = new MTImageButton(eraser,pa);
        b.setNoStroke(true);
        b.translate(new Vector3D(2,0,0));
        b.addGestureListener(TapProcessor.class, new IGestureEventListener() {
			public boolean processGestureEvent(MTGestureEvent ge) {
				TapEvent te = (TapEvent)ge;
				if (te.isTapped()){
//					//As we are messing with opengl here, we make sure it happens in the rendering thread
					pa.invokeLater(new Runnable() {
						public void run() {
							sceneTexture.getFbo().clear(true, 255, 255, 255, 0, true);						
						}
					});
				}
				return true;
			}
        });
        frame.addChild(b);
        
		//THE PHYSICS ASPECT OF THE APPLICATION
		float worldOffSet = 10;//Make Physics world slightly bigger than screen borders
		
		//Physics world dimensions
		AABB worldAABB = new AABB(new Vec2(-worldOffSet, -worldOffSet), new Vec2((pa.width)/scale + worldOffSet, (pa.height)/scale + worldOffSet));
		Vec2 gravity = new Vec2(0,0); // NO Gravity in this world
		boolean sleep = true;
		
		//CREATE THE PHYSICS WORLD
		this.world = new World(worldAABB, gravity, sleep);
		
		this.registerGlobalInputProcessor(new CursorTracer(mtApplication, this));
		
		//Update the positions of the components according to the physics simulation
		this.registerPreDrawAction(new UpdatePhysicsAction(world, timeStep, constraintIterations, scale));
		
		physicsContainer = new MTComponent(pa);
		physicsContainer.scale(scale,scale,1,Vector3D.ZERO_VECTOR);
		this.getCanvas().addChild(physicsContainer);
		
        this.createScreenBorders(physicsContainer);
	}
	
	private void createScreenBorders(MTComponent parent){
		//Left border 
		float borderWidth = 50f;
		float borderHeight = pa.height;
		Vector3D pos = new Vector3D(-(borderWidth/2f) , pa.height/2f);
		PhysicsRectangle borderLeft = new PhysicsRectangle(pos, borderWidth, borderHeight, pa, world, 0,0,0, scale);
		borderLeft.setName("borderLeft");
		parent.addChild(borderLeft);
		//Right border
		pos = new Vector3D(pa.width + (borderWidth/2), pa.height/2);
		PhysicsRectangle borderRight = new PhysicsRectangle(pos, borderWidth, borderHeight, pa, world, 0,0,0, scale);
		borderRight.setName("borderRight");
		parent.addChild(borderRight);
		//Top border
		borderWidth = pa.width;
		borderHeight = 50f;
		pos = new Vector3D(pa.width/2, -(borderHeight/2));
		PhysicsRectangle borderTop = new PhysicsRectangle(pos, borderWidth, borderHeight, pa, world, 0,0,0, scale);
		borderTop.setName("borderTop");
		parent.addChild(borderTop);
		//Bottom border
		pos = new Vector3D(pa.width/2 , pa.height + (borderHeight/2));
		PhysicsRectangle borderBottom = new PhysicsRectangle(pos, borderWidth, borderHeight, pa, world, 0,0,0, scale);
		borderBottom.setName("borderBottom");
		parent.addChild(borderBottom);
	}

	public void init() {	}
	public void shutDown() {	}
	
	@Override
	public boolean destroy() {
		boolean destroyed = super.destroy();
		if (destroyed){
			drawingScene.destroy(); //Destroy the scene manually since it isnt destroyed in the MTSceneTexture atm!
		}
		return destroyed;
	}
	
	private class DrawingPhysicsScene extends AbstractScene {

		private MTApplication mtApp;

		private AbstractShape drawShape;

		private float stepDistance;

		private Vector3D localBrushCenter;

		private float brushWidthHalf;

		private HashMap<InputCursor, Vector3D> cursorToLastDrawnPoint;

		private float brushHeightHalf;

		private float brushScale;
		
		private MTColor brushColor;
		
		private boolean dynamicBrush;
		
		//TODO only works as lightweight scene atm because the framebuffer isnt cleared each frame
		//TODO make it work as a heavywight scene
		//TODO scale smaller at higher speeds?
		//TODO eraser?
		//TODO get blobwidth from win7 touch events and adjust the brush scale
		
		public DrawingPhysicsScene(MTApplication mtApplication, String name) {
			super(mtApplication, name);
			this.mtApp = mtApplication;
			
			this.getCanvas().setDepthBufferDisabled(true);
			
			/*
			this.drawShape = getDefaultBrush();
			this.localBrushCenter = drawShape.getCenterPointLocal();
			this.brushWidthHalf = drawShape.getWidthXY(TransformSpace.LOCAL)/2f;
			this.brushHeightHalf = drawShape.getHeightXY(TransformSpace.LOCAL)/2f;
			this.stepDistance = brushWidthHalf/2.5f;
			*/
			
			this.brushColor = new MTColor(0,0,0);
			this.brushScale = 1.0f;
			this.dynamicBrush = true;
//			this.stepDistance = 5.5f;
			
			this.cursorToLastDrawnPoint = new HashMap<InputCursor, Vector3D>();
			
			this.getCanvas().addInputListener(new IMTInputEventListener() {
				public boolean processInputEvent(MTInputEvent inEvt){
					if(inEvt instanceof AbstractCursorInputEvt){
						final AbstractCursorInputEvt posEvt = (AbstractCursorInputEvt)inEvt;
						final InputCursor m = posEvt.getCursor();
//						System.out.println("PrevPos: " + prevPos);
//						System.out.println("Pos: " + pos);

						if (posEvt.getId() != AbstractCursorInputEvt.INPUT_ENDED){
							registerPreDrawAction(new IPreDrawAction() {
								public void processAction() {
									boolean firstPoint = false;
									Vector3D lastDrawnPoint = cursorToLastDrawnPoint.get(m);
									Vector3D pos = new Vector3D(posEvt.getPosX(), posEvt.getPosY(), 0);

									if (lastDrawnPoint == null){
										lastDrawnPoint = new Vector3D(pos);
										cursorToLastDrawnPoint.put(m, lastDrawnPoint);
										firstPoint = true;
									}else{
										if (lastDrawnPoint.equalsVector(pos))
											return;	
									}
									
									float scaledStepDistance = stepDistance*brushScale;

									Vector3D direction = pos.getSubtracted(lastDrawnPoint);
									float distance = direction.length();
									direction.normalizeLocal();
									direction.scaleLocal(scaledStepDistance);

									float howManySteps = distance/scaledStepDistance;
									int stepsToTake = Math.round(howManySteps);

									//Force draw at 1st point
									if (firstPoint && stepsToTake == 0){
										stepsToTake = 1;
									}
//									System.out.println("Steps: " + stepsToTake);

//									GL gl = Tools3D.getGL(mtApp);
//									gl.glBlendFuncSeparate(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA, GL.GL_ONE, GL.GL_ONE_MINUS_SRC_ALPHA);

									mtApp.pushMatrix();
									//We would have to set up a default view here for stability? (default cam etc?)
									getSceneCam().update(); 

									Vector3D currentPos = new Vector3D(lastDrawnPoint);
									for (int i = 0; i < stepsToTake; i++) { //start i at 1? no, we add first step at 0 already
										currentPos.addLocal(direction);
										//Draw new brush into FBO at correct position
										Vector3D diff = currentPos.getSubtracted(localBrushCenter);

										mtApp.pushMatrix();
										mtApp.translate(diff.x, diff.y);

										//FIXME works only if brush upper left at 0,0
										mtApp.translate(brushWidthHalf, brushHeightHalf);
										mtApp.scale(brushScale);
										
										if (dynamicBrush){
										//Rotate brush randomly
//										mtApp.rotateZ(PApplet.radians(Tools3D.getRandom(0, 179)));
//										mtApp.rotateZ(PApplet.radians(Tools3D.getRandom(-85, 85)));
										mtApp.rotateZ(PApplet.radians(ToolsMath.getRandom(-25, 25)));
//										mtApp.rotateZ(PApplet.radians(Tools3D.getRandom(-9, 9)));
										mtApp.translate(-brushWidthHalf, -brushHeightHalf);
										}

										/*
			        					//Use random brush from brushes
			        					int brushIndex = Math.round(Tools3D.getRandom(0, brushes.length-1));
			        					AbstractShape brushToDraw = brushes[brushIndex];
										 */
										AbstractShape brushToDraw = drawShape;

										//Draw brush
										brushToDraw.drawComponent(mtApp.g);

										mtApp.popMatrix();
									}
									mtApp.popMatrix();

									cursorToLastDrawnPoint.put(m, currentPos);
								}

								public boolean isLoop() {
									return false;
								}
							});
						}else{
							cursorToLastDrawnPoint.remove(m);
						}
					}
					return false;
				}
			});

		}
		
		
		public void setBrush(AbstractShape brush){
			this.drawShape = brush;
			this.localBrushCenter = drawShape.getCenterPointLocal();
			this.brushWidthHalf = drawShape.getWidthXY(TransformSpace.LOCAL)/2f;
			this.brushHeightHalf = drawShape.getHeightXY(TransformSpace.LOCAL)/2f;
			this.stepDistance = brushWidthHalf/2.8f;
			this.drawShape.setFillColor(this.brushColor);
			this.drawShape.setStrokeColor(this.brushColor);
		}
		
		public void setBrushColor(MTColor color){
			this.brushColor = color;
			if (this.drawShape != null){
				drawShape.setFillColor(color);
				drawShape.setStrokeColor(color);
			}
		}
		
		public void setBrushScale(float scale){
			this.brushScale = scale;
		}
		
		
		public void init() {
		}
		
		public void shutDown() {
		}
	}
}
