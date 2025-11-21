package de.vill.model.building;

import de.vill.exception.ParseError;
import de.vill.model.*;

public class DefaultUVLElementFactory extends AbstractUVLElementFactory{

    @Override
    public Feature createFeature(String name){
        return new Feature(name);
    }

    @Override
    public <T> Attribute<T> createAttribute(String name, T value, Feature correspondingFeature){
        return new Attribute<>(name, value, correspondingFeature);
    }   

    @Override
    public GlobalAttribute createGlobalAttribute(String identifier, FeatureModel featureModel){
        return new GlobalAttribute(identifier, featureModel);
    }
}
