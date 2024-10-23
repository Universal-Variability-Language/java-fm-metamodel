package de.vill.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import de.vill.config.Configuration;
import de.vill.model.constraint.Constraint;
import de.vill.model.constraint.LiteralConstraint;
import de.vill.model.expression.AggregateFunctionExpression;
import de.vill.model.expression.Expression;
import de.vill.model.expression.LiteralExpression;
import de.vill.model.pbc.PBConstraint;
import de.vill.util.Util;

import static de.vill.util.Util.*;

/**
 * This class represents a feature model and all its sub feature models if the
 * model is composed.
 */
public class FeatureModel {
    private final Set<LanguageLevel> usedLanguageLevels = new HashSet<>();
    {
    	usedLanguageLevels.add(LanguageLevel.BOOLEAN_LEVEL);
    };
    private String namespace;
    private final List<Import> imports = new LinkedList<>();
    private Feature rootFeature;
    private final Map<String, Feature> featureMap = new HashMap<>();
    private final List<Constraint> ownConstraints = new LinkedList<>();
    private boolean explicitLanguageLevels = false;

    /*
     * These three lists are just for performance. They contain all points where
     * features are referenced e.g. in constraints or aggregate functions. The
     * corresponding feature can not be set during parsing, because the feature may
     * be in a sub-model which is not parsed then. Therefore. we store all this
     * objects to reference the features after everything is parsed without
     * searching for them.
     */
    private final List<LiteralConstraint> literalConstraints = new LinkedList<>();
    private final List<LiteralExpression> literalExpressions = new LinkedList<>();
    private final List<Expression> aggregateFunctionsWithRootFeature = new LinkedList<>();

    /**
     * Be very careful when creating your own featuremodel to set all information in
     * all objects. Especially when working with decomposed models it is important
     * to set all namespaces etc. right. If you are not sure look in the
     * {@link de.vill.main.UVLModelFactory} class how the feature model is assembled
     * there.
     */
    public FeatureModel() {
    }

    /**
     * Get a set with all in this featuremodel used language levels (major and
     * minor). This list contains the levels used in this feature model and all its
     * sub feature models (that are actually used) The returned set is a copy,
     * therefore changing it will NOT change the featuremodel.
     *
     * @return the used language levels as set
     */
    public Set<LanguageLevel> getUsedLanguageLevelsRecursively() {
        Set<LanguageLevel> languageLevels = new HashSet<>();
        languageLevels.addAll(getUsedLanguageLevels());
        for (Import importLine : imports) {
            if (importLine.isReferenced()) {
                languageLevels.addAll(importLine.getFeatureModel().getUsedLanguageLevelsRecursively());
            }
        }
        return languageLevels;
    }

    /**
     * Get a set with all in this featuremodel used language levels (major and
     * minor). This list contains the levels used in this feature model but not the
     * ones used in submodels. The returned set is no copy, therefore changing it
     * will change the featuremodel.
     *
     * @return the used language levels as set
     */
    public Set<LanguageLevel> getUsedLanguageLevels() {
        return usedLanguageLevels;
    }

    /**
     * Returns the namespace of the featuremodel. If no namespace is set, the
     * featuremodel returns the name of the root feature.
     *
     * @return the namespace of the feature model
     */
    public String getNamespace() {
        return namespace == null && rootFeature != null
            ? rootFeature.getFeatureName()
            : namespace;
    }

    /**
     * Setter for the namespace of the feature model.
     *
     * @param namespace Namespace of the feature model.
     */
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    /**
     * This method returns a list of the imports of the feature model. The list does
     * not contain recursivly all imports (including imports of imported feature
     * models), just the directly imported ones in this feature model. This list is
     * no clone. Changing it will actually change the feature model.
     *
     * @return A list containing all imports of this feature model.
     */
    public List<Import> getImports() {
        return imports;
    }

    /**
     * Get the root feature of the feature model
     *
     * @return root feature
     */
    public Feature getRootFeature() {
        return rootFeature;
    }

    /**
     * Set the root feature of the feature model
     *
     * @param rootFeature the root feature
     */
    public void setRootFeature(Feature rootFeature) {
        this.rootFeature = rootFeature;
    }

    /**
     * This map contains all features of this featuremodel and recursively all
     * features of its imported submodels. This means in a decomposed feature model
     * only the feature model object of the root feature model contains all features
     * over all sub feature models in this map. The key is the reference with
     * namespace and feature name viewed from this model (this means the key is how
     * the feature is referenced from this feature model). Therefore in the root
     * feature model the key for each feature is its complete reference from the
     * root feature model. When adding or deleting features from this map the
     * featuremodel gets inconsistent when not also adding / removing them from the
     * feature tree.
     *
     * @return A map with all features of the feature model
     */
    public Map<String, Feature> getFeatureMap() {
        return featureMap;
    }

    /**
     * A list with all the constraints of this featuremodel. This does not contain
     * the constraints of the imported sub feature models or constraints in feature
     * attributes.
     *
     * @return A list of the constraints of this featuremodel.
     */
    public List<Constraint> getOwnConstraints() {
        return ownConstraints;
    }

    /**
     * A list with all the constraints of that are part of feature attributes of
     * features in this feature model. This does not contain the constraints of the
     * imported features of sub feature models.
     *
     * @return A list of the constraints of feature attributes in this feature model.
     */
    public List<Constraint> getFeatureConstraints() {
        return getFeatureConstraints(getRootFeature());
    }

    private List<Constraint> getFeatureConstraints(Feature feature) {
        List<Constraint> featureConstraints = new LinkedList<>();
        Attribute<Constraint> featureConstraint = (Attribute<Constraint>) feature.getAttributes().get("constraint");
        Attribute<List<Constraint>> featureConstraintList = (Attribute<List<Constraint>>) feature.getAttributes().get("constraints");
        if (featureConstraint != null) {
            featureConstraints.add(featureConstraint.getValue());
        }
        if (featureConstraintList != null) {
            featureConstraints.addAll(featureConstraintList.getValue());
        }
        for (Group childGroup : feature.getChildren()) {
            for (Feature childFeature : childGroup.getFeatures()) {
                if (!childFeature.isSubmodelRoot()) {
                    featureConstraints.addAll(getFeatureConstraints(childFeature));
                }
            }
        }
        return featureConstraints;
    }

    /**
     * A list will all constraints of this feature model and recursively of all its
     * imported sub feature models (that are used). This includes constraints in
     * feature attributes. This list is not stored but gets calculated with every
     * call. This means changing a constraint will have an effect to the feature
     * model, but adding deleting constraints will have no effect. This must be done
     * in the corresponding ownConstraint lists of the feature models. This means
     * when calling this method on the root feature model it returns with all
     * constraints of the decomposed feature model.
     *
     * @return a list will all constraints of this feature model.
     */
    public List<Constraint> getConstraints() {
    	List<Constraint> featureConstraints = getFeatureConstraints();
        List<Constraint> constraints = new ArrayList<>(ownConstraints.size() + featureConstraints.size());
        constraints.addAll(ownConstraints);
		constraints.addAll(featureConstraints);
        for (Import importLine : imports) {
            if (importLine.isReferenced()) {
                constraints.addAll(importLine.getFeatureModel().getConstraints());
            }
        }
        return constraints;
    }

    /**
     * Boolean whether the used language levels are explicitly imported or not
     *
     * @return true if all levels must be explicitly imported, fales if not
     */
    public boolean isExplicitLanguageLevels() {
        return explicitLanguageLevels;
    }

    /**
     * Boolean whether the used language levels are explicitly imported or not. This
     * determines if the used levels are printed in the toSring method
     *
     * @param explicitLanguageLevels true if all levels must be explicitly imported,
     *                               false if not
     */
    public void setExplicitLanguageLevels(boolean explicitLanguageLevels) {
        this.explicitLanguageLevels = explicitLanguageLevels;
    }

    /**
     * Prints the current featuremodel without composing it with the other models.
     *
     * @return the uvl representation of the current model without submodels
     */
    @Override
    public String toString() {
        return toString(false, "");
    }

    public String toOPBString(){
        String result = "";
        result += getRootFeature().getFeatureName().replace(" ", "_") + " >= 1;\n";
        result += getRootFeature().toOPBString();
        int counter = 0;

        List<Constraint> constraints = getConstraints();
        constraints.addAll(getFeatureConstraints());

        List<PBConstraint> additionalConstraints = new LinkedList<>();

        for(Constraint constraint : constraints){
            HashMap<Integer, Constraint> subMap = new HashMap<>();
            int n = 1;
            constraint.extractTseitinSubConstraints(subMap, n, counter);
            var map = transformSubFormulas(subMap, additionalConstraints);
            List<PBConstraint> pbcList = transformImplicationMap(map, counter);
            for(PBConstraint PBConstraint : pbcList){
                result += PBConstraint.toString() + ";\n";
            }
            counter++;
        }

        for (PBConstraint PBConstraint : additionalConstraints){
            result += PBConstraint.toString() + ";\n";
        }
        return result;
    }

    public StringBuilder toSMT2string() {
        StringBuilder builder = new StringBuilder();
        builder.append("(set-logic QF_UF)\n");
        for (String name : this.getFeatureMap().keySet()) {
            builder.append("(declare-const ");
            builder.append(name);
            builder.append(" Bool)\n");
        }
        builder.append("(assert \n");
        builder.append(rootFeature.getFeatureName());
        builder.append(")\n");
        for (Group group : rootFeature.getChildren()){
            builder.append(group.toSMT2string());
        }
        for (Constraint constraint : this.getConstraints()) {
            builder.append("(assert \n");
            builder.append(constraint.toSMT2string());
            builder.append(")\n");
        }
        builder.append("(apply tseitin-cnf)");
        return builder;
    }

    /**
     * Returns a single uvl feature model composed out of all submodels. To avoid
     * naming conflicts all feature names are changed and a unique id is added. If
     * you do not work with decomposed models do not this method for printing, use
     * the other print methods instead!
     *
     * @return A single uvl representation of the composed model.
     */
    public String composedModelToString() {
        return this.toString(true, "");
    }

    /**
     * Prints the feature model and all it submodels to uvl according to its
     * original decomposition. This method should be called on the root feature
     * model.
     *
     * @return a map with namespaces of featuremodel and submodels as key and uvl
     *         strings as value
     */
    public Map<String, String> decomposedModelToString() {
        return decomposedModelToString("");
    }

    private Map<String, String> decomposedModelToString(String currentAlias) {
        Map<String, String> models = new HashMap<>();
        models.put(getNamespace(), toString(false, currentAlias));
        for (Import importLine : imports) {
            // only print sub feature models if the import is actually used
            if (importLine.isReferenced()) {
                String newCurrentAlias = ("".equals(currentAlias)
                    ? importLine.getAlias()
                    : currentAlias + "." + importLine.getAlias());
                models.putAll(importLine.getFeatureModel().decomposedModelToString(newCurrentAlias));
            }
        }
        return models;
    }

    private String toString(boolean withSubmodels, String currentAlias) {
        StringBuilder result = new StringBuilder();
        if (namespace != null) {
            result.append("namespace ");
            result.append(addNecessaryQuotes(namespace));
            result.append(Configuration.getNewlineSymbol());
            result.append(Configuration.getNewlineSymbol());
        }
        if (explicitLanguageLevels && !usedLanguageLevels.isEmpty()) {
            result.append("include");
            result.append(Configuration.getNewlineSymbol());
            Set<LanguageLevel> levelsToPrint = withSubmodels
                ? getUsedLanguageLevelsRecursively()
                : getUsedLanguageLevels();
            for (LanguageLevel languageLevel : getUsedLanguageLevels()) {
                result.append(Configuration.getTabulatorSymbol());
                if (!LanguageLevel.isMajorLevel(languageLevel)) {
                    result.append(LanguageLevel.valueOf(languageLevel.getValue() - 1).get(0).getName());
                    result.append(".");
                }
                result.append(languageLevel.getName());
                result.append(Configuration.getNewlineSymbol());
            }
            result.append(Configuration.getNewlineSymbol());
        }
        if (!imports.isEmpty() && !withSubmodels) {
            result.append("imports");
            result.append(Configuration.getNewlineSymbol());
            for (Import importLine : imports) {
                result.append(Configuration.getTabulatorSymbol());
                result.append(addNecessaryQuotes(importLine.getNamespace()));
                if (!importLine.getAlias().equals(importLine.getNamespace())) {
                    result.append(" as ");
                    result.append(addNecessaryQuotes(importLine.getAlias()));
                }
                result.append(Configuration.getNewlineSymbol());
            }
            result.append(Configuration.getNewlineSymbol());
        }
        if (rootFeature != null) {
            result.append("features");
            result.append(Configuration.getNewlineSymbol());
            result.append(Util.indentEachLine(withSubmodels
                ? getRootFeature().toString(withSubmodels, currentAlias)
                : getRootFeature().toStringAsRoot(currentAlias)));
            result.append(Configuration.getNewlineSymbol());
        }

        List<Constraint> constraintList;
        if (withSubmodels) {
            constraintList = new LinkedList<>();
            constraintList.addAll(ownConstraints);
            for (Import importLine : imports) {
                // only print the constraints of a submodel if the import is actually used
                if (importLine.isReferenced()) {
                    constraintList.addAll(importLine.getFeatureModel().getOwnConstraints());
                }
            }
        } else {
            constraintList = getOwnConstraints();
        }
        if (!constraintList.isEmpty()) {
            result.append("constraints");
            result.append(Configuration.getNewlineSymbol());
            for (Constraint constraint : constraintList) {
                result.append(Configuration.getTabulatorSymbol());
                result.append(constraint.toString(withSubmodels, currentAlias));
                result.append(Configuration.getNewlineSymbol());
            }
        }

        return result.toString();
    }

    /**
     * This list exists just for performance reasons when building a decomposed
     * feature model from several uvl files. This list does not have to be set, when
     * building your own feature model. It is also not necessary to update this list
     * when adding for example constraints.
     *
     * @return a list with all {@link LiteralConstraint} objects in the constraints
     *         of this feature model.
     */
    public List<LiteralConstraint> getLiteralConstraints() {
        return literalConstraints;
    }

    /**
     * This list exists just for performance reasons when building a decomposed
     * feature model from several uvl files. This list does not have to be set, when
     * building your own feature model. It is also not necessary to update this list
     * when adding for example constraints.
     *
     * @return a list with all {@link LiteralExpression} objects in the constraints
     *         of this feature model.
     */
    public List<LiteralExpression> getLiteralExpressions() {
        return literalExpressions;
    }

    /**
     * This list exists just for performance reasons when building a decomposed
     * feature model from several uvl files. This list does not have to be set, when
     * building your own feature model. It is also not necessary to update this list
     * when adding for example constraints.
     *
     * @return a list with all {@link AggregateFunctionExpression} objects in the
     *         constraints of this feature model.
     */
    public List<Expression> getAggregateFunctionsWithRootFeature() {
        return aggregateFunctionsWithRootFeature;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FeatureModel)) {
            return false;
        }

        if (!this.namespace.equals(((FeatureModel) obj).namespace)) {
            return false;
        }

        if (!this.getRootFeature().equals(((FeatureModel) obj).getRootFeature())) {
            return false;
        }


        if (this.getImports().size() != ((FeatureModel) obj).getImports().size()) {
            return false;
        }

        if (this.getUsedLanguageLevels().size() != ((FeatureModel) obj).getUsedLanguageLevels().size()) {
            return false;
        }

        for (LanguageLevel languageLevel: this.getUsedLanguageLevels()) {
            if (!(((FeatureModel) obj).getUsedLanguageLevels().contains(languageLevel))) {
                return false;
            }
        }

        List<Import> objImports = ((FeatureModel) obj).getImports();
        for (Import thisImport: this.getImports()) {
            final Optional<Import> identicalImport = objImports.stream()
                    .filter(imp -> imp.getFeatureModel().equals(thisImport.getFeatureModel()))
                    .findFirst();

            if (!identicalImport.isPresent()) {
                return false;
            }
        }

        if (this.getOwnConstraints().size() != ((FeatureModel) obj).getOwnConstraints().size()) {
            return false;
        }

        List<Constraint> objConstraints = ((FeatureModel) obj).getOwnConstraints();
        for (final Constraint constraint : this.getOwnConstraints()) {
            if (!objConstraints.contains(constraint)) {
                return false;
            }
        }

        return true;
    }
}
