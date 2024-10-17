package de.vill.model;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

import de.vill.config.Configuration;
import de.vill.model.constraint.Constraint;
import de.vill.util.Util;

/**
 * This class represents all kinds of groups (or, alternative, mandatory,
 * optional, cardinality)
 */
public class Group {
    /**
     * An enum with all possible group types.
     */
    public enum GroupType {
        OR, ALTERNATIVE, MANDATORY, OPTIONAL, GROUP_CARDINALITY
    }

    /// The type of the group (if type is GROUP_CARDINALITY or FEATURE_CARDINALITY
    /// lower and upper bound must be set!)
    public GroupType GROUPTYPE;
    private final List<Feature> features;
    private Cardinality cardinality;
    private Feature parent;

    public StringBuilder toSMT2string() {
        StringBuilder builder = new StringBuilder();

        builder.append("(assert \n");
        switch (GROUPTYPE) {
            case OR:
                builder.append("(= \n");
                builder.append(parent.getFeatureName());
                builder.append("\n(or\n");
                for (Feature feature : features) {
                    builder.append(feature.getFeatureName());
                    builder.append("\n");
                }

                builder.append("))\n");
                break;
            case ALTERNATIVE:
                builder.append("(and \n");
                builder.append("(= \n");
                builder.append(parent.getFeatureName());
                builder.append("\n(or\n");
                for (Feature feature : features) {
                    builder.append(feature.getFeatureName());
                    builder.append("\n");
                }

                builder.append("))\n");
                for (int i=0;i<features.size();i++){
                    for (int j=i+1;j<features.size();j++) {
                        builder.append("(or\n");
                        builder.append("(not ");
                        builder.append(features.get(i).getFeatureName());
                        builder.append(")\n");
                        builder.append("(not ");
                        builder.append(features.get(j).getFeatureName());
                        builder.append("))\n");
                    }
                }
                builder.append(")\n");
                break;
            case OPTIONAL:
                builder.append("(and\n");
                for (Feature feature : features) {
                    builder.append("(=> ");
                    builder.append(feature.getFeatureName());
                    builder.append(" ");
                    builder.append(parent.getFeatureName());
                    builder.append(")\n");
                }
                builder.append(")\n");
                break;
            case MANDATORY:
                builder.append("(and\n");
                for (Feature feature : features) {
                    builder.append("(= ");
                    builder.append(feature.getFeatureName());
                    builder.append(" ");
                    builder.append(parent.getFeatureName());
                    builder.append(")\n");
                }
                builder.append(")\n");
                break;
        }
        builder.append(")\n");
        for (Feature feature : features) {
            for (Group group : feature.getChildren()) {
                builder.append(group.toSMT2string());
            }
        }
        return builder;
    }

    /**
     * The constructor of the group class.
     *
     * @param groupType The type of the group.
     */
    public Group(GroupType groupType) {
        this.GROUPTYPE = groupType;
        features = new LinkedList<Feature>() {

        	private static final long serialVersionUID = 3856024708694486586L;

            @Override
            public boolean add(Feature e) {
                if (super.add(e)) {
                    e.setParentGroup(Group.this);
                    return true;
                }
                return false;
            }

            @Override
            public void add(int index, Feature element) {
                super.set(index, element);
                element.setParentGroup(Group.this);
            }

            @Override
            public Feature remove(int index) {
                Feature f = super.remove(index);
                f.setParentGroup(null);
                return f;
            }

            @Override
            public boolean remove(Object o) {
                if (super.remove(o)) {
                    ((Feature) o).setParentGroup(null);
                    return true;
                }
                return false;
            }

            @Override
            public boolean addAll(int index, Collection<? extends Feature> c) {
                if (super.addAll(index, c)) {
                    c.forEach(e -> e.setParentGroup(Group.this));
                    return true;
                }
                return false;
            }

            @Override
            public void clear() {
                ListIterator<Feature> it = this.listIterator();
                while (it.hasNext()) {
                    it.next().setParentGroup(null);
                }
                super.clear();
            }

            @Override
            public Feature set(int index, Feature element) {
                Feature f;
                if ((f = super.set(index, element)) != null) {
                    f.setParentGroup(Group.this);
                    return f;
                }
                return null;
            }

            class FeatureIterator implements ListIterator<Feature> {
                private ListIterator<Feature> itr;
                Feature lastReturned;

                public FeatureIterator(ListIterator<Feature> itr) {
                    this.itr = itr;
                }

                @Override
                public boolean hasNext() {
                    return itr.hasNext();
                }

                @Override
                public Feature next() {
                    lastReturned = itr.next();
                    return lastReturned;
                }

                @Override
                public boolean hasPrevious() {
                    return itr.hasPrevious();
                }

                @Override
                public Feature previous() {
                    lastReturned = itr.previous();
                    return lastReturned;
                }

                @Override
                public int nextIndex() {
                    return itr.nextIndex();
                }

                @Override
                public int previousIndex() {
                    return itr.previousIndex();
                }

                @Override
                public void remove() {
                    itr.remove();
                    lastReturned.setParentGroup(null);
                }

                @Override
                public void set(Feature e) {
                    itr.set(e);
                    lastReturned.setParentGroup(null);
                    e.setParentGroup(Group.this);
                }

                @Override
                public void add(Feature e) {
                    itr.add(e);
                    e.setParentGroup(Group.this);
                }

            }

            @Override
            public ListIterator<Feature> listIterator(int index) {
                return new FeatureIterator(super.listIterator(index));
            }

            ;
        };
    }

    public void setCardinality(Cardinality cardinality) {
        this.cardinality = cardinality;
    }

    public Cardinality getCardinality() {
        return this.cardinality;
    }

    /**
     * Returns all children of the group (Features). If there are non an empty list
     * is returned, not null. The returned list is not a copy, which means editing
     * this list will actually change the group.
     *
     * @return A list with all child features.
     */
    public List<Feature> getFeatures() {
        return features;
    }

    /**
     * Returns the group and all its children as uvl valid string.
     *
     * @param withSubmodels true if the feature model is printed as composed
     *                      feature model with all its submodels as one model, false
     *                      if the model is printed with separated sub models
     * @param currentAlias  the namspace from the one referencing the group (or the
     *                      features in the group) to the group (or the features in
     *                      the group)
     * @return uvl representation of the group
     */
    public String toString(boolean withSubmodels, String currentAlias) {
        StringBuilder result = new StringBuilder();

        switch (GROUPTYPE) {
            case OR:
                result.append("or");
                break;
            case ALTERNATIVE:
                result.append("alternative");
                break;
            case OPTIONAL:
                result.append("optional");
                break;
            case MANDATORY:
                result.append("mandatory");
                break;
            case GROUP_CARDINALITY:
                result.append(getCardinalityAsSting());
                break;
        }

        result.append(Configuration.getNewlineSymbol());

        for (Feature feature : features) {
            result.append(Util.indentEachLine(feature.toString(withSubmodels, currentAlias)));
        }

        return result.toString();
    }

    private String getCardinalityAsSting() {
        return cardinality.toString();
    }

    @Override
    public Group clone() {
        Group group = new Group(GROUPTYPE);
        group.setCardinality(cardinality);
        for (Feature feature : getFeatures()) {
            group.getFeatures().add(feature.clone());
        }
        for (Feature feature : group.getFeatures()) {
            feature.setParentGroup(group);
        }
        return group;
    }

    /**
     * Returns the parent feature of the group.
     *
     * @return Parent Feature of the group.
     */
    public Feature getParentFeature() {
        return parent;
    }

    /**
     * Sets the parent feature of the group.
     *
     * @param parent The parent feature of the group.
     */
    public void setParentFeature(Feature parent) {
        this.parent = parent;
    }

	@Override
	public int hashCode() {
		return Objects.hash(GROUPTYPE, cardinality, parent);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null || getClass() != obj.getClass())
			return false;
		Group other = (Group) obj;
		return GROUPTYPE == other.GROUPTYPE
				&& Objects.equals(cardinality, other.cardinality)
				&& Objects.equals(parent, other.parent)
				&& Objects.equals(parent.getChildren(), other.parent.getChildren());
	}
}
