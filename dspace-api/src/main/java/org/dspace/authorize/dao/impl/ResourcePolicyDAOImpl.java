/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authorize.dao.impl;

import java.sql.SQLException;
import java.util.List;

import org.dspace.authorize.AuthorizableEntity;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.dao.ResourcePolicyDAO;
import org.dspace.content.DSpaceObject;
import org.dspace.core.AbstractHibernateDAO;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;

/**
 * Hibernate implementation of the Database Access Object interface class for the ResourcePolicy object.
 * This class is responsible for all database calls for the ResourcePolicy object and is autowired by spring
 * This class should never be accessed directly.
 *
 * @author kevinvandevelde at atmire.com
 */
public class ResourcePolicyDAOImpl extends AbstractHibernateDAO<ResourcePolicy> implements ResourcePolicyDAO
{

    protected ResourcePolicyDAOImpl()
    {
        super();
    }

    @Override
    public List<ResourcePolicy> findByDso(Context context, AuthorizableEntity dso) throws SQLException {
        Criteria criteria = createCriteria(context, ResourcePolicy.class);
        criteria.add(Restrictions.and(
                Restrictions.eq("dSpaceObject", dso)
        ));
        return list(criteria);
    }

    @Override
    public List<ResourcePolicy> findByDsoAndType(Context context, AuthorizableEntity dso, String type) throws SQLException
    {
        Criteria criteria = createCriteria(context, ResourcePolicy.class);
        criteria.add(Restrictions.and(
                Restrictions.eq("dSpaceObject", dso),
                Restrictions.eq("rptype", type)
        ));
        return list(criteria);
    }

    @Override
    public List<ResourcePolicy> findByGroup(Context context, Group group) throws SQLException {
        Criteria criteria = createCriteria(context, ResourcePolicy.class);
        criteria.add(Restrictions.eq("epersonGroup", group));
        return list(criteria);
    }

    @Override
    public List<ResourcePolicy> findByDSoAndAction(Context context, AuthorizableEntity dso, int actionId) throws SQLException
    {
        Criteria criteria = createCriteria(context, ResourcePolicy.class);
        try {
			criteria.add(Restrictions.and(
			        Restrictions.eq("dSpaceObject", DSpaceObject.unwrapProxy(dso)),
			        Restrictions.eq("actionId", actionId)
			));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
        return list(criteria);
    }

    @Override
    public List<ResourcePolicy> findByTypeGroupAction(Context context, AuthorizableEntity dso, Group group, int action) throws SQLException {
        Criteria criteria = createCriteria(context, ResourcePolicy.class);
        criteria.add(Restrictions.and(
                Restrictions.eq("dSpaceObject", dso),
                Restrictions.eq("epersonGroup", group),
                Restrictions.eq("actionId", action)
        ));
        criteria.setMaxResults(1);
        return list(criteria);
    }
    
    @Override
    public List<ResourcePolicy> findByTypeGroupActionExceptId(Context context, AuthorizableEntity dso, Group group, int action, int notPolicyID) throws SQLException {
        Criteria criteria = createCriteria(context, ResourcePolicy.class);
        criteria.add(Restrictions.and(
                Restrictions.eq("dSpaceObject", dso),
                Restrictions.eq("epersonGroup", group),
                Restrictions.eq("actionId", action)
        ));
        criteria.add(Restrictions.and(Restrictions.not(Restrictions.eq("id", notPolicyID))));
        return list(criteria);
    }
    
     public List<ResourcePolicy> findByEPersonGroupTypeIdAction(Context context, EPerson e, List<Group> groups, int action, int type_id) throws SQLException
     {
         Criteria criteria = createCriteria(context, ResourcePolicy.class);
         criteria.add(Restrictions.and(
                  Restrictions.eq("resourceTypeId", type_id),
                  Restrictions.eq("actionId", action),
                  (Restrictions.or(
                    Restrictions.eq("eperson", e),
                    Restrictions.in("epersonGroup", groups)
                    ))
                 ));
         return list(criteria);
     }

    @Override
    public void deleteByDso(Context context, AuthorizableEntity dso) throws SQLException
    {
        String queryString = "delete from ResourcePolicy where dSpaceObject= :dSpaceObject";
        Query query = createQuery(context, queryString);
        query.setParameter("dSpaceObject", dso);
        query.executeUpdate();
    }

    @Override
    public void deleteByDsoAndAction(Context context, AuthorizableEntity dso, int actionId) throws SQLException {
        String queryString = "delete from ResourcePolicy where dSpaceObject= :dSpaceObject AND actionId= :actionId";
        Query query = createQuery(context, queryString);
        query.setParameter("dSpaceObject", dso);
        query.setInteger("actionId", actionId);
        query.executeUpdate();
    }

    @Override
    public void deleteByDsoAndType(Context context, AuthorizableEntity dso, String type) throws SQLException {
        String queryString = "delete from ResourcePolicy where dSpaceObject.id = :dsoId AND rptype = :rptype";
        Query query = createQuery(context, queryString);
        query.setParameter("dsoId", dso.getID());
        query.setString("rptype", type);
        query.executeUpdate();
    }

    @Override
    public void deleteByGroup(Context context, Group group) throws SQLException {
        String queryString = "delete from ResourcePolicy where epersonGroup= :epersonGroup";
        Query query = createQuery(context, queryString);
        query.setParameter("epersonGroup", group);
        query.executeUpdate();
    }

    @Override
    public void deleteByDsoGroupPolicies(Context context, AuthorizableEntity dso, Group group) throws SQLException {
        String queryString = "delete from ResourcePolicy where dSpaceObject = :dso AND epersonGroup= :epersonGroup";
        Query query = createQuery(context, queryString);
        query.setParameter("dso", dso);
        query.setParameter("epersonGroup", group);
        query.executeUpdate();

    }

    @Override
    public void deleteByDsoEPersonPolicies(Context context, AuthorizableEntity dso, EPerson ePerson) throws SQLException {
        String queryString = "delete from ResourcePolicy where dSpaceObject= :dso AND eperson= :eperson";
        Query query = createQuery(context, queryString);
        query.setParameter("dso", dso);
        query.setParameter("eperson", ePerson);
        query.executeUpdate();

    }

    @Override
    public void deleteByDsoAndTypeNotEqualsTo(Context context, AuthorizableEntity dso, String type) throws SQLException {

        String queryString = "delete from ResourcePolicy where dSpaceObject=:dso AND rptype <> :rptype";
        Query query = createQuery(context, queryString);
        query.setParameter("dso", dso);
        query.setParameter("rptype", type);
        query.executeUpdate();
    }
}
