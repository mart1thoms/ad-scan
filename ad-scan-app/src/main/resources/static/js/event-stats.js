/**
 * Live statistics of an event (payload of GET /api/events/{id}/stats).
 *
 *   const view = createStatsView(container, { onAdjust: (category, delta) => ... });
 *   view.update(stats);
 *
 * Shows a segmented control "Tous / Adhérents / Sans badge" over the three breakdowns of the payload,
 * and, when `onAdjust` is given and the event is open, a +/- panel over the walk-in categories.
 */
function createStatsView(container, options) {
    const settings = options || {};
    let view = "all";
    let latest = null;

    const VIEWS = [
        {key: "all", label: "Tous"},
        {key: "members", label: "Adhérents"},
        {key: "walkIns", label: "Sans badge"}
    ];

    function render() {
        if (!latest) {
            return;
        }
        const stats = latest;
        const breakdown = stats[view] || stats.all || {};
        const total = breakdown.total || 0;
        const students = breakdown.ensim || 0;
        const withFiliere = (breakdown.alternants || 0) + (breakdown.nonAlternants || 0);

        const children = [
            statTiles([
                {label: "Entrées", value: stats.totalEntries || 0, tone: "ok"},
                {label: "Adhérents", value: (stats.members && stats.members.total) || 0, tone: "muted"},
                {label: "Sans badge", value: (stats.walkIns && stats.walkIns.total) || 0, tone: "muted"},
                {label: "Paiement en attente", value: stats.pendingPayment || 0, tone: "warn"}
            ]),
            segmentedControl(),
            statGroup("Provenance", "sur " + plural(total, "entrée"), [
                {label: "ENSIM (étudiants)", value: students},
                {label: "Extérieur", value: breakdown.exterieur || 0},
                {label: "Personnel ENSIM", value: breakdown.personnel || 0},
                ...(breakdown.unknownOrigin ? [{label: "Non renseigné", value: breakdown.unknownOrigin}] : [])
            ], total),
            statGroup("Année d'étude", "sur " + plural(students, "étudiant"),
                Object.entries(breakdown.years || {}).map(([year, count]) => ({label: year, value: count})), students),
            statGroup("Filière (3A–5A)", "sur " + plural(withFiliere, "étudiant"),
                Object.entries(breakdown.filieres || {}).map(([filiere, count]) => ({label: filiere, value: count})),
                withFiliere),
            statGroup("Alternance (3A–5A)", "sur " + plural(withFiliere, "étudiant"), [
                {label: "Alternants", value: breakdown.alternants || 0},
                {label: "Non alternants", value: breakdown.nonAlternants || 0}
            ], withFiliere)
        ];

        if (view === "walkIns") {
            children.push(manualCategoriesPanel(stats));
        }
        if (stats.duplicateScans) {
            const note = document.createElement("p");
            note.className = "stat-note";
            note.textContent = plural(stats.duplicateScans, "badge") + " scanné" + (stats.duplicateScans > 1 ? "s" : "")
                + " plusieurs fois (doublons, non comptés).";
            children.push(note);
        }
        container.replaceChildren(...children);
    }

    function segmentedControl() {
        const control = document.createElement("div");
        control.className = "segmented";
        control.setAttribute("role", "tablist");
        for (const item of VIEWS) {
            const button = document.createElement("button");
            button.type = "button";
            button.className = "segmented-item" + (item.key === view ? " active" : "");
            button.textContent = item.label;
            button.addEventListener("click", () => {
                view = item.key;
                render();
            });
            control.appendChild(button);
        }
        return control;
    }

    function manualCategoriesPanel(stats) {
        const panel = document.createElement("div");
        panel.className = "stat-group manual-categories";
        const heading = document.createElement("div");
        heading.className = "stat-group-title";
        const strong = document.createElement("strong");
        strong.textContent = "Entrées sans badge";
        const small = document.createElement("small");
        small.textContent = stats.closed ? "soirée fermée" : (settings.onAdjust ? "ajuster avec + / −" : "");
        heading.append(strong, small);
        panel.appendChild(heading);

        const categories = stats.manualCategories || [];
        if (categories.length === 0) {
            const empty = document.createElement("p");
            empty.className = "stat-empty";
            empty.textContent = "Aucune entrée sans badge pour le moment.";
            panel.appendChild(empty);
        }
        for (const category of categories) {
            const line = document.createElement("div");
            line.className = "manual-category";
            const label = document.createElement("span");
            label.className = "manual-category-label";
            label.textContent = category.label;
            const count = document.createElement("span");
            count.className = "manual-category-count";
            count.textContent = String(category.count);
            line.append(label, count);
            if (settings.onAdjust && !stats.closed) {
                line.appendChild(adjustButton("−", () => settings.onAdjust(category, -1)));
                line.appendChild(adjustButton("+", () => settings.onAdjust(category, +1)));
            }
            panel.appendChild(line);
        }
        return panel;
    }

    function adjustButton(text, onClick) {
        const button = document.createElement("button");
        button.type = "button";
        button.className = "btn btn-small adjust-btn";
        button.textContent = text;
        button.addEventListener("click", onClick);
        return button;
    }

    return {
        update(stats) {
            latest = stats;
            render();
        },
        get view() {
            return view;
        }
    };
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
