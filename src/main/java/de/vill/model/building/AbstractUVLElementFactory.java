package de.vill.model.building;

import de.vill.exception.ParseError;
import de.vill.model.*;
import de.vill.model.constraint.LiteralConstraint;

public abstract class AbstractUVLElementFactory {

    public abstract Feature createFeature(String name);

    public abstract <T> Attribute<T> createAttribute(String name, T value, Feature correspondingFeature);

    public abstract GlobalAttribute createGlobalAttribute(String identifier, FeatureModel featureModel);

}