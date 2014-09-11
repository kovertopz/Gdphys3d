package net.smert.gdphys3d;

import com.chrishecker.gdphys3d.Physics;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

/*
 * ----------------------------------------------------------------------------
 *
 * 3D Physics Test Program - a cheesy test harness for 3D physics
 *
 * by Chris Hecker for my Game Developer Magazine articles. See my homepage for
 * more information.
 *
 * NOTE: This is a hacked test program, not a nice example of Windows
 * programming. physics.cpp the only part of this you should look at!!!
 *
 * This material is Copyright 1997 Chris Hecker, All Rights Reserved. It's for
 * you to read and learn from, not to put in your own articles or books or on
 * your website, etc. Thank you.
 *
 * Chris Hecker checker@d6.com http://www.d6.com/users/checker
 *
 */
public class Demo {

    private final String WINDOW_TITLE = "Cheesy 3D Physics App";
    private boolean configDebugMode = false;
    private boolean configFullScreen = false;
    private boolean configRunning = false;
    private boolean configVSync = false;
    private boolean configKeysPressed[] = new boolean[Keyboard.KEYBOARD_SIZE];
    private DisplayMode configDisplayMode = null;
    private float configCameraFieldOfView = 70.0f;
    private float configCameraZClipNear = 0.1f;
    private float configCameraZClipFar = 256.0f;
    private float configClearColor[] = {0.0f, 0.0f, 0.0f, 0.0f};
    private int configWindowDepth = 32;
    private int configWindowHeight = 768;
    private int configWindowWidth = 1024;
    private String[] commandLineArgs;
    private float Zoom = -10.0f;
    private float dZoom = 0.0f;
    private float Azimuth = 0.0f;
    private float dAzimuth = 0.0f;
    private float Altitude = 0.0f;
    private float dAltitude = 0.0f;
    private Physics physics;

    public static void main(String[] args) {
        Demo demo = new Demo();
        demo.run(args);
    }

    public void run(String[] args) {
        commandLineArgs = args;

        parseCommandLineOptions();

        try {
            init();

            while (configRunning) {
                mainLoop();
            }

            shutdown();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    private void parseCommandLineOptions() {
        for (String commandlinearg : commandLineArgs) {
            parseCommandLineOption(commandlinearg);
        }
    }

    private void parseCommandLineOption(String commandlinearg) {
        if (commandlinearg.equalsIgnoreCase("debug") == true) {
            configDebugMode = true;
        } else if (commandlinearg.equalsIgnoreCase("fullscreen") == true) {
            configFullScreen = true;
        } else if (commandlinearg.equalsIgnoreCase("vsync") == true) {
            configVSync = true;
        }
    }

    private void init() throws Exception {
        createWindow();
        initGL();
        initPhysics();
        startLesson();
    }

    private void createWindow() {
        try {
            DisplayMode displaymodes[] = Display.getAvailableDisplayModes();

            for (int i = 0; i < displaymodes.length; i++) {
                if ((displaymodes[i].getWidth() == configWindowWidth)
                        && (displaymodes[i].getHeight() == configWindowHeight)
                        && (displaymodes[i].getBitsPerPixel() == configWindowDepth)) {
                    configDisplayMode = displaymodes[i];
                }

                if (configDebugMode == true) {
                    System.out.println("Found Mode: Width: " + displaymodes[i].getWidth()
                            + "px Height: " + displaymodes[i].getHeight() + "px Depth: "
                            + displaymodes[i].getBitsPerPixel() + "bpp");
                }
            }

            if (configDisplayMode == null) {
                configDisplayMode = Display.getDesktopDisplayMode();
            }

            if (configDebugMode == true) {
                System.out.println("Using Display Mode: Width: " + configDisplayMode.getWidth()
                        + "px Height: " + configDisplayMode.getHeight() + "px Depth: "
                        + configDisplayMode.getBitsPerPixel() + "bpp");
            }

            Display.setDisplayMode(configDisplayMode);
            Display.setTitle(WINDOW_TITLE);
            Display.create();

            if ((Display.getDisplayMode().isFullscreenCapable() == true) && (configFullScreen == true)) {
                Display.setFullscreen(configFullScreen);
            }

            Display.setVSyncEnabled(configVSync);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initGL() {
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LESS);
        GL11.glClearDepth(1.0);
        GL11.glShadeModel(GL11.GL_SMOOTH);

        initProjectionMatrix();
        initModelViewMatrix();
    }

    private void initProjectionMatrix() {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();

        GLU.gluPerspective(
                configCameraFieldOfView,
                (float) configDisplayMode.getWidth() / (float) configDisplayMode.getHeight(),
                configCameraZClipNear,
                configCameraZClipFar);
        GL11.glHint(GL11.GL_PERSPECTIVE_CORRECTION_HINT, GL11.GL_NICEST);
    }

    private void initModelViewMatrix() {
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();

        GL11.glClearColor(configClearColor[0], configClearColor[1], configClearColor[2], configClearColor[3]);
    }

    private void initPhysics() {
        physics = new Physics();
    }

    private void startLesson() {
        configRunning = true;
    }

    private void mainLoop() {
        input();
        render();

        Display.update();
    }

    private void input() {
        if (Display.isCloseRequested() == true) {
            configRunning = false;
        }

        if (Keyboard.isKeyDown(Keyboard.KEY_ESCAPE) == true) {
            configRunning = false;
        }

        if ((Keyboard.isKeyDown(Keyboard.KEY_F1) == true)
                && (configKeysPressed[Keyboard.KEY_F1] == false)) {
            configKeysPressed[Keyboard.KEY_F1] = true;

            switchFullScreen();
        } else if (Keyboard.isKeyDown(Keyboard.KEY_F1) == false) {
            configKeysPressed[Keyboard.KEY_F1] = false;
        }

        if (Keyboard.isKeyDown(Keyboard.KEY_ESCAPE) == true) {
            configRunning = false;
        }

        if ((Keyboard.isKeyDown(Keyboard.KEY_DOWN) == true)
                && (configKeysPressed[Keyboard.KEY_DOWN] == false)) {
            configKeysPressed[Keyboard.KEY_DOWN] = true;

            Altitude += -3.0f;
        } else if (Keyboard.isKeyDown(Keyboard.KEY_DOWN) == false) {
            configKeysPressed[Keyboard.KEY_DOWN] = false;
        }
        if ((Keyboard.isKeyDown(Keyboard.KEY_LEFT) == true)
                && (configKeysPressed[Keyboard.KEY_LEFT] == false)) {
            configKeysPressed[Keyboard.KEY_LEFT] = true;

            Azimuth += 3.0f;
        } else if (Keyboard.isKeyDown(Keyboard.KEY_LEFT) == false) {
            configKeysPressed[Keyboard.KEY_LEFT] = false;
        }
        if ((Keyboard.isKeyDown(Keyboard.KEY_RIGHT) == true)
                && (configKeysPressed[Keyboard.KEY_RIGHT] == false)) {
            configKeysPressed[Keyboard.KEY_RIGHT] = true;

            Azimuth += -3.0f;
        } else if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT) == false) {
            configKeysPressed[Keyboard.KEY_RIGHT] = false;
        }
        if ((Keyboard.isKeyDown(Keyboard.KEY_UP) == true)
                && (configKeysPressed[Keyboard.KEY_UP] == false)) {
            configKeysPressed[Keyboard.KEY_UP] = true;

            Altitude += 3.0f;
        } else if (Keyboard.isKeyDown(Keyboard.KEY_UP) == false) {
            configKeysPressed[Keyboard.KEY_UP] = false;
        }
        if ((Keyboard.isKeyDown(Keyboard.KEY_B) == true)
                && (configKeysPressed[Keyboard.KEY_B] == false)) {
            configKeysPressed[Keyboard.KEY_B] = true;

            physics.ToggleBodySprings();
        } else if (Keyboard.isKeyDown(Keyboard.KEY_B) == false) {
            configKeysPressed[Keyboard.KEY_B] = false;
        }
        if ((Keyboard.isKeyDown(Keyboard.KEY_D) == true)
                && (configKeysPressed[Keyboard.KEY_D] == false)) {
            configKeysPressed[Keyboard.KEY_D] = true;

            physics.ToggleDamping();
        } else if (Keyboard.isKeyDown(Keyboard.KEY_D) == false) {
            configKeysPressed[Keyboard.KEY_D] = false;
        }
        if ((Keyboard.isKeyDown(Keyboard.KEY_G) == true)
                && (configKeysPressed[Keyboard.KEY_G] == false)) {
            configKeysPressed[Keyboard.KEY_G] = true;

            physics.ToggleGravity();
        } else if (Keyboard.isKeyDown(Keyboard.KEY_G) == false) {
            configKeysPressed[Keyboard.KEY_G] = false;
        }
        if ((Keyboard.isKeyDown(Keyboard.KEY_I) == true)
                && (configKeysPressed[Keyboard.KEY_I] == false)) {
            configKeysPressed[Keyboard.KEY_I] = true;

            Zoom += 1.0f;
        } else if (Keyboard.isKeyDown(Keyboard.KEY_I) == false) {
            configKeysPressed[Keyboard.KEY_I] = false;
        }
        if ((Keyboard.isKeyDown(Keyboard.KEY_O) == true)
                && (configKeysPressed[Keyboard.KEY_O] == false)) {
            configKeysPressed[Keyboard.KEY_O] = true;

            Zoom += -1.0f;
        } else if (Keyboard.isKeyDown(Keyboard.KEY_O) == false) {
            configKeysPressed[Keyboard.KEY_O] = false;
        }
        if ((Keyboard.isKeyDown(Keyboard.KEY_W) == true)
                && (configKeysPressed[Keyboard.KEY_W] == false)) {
            configKeysPressed[Keyboard.KEY_W] = true;

            physics.ToggleWorldSprings();
        } else if (Keyboard.isKeyDown(Keyboard.KEY_W) == false) {
            configKeysPressed[Keyboard.KEY_W] = false;
        }

        if ((Keyboard.isKeyDown(Keyboard.KEY_SPACE) == true)
                && (configKeysPressed[Keyboard.KEY_SPACE] == false)) {
            configKeysPressed[Keyboard.KEY_SPACE] = true;

            physics.ToggleIntegration();
        } else if (Keyboard.isKeyDown(Keyboard.KEY_SPACE) == false) {
            configKeysPressed[Keyboard.KEY_SPACE] = false;
        }
    }

    private void switchFullScreen() {
        configFullScreen = !configFullScreen;

        if (configDebugMode == true) {
            if (configFullScreen == true) {
                System.out.println("Entering Full Screen Mode");
            } else {
                System.out.println("Exiting Full Screen Mode");
            }
        }

        try {
            Display.setFullscreen(configFullScreen);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void render() {
        clearScene();

        GL11.glTranslatef(0.0f, 0.0f, Zoom);
        GL11.glRotatef(Altitude, 1.0f, 0.0f, 0.0f);
        GL11.glRotatef(Azimuth, 0.0f, 1.0f, 0.0f);

        GL11.glRotatef(-90.0f, 1.0f, 0.0f, 0.0f);   // make world z point up in view

        physics.Run();      // run the physics

        Altitude += dAltitude;
        Azimuth += dAzimuth;
        Zoom += dZoom;
    }

    private void clearScene() {
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
        GL11.glLoadIdentity();
    }

    private void shutdown() {
        Display.destroy();
    }

}
