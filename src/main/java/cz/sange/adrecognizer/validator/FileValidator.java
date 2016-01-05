package cz.sange.adrecognizer.validator;

import cz.sange.adrecognizer.ShellExecutor;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;
import javax.servlet.http.Part;
import java.io.IOException;
import java.util.ResourceBundle;

/**
 * Created by sange on 15/11/15.
 */
@FacesValidator("cz.sange.adrecognizer.validator.FileValidator")
public class FileValidator implements Validator {


    public void validate(FacesContext facesContext, UIComponent uiComponent, Object o) throws ValidatorException {
        ResourceBundle rd = ResourceBundle.getBundle("msg");

        Part file = (Part)o;
//        System.out.println("FILE: " + file.getContentType() + "; " + file.getSize() + "; " + file.getName());

        if(file == null){
            throw  new ValidatorException(new FacesMessage(rd.getString("chooseFile")));
        }

        if(!file.getContentType().startsWith("video") && !file.getContentType().contains("octet-stream") ) {
            throw  new ValidatorException(new FacesMessage(rd.getString("notVideo")));
        }
    }

}
