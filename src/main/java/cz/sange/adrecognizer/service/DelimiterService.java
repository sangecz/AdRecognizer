package cz.sange.adrecognizer.service;

import cz.sange.adrecognizer.model.Delimiter;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.util.List;

/**
 * Created by sange on 17/11/15.
 */

@Stateless
public class DelimiterService {

    @PersistenceContext
    private EntityManager em;

    public void addDelimiter(Delimiter d) {
        em.persist(d);
    }

    public List<Delimiter> getAll() {
        TypedQuery<Delimiter> l = em.createNamedQuery("delimiter.getAll", Delimiter.class);
        return l.getResultList();
    }

    public void deleteCrossedLists(long id) {
        TypedQuery<Long> l = em.createNamedQuery("delimiter.deleteItem", Long.class);
        l.setParameter("id", id);
        l.executeUpdate();
    }

}
