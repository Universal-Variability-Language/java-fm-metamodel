package de.vill.parsing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.vill.main.UVLModelFactory;
import de.vill.model.FeatureModel;

public class ParsingTests {

    // root directory
    private static final String TEST_MODEL_PREFIX = "src" + File.separator + "test" + File.separator + "resources" + File.separator + "parsing" + File.separator;

    // sub directories
    private static final String CONCEPTS_MODEL_PREFIX = TEST_MODEL_PREFIX + "concepts" + File.separator;
    private static final String FAULTY_MODEL_PREFIX = TEST_MODEL_PREFIX + "faulty" + File.separator;
    private static final String EXPLICIT_LANGUAGE_LEVELS_PREFIX = TEST_MODEL_PREFIX + File.separator + "language-levels" + File.separator;
    private static final String COMPLEX_MODEL_PREFIX = TEST_MODEL_PREFIX + "complex" + File.separator;
    private static final String COMPOSITION_MODEL_PREFIX = TEST_MODEL_PREFIX + "composition" + File.separator;
    
    // sub sub directories
    private static final String NESTED_MODEL_PREFIX = COMPOSITION_MODEL_PREFIX + "nested" + File.separator;

    // Boolean level models
    private static final String SIMPLE_BOOLEAN = CONCEPTS_MODEL_PREFIX + "boolean.uvl";
    private static final String NAMESPACE = CONCEPTS_MODEL_PREFIX + "namespace.uvl"; 
    private static final String ATTRIBUTES = CONCEPTS_MODEL_PREFIX + "attributes.uvl";
    private static final String GROUP_CARDINALITY = CONCEPTS_MODEL_PREFIX + "cardinality.uvl";

    // Arithmetic level models
    private static final String ARITHMETIC_SIMPLE_CONSTRAINTS = CONCEPTS_MODEL_PREFIX + "arithmetic-simpleconstraints.uvl";
    private static final String AGGREGATE = CONCEPTS_MODEL_PREFIX + "aggregate.uvl";
    private static final String FEATURE_CARDINALITY = CONCEPTS_MODEL_PREFIX + "feature-cardinality.uvl";
    private static final String MATHEMATICAL_EXPRESSION = TEST_MODEL_PREFIX + "expressions.uvl";

    // Type level models
    private static final String TYPE = CONCEPTS_MODEL_PREFIX + "arithmetic-simpleconstraints.uvl";
    private static final String STRING_CONSTRAINTS = CONCEPTS_MODEL_PREFIX + "string-constraints.uvl";
    private static final String STRING_ATTRIBUTES = CONCEPTS_MODEL_PREFIX + "string-attributes.uvl";

    // Language level models
    private static final String ALL = EXPLICIT_LANGUAGE_LEVELS_PREFIX + "all.uvl";
    private static final String ARITHMETIC = EXPLICIT_LANGUAGE_LEVELS_PREFIX + "arithmetic.uvl";
    private static final String BOOLEAN_LEVEL = EXPLICIT_LANGUAGE_LEVELS_PREFIX + "boolean.uvl";
    private static final String TYPE_LEVEL = EXPLICIT_LANGUAGE_LEVELS_PREFIX + "type.uvl";

    // Comments and Syntax
    private static final String COMMENTS = TEST_MODEL_PREFIX + "comments.uvl";
    private static final String DASH_MODEL = TEST_MODEL_PREFIX + "dash.uvl";

    // Faulty UVL models
    private static final String ILLEGAL_NAME = FAULTY_MODEL_PREFIX + "illegalname.uvl";
    private static final String MISSING_REFRENCE = FAULTY_MODEL_PREFIX + "missingreference.uvl";
    private static final String WRONG_INDENT = FAULTY_MODEL_PREFIX + "wrongindent.uvl";
    private static final String SAME_FEATURE_NAMES = FAULTY_MODEL_PREFIX + "same_feature_names.uvl";
    private static final String WRONG_ATTRIBUTE_NAME = FAULTY_MODEL_PREFIX + "wrong_attribute_name.uvl";

    // Composition uvl models
    private static final String COMPOSITION_ROOT = COMPOSITION_MODEL_PREFIX +  "composition_root.uvl";
    private static final String NESTED_COMPOSITION_ROOT = COMPOSITION_MODEL_PREFIX + "nested_main.uvl";
    private static final String NESTED_SUB_COMPOSITION_ROOT = NESTED_MODEL_PREFIX  + "nested_sub.uvl";

    // Generated models
    private static final String GENERATED_DIRECTORY = TEST_MODEL_PREFIX + "generated";
    

    // -------------------------------------------  Boolean level models  -------------------------------------------
    @Test
    void testBooleanModel() throws Exception {
        testModelParsing(SIMPLE_BOOLEAN);
    }

    @Test
    void testNamespace() throws Exception {
        testModelParsing(NAMESPACE);
    }

    @Test
    void testAttributes() throws Exception {
        testModelParsing(ATTRIBUTES);
    }

    @Test
    void testGroupCardinality() throws Exception {
        testModelParsing(GROUP_CARDINALITY);
    }

    // ------------------------------------------ Arithmetic level models --------------------------------------------

    @Test
    void testArithmethicSimpleConstraints() throws Exception {
        testModelParsing(ARITHMETIC_SIMPLE_CONSTRAINTS);
    }

    @Test
    void testAggregate() throws Exception {
        testModelParsing(AGGREGATE);
    }

    @Test
    void testFeatureCardinality() throws Exception {
        testModelParsing(FEATURE_CARDINALITY);
    }

    @Test
    void testMathematicalExpression() throws Exception {
        testModelParsing(MATHEMATICAL_EXPRESSION);
    }

    // ------------------------------------------- Type level models ------------------------------------------------

    @Test
    void testType() throws Exception {
        testModelParsing(TYPE);
    }

    @Test
    void testStringConstraints() throws Exception {
        testModelParsing(STRING_CONSTRAINTS);
    }

    @Test
    void testStringAttributes() throws Exception {
        testModelParsing(STRING_ATTRIBUTES);
    }

    // ------------------------------------------- Language level models --------------------------------------------

    @Test
    void checkLanguageLevelInclude() throws Exception {
        testModelParsing(ALL);
        testModelParsing(ARITHMETIC);
        testModelParsing(BOOLEAN_LEVEL);
        testModelParsing(TYPE_LEVEL);

    }

    // --------------------------------------- Comments and Syntax ----------------------------------------------------

    @Test
    void checkComments() throws Exception {
        testModelParsing(COMMENTS);
    }

    @Test
    void testDashModel() throws Exception {
        testModelParsing(DASH_MODEL);
    }

    // ------------------------------------------- Faulty models -----------------------------------------------------

    @Test
    void checkFaultyModels() throws Exception {
        testModelParsing(ILLEGAL_NAME, false);
        testModelParsing(MISSING_REFRENCE, false);
        testModelParsing(WRONG_INDENT, false);
        testModelParsing(SAME_FEATURE_NAMES, false);
        testModelParsing(WRONG_ATTRIBUTE_NAME, false);
    }

    // ------------------------------------------- Complex models -----------------------------------------------------
    @Test
    void checkComplexModels() throws Exception {
        checkAllModelsInDirectory(COMPLEX_MODEL_PREFIX);
    }

    // -------------------------------------------- Generated models -------------------------------------------------

    @Test
    void checkGeneratedModels() throws Exception {
        checkAllModelsInDirectory(GENERATED_DIRECTORY);
    }

    // ------------------------------------------- Composition models ------------------------------------------------

    @Test
    void checkCompositionModels() throws  Exception {
        testModelParsing(COMPOSITION_ROOT);
        testModelParsing(NESTED_COMPOSITION_ROOT);
        testModelParsing(NESTED_SUB_COMPOSITION_ROOT);
    }






    // ------------------------------------------- Helper methods ----------------------------------------------------

    private void testModelParsing(String path) {
        testModelParsing(path, true);
    }

    public static FeatureModel testModelParsing(String path, boolean expectSuccess) {
        UVLModelFactory uvlModelFactory = new UVLModelFactory();
        FeatureModel result = null;
        boolean error = false;
        try {
            result = uvlModelFactory.parse(Paths.get(path));
        } catch (Exception e) {
            error = true;
        }

        boolean actuallySuccess = !error & (result != null);

        assertEquals(expectSuccess, actuallySuccess, "Exception! Failed to assert: " + path);

        return result;
    }

    private void checkAllModelsInDirectory(String dirPath) throws IOException {
        List<File> files = new ArrayList<>();
        getAllModelsInDirectory(dirPath, files);
        for (File file : files) {
            testModelParsing(file.getAbsolutePath());
        }
    }

    public static void getAllModelsInDirectory(String directoryName, List<File> files) {
        File directory = new File(directoryName);

        File[] fileList = directory.listFiles();
        if (fileList != null)
            for (File file : fileList) {
                if (file.isFile()) {
                    if (file.getName().endsWith(".uvl")) {
                        files.add(file);
                    }
                } else if (file.isDirectory()) {
                    getAllModelsInDirectory(file.getAbsolutePath(), files);
                }
            }
    }

}
