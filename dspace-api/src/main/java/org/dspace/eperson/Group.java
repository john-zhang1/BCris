/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.DSpaceObject;
import org.dspace.content.DSpaceObjectLegacySupport;
import org.dspace.content.IMetadataValue;
import org.dspace.content.MetadataSchema;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.proxy.HibernateProxyHelper;

/**
 * Class representing a group of e-people.
 *
 * @author David Stuve
 */
@Entity
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, include = "non-lazy")
@Table(name = "epersongroup" )
public class Group extends DSpaceObject implements DSpaceObjectLegacySupport
{

    @Transient
    public static final String ANONYMOUS = "Anonymous";

    @Transient
    public static final String ADMIN = "Administrator";
    
    @Transient
    public static final String EMBARGO = "Embargo";

    /**
     * Initial value is set to 2 since 0 & 1 are reserved for anonymous & administrative uses
     */
    @Column(name="eperson_group_id", insertable = false, updatable = false)
    private Integer legacyId;

    /** This Group may not be deleted or renamed. */
    @Column
    private Boolean permanent = false;

    @Column(length = 250, unique = true)
    private String name;

    /** lists of epeople and groups in the group */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "epersongroup2eperson",
            joinColumns = {@JoinColumn(name = "eperson_group_id") },
            inverseJoinColumns = {@JoinColumn(name = "eperson_id") }
    )
    private final List<EPerson> epeople = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "group2group",
            joinColumns = {@JoinColumn(name = "parent_id") },
            inverseJoinColumns = {@JoinColumn(name = "child_id") }
    )
    private final List<Group> groups = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "groups")
    private final List<Group> parentGroups = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "supervisorGroups")
    private final List<WorkspaceItem> supervisedItems = new ArrayList<>();

    @Transient
    private boolean groupsChanged;

    /**
     * Protected constructor, create object using:
     * {@link org.dspace.eperson.service.GroupService#create(Context)}
     *
     */
    protected Group()
    {

    }

    void addMember(EPerson e)
    {
        getMembers().add(e);
    }

    /**
     * Return EPerson members of a Group
     */
    public List<EPerson> getMembers()
    {
        return epeople;
    }

    void addMember(Group g)
    {
        getMemberGroups().add(g);
        groupsChanged = true;
    }

    void addParentGroup(Group group)
    {
        getParentGroups().add(group);
        groupsChanged = true;
    }

    void removeParentGroup(Group group)
    {
        getParentGroups().remove(group);
        groupsChanged = true;
    }

    boolean remove(EPerson e)
    {
        return getMembers().remove(e);
    }

    boolean remove(Group g)
    {
        groupsChanged = true;
        return getMemberGroups().remove(g);
    }

    boolean contains(Group g)
    {
        return getMemberGroups().contains(g);
    }

    boolean contains(EPerson e)
    {
        return getMembers().contains(e);
    }

    List<Group> getParentGroups() {
        return parentGroups;
    }

    /**
     * Return Group members of a Group.
     */
    public List<Group> getMemberGroups()
    {
        return groups;
    }

    /**
     * Return <code>true</code> if <code>other</code> is the same Group as
     * this object, <code>false</code> otherwise
     *
     * @param obj
     *            object to compare to
     *
     * @return <code>true</code> if object passed in represents the same group
     *         as this object
     */
     @Override
     public boolean equals(Object obj)
     {
         if (obj == null)
         {
             return false;
         }
         Class<?> objClass = HibernateProxyHelper.getClassWithoutInitializingProxy(obj);
         if (getClass() != objClass)
         {
             return false;
         }
         final Group other = (Group) obj;
         return this.getID().equals(other.getID());
     }

     @Override
     public int hashCode()
     {
         int hash = 7;
         hash = 59 * hash + this.getID().hashCode();
         hash = 59 * hash + (this.getName() != null? this.getName().hashCode():0);
         return hash;
     }



    @Override
    public int getType()
    {
        return Constants.GROUP;
    }

    @Override
    public String getName()
    {
        return name;
    }

    /** Change the name of this Group. */
    void setName(String name) throws SQLException
    {
        if(!StringUtils.equals(this.name, name) && !isPermanent()) {
            this.name = name;
            groupsChanged = true;
        }
    }

    public boolean isGroupsChanged() {
        return groupsChanged;
    }

    public void clearGroupsChanged() {
        this.groupsChanged = false;
    }

    @Override
    public Integer getLegacyId() {
        return legacyId;
    }

    public List<WorkspaceItem> getSupervisedItems() {
        return supervisedItems;
    }

    /**
     * May this Group be renamed or deleted?  (The content of any group may be
     * changed.)
     *
     * @return true if this Group may not be renamed or deleted.
     */
    public Boolean isPermanent()
    {
        return permanent;
    }

    /**
     * May this Group be renamed or deleted?  (The content of any group may be
     * changed.)
     *
     * @param permanence true if this group may not be renamed or deleted.
     */
    void setPermanent(boolean permanence)
    {
        permanent = permanence;
        setModified();
    }
    
    public String getTypeText() {
        return Constants.typeText[Constants.GROUP];
    }

	@Override
	public List<String> getMetadataValue(String mdString) {
		return EPersonServiceFactory.getInstance().getGroupService().getAllMetadata(this, mdString);
	}

	@Override
	public List<IMetadataValue> getMetadataValueInDCFormat(String mdString) {
		return EPersonServiceFactory.getInstance().getGroupService().getMetadataByMetadataString(this, mdString);
	}
    
	public boolean isNotRelevant() {
		List<String> values = getMetadataValue(MetadataSchema.GROUP_SCHEMA +".notrelevant");
        return (values!= null && !values.isEmpty());
    }
    
	@Override
	public boolean haveHierarchy() {
		return false;
	}
}
