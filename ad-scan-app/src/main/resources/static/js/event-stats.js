/**
 * Renders the live statistics of an event (payload of GET /api/events/{id}/stats) into a container.
 * Shared by the scan page (live panel) and the event page (after the event is closed).
 */
function renderEventStats(container, stats) {
    const total = stats.totalEntries || 0;
    const students = stats.ensim || 0;
    const withFiliere = (stats.alternants || 0) + (stats.nonAlternants || 0);

    container.replaceChildren(
        statTiles([
            {label: "Entrées validées", value: total, tone: "ok"},
            {label: "Paiement en attente", value: stats.pendingPayment || 0, tone: "warn"},
            {label: "Doublons", value: stats.duplicateScans || 0, tone: "muted"}
        ]),
        statGroup("Provenance", "sur " + plural(total, "entrée"), [
            {label: "ENSIM (étudiants)", value: students},
            {label: "Extérieur", value: stats.exterieur || 0},
            {label: "Personnel ENSIM", value: stats.personnel || 0},
            ...(stats.unknownOrigin ? [{label: "Non renseigné", value: stats.unknownOrigin}] : [])
        ], total),
        statGroup("Année d'étude", "sur " + plural(students, "étudiant"),
            Object.entries(stats.years || {}).map(([year, count]) => ({label: year, value: count})), students),
        statGroup("Filière (3A–5A)", "sur " + plural(withFiliere, "étudiant"),
            Object.entries(stats.filieres || {}).map(([filiere, count]) => ({label: filiere, value: count})), withFiliere),
        statGroup("Alternance (3A–5A)", "sur " + plural(withFiliere, "étudiant"), [
            {label: "Alternants", value: stats.alternants || 0},
            {label: "Non alternants", value: stats.nonAlternants || 0}
        ], withFiliere)
    );
}

function plural(count, noun) {
    return count + " " + noun + (count > 1 ? "s" : "");
}

function percent(value, base) {
    if (!base) {
        return "—";
    }
    return Math.round((value / base) * 100) + " %";
}

function statTiles(tiles) {
    const row = document.createElement("div");
    row.className = "stat-tiles";
    for (const tile of tiles) {
        const el = document.createElement("div");
        el.className = "stat-tile " + tile.tone;
        const value = document.createElement("div");
        value.className = "stat-tile-value";
        value.textContent = String(tile.value);
        const label = document.createElement("div");
        label.className = "stat-tile-label";
        label.textContent = tile.label;
        el.append(value, label);
        row.appendChild(el);
    }
    return row;
}

function statGroup(title, baseLabel, rows, base) {
    const group = document.createElement("div");
    group.className = "stat-group";

    const heading = document.createElement("div");
    heading.className = "stat-group-title";
    const strong = document.createElement("strong");
    strong.textContent = title;
    const small = document.createElement("small");
    small.textContent = baseLabel;
    heading.append(strong, small);
    group.appendChild(heading);

    for (const row of rows) {
        const line = document.createElement("div");
        line.className = "stat-row";

        const label = document.createElement("span");
        label.className = "stat-row-label";
        label.textContent = row.label;

        const bar = document.createElement("span");
        bar.className = "stat-row-bar";
        const fill = document.createElement("span");
        fill.className = "stat-row-fill";
        fill.style.width = base ? Math.round((row.value / base) * 100) + "%" : "0";
        bar.appendChild(fill);

        const figures = document.createElement("span");
        figures.className = "stat-row-figures";
        figures.textContent = percent(row.value, base) + " · " + row.value;

        line.append(label, bar, figures);
        group.appendChild(line);
    }
    return group;
}
