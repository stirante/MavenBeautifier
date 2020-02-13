package com.stirante.mavenBeautifier;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class Installer {

    public static void main(String[] args) {
        String mavenHome = null;
        String[] path = System.getenv("path").split(";");
        for (String s : path) {
            File f = new File(s);
            File[] mvn = f.listFiles(file -> file.getName().equals("mvn"));
            if (mvn != null && mvn.length != 0) {
                mavenHome = f.getAbsolutePath();
                break;
            }
        }
        if (mavenHome == null) {
            System.err.println("Maven not found in path!");
            System.exit(1);
        }
        try {
            Files.copy(Installer.class.getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .openStream(), new File(mavenHome, "../MavenBeautifier.jar").toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("Failed to save jar file!");
            e.printStackTrace();
            System.exit(1);
        }
        try {
            Files.copy(Installer.class.getResourceAsStream("/mvnb"), new File(mavenHome, "mvnb").toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("Failed to save script!");
            e.printStackTrace();
            System.exit(1);
        }
        System.out.println("Maven beautifier has been installed!");
    }

}
