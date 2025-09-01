package de.vill.conversion;

import de.vill.model.FeatureModel;
import de.vill.model.LanguageLevel;

import java.util.Set;

public interface IConversionStrategy {
    Set<LanguageLevel> getLevelsToBeRemoved();

    Set<LanguageLevel> getTargetLevelsOfConversion();

    /**
     * Converts the given feature model according to the implemented strategy.
     * <p>
     * The {@code rootFeatureModel} provides the context of the entire feature model,
     * while {@code featureModel} is the (sub-)model that will be modified during the conversion.
     * <p>
     * Typically, only {@code featureModel} is changed; {@code rootFeatureModel} is used for reference.
     * If the entire model should be converted, both parameters can reference the same instance.
     *
     * @param rootFeatureModel the root or complete feature model, used as context/reference (not modified)
     * @param featureModel the feature model (or submodel) to be converted (will be modified)
     */
    void convertFeatureModel(FeatureModel rootFeatureModel, FeatureModel featureModel);
}
