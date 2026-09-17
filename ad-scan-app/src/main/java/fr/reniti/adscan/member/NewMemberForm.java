package fr.reniti.adscan.member;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public class NewMemberForm {

    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String formation;
    private String filiere;
    private Boolean alternant;
    // ISO so the value renders as yyyy-MM-dd, the only format an <input type=\"date\"> accepts.
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate = LocalDate.now();
    // ISO so the value renders as yyyy-MM-dd, the only format an <input type=\"date\"> accepts.
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getFormation() {
        return formation;
    }

    public void setFormation(String formation) {
        this.formation = formation;
    }

    public String getFiliere() {
        return filiere;
    }

    public void setFiliere(String filiere) {
        this.filiere = filiere;
    }

    public Boolean getAlternant() {
        return alternant;
    }

    public void setAlternant(Boolean alternant) {
        this.alternant = alternant;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}
