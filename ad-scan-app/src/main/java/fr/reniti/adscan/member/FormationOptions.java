package fr.reniti.adscan.member;

import java.util.List;

/** Fixed list of choices for the "formation" field, shared by the admin and public registration forms. */
public final class FormationOptions {

    public static final List<String> ALL = List.of(
            "1A",
            "2A",
            "3A A&I ou INFO / Case Alternant",
            "4A A&I ou INFO / Case alternant",
            "5A A&I ou INFO / Case alternant",
            "Personnel ENSIM",
            "Extérieur"
    );

    private FormationOptions() {
    }
}
