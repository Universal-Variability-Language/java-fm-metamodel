package de.vill.main;

import de.vill.model.*;
import de.vill.model.constraint.Constraint;
import de.vill.model.pbc.Literal;
import de.vill.model.pbc.PBConstraint;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Example {
    public static void main(String[] args) throws IOException {

        FeatureModel featureModel = loadUVLFeatureModelFromFile("test.uvl");
        UVLModelFactory uvlModelFactory = new UVLModelFactory();
        //System.out.println(featureModel.toOPBString());



        Set<LanguageLevel> levels = new HashSet<>();
        levels.add(LanguageLevel.BOOLEAN_LEVEL);
        levels.add( LanguageLevel.TYPE_LEVEL);
        uvlModelFactory.convertExceptAcceptedLanguageLevel(featureModel, levels);
        System.out.println(featureModel.composedModelToString());







        /*
        var c = featureModel.getConstraints().get(0);
        HashMap<Integer, Constraint> subMap = new HashMap<>();
        int n = 1;
        c.extractTseitinSubConstraints(subMap, n);
        var map = transformSubFormulas(subMap);
        List<PBCConstraint> pbcList = transformImplicationMap(map);
        System.out.println(pbcList.toString());

         */
//dimacsToOPB("/home/stefan/stefan-vill-master/tmp_eval/tmp5.dimacs");

    }

    private static void dimacsToOPB(String path) throws IOException {
        String[] lines = new String(Files.readAllBytes(Paths.get(path))).split("\n");
        List<PBConstraint> constraintList = new LinkedList<>();

        for(String line : lines){
            if(!line.startsWith("c") && !line.startsWith("p")){
                PBConstraint PBConstraint = new PBConstraint();
                PBConstraint.literalList = new LinkedList<>();
                PBConstraint.k = 1;
                for(String l : line.split(" ")){
                    int v = Integer.parseInt(l);
                    if(v > 0){
                        Literal literal = new Literal();
                        literal.name = "x_" + v;
                        literal.factor = 1;
                        PBConstraint.literalList.add(literal);
                    }else if(v < 0){
                        Literal literal = new Literal();
                        literal.name = "x_" + (-1 * v);
                        literal.factor = -1;
                        PBConstraint.literalList.add(literal);
                        PBConstraint.k = PBConstraint.k -1;
                    }
                }
                constraintList.add(PBConstraint);
            }
        }

        for(PBConstraint PBConstraint : constraintList){
            System.out.println(PBConstraint.toString() + ";");
        }

    }

    /**
     * Parse uvl model from file (if decomposed all submodels must be in the current working directory)
     *
     * @param path path to the file with uvl model
     * @return the uvl model described in the file
     * @throws IOException for io exceptions while loading the file content
     */
    private static FeatureModel loadUVLFeatureModelFromFile(String path) throws IOException {
        Path filePath = Paths.get(path);
        String content = new String(Files.readAllBytes(filePath));
        UVLModelFactory uvlModelFactory = new UVLModelFactory();
        FeatureModel featureModel = uvlModelFactory.parse(content);
        return featureModel;
    }

    /**
     * Parse a decomposed uvl model where all submodels are in a directory and named according to their namespaces.
     *
     * @param rootModelPath Path to the uvl root model file
     * @param subModelDir   Path to the directory with all submodels
     * @return the uvl model described in the file
     * @throws IOException for io exceptions while loading the file content
     */
    private static FeatureModel loadUVLFeatureModelFromDirectory(String rootModelPath, String subModelDir) throws IOException {
        Path filePath = Paths.get(rootModelPath);
        String content = new String(Files.readAllBytes(filePath));
        UVLModelFactory uvlModelFactory = new UVLModelFactory();
        FeatureModel featureModel = uvlModelFactory.parse(content, subModelDir);
        return featureModel;
    }


    private static void traverseConstraint(Constraint constraint) {
        for (Constraint subConstraint : constraint.getConstraintSubParts()) {
            //... do something with constraint
            traverseConstraint(subConstraint);
        }
    }

    public static void traverseAllFeatures(Feature feature) {
        for (Group group : feature.getChildren()) {
            for (Feature childFeature : group.getFeatures()) {
                //... do something with feature
                //or stop at submodel with if(!childfeature.isSubmodelroot())
                traverseAllFeatures(childFeature);
            }
        }
    }
}
