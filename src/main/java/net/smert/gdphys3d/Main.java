package net.smert.gdphys3d;

import com.jdotsoft.jarloader.JarClassLoader;

/**
 *
 * @author Jason Sorensen <sorensenj@smert.net>
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            JarClassLoader jarClassLoader = new JarClassLoader();
            jarClassLoader.invokeMain("net.smert.gdphys3d.Demo", args);
        } catch (Throwable e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

}
