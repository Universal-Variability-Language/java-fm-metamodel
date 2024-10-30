package de.vill.main;


import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.base.impl.*;
import de.ovgu.featureide.fm.core.init.FMCoreLibrary;
import de.ovgu.featureide.fm.core.init.LibraryManager;
import de.ovgu.featureide.fm.core.io.dimacs.DIMACSFormat;
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;
import de.ovgu.featureide.fm.core.io.manager.FileHandler;
import de.ovgu.featureide.fm.core.io.uvl.UVLFeatureModelFormat;
import de.vill.model.FeatureModel;
import de.vill.model.LanguageLevel;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public class Eval {

    public static final String WORKING_DIR = "/home/stefan/stefan-vill-master/tmp_eval/";

    public static void main(String[] args) throws IOException {
        long start = System.currentTimeMillis();
        uvlToOPB("test");
        long finish = System.currentTimeMillis();
        System.out.println("opb_encoding: " + (finish - start));
        start = System.currentTimeMillis();
        uvlToDimacsFeatureIDE("test");
        finish = System.currentTimeMillis();
        System.out.println("dimacs_encoding: " + (finish - start));
    }

    public static void uvlToOPB(String modelName) throws IOException {
        UVLModelFactory uvlModelFactory = new UVLModelFactory();
        FeatureModel featureModel = loadUVLFeatureModelFromFile(WORKING_DIR + modelName + ".uvl");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(WORKING_DIR + "test.opb"))) {
            writer.append(featureModel.toOPBString());
            writer.flush();
        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }
    }

    public static void uvlToDimacsFeatureIDE(String modelName) throws IOException {
        LibraryManager.registerLibrary(FMCoreLibrary.getInstance());
        FMFormatManager.getInstance().addExtension(new UVLFeatureModelFormat());
        IFeatureModel featureModel = FeatureModelManager.load(Paths.get(WORKING_DIR + modelName + ".uvl"));
        FileHandler.save(Paths.get(WORKING_DIR + modelName + ".dimacs"), featureModel, new DIMACSFormat());
    }

    public static void uvlToDimacsZ3(String modelName) throws IOException {
        uvlToSMT2(modelName + ".uvl", modelName + ".smt2");
        smt2ToDimacs(modelName + ".smt2", modelName + ".dimacs");
    }

    public static void smt2ToDimacs(String smt2Path, String dimacsPath) throws IOException {
        long start = System.currentTimeMillis();
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("python3", WORKING_DIR + "smt2ToDimacs.py", WORKING_DIR + smt2Path, WORKING_DIR + dimacsPath);

        try {
            // Start the process
            Process process = processBuilder.start();
            // Read the output from the process
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            // Wait for the process to complete and get the exit value
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("Z3 error");
            }
            System.out.println("Process exited with code: " + exitCode);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        long finish = System.currentTimeMillis();
        System.out.println("z3_with_io: " + (finish - start));
    }

    public static void uvlToSMT2(String uvlPath, String dimacsPath) throws IOException {
        long start = System.currentTimeMillis();
        UVLModelFactory uvlModelFactory = new UVLModelFactory();
        FeatureModel featureModel = loadUVLFeatureModelFromFile(WORKING_DIR + uvlPath);
        Set<LanguageLevel> levels = new HashSet<>();
        levels.add(LanguageLevel.BOOLEAN_LEVEL);
        levels.add(LanguageLevel.TYPE_LEVEL);
        uvlModelFactory.convertExceptAcceptedLanguageLevel(featureModel, levels);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(WORKING_DIR + "test.smt2"))) {
            writer.append(featureModel.toSMT2string());
            writer.flush();
        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }
        long finish = System.currentTimeMillis();
        System.out.println("smt_encoding_with_io: " + (finish - start));
    }

    private static FeatureModel loadUVLFeatureModelFromFile(String path) throws IOException {
        Path filePath = Paths.get(path);
        String content = new String(Files.readAllBytes(filePath));
        UVLModelFactory uvlModelFactory = new UVLModelFactory();
        FeatureModel featureModel = uvlModelFactory.parse(content);
        return featureModel;
    }
}
