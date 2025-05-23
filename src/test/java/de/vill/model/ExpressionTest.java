package de.vill.model;

import de.vill.main.UVLModelFactory;
import de.vill.model.constraint.Constraint;
import de.vill.model.constraint.ExpressionConstraint;
import de.vill.model.expression.Expression;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ExpressionTest {

    @Test
    void testMathExpressionsModel() {
        // get the feature model and parse it using the UVLModelFactory
        UVLModelFactory uvlModelFactory = new UVLModelFactory();
        FeatureModel featureModel = uvlModelFactory.parse(Paths.get("src/test/resources/parsing/expressions.uvl"));
        // select features for evaluation
        Map<String, Feature> featureMap = featureModel.getFeatureMap();
        Set<Feature> selectedFeatures = Set.of(
                featureMap.get("B"),
                featureMap.get("C")
        );
    
        // test the constraints
        for (Constraint constraint : featureModel.getOwnConstraints()) {
            if (constraint instanceof ExpressionConstraint) {
                ExpressionConstraint exprConstraint = (ExpressionConstraint) constraint;
                Expression left = exprConstraint.getLeft();
                Expression right = exprConstraint.getRight();
                double expected = right.evaluate(selectedFeatures);
                double actual = left.evaluate(selectedFeatures);
                assertEquals(expected, actual, 0.0001, "Constraint '" + exprConstraint );
            }
        }
    }

    @Test
    void testAggregateFunctions() {
        // get the feature model and parse it using the UVLModelFactory
        UVLModelFactory uvlModelFactory = new UVLModelFactory();
        FeatureModel featureModel = uvlModelFactory.parse(Paths.get("src/test/resources/parsing/aggregateFunctions.uvl"));
        // select features for evaluation
        Map<String, Feature> featureMap = featureModel.getFeatureMap();
        Set<Feature> selectedFeatures = Set.of(
                featureMap.get("B"),
                featureMap.get("C")
        );
    
        // test the constraints
        for (Constraint constraint : featureModel.getOwnConstraints()) {
            if (constraint instanceof ExpressionConstraint) {
                ExpressionConstraint exprConstraint = (ExpressionConstraint) constraint;
                Expression left = exprConstraint.getLeft();
                Expression right = exprConstraint.getRight();
                double expected = right.evaluate(selectedFeatures);
                double actual = left.evaluate(selectedFeatures);
                assertEquals(expected, actual, 0.0001, "Constraint '" + exprConstraint );
            }
        }
    }
}