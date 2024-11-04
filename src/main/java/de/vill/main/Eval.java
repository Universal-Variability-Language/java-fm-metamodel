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
import de.vill.util.ConvertFeatureCardinalityForOPB;
import de.vill.util.CountingResult;
import de.vill.util.ModelEvalResult;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Eval {

    public static final String WORKING_DIR = "/home/stefan/stefan-vill-master/tmp_eval/";
    public static final String D4_PATH = "/home/stefan/Downloads/d4/d4";
    public static final String P2D_PATH = "/home/stefan/p2d/target/release/p2d";

    public static void main(String[] args) throws IOException, InterruptedException {
        runSingleFile("test.uvl");
        //runEval();

    }
    public static void runSingleFile(String modelName) throws IOException, InterruptedException {
        File file = Paths.get(WORKING_DIR, modelName).toFile();
        ModelEvalResult result = evaluateModel(file);
        System.out.println("name;d4 time;p2d time;dimacs encoding time; opb encoding time; same result;solving-factor;encoding-factor;total-factor");
        System.out.println(result.toCSVString());
    }

    public static void runEval() throws IOException, InterruptedException {
        final int NUMBER_RUNS = 10;
        List<ModelEvalResult> resultList = new LinkedList<>();
        File folder = Paths.get(WORKING_DIR, "models").toFile();
        File[] files = folder.listFiles();
        for (File f : files) {
            List<ModelEvalResult> tmpResultList = new LinkedList<>();
            for (int i=0;i<NUMBER_RUNS;i++){
                tmpResultList.add(evaluateModel(f));
            }
            resultList.add(new ModelEvalResult(f.getName(),
                    tmpResultList.stream().mapToDouble(x -> x.TIME_TO_COMPUTE_D4).sum() / NUMBER_RUNS,
                    tmpResultList.stream().mapToDouble(x -> x.TIME_TO_COMPUTE_P2D).sum() / NUMBER_RUNS,
                    tmpResultList.get(0).MODEL_COUNT_D4,
                    tmpResultList.get(0).MODEL_COUNT_P2D,
                    tmpResultList.get(0).SAME_RESULT,
                    tmpResultList.stream().mapToDouble(x -> x.TO_DIMACS_TIME).sum() / NUMBER_RUNS,
                    tmpResultList.stream().mapToDouble(x -> x.TO_OPB_TIME).sum() / NUMBER_RUNS
            ));
        }
        StringBuilder result = new StringBuilder();
        result.append("name;d4 time;p2d time;dimacs encoding time; opb encoding time; same result;solving-factor;encoding-factor;total-factor\n");
        for (ModelEvalResult r : resultList) {
            result.append(r.toCSVString());
            result.append("\n");
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(Paths.get(WORKING_DIR, "result.csv").toString()))) {
            writer.append(result);
            writer.flush();
        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }
    }

    public static ModelEvalResult evaluateModel(File file) throws IOException, InterruptedException {
        double totalTimeDimacs = 0;
        final long NUMBER_RUNS = 1;
        for (int i=0;i<NUMBER_RUNS;i++){
            long start = System.currentTimeMillis();
            uvlToDimacsFeatureIDE(file);
            long finish = System.currentTimeMillis();
            totalTimeDimacs += finish - start;
        }

        double totalTimeOpb = 0;
        for (int i=0;i<NUMBER_RUNS;i++){
            long start = System.currentTimeMillis();
            uvlToOPB(file);
            long finish = System.currentTimeMillis();
            totalTimeOpb += finish - start;
        }
        CountingResult d4 = runD4(file);
        CountingResult p2d = runp2d(file);
        return new ModelEvalResult(file.getName(), d4.TIME_TO_COMPUTE, p2d.TIME_TO_COMPUTE, d4.MODEL_COUNT, p2d.MODEL_COUNT, d4.MODEL_COUNT.equals(p2d.MODEL_COUNT), totalTimeDimacs / NUMBER_RUNS, totalTimeOpb / NUMBER_RUNS);
    }

    public static CountingResult runD4(File file) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(D4_PATH, Paths.get(WORKING_DIR, "tmp_files", file.getName() + ".dimacs").toString(), "-mc");
        // Start the process
        Process process = processBuilder.start();
        // Read the output from the process
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        String time = "";
        String result = "";
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
            if (line.contains("Final time:")) {
                time = String.valueOf(Double.parseDouble(line.split("Final time: ")[1]) * 1000);
            }else if (line.charAt(0) == 's') {
                result = line.split("s ")[1];
            }
        }

        // Wait for the process to complete and get the exit value
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("d4 error");
        }
        System.out.println("Process exited with code: " + exitCode);
        return new CountingResult(Double.parseDouble(time), result);
    }

    public static CountingResult runp2d(File file) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(P2D_PATH, Paths.get(WORKING_DIR, "tmp_files", file.getName() + ".opb").toString());
        // Start the process
        Process process = processBuilder.start();
        // Read the output from the process
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        String time = "";
        String result = "";
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
            if (line.contains("time_to_compute:")) {
                time = line.split("time_to_compute: ")[1];
                time = time.substring(0, time.length() - 1);
            }else if (line.contains("result: ")) {
                result = line.split("result: ")[1];
            }
        }

        // Wait for the process to complete and get the exit value
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("p2d error");
        }
        System.out.println("Process exited with code: " + exitCode);
        return new CountingResult(Double.parseDouble(time), result);
    }

    public static void uvlToOPB(File file) throws IOException {
        UVLModelFactory uvlModelFactory = new UVLModelFactory();
        FeatureModel featureModel = loadUVLFeatureModelFromFile(file.toString());
        ConvertFeatureCardinalityForOPB convertFeatureCardinalityForOPB = new ConvertFeatureCardinalityForOPB();
        convertFeatureCardinalityForOPB.convertFeatureModel(featureModel);
        Set<LanguageLevel> levels = new HashSet<>();
        levels.add(LanguageLevel.AGGREGATE_FUNCTION);
        levels.add( LanguageLevel.STRING_CONSTRAINTS);
        uvlModelFactory.convertLanguageLevel(featureModel, levels);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(Paths.get(WORKING_DIR, "tmp_files", file.getName() + ".opb").toString()))) {
            writer.append(featureModel.toOPBString());
            writer.flush();
        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }
    }

    public static void uvlToDimacsFeatureIDE(File file) throws IOException {
        UVLModelFactory uvlModelFactory = new UVLModelFactory();
        FeatureModel featureModel = loadUVLFeatureModelFromFile(file.toString());
        Set<LanguageLevel> levels = new HashSet<>();
        levels.add(LanguageLevel.BOOLEAN_LEVEL);
        levels.add( LanguageLevel.TYPE_LEVEL);
        uvlModelFactory.convertExceptAcceptedLanguageLevel(featureModel, levels);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(Paths.get(WORKING_DIR, "tmp_files", file.getName() + "_sat_level.uvl").toString()))) {
            writer.append(featureModel.toString());
            writer.flush();
        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }
        LibraryManager.registerLibrary(FMCoreLibrary.getInstance());
        FMFormatManager.getInstance().addExtension(new UVLFeatureModelFormat());
        IFeatureModel fm = FeatureModelManager.load(Paths.get(WORKING_DIR, "tmp_files", file.getName() + "_sat_level.uvl"));
        FileHandler.save(Paths.get(WORKING_DIR, "tmp_files", file.getName() + ".dimacs"), fm, new DIMACSFormat());
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
