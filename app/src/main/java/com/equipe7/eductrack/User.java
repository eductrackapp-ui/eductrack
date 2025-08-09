package com.equipe7.eductrack;

public class User {
    public String username;
    public String email;
    public String role;
    public String firstName;
    public String lastName;
    public String school;
    public String relation;
    public String institutionName;
    public String companyName;
    public String timNumber;
    public String sdmcCode;
    public boolean acceptedTerms;

    // Constructor vide requis pour Firebase
    public User() {}

    // Constructor avec paramètres
    public User(String username, String email, String role) {
        this.username = username;
        this.email = email;
        this.role = role;
        this.acceptedTerms = false;
    }

    // Getters et Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getSchool() { return school; }
    public void setSchool(String school) { this.school = school; }

    public String getRelation() { return relation; }
    public void setRelation(String relation) { this.relation = relation; }

    public String getInstitutionName() { return institutionName; }
    public void setInstitutionName(String institutionName) { this.institutionName = institutionName; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getTimNumber() { return timNumber; }
    public void setTimNumber(String timNumber) { this.timNumber = timNumber; }

    public String getSdmcCode() { return sdmcCode; }
    public void setSdmcCode(String sdmcCode) { this.sdmcCode = sdmcCode; }

    public boolean isAcceptedTerms() { return acceptedTerms; }
    public void setAcceptedTerms(boolean acceptedTerms) { this.acceptedTerms = acceptedTerms; }
} 