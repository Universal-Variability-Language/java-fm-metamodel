package de.vill.main;

import de.vill.model.FeatureModel;
import de.vill.model.LanguageLevel;
import de.vill.model.constraint.Constraint;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static de.vill.util.Util.transformSubFormulas;

public class Eval {

    public static final String WORKING_DIR = "/home/stefan/eval/";

    public static void main(String[] args) throws IOException {
        uvlToOPB("test");
        uvlToDimacs("test");
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

    public static void uvlToDimacs(String modelName) throws IOException {
        uvlToSMT2(modelName + ".uvl", modelName + ".smt2");
        smt2ToDimacs(modelName + ".smt2", modelName + ".dimacs");
    }

    public static void smt2ToDimacs(String smt2Path, String dimacsPath) throws IOException {
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
            System.out.println("Process exited with code: " + exitCode);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void uvlToSMT2(String uvlPath, String dimacsPath) throws IOException {
        UVLModelFactory uvlModelFactory = new UVLModelFactory();
        FeatureModel featureModel = loadUVLFeatureModelFromFile(WORKING_DIR + uvlPath);
        Set<LanguageLevel> levels = new HashSet<>();
        levels.add(LanguageLevel.BOOLEAN_LEVEL);
        uvlModelFactory.convertExceptAcceptedLanguageLevel(featureModel, levels);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(WORKING_DIR + "test.smt2"))) {
            writer.append(featureModel.toSMT2string());
            writer.flush();
        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }
    }

    private static FeatureModel loadUVLFeatureModelFromFile(String path) throws IOException {
        Path filePath = Paths.get(path);
        String content = new String(Files.readAllBytes(filePath));
        UVLModelFactory uvlModelFactory = new UVLModelFactory();
        FeatureModel featureModel = uvlModelFactory.parse(content);
        return featureModel;
    }
}
