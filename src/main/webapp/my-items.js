const reportCategories = [
    "Electronics",
    "Books",
    "ID Card",
    "Wallet or Purse",
    "Clothing",
    "Keys",
    "Other"
];

(async function loadMyItems() {
    const message = document.getElementById("message");
    const container = document.getElementById("itemsContainer");
    try {
        const response = await fetch("my-items", {
            credentials: "same-origin",
            headers: { Accept: "application/json" }
        });
        if (response.status === 401) {
            window.CampusCsrf.redirectToLogin();
            return;
        }
        if (!response.ok) throw new Error("Unable to load reports.");

        const items = await response.json();
        container.replaceChildren();
        message.hidden = true;
        if (!items.length) {
            message.textContent = "You have not reported any items yet. Your lost and found reports will appear here.";
            message.className = "state-message empty-state";
            message.hidden = false;
            return;
        }

        for (const item of items) {
            const card = document.createElement("article");
            card.className = "item-card";

            const heading = document.createElement("h2");
            heading.textContent = item.itemName;
            const type = document.createElement("p");
            type.className = "report-type";
            type.textContent = item.type + " report";
            const details = document.createElement("p");
            details.textContent = item.category + " · " + item.location + " · " + item.reportDate;

            const statusBadge = document.createElement("span");
            statusBadge.className = "item-status returned-badge";
            statusBadge.textContent = "Returned";
            statusBadge.hidden = item.status !== "RETURNED";
            statusBadge.tabIndex = -1;
            statusBadge.setAttribute("role", "status");

            const actions = document.createElement("div");
            actions.className = "report-actions";

            const editButton = document.createElement("button");
            editButton.type = "button";
            editButton.className = "report-action-button action-edit";
            editButton.textContent = "Edit";
            editButton.addEventListener("click", () => createEditor(item, card));
            actions.append(editButton);

            const deleteButton = document.createElement("button");
            deleteButton.type = "button";
            deleteButton.className = "report-action-button action-delete";
            deleteButton.textContent = "Delete";
            deleteButton.addEventListener("click", () => removeReport(item, deleteButton));
            actions.append(deleteButton);

            card.append(heading, type, details, statusBadge);
            if (item.status === item.type) {
                const returnedButton = document.createElement("button");
                returnedButton.type = "button";
                returnedButton.className = "report-action-button action-return";
                returnedButton.textContent = "Mark as returned";
                returnedButton.addEventListener("click", () => markReturned(item, returnedButton, statusBadge));
                actions.append(returnedButton);
            }
            card.append(actions);
            container.append(card);
        }
    } catch (error) {
        message.textContent = "Unable to load your reports. Please try again later.";
        message.className = "state-message error-state";
        message.hidden = false;
        console.error(error);
    }
})();

function createEditor(item, card) {
    const existing = card.querySelector(".edit-form");
    if (existing) {
        existing.remove();
        return;
    }

    const form = document.createElement("form");
    form.className = "app-form edit-form";

    const message = document.createElement("p");
    message.className = "form-message error-message field-wide";
    message.setAttribute("role", "status");
    message.setAttribute("aria-live", "polite");
    form.append(message);

    const title = addInput(form, "Item name", "itemName", "text", item.itemName, true, 150);
    const categoryField = document.createElement("div");
    categoryField.className = "field";
    const categoryLabel = document.createElement("label");
    categoryLabel.textContent = "Category";
    const category = document.createElement("select");
    category.name = "category";
    category.required = true;
    categoryLabel.htmlFor = "edit-category-" + item.id;
    category.id = categoryLabel.htmlFor;
    const placeholder = document.createElement("option");
    placeholder.value = "";
    placeholder.textContent = "Select a category";
    category.append(placeholder);
    for (const value of reportCategories) {
        const option = document.createElement("option");
        option.value = value;
        option.textContent = value;
        category.append(option);
    }
    category.value = item.category;
    categoryField.append(categoryLabel, category);
    form.append(categoryField);
    if (!reportCategories.includes(item.category)) {
        const oldCategoryNotice = document.createElement("p");
        oldCategoryNotice.className = "field-wide";
        oldCategoryNotice.textContent = "This report has an older category. Select an allowed category before saving.";
        form.append(oldCategoryNotice);
    }

    const descriptionField = document.createElement("div");
    descriptionField.className = "field field-wide";
    const descriptionLabel = document.createElement("label");
    descriptionLabel.textContent = "Description (optional)";
    descriptionLabel.htmlFor = "edit-description-" + item.id;
    const description = document.createElement("textarea");
    description.id = descriptionLabel.htmlFor;
    description.name = "description";
    description.rows = 4;
    description.maxLength = 2000;
    description.value = item.description || "";
    descriptionField.append(descriptionLabel, description);
    form.append(descriptionField);

    const locationName = item.type === "LOST" ? "lostLocation" : "foundLocation";
    const location = addInput(form, "Location", locationName, "text", item.location, true, 255);
    const dateName = item.type === "LOST" ? "lostDate" : "foundDate";
    const date = addInput(form, "Report date", dateName, "date", item.reportDate, true);
    date.max = localToday();
    const contact = addInput(
        form,
        "New contact information (optional)",
        "contactInfo",
        "text",
        "",
        false,
        150
    );
    contact.placeholder = "Leave blank to keep the saved contact";

    const contactHint = document.createElement("p");
    contactHint.className = "field-wide";
    contactHint.textContent = "Visible to logged-in users so they can contact you about this item. Enter an email address or Indian mobile number to replace it.";
    form.append(contactHint);

    const actions = document.createElement("div");
    actions.className = "edit-actions field-wide";
    const cancel = document.createElement("button");
    cancel.type = "button";
    cancel.className = "edit-button edit-cancel";
    cancel.textContent = "Cancel";
    cancel.addEventListener("click", () => form.remove());
    const save = document.createElement("button");
    save.type = "submit";
    save.className = "edit-button edit-save";
    save.textContent = "Save changes";
    actions.append(cancel, save);
    form.append(actions);
    card.append(form);

    form.addEventListener("submit", async event => {
        event.preventDefault();
        if (form.dataset.csrfPending === "true") return;
        message.textContent = "";
        const required = [title, category, location, date];
        const empty = required.find(field => !field.value.trim());
        if (empty) {
            showEditError(message, empty.labels[0].textContent + " is required.", empty);
            return;
        }
        if (date.value > date.max || !isRealDate(date.value)) {
            showEditError(message, "Enter a real report date on or before today.", date);
            return;
        }
        if (contact.value !== "" && !validContact(contact.value)) {
            showEditError(message, "Enter a valid email address or Indian 10-digit mobile number.", contact);
            return;
        }
        const tooLong = [...form.querySelectorAll("[maxlength]")]
            .find(field => field.value.length > field.maxLength);
        if (tooLong) {
            showEditError(message, tooLong.labels[0].textContent + " is too long.", tooLong);
            return;
        }

        save.disabled = true;
        form.dataset.csrfPending = "true";
        try {
            const body = new URLSearchParams({
                type: item.type,
                itemName: title.value,
                category: category.value,
                description: description.value,
                [locationName]: location.value,
                [dateName]: date.value,
                csrfToken: await window.CampusCsrf.getToken()
            });
            if (contact.value !== "") body.set("contactInfo", contact.value);

            const response = await fetch(
                window.CampusCsrf.url(
                    "api/reports/" + encodeURIComponent(item.id)
                        + "?type=" + encodeURIComponent(item.type)
                ),
                {
                    method: "POST",
                    credentials: "same-origin",
                    headers: { "Content-Type": "application/x-www-form-urlencoded", Accept: "application/json" },
                    body
                }
            );
            if (window.CampusCsrf.isLoginRedirect(response)) {
                window.CampusCsrf.redirectToLogin();
                return;
            }
            if (response.ok) {
                window.location.reload();
                return;
            }
            if (response.status === 401) {
                window.CampusCsrf.redirectToLogin();
                return;
            }
            if (response.status === 403) {
                showEditError(message, "Your security check expired. Reload the page and try again.");
                return;
            }
            if (response.status === 400) {
                const error = await response.json();
                const field = error.field && form.elements[error.field];
                showEditError(message, error.message || "Please correct the report details.", field);
                return;
            }
            showEditError(message, "The server could not update this report. Please try again.");
        } catch (error) {
            showEditError(message, "Unable to update this report. Check your connection and try again.");
            console.error(error);
        } finally {
            delete form.dataset.csrfPending;
            save.disabled = false;
        }
    });
}

function addInput(form, labelText, name, type, value, required, maxLength) {
    const field = document.createElement("div");
    field.className = "field";
    const label = document.createElement("label");
    const input = document.createElement("input");
    input.type = type;
    input.name = name;
    input.value = value || "";
    input.required = required;
    if (maxLength) input.maxLength = maxLength;
    label.textContent = labelText;
    label.htmlFor = "edit-" + name + "-" + Math.random().toString(36).slice(2);
    input.id = label.htmlFor;
    field.append(label, input);
    form.append(field);
    return input;
}

function localToday() {
    const now = new Date();
    return [now.getFullYear(), String(now.getMonth() + 1).padStart(2, "0"), String(now.getDate()).padStart(2, "0")].join("-");
}

function isRealDate(value) {
    return /^\d{4}-\d{2}-\d{2}$/.test(value)
        && !Number.isNaN(Date.parse(value))
        && new Date(value).toISOString().slice(0, 10) === value;
}

function validContact(value) {
    const contact = value.trim();
    const compactPhone = contact.replace(/[ -]/g, "");
    const parts = contact.split("@");
    const validEmail = parts.length === 2
        && /^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+$/.test(parts[0])
        && parts[0].length <= 64
        && !parts[0].startsWith(".")
        && !parts[0].endsWith(".")
        && !parts[0].includes("..")
        && /^[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$/.test(parts[1]);
    return validEmail || /^(?:\+91)?[6-9]\d{9}$/.test(compactPhone);
}

function showEditError(message, text, field) {
    message.textContent = text;
    if (field && typeof field.focus === "function") field.focus();
}

async function removeReport(item, button) {
    if (!window.confirm("Delete this report? This cannot be undone.")) return;
    button.disabled = true;
    try {
        const token = await window.CampusCsrf.getToken();
        const response = await fetch(
            window.CampusCsrf.url(
                "api/reports/" + encodeURIComponent(item.id)
                    + "?type=" + encodeURIComponent(item.type)
                    + "&csrfToken=" + encodeURIComponent(token)
            ),
            { method: "DELETE", credentials: "same-origin" }
        );
        if (window.CampusCsrf.isLoginRedirect(response)) {
            window.CampusCsrf.redirectToLogin();
            return;
        }
        if (response.ok) {
            window.location.reload();
        } else if (response.status === 401) {
            window.CampusCsrf.redirectToLogin();
        } else if (response.status === 403) {
            window.alert("Your security check expired. Reload the page and try again.");
        } else {
            window.alert("Could not delete this report. Please try again.");
        }
    } catch (error) {
        window.alert("Unable to delete this report. Check your connection and try again.");
        console.error(error);
    } finally {
        button.disabled = false;
    }
}

async function markReturned(item, button, statusBadge) {
    button.disabled = true;
    let updated = false;
    try {
        const response = await fetch(
            window.CampusCsrf.url(
                "api/reports/" + encodeURIComponent(item.id) + "/return?type="
                    + encodeURIComponent(item.type)
            ),
            {
                method: "POST",
                credentials: "same-origin",
                headers: { "Content-Type": "application/x-www-form-urlencoded" },
                body: new URLSearchParams({
                    csrfToken: await window.CampusCsrf.getToken()
                })
            }
        );
        if (window.CampusCsrf.isLoginRedirect(response)) {
            window.CampusCsrf.redirectToLogin();
            return;
        }
        if (response.ok) {
            item.status = "RETURNED";
            statusBadge.hidden = false;
            button.hidden = true;
            statusBadge.focus();
            updated = true;
        } else if (response.status === 401) {
            window.CampusCsrf.redirectToLogin();
        } else {
            window.alert(response.status === 403
                ? "Your security check expired. Reload the page and try again."
                : "Could not mark this report as returned. Please try again.");
        }
    } catch (error) {
        window.alert("Unable to update this report. Check your connection and try again.");
        console.error(error);
    } finally {
        if (!updated) {
            button.disabled = false;
        }
    }
}
