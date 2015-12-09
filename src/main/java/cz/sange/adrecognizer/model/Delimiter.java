package cz.sange.adrecognizer.model;

import javax.persistence.*;
import java.util.Arrays;

/**
 * Created by sange on 17/11/15.
 */

@Entity
@Table(name = "delimiter")
@NamedQueries({
@NamedQuery(name = "delimiter.getAll", query = "SELECT d FROM Delimiter d"),
@NamedQuery(name = "delimiter.deleteItem", query = "DELETE FROM Delimiter d WHERE d.id = :id")
})
public class Delimiter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private long id;

    @Column(name = "name")
    private String name;

    private byte [] data;

    public Delimiter() {
    }

    public Delimiter(String name, byte[] data) {
        this.name = name;
        this.data = data;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
