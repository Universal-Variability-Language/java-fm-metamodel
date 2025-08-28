package de.vill.main;

import de.vill.model.Attribute;
import de.vill.model.Feature;
import de.vill.model.FeatureModel;
import de.vill.model.Group;
import de.vill.model.constraint.Constraint;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class Example {
    public static void main(String[] args) throws IOException {
        // Load a UVL feature model from a file
        FeatureModel featureModel = loadUVLFeatureModelFromFile("src/test/resources/test_resources/parsing/complex/bike.uvl");
        System.out.println("Loaded feature model from file:" + featureModel.toString());

        // Traverse all features in the model using depth-first search
        System.out.println("Traversing all features:");
        traverseAllFeatures(featureModel.getRootFeature());

        // Retrieve constraints defined directly in the feature model (excluding submodels and attributes)
        List<Constraint> ownConstraints = featureModel.getOwnConstraints();
        System.out.println("Number of own constraints: " + ownConstraints.size());

        // If there are constraints, traverse the first one
        if (!ownConstraints.isEmpty()) {
            System.out.println("Traversing first own constraint:");
            traverseConstraint(ownConstraints.get(0));
        }

        // Retrieve all constraints, including those from submodels and attributes
        List<Constraint> allConstraints = featureModel.getConstraints();
        System.out.println("Total constraints (including submodels/attributes): " + allConstraints.size());

        // Access a specific attribute of a feature
        String featureName = "Brake";
        String attributeName = "Weight";
        Feature feature = featureModel.getFeatureMap().get(featureName);
        if (feature != null) {
            Attribute<?> attribute = feature.getAttributes().get(attributeName);
            if (attribute != null) {
                Object value = attribute.getValue();
                System.out.println("Attribute " + attributeName + " of feature " + featureName + " is: " + value.toString());
            } else {
                System.err.println("Attribute " + attributeName + " in feature " + featureName + " not found!");
            }
        } else {
            System.err.println("Feature " + featureName + " not found!");
        }

        // Make a feature abstract by adding an "abstract" attribute
        featureName = "Inch";
        feature = featureModel.getFeatureMap().get(featureName);
        if (feature != null) {
            feature.getAttributes().put("abstract", new Attribute<>("abstract", true, feature));
            System.out.println("Feature " + featureName + " set to abstract.");
        } else {
            System.err.println("Feature " + featureName + " not found!");
        }

        // Save the feature model as a single UVL file (ignoring submodels)
        String uvlModel = featureModel.toString();
        Path filePath = Paths.get("src/main/java/de/vill/main/modified_files/test_singleModel.uvl");
        try {
            Files.write(filePath, uvlModel.getBytes());
        } catch (IOException e) {
            System.err.println("Error saving single UVL model: " + e.getMessage());
            return;
        }
        System.out.println("Saved single UVL model to test_singleModel.uvl");

        // Save a decomposed UVL model with all submodels to individual files
        Map<String, String> modelList = featureModel.decomposedModelToString();
        for (Map.Entry<String, String> uvlSubModel : modelList.entrySet()) {
            Files.createDirectories(Paths.get("./src/main/java/de/vill/main/modified_files/subModels/"));
            filePath = Paths.get("./src/main/java/de/vill/main/modified_files/subModels/" + uvlSubModel.getKey() + ".uvl");
            Files.write(filePath, uvlSubModel.getValue().getBytes());
            System.out.println("Saved submodel: " + uvlSubModel.getKey());
        }

        // Compose a single UVL representation from a decomposed model and save it
        uvlModel = featureModel.composedModelToString();
        filePath = Paths.get("src/main/java/de/vill/main/modified_files/test_composedModel.uvl");
        Files.write(filePath, uvlModel.getBytes());
        System.out.println("Saved composed UVL model to test_composedModel.uvl");
    }

    // TODO: Add conversion example methods for converting between different formats (e.g., JSON, XML, etc.)

    // ----------------------------------- Utility Methods -----------------------------------

    /**
     * Loads a UVL feature model from a single file.
     * If the model is decomposed, all submodels must be present in the current working directory.
     * @param path The path to the UVL model file.
     * @return The loaded feature model.
     * @throws IOException If an I/O error occurs while reading the file.
     */
    private static FeatureModel loadUVLFeatureModelFromFile(String path) throws IOException {
        UVLModelFactory uvlModelFactory = new UVLModelFactory();
        FeatureModel featureModel = uvlModelFactory.parse(Paths.get(path));
        return featureModel;
    }

    /**
     * Traverses a constraint and all its sub-parts using depth-first search.
     * @param constraint The constraint to traverse.
     */
    private static void traverseConstraint(Constraint constraint) {
        System.out.println("\tVisiting constraint: " + constraint);
        for (Constraint subConstraint : constraint.getConstraintSubParts()) {
            traverseConstraint(subConstraint);
        }
    }

    /**
     * Traverses all features in the feature tree using depth-first search.
     * @param feature The root feature to start traversal from.
     */
    public static void traverseAllFeatures(Feature feature) {
        System.out.println("\tVisiting feature: " + feature.getFeatureName());
        for (Group group : feature.getChildren()) {
            for (Feature childFeature : group.getFeatures()) {
                // Optionally, skip submodel roots: if (!childFeature.isSubmodelroot()) { ... }
                traverseAllFeatures(childFeature);
            }
        }
    }
}
