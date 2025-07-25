package jge3d.controller;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.Queue;

import jge3d.EntityList;
import jge3d.Input;
import jge3d.TextureList;
import jge3d.gui.EntityComboBox;
import jge3d.gui.FPSView;
import jge3d.gui.LevelView;
import jge3d.physics.Physics;
import jge3d.render.Renderer;

import org.lwjgl.LWJGLException;

public class Controller {
	//the game always runs (except when it doesn't)
	final boolean isRunning = true;
	
	private static Controller uniqueInstance = new Controller();
	Queue<Command> controller_queue = new LinkedList<Command>();
	
	long frames=0;
	
	//Create the Input Listening thread
	Thread input_thread = new Thread(new Runnable(){
		@Override
		public void run() {
			//Get rid of the loop here
			while (isRunning) 
			{
				//read keyboard and mouse
				try {
					//run this once
					Input.getInstance().updateInput();
					
					//rejoin the controller thread
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (LWJGLException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	},"Input");
	
	//Create the Physics Listening thread
	Thread physics_thread = new Thread(new Runnable(){
		@Override
		public void run() {
			//remove this
			while (isRunning) 
			{
				//Update the physics world
				Physics.getInstance().clientUpdate();
				
				//Rejoin the controller thread
			}
		}
	},"Physics");
	
	//Create the vidya thread
	Thread render_thread = new Thread(new Runnable(){
		@Override
		public void run() {
			//remove this
			while (isRunning) 
			{
				//Draw the next frame
				try {
					Renderer.getInstance().draw();
					
					//rejoin the controller
					//
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (LWJGLException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	},"Renderer");
	
	
	public static Controller getInstance()
	{
		return uniqueInstance;
	}
	
	public void priorityRun(Object classInstance, String methodToInvoke) {
		
	}
	
	public void enqueue(Object classInstance, String methodToInvoke) {
		controller_queue.add(new Command(classInstance,methodToInvoke));
	}
	
	public Boolean hasQueuedItems() {
		if(controller_queue.size() == 0) {
			return false;
		} else {
			return true;
		}
	}
	
	public void start() {
		input_thread.start();
		physics_thread.start();
		render_thread.start();
		
		//magic numbers go!
		input_thread.setPriority(3);
		physics_thread.setPriority(5);
		render_thread.setPriority(6);
		
	}
	
	public void run_queue() {
		Command commandToRunCommand;
		try {
			for(int i=0; i < controller_queue.size(); i++) {
				commandToRunCommand = controller_queue.poll();
				Method methodToInvoke = commandToRunCommand.getClassInstance().getClass().getDeclaredMethod(commandToRunCommand.getMethodToInvoke());
				methodToInvoke.invoke(commandToRunCommand.getClassInstance());
			}
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		
		++frames;
	}
	
	public void monitor()
	{
		if(LevelView.getInstance().getLoadLevel()) {
			//level.load();
			System.out.println("You loaded the level\n");
		}
		
		//Check if textureList has been altered since last frame
		if(TextureList.getInstance().hasChanged()) {
			try {
				TextureList.getInstance().loadQueuedTexture();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (LWJGLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		//Check to make sure none of the entities are marked as dead
		EntityList.getInstance().pruneEntities();
		
		//Update the world's physical layout
		//Physics.getInstance().clientUpdate();

		//Camera check versus player position
		//Camera.getInstance().moveToPlayerLocation(player);
		
		/*
		if(Controller.getInstance().hasQueuedItems()) {
			//Controller.getInstance().run_queue();
		}
		*/
		
		//Draw world
		//Renderer.getInstance().draw();

		FPSView.getInstance().updateFPS();
		
		//Here's the idea.  Branch out, come back together.  Input run twice for every 1 render/physics run.
		//	The functions we call in the thread will go, any then join back. We wait for them to do so, run 
		//	our entity checks and process the queue, then throw out the thread branches again.
		
		//Start the Render thread going
		//Start the Physics thread going
		//Start the Input thread going
		//Wait for the input thread to rejoin
		//Start it again
		//wait for the input thread to rejoin
		//wait for both the physics and render threads to rejoin
		
		run_queue();

		
		try {
			check_entities();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (LWJGLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		FPSView.getInstance().updateFPS();
	}

	private void check_entities() throws InterruptedException, FileNotFoundException, LWJGLException, IOException {
		if(EntityList.getInstance().getChanged().size() != 0)
		{
			EntityList.getInstance().getChanged().removeAll(EntityList.getInstance().getChanged());
			//update the entity table if necessary
			EntityComboBox.getInstance().update();
			
			//notify the renderer if a level entity changed
			render_thread.wait();
			Renderer.getInstance().makeLevelList();
			render_thread.notify();
		}
	}
	
	public long getFrames() {
		return frames;
	}
	
	public void resetFrames() {
		frames=0;
	}
}
