package de.vill.main;


import de.ovgu.featureide.fm.core.ExtensionManager;
import de.ovgu.featureide.fm.core.analysis.cnf.CNF;
import de.ovgu.featureide.fm.core.analysis.cnf.Nodes;
import de.ovgu.featureide.fm.core.analysis.cnf.Variables;
import de.ovgu.featureide.fm.core.base.FeatureUtils;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.base.impl.*;
import de.ovgu.featureide.fm.core.editing.AdvancedNodeCreator;
import de.ovgu.featureide.fm.core.init.FMCoreLibrary;
import de.ovgu.featureide.fm.core.init.LibraryManager;
import de.ovgu.featureide.fm.core.io.IPersistentFormat;
import de.ovgu.featureide.fm.core.io.Problem;
import de.ovgu.featureide.fm.core.io.dimacs.DIMACSFormat;
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;
import de.ovgu.featureide.fm.core.io.manager.FileHandler;
import de.ovgu.featureide.fm.core.io.uvl.UVLFeatureModelFormat;
import de.vill.model.*;
import de.vill.model.Feature;
import de.vill.model.FeatureModel;
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

import static java.awt.SystemColor.text;

public class Eval {

    public static final String WORKING_DIR = "/home/stefan/stefan-vill-master/tmp_eval/";
    public static final String D4_PATH = "/home/stefan/Downloads/d4/d4";
    public static final String P2D_PATH = "/home/stefan/p2d/target/release/p2d";
    final static long NUMBER_RUNS = 1;
    public static boolean tseitin = false;

    public static FeatureModel genGroupCardN(int n){
        int lo = (int) (0.25 * n);
        int hi = (int) (0.75 * n);
        FeatureModel featureModel = new FeatureModel();
        Feature root = new Feature("r");
        featureModel.setRootFeature(root);
        Group g = new Group(Group.GroupType.GROUP_CARDINALITY);
        g.setCardinality(new Cardinality(lo, hi));
        root.getChildren().add(g);
        for (int i=1;i<=n;i++){
            Feature f = new Feature("f_" + i);
            g.getFeatures().add(f);
        }
        return featureModel;
    }

    public static void safeFMGroupCardFrom1ToN(String path, int n) throws FileNotFoundException {
        for (int i=1;i<=n;i++){
            FeatureModel fm = genGroupCardN(i);
            try (PrintWriter out = new PrintWriter(path + "/fm_" + i + ".uvl")) {
                out.println(fm.toString());
            }
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        //runSingleFile("./models/automotive01.uvl");
        //bs("rnd_models/fm0.uvl");
        //runEval();

        //safeFMGroupCardFrom1ToN("/home/stefan/stefan-vill-master/eval/genGroupCardModel", 10);

        //System.exit(0);

        final File UVL_FILE = new File(args[0]);
        final File TARGET_FILE = new File(args[1]);
        final Target target = args[2].equals("dimacs") ? Target.DIMACS : Target.OPB;
        tseitin = args[3] != null && args[3].equals("tseitin");

        switch (target) {
            case DIMACS: {
                uvlToDimacsFeatureIDE(UVL_FILE, TARGET_FILE);
                break;
            }
            case OPB: {
                uvlToOPB(UVL_FILE, TARGET_FILE);
                break;
            }
        }

    }

    public static enum Target{
        DIMACS,
        OPB

    }

    public static void bs(String modelName)throws IOException, InterruptedException {
        File file = Paths.get(WORKING_DIR, modelName).toFile();
        File opbTargetFile = Paths.get(WORKING_DIR, "tmp_files", file.getName() + ".opb").toFile();
        uvlToOPB(file, opbTargetFile);
        runp2d(file);
    }
    public static void runSingleFile(String modelName) throws IOException, InterruptedException {
        File file = Paths.get(WORKING_DIR, modelName).toFile();
        ModelEvalResult result = evaluateModel(file);
        System.out.println("name;d4 time;p2d time;dimacs encoding time; opb encoding time; same result;solving-factor;encoding-factor;total-factor");
        System.out.println(result.toCSVString());
    }

    public static void runEval() throws IOException, InterruptedException {
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
        File dimacsTargetFile = Paths.get(WORKING_DIR, "tmp_files", file.getName() + ".dimacs").toFile();
        File opbTargetFile = Paths.get(WORKING_DIR, "tmp_files", file.getName() + ".opb").toFile();
        for (int i=0;i<NUMBER_RUNS;i++){
            long start = System.currentTimeMillis();
            uvlToDimacsFeatureIDE(file, dimacsTargetFile);
            long finish = System.currentTimeMillis();
            totalTimeDimacs += finish - start;
        }

        double totalTimeOpb = 0;
        for (int i=0;i<NUMBER_RUNS;i++){
            long start = System.currentTimeMillis();
            uvlToOPB(file, opbTargetFile);
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
        String time = "0";
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

    public static void uvlToOPB(File modelFile, File targetFile) throws IOException {
        UVLModelFactory uvlModelFactory = new UVLModelFactory();
        FeatureModel featureModel = loadUVLFeatureModelFromFile(modelFile.toString());
        ConvertFeatureCardinalityForOPB convertFeatureCardinalityForOPB = new ConvertFeatureCardinalityForOPB();
        convertFeatureCardinalityForOPB.convertFeatureModel(featureModel);

        Set<LanguageLevel> levels = new HashSet<>();
        levels.add(LanguageLevel.AGGREGATE_FUNCTION);
        levels.add( LanguageLevel.STRING_CONSTRAINTS);
        uvlModelFactory.convertLanguageLevel(featureModel, levels);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(targetFile))) {
            if (tseitin) {
                writer.append(featureModel.toOPBString());
            }else{
                featureModel.writeOPBStringToFile(modelFile, targetFile,writer);
            }
            writer.flush();
        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }
    }

    public static void uvlToDimacsFeatureIDE(File modelFile, File targetFile) throws IOException {
        UVLModelFactory uvlModelFactory = new UVLModelFactory();
        FeatureModel featureModel = loadUVLFeatureModelFromFile(modelFile.toString());
        Set<LanguageLevel> levels = new HashSet<>();
        levels.add(LanguageLevel.BOOLEAN_LEVEL);
        levels.add(LanguageLevel.TYPE_LEVEL);
        uvlModelFactory.convertExceptAcceptedLanguageLevel(featureModel, levels);

        LibraryManager.registerLibrary(FMCoreLibrary.getInstance());
        FMFormatManager.getInstance().addExtension(new UVLFeatureModelFormat());

        IFeatureModel fm = getFeatureIdeFMFromString(modelFile.toPath(), featureModel.toString());
        FileHandler.save(targetFile.toPath(), fm, new DIMACSFormat());
    }


    public static IFeatureModel getFeatureIdeFMFromString(Path path, String content) throws IOException {
        final FileHandler<IFeatureModel> fileHandler = new FileHandler<>(path, null, null);
        final UVLFeatureModelFormat format = new UVLFeatureModelFormat();
        try {
            final IFeatureModel fm = FMFactoryManager.getInstance().getFactory(path, format).create();
            fileHandler.setObject(fm);
            fileHandler.setFormat(format);
            format.getInstance().read(fm, content, path);

        }catch (ExtensionManager.NoSuchExtensionException e) {
            throw new IOException("Error while parsing UVL model");
        }
        return fileHandler.getObject();
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
