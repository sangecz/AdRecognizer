package cz.sange.adrecognizer.bean;

import cz.sange.adrecognizer.service.DelimiterService;
import cz.sange.adrecognizer.model.Delimiter;

import javax.ejb.EJB;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import java.util.List;

/**
 * Created by sange on 17/11/15.
 */
@ManagedBean(name = "database")
@SessionScoped
public class DatabaseManagedBean {

    @EJB
    private DelimiterService delimiterService;

    public void addDelimiter(Delimiter d) {
        delimiterService.addDelimiter(d);
    }

    public List<Delimiter> getAllDelimiters() {
        return delimiterService.getAll();
    }

    public void deleteDelimiter(long id){
        delimiterService.deleteCrossedLists(id);
    }


}
