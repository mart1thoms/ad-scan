package fr.reniti.adscan.member;

import java.util.List;

/** Fixed list of choices for the "formation" field, shared by the admin and public registration forms. */
public final class FormationOptions {

    public static final String PERSONNEL_ENSIM = "Personnel ENSIM";
    public static final String EXTERIEUR = "Extérieur";

    /** Study years of an ENSIM student; filière / alternance only apply from the 3rd year on. */
    public static final List<String> STUDENT_YEARS = List.of("1A", "2A", "3A", "4A", "5A");
    public static final List<String> YEARS_WITH_FILIERE = List.of("3A", "4A", "5A");

    public static final List<String> FILIERES = List.of("A&I", "Info");

    public static final List<String> ALL = List.of(
            "1A",
            "2A",
            "3A",
            "4A",
            "5A",
            PERSONNEL_ENSIM,
            EXTERIEUR
    );

    private FormationOptions() {
    }

    public static boolean isStudent(String formation) {
        return formation != null && STUDENT_YEARS.contains(formation);
    }

    public static boolean hasFiliere(String formation) {
        return formation != null && YEARS_WITH_FILIERE.contains(formation);
    }
}
